package ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer.DurationBarTooltip;
import ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer.PotionEffectEntry;
import ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer.PotionTrackingClient;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionEffect;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.*;

public final class PotionTabRenderer {

    private static final int ICON_SIZE     = 16;
    private static final int GAP           = 2;
    /** Вертикальный зазор между рядами (зелья / мобы) — больше горизонтального GAP,
     *  т.к. gui.renderItem рисует предметы с лёгкой 3D-перспективой, которая
     *  визуально "вылезает" за границы 16x16 рамки и при GAP=2 накладывается на следующий ряд. */
    private static final int ROW_GAP       = 6;

    /** Спрайт-лист, из которого вырезаются статичные иконки (моб-источник, еда-категория, зелья-категория). */
    private static final ResourceLocation STATUS_ICONS_SHEET =
            new ResourceLocation(DamageCore.MODID, "textures/gui/icons/status_effect_icons.png");

    /** Полный размер текстуры status_effect_icons.png (нужен для корректного маппинга UV).
     *  Если реальный размер файла другой — поправьте эти два значения. */
    private static final int ICONS_TEX_W = 256;
    private static final int ICONS_TEX_H = 256;

    /** Координаты вырезки иконки моба-источника: X51,Y4 — X61,Y14 (10x10 пикселей). */
    private static final int MOB_ICON_U      = 51;
    private static final int MOB_ICON_V      = 4;
    private static final int MOB_ICON_REGION = 10;

    /** Координаты вырезки общей иконки категории "Еда": X3,Y4 — X13,Y14 (10x10 пикселей). */
    private static final int FOOD_ICON_U      = 3;
    private static final int FOOD_ICON_V      = 4;
    private static final int FOOD_ICON_REGION = 10;

    /** Координаты вырезки общей иконки категории "Зелья": X83,Y4 — X93,Y14 (10x10 пикселей). */
    private static final int POTION_ICON_U      = 83;
    private static final int POTION_ICON_V      = 4;
    private static final int POTION_ICON_REGION = 10;

    private PotionTabRenderer() {}

    /**
     * Считаем источник "игроком" (т.е. показываем иконку зелья, а не иконку моба),
     * если sourceEntityType не задан вовсе (DRINK — игрок выпил сам) или
     * явно равен EntityType.PLAYER (игрок бросил splash/lingering зелье).
     * Любой другой EntityType — это моб, для него рисуем вырезку из status_effect_icons.png.
     */
    private static boolean isPlayerSource(EntityType<?> sourceEntityType) {
        return sourceEntityType == null || sourceEntityType == EntityType.PLAYER;
    }

    public static void render(GuiGraphics gui,
                              int areaX,
                              int areaY,
                              Minecraft mc,
                              int mouseX,
                              int mouseY) {

        Player player = mc.player;
        if (player == null) return;

        FoodProtectionManager manager = FoodProtectionCapability.get(player);

        List<FoodProtectionEffect> allActive = manager == null
                ? List.of()
                : manager.getAllEffects().stream()
                .filter(e -> !e.isExpired())
                .toList();

        Set<MobEffect> foodGranted = new HashSet<>();
        for (FoodProtectionEffect eff : allActive) {
            for (MobEffectInstance inst : eff.getMobEffects()) {
                foodGranted.add(inst.getEffect());
            }
        }

        List<MobEffectInstance> playerEffects = player.getActiveEffects().stream()
                .filter(e -> !foodGranted.contains(e.getEffect()))
                .toList();

        final int padL = 4;
        final int padT = 4;

        int x = areaX + padL;
        int y = areaY + padT;

        gui.drawString(mc.font,
                Component.translatable("damagecore.potion_tab.food_header"),
                x, y, 0xFFFFFF, true);

        y += mc.font.lineHeight + 4;

        // =========================
        // FOOD SECTION
        // =========================

        if (allActive.isEmpty()) {
            gui.drawString(mc.font,
                    Component.translatable("damagecore.potion_tab.no_effects"),
                    x, y, 0xAAAAAA, true);
        } else {

            Map<Item, List<FoodProtectionEffect>> byItem = new LinkedHashMap<>();

            for (FoodProtectionEffect eff : allActive) {
                byItem.computeIfAbsent(eff.getItem(), k -> new ArrayList<>()).add(eff);
            }

            Item hoveredItem = null;
            List<FoodProtectionEffect> hoveredEffects = null;

            int iconX = x;

            for (var entry : byItem.entrySet()) {
                Item item = entry.getKey();

                // Все предметы еды отображаются одним общим значком из спрайт-листа.
                renderFoodSourceIcon(gui, iconX, y);

                if (mouseX >= iconX && mouseX < iconX + ICON_SIZE
                        && mouseY >= y && mouseY < y + ICON_SIZE) {
                    hoveredItem = item;
                    hoveredEffects = entry.getValue();
                }

                iconX += ICON_SIZE + GAP;
            }

            if (hoveredItem != null && hoveredEffects != null) {
                renderFoodTooltip(gui, mc, hoveredItem, hoveredEffects, mouseX, mouseY);
            }
        }

        y += ICON_SIZE + 6;

        // =========================
        // SPLIT PLAYER EFFECTS INTO 3 BUCKETS
        // =========================

        // 1) зелья, выпитые/брошенные ИГРОКОМ (или DRINK без источника) -> общая иконка категории "Зелья"
        // 2) эффекты, наложенные НЕ игроком (моб бросил зелье ИЛИ ударил/выстрелил) -> вырезка status_effect_icons.png,
        //    сгруппированные по EntityType источника, чтобы один моб = одна иконка
        // 3) совсем неизвестные эффекты (нет записи в трекере) -> идут вместе с зельями игрока
        Map<PotionEffectEntry, List<MobEffectInstance>> byPotion = new LinkedHashMap<>();
        Map<EntityType<?>, List<MobEffectInstance>> byMobType = new LinkedHashMap<>();
        List<MobEffectInstance> unknown = new ArrayList<>();

        for (MobEffectInstance inst : playerEffects) {
            MobEffect effect = inst.getEffect();
            PotionEffectEntry entry = PotionTrackingClient.get(effect);

            if (entry == null) {
                unknown.add(inst);
                continue;
            }

            if (isPlayerSource(entry.getSourceEntityType())) {
                byPotion.computeIfAbsent(entry, k -> new ArrayList<>()).add(inst);
            } else {
                byMobType.computeIfAbsent(entry.getSourceEntityType(), k -> new ArrayList<>()).add(inst);
            }
        }

        ItemStack hoveredPotion = null;
        PotionEffectEntry hoveredEntry = null;
        List<MobEffectInstance> hoveredPotionEffects = null;
        MobEffectInstance hoveredUnknown = null;

        EntityType<?> hoveredMobType = null;
        List<MobEffectInstance> hoveredMobEffects = null;

        // =========================
        // POTIONS SECTION (свой заголовок, своя секция)
        // =========================

        boolean hasPotionRow = !byPotion.isEmpty() || !unknown.isEmpty();

        if (hasPotionRow) {

            gui.drawString(mc.font,
                    Component.translatable("damagecore.potion_tab.potions_header"),
                    x, y, 0xFFFFFF, true);

            y += mc.font.lineHeight + 4;

            int iconX = x;
            int potionRowY = y;

            for (var entry : byPotion.entrySet()) {
                PotionEffectEntry source = entry.getKey();
                ItemStack stack = source.getPotionStack();

                // Все зелья игрока отображаются одним общим значком из спрайт-листа,
                // вместо реальной иконки конкретного зелья (stack используется только для тултипа).
                renderPotionSourceIcon(gui, iconX, potionRowY);

                if (mouseX >= iconX && mouseX < iconX + ICON_SIZE
                        && mouseY >= potionRowY && mouseY < potionRowY + ICON_SIZE) {
                    hoveredPotion = stack;
                    hoveredEntry = source;
                    hoveredPotionEffects = entry.getValue();
                }

                iconX += ICON_SIZE + GAP;
            }

            for (MobEffectInstance inst : unknown) {
                var sprite = mc.getMobEffectTextures().get(inst.getEffect());
                if (sprite != null) {
                    gui.blit(iconX, potionRowY, 0, ICON_SIZE, ICON_SIZE, sprite);
                }

                if (mouseX >= iconX && mouseX < iconX + ICON_SIZE
                        && mouseY >= potionRowY && mouseY < potionRowY + ICON_SIZE) {
                    hoveredUnknown = inst;
                }

                iconX += ICON_SIZE + GAP;
            }

            y = potionRowY + ICON_SIZE + 6;
        }

        // =========================
        // MOBS SECTION (свой заголовок, своя секция — отдельно от зелий)
        // =========================

        if (!byMobType.isEmpty()) {

            gui.drawString(mc.font,
                    Component.translatable("damagecore.potion_tab.mobs_header"),
                    x, y, 0xFFFFFF, true);

            y += mc.font.lineHeight + 4;

            int mobX = x;
            int mobRowY = y;

            for (var entry : byMobType.entrySet()) {
                EntityType<?> sourceType = entry.getKey();

                renderMobSourceIcon(gui, mobX, mobRowY);

                if (mouseX >= mobX && mouseX < mobX + ICON_SIZE
                        && mouseY >= mobRowY && mouseY < mobRowY + ICON_SIZE) {
                    hoveredMobType = sourceType;
                    hoveredMobEffects = entry.getValue();
                }

                mobX += ICON_SIZE + GAP;
            }

            y = mobRowY + ICON_SIZE + 6;
        }

        // =========================
        // TOOLTIPS
        // =========================

        if (hoveredPotion != null && hoveredPotionEffects != null) {
            renderPotionTooltip(gui, mc,
                    hoveredPotion,
                    hoveredEntry,
                    hoveredPotionEffects,
                    mouseX, mouseY);
        }

        if (hoveredUnknown != null) {
            List<Component> lines = new ArrayList<>();

            lines.add(Component.translatable(
                            hoveredUnknown.getEffect().getDescriptionId())
                    .withStyle(s -> s.withColor(0xFFFF55)));

            lines.add(Component.literal(toRoman(hoveredUnknown.getAmplifier() + 1)));

            float progress = PotionTrackingClient.getRemainingFraction(
                    hoveredUnknown.getEffect(), hoveredUnknown.getDuration());

            // FIX: use hoveredUnknown here, not a non-existent `longest`
            Component effectName = Component.translatable(hoveredUnknown.getEffect().getDescriptionId());
            int amplifier = hoveredUnknown.getAmplifier() + 1;

            gui.renderTooltip(mc.font, lines,
                    Optional.of(new DurationBarTooltip(progress, effectName, amplifier)),
                    mouseX, mouseY);
        }

        if (hoveredMobType != null && hoveredMobEffects != null) {
            renderMobTooltip(gui, mc, hoveredMobType, hoveredMobEffects, mouseX, mouseY);
        }
    }

    // =========================
    // RENDER: STATIC SPRITE ICONS (вырезки из status_effect_icons.png, растянутые до ICON_SIZE)
    // =========================

    private static void renderMobSourceIcon(GuiGraphics gui, int x, int y) {
        renderStatusIcon(gui, x, y, MOB_ICON_U, MOB_ICON_V, MOB_ICON_REGION);
    }

    private static void renderFoodSourceIcon(GuiGraphics gui, int x, int y) {
        renderStatusIcon(gui, x, y, FOOD_ICON_U, FOOD_ICON_V, FOOD_ICON_REGION);
    }

    private static void renderPotionSourceIcon(GuiGraphics gui, int x, int y) {
        renderStatusIcon(gui, x, y, POTION_ICON_U, POTION_ICON_V, POTION_ICON_REGION);
    }

    private static void renderStatusIcon(GuiGraphics gui, int x, int y, int u, int v, int region) {
        gui.blit(STATUS_ICONS_SHEET,
                x, y, ICON_SIZE, ICON_SIZE,
                u, v,
                region, region,
                ICONS_TEX_W, ICONS_TEX_H);
    }

    // =========================
    // TOOLTIP: FOOD (без изменений — своя структура: % защиты + время по каждому DamageType)
    // =========================

    private static void renderFoodTooltip(GuiGraphics gui,
                                          Minecraft mc,
                                          Item item,
                                          List<FoodProtectionEffect> effects,
                                          int mouseX,
                                          int mouseY) {

        List<Component> lines = new ArrayList<>();

        lines.add(new ItemStack(item).getHoverName()
                .copy().withStyle(s -> s.withColor(0xFFFF55)));

        Map<DamageType, Float> percent = new EnumMap<>(DamageType.class);
        Map<DamageType, Integer> time = new EnumMap<>(DamageType.class);

        for (FoodProtectionEffect eff : effects) {
            percent.merge(eff.getDamageType(), eff.getProtectionPercent(), Float::sum);
            time.merge(eff.getDamageType(), eff.getRemainingTicks(), Math::max);
        }

        for (var e : percent.entrySet()) {
            DamageType type = e.getKey();

            lines.add(Component.translatable(getKey(type))
                    .append(Component.literal(
                            String.format(": §a%.0f%%§7 (%s)§r",
                                    Math.min(e.getValue(), 1f) * 100,
                                    formatTicks(time.getOrDefault(type, 0))
                            )
                    )));
        }

        Player player = mc.player;

        Map<MobEffect, MobEffectInstance> best = new LinkedHashMap<>();

        for (FoodProtectionEffect eff : effects) {
            for (MobEffectInstance saved : eff.getMobEffects()) {

                MobEffect type = saved.getEffect();
                if (best.containsKey(type)) continue;

                MobEffectInstance live =
                        player != null ? player.getEffect(type) : null;

                if (live != null) best.put(type, live);
            }
        }

        if (!best.isEmpty()) {
            lines.add(Component.literal("§8————————————"));

            for (MobEffectInstance inst : best.values()) {
                lines.add(Component.translatable(inst.getEffect().getDescriptionId())
                        .append(Component.literal(
                                " " + toRoman(inst.getAmplifier() + 1)
                                        + " §7(" + formatTicks(inst.getDuration()) + ")§r"
                        )));
            }
        }

        gui.renderTooltip(mc.font, lines, Optional.empty(), mouseX, mouseY);
    }

    // =========================
    // TOOLTIP: POTION (всегда есть стек — DRINK / SPLASH / LINGERING игрока)
    // =========================

    private static void renderPotionTooltip(GuiGraphics gui,
                                            Minecraft mc,
                                            ItemStack stack,
                                            PotionEffectEntry entry,
                                            List<MobEffectInstance> effects,
                                            int mouseX,
                                            int mouseY) {

        List<Component> lines = new ArrayList<>();

        lines.add(stack.getHoverName()
                .copy().withStyle(s -> s.withColor(0xFFFF55)));

        // Полоска показывает оставшееся время самого долгого из наложенных этим зельем эффектов
        // (раньше тут был текст "§7(Xs)§r" с тем же значением).
        MobEffectInstance longest = effects.stream()
                .max(Comparator.comparingInt(MobEffectInstance::getDuration))
                .orElse(null);

        float progress = longest != null
                ? PotionTrackingClient.getRemainingFraction(longest.getEffect(), longest.getDuration())
                : 1f;

        Component effectName = longest != null
                ? Component.translatable(longest.getEffect().getDescriptionId())
                : Component.empty();
        int amplifier = longest != null ? longest.getAmplifier() + 1 : 1;

        gui.renderTooltip(mc.font, lines,
                Optional.of(new DurationBarTooltip(progress, effectName, amplifier)),
                mouseX, mouseY);
    }

    // =========================
    // TOOLTIP: MOB SOURCE (бросил зелье или ударил/выстрелил) —
    // показывает имя моба и ВСЕ эффекты, которые он наложил (может быть несколько)
    // =========================

    private static void renderMobTooltip(GuiGraphics gui,
                                         Minecraft mc,
                                         EntityType<?> sourceType,
                                         List<MobEffectInstance> effects,
                                         int mouseX,
                                         int mouseY) {

        List<Component> lines = new ArrayList<>();

        if (sourceType != null) {
            lines.add(Component.translatable(sourceType.getDescriptionId())
                    .copy().withStyle(s -> s.withColor(0xFFFF55)));
        }

        for (MobEffectInstance inst : effects) {
            lines.add(Component.translatable(inst.getEffect().getDescriptionId())
                    .append(Component.literal(" " + toRoman(inst.getAmplifier() + 1))));
        }

        // Один моб может наложить сразу несколько эффектов с разной длительностью —
        // gui.renderTooltip поддерживает только один доп.-компонент, поэтому полоска
        // показывает остаток самого долгого из них (как и раньше "(Xs)" показывал max-длительность общим текстом).
        MobEffectInstance longest = effects.stream()
                .max(Comparator.comparingInt(MobEffectInstance::getDuration))
                .orElse(null);

        float progress = longest != null
                ? PotionTrackingClient.getRemainingFraction(longest.getEffect(), longest.getDuration())
                : 1f;

        Component effectName = longest != null
                ? Component.translatable(longest.getEffect().getDescriptionId())
                : Component.empty();
        int amplifier = longest != null ? longest.getAmplifier() + 1 : 1;
        gui.renderTooltip(mc.font, lines,
                Optional.of(new DurationBarTooltip(progress, effectName, amplifier)),
                mouseX, mouseY);
    }

    // =========================
    // UTILS
    // =========================

    private static String formatTicks(int ticks) {
        int sec = ticks / 20;
        if (sec >= 60) return (sec / 60) + ":" + String.format("%02d", sec % 60);
        return sec + "s";
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    private static String getKey(DamageType type) {
        return switch (type) {
            case PIERCING -> "damagecore.damage_type.piercing";
            case SLASHING -> "damagecore.damage_type.slashing";
            case FIRE -> "damagecore.damage_type.fire";
            case COLD -> "damagecore.damage_type.cold";
            case SUFFOCATION -> "damagecore.damage_type.suffocation";
            case BLEEDING -> "damagecore.damage_type.bleeding";
            case LUMINOUS_RADIANT -> "damagecore.damage_type.luminous_radiant";
            case NECROTIC -> "damagecore.damage_type.necrotic";
            case LIGHTNING -> "damagecore.damage_type.lightning";
            case POISON -> "damagecore.damage_type.poison";
            case SOUNDER -> "damagecore.damage_type.sounder";
            case PSY -> "damagecore.damage_type.psy";
            case BLUDGEONING -> "damagecore.damage_type.bludgeoning";
        };
    }
}