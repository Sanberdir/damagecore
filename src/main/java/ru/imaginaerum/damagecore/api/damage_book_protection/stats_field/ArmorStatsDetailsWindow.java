package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionEffect;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArmorStatsDetailsWindow {

    private ArmorStatsDetailsWindow() {}

    public static void render(GuiGraphics gui, InventoryScreen screen,
                              int topFieldY, int fieldWidth, int fieldHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        DamageType selectedType = ArmorStatsHoverHandler.getSelectedDamageType();
        if (selectedType == null) return;

        float totalPercent  = ArmorStatsCalculator.getPlayerTotalProtectionPercent(mc.player)
                .getOrDefault(selectedType, 0f) * 100;
        float armorPercent  = ArmorStatsCalculator.getArmorOnlyProtectionPercent(mc.player)
                .getOrDefault(selectedType, 0f) * 100;
        float foodPercent   = 0f;

        FoodProtectionManager foodManager = FoodProtectionCapability.get(mc.player);
        if (foodManager != null) {
            foodPercent = foodManager.getTotalProtectionPercent(selectedType) * 100;
        }

        float enchantPercent = Math.max(0, totalPercent - armorPercent - foodPercent);

        List<Component> lines = new ArrayList<>();
        int nonZeroCount = 0;

        if (armorPercent   > 0.01f) { lines.add(line("armor_stat.armor",   armorPercent));   nonZeroCount++; }
        if (enchantPercent > 0.01f) { lines.add(line("armor_stat.enchant", enchantPercent)); nonZeroCount++; }
        if (foodPercent    > 0.01f) { lines.add(line("armor_stat.food",    foodPercent));    nonZeroCount++; }

        if (nonZeroCount >= 2) {
            lines.add(line("armor_stat.total", totalPercent));
        }

        int lineHeight   = mc.font.lineHeight + 2;
        int titleHeight  = mc.font.lineHeight + 4;
        int paddingTop   = 6;
        int paddingBottom = 6;
        int windowHeight = paddingTop + titleHeight + (lines.size() * lineHeight) + paddingBottom;

        int windowX = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int windowY = topFieldY + fieldHeight;

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 300);

        gui.fill(windowX, windowY, windowX + fieldWidth, windowY + windowHeight, 0xCC000000);
        ArmorStatsBarRenderer.drawWindowWithTile(gui, ArmorStatsFieldRenderer.TEXTURE,
                windowX, windowY, fieldWidth, windowHeight);

        int textX = windowX + ArmorStatsBarRenderer.EDGE + 4;
        int textY = windowY + paddingTop;

        Component title = Component.translatable("damage_type." + selectedType.getDamageName());
        gui.drawString(mc.font, title,
                windowX + (fieldWidth - mc.font.width(title)) / 2, textY, 0xFFFFFF, false);
        textY += titleHeight;

        for (int i = 0; i < lines.size(); i++) {
            int color = (nonZeroCount >= 2 && i == lines.size() - 1) ? 0xFFD700 : 0xEEEEEE;
            gui.drawString(mc.font, lines.get(i).getString(), textX, textY, color, false);
            textY += lineHeight;
        }
        gui.pose().popPose();
    }
    public static void renderPotionDetails(GuiGraphics gui, InventoryScreen screen,
                                           int bottomFieldY, int fieldWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ResourceLocation potionKey = ArmorStatsHoverHandler.getHoveredPotion();
        if (potionKey == null) return;

        ItemStack potionStack = PotionTracker.getActivePotions().get(potionKey);
        if (potionStack == null) return;

        // Название зелья — берём из hover-имени предмета
        net.minecraft.world.item.alchemy.Potion potion = PotionUtils.getPotion(potionStack);
        String potionName = Component.translatable(potion.getName("item.minecraft.potion.effect.")).getString();

        // Эффекты зелья с учётом только тех, что ещё активны у игрока
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance pe : PotionUtils.getMobEffects(potionStack)) {
            MobEffectInstance active = mc.player.getEffect(pe.getEffect());
            if (active != null) effects.add(active); // берём активный (актуальное время)
        }

        if (effects.isEmpty()) return;

        int iconSize      = 9;
        int lineHeight    = mc.font.lineHeight + 2;
        int titleHeight   = mc.font.lineHeight + 4;
        int paddingTop    = 6;
        int paddingBottom = 6;
        int windowHeight  = paddingTop + titleHeight + effects.size() * lineHeight + paddingBottom;

        int windowX = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int windowY = bottomFieldY - windowHeight;

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 300);

        gui.fill(windowX, windowY, windowX + fieldWidth, windowY + windowHeight, 0xCC000000);
        ArmorStatsBarRenderer.drawWindowWithTile(gui, ArmorStatsFieldRenderer.TEXTURE,
                windowX, windowY, fieldWidth, windowHeight);

        int textX = windowX + ArmorStatsBarRenderer.EDGE + 4;
        int textY = windowY + paddingTop;

        // Заголовок — название зелья по центру
        gui.drawString(mc.font, potionName,
                windowX + (fieldWidth - mc.font.width(potionName)) / 2,
                textY, 0xFFFFFF, false);
        textY += titleHeight;

        // Каждый эффект: иконка + "Название [уровень]  время"
        for (MobEffectInstance inst : effects) {
            // Иконка эффекта
            TextureAtlasSprite sprite = mc.getMobEffectTextures().get(inst.getEffect());
            if (sprite != null) {
                gui.blit(textX, textY - 1, 0, iconSize, iconSize, sprite);
            }

            // Название + усилитель
            String amplifier = inst.getAmplifier() > 0
                    ? " " + toRoman(inst.getAmplifier() + 1)
                    : "";
            String effectName = inst.getEffect().getDisplayName().getString() + amplifier;

            // Оставшееся время
            int totalTicks = inst.getDuration();
            String timeStr;
            if (totalTicks == Integer.MAX_VALUE) {
                timeStr = "\u221E";
            } else {
                int sec = totalTicks / 20;
                int min = sec / 60;
                sec = sec % 60;
                timeStr = min > 0
                        ? min + ":" + String.format("%02d", sec)
                        : sec + "s";
            }

            gui.drawString(mc.font, effectName + "  " + timeStr,
                    textX + iconSize + 3, textY, 0xEEEEEE, false);
            textY += lineHeight;
        }

        gui.pose().popPose();
    }

    // Уже есть в файле, но если нет — добавить:
    private static String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";   case 2 -> "II";  case 3 -> "III";
            case 4 -> "IV";  case 5 -> "V";   case 6 -> "VI";
            case 7 -> "VII"; case 8 -> "VIII";case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }
    public static void renderFoodDetails(GuiGraphics gui, InventoryScreen screen,
                                         int bottomFieldY, int fieldWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ResourceLocation foodKey = ArmorStatsHoverHandler.getHoveredFood();
        if (foodKey == null) return;

        FoodProtectionManager foodManager = FoodProtectionCapability.get(mc.player);
        if (foodManager == null) return;

        Map<DamageType, Float> grouped = new LinkedHashMap<>();
        Map<DamageType, Integer> maxTicks = new LinkedHashMap<>();
        Item foodItem = null;

        for (FoodProtectionEffect effect : foodManager.getAllEffects()) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(effect.getItem());
            if (!foodKey.equals(key)) continue;
            if (effect.getProtectionPercent() <= 0) continue;

            if (foodItem == null) foodItem = effect.getItem();
            grouped.merge(effect.getDamageType(), effect.getProtectionPercent(), Float::sum);
            maxTicks.merge(effect.getDamageType(), effect.getRemainingTicks(), Math::max);
        }

        if (grouped.isEmpty()) return;

        grouped.entrySet().removeIf(e -> Math.min(e.getValue(), 1.0f) <= 0.001f);
        if (grouped.isEmpty()) return;

        Map<ResourceLocation, MobEffectInstance> mobEffectMap = new LinkedHashMap<>();
        if (foodItem != null) {
            var foodProps = new ItemStack(foodItem).getFoodProperties(mc.player);
            if (foodProps != null) {
                for (var pair : foodProps.getEffects()) {
                    var effect = pair.getFirst();
                    MobEffectInstance active = mc.player.getEffect(effect.getEffect());
                    if (active != null) {
                        ResourceLocation key = BuiltInRegistries.MOB_EFFECT.getKey(active.getEffect());
                        if (key != null) mobEffectMap.put(key, active);
                    }
                }
            }
        }

        int iconSize      = 9;
        int lineHeight    = mc.font.lineHeight + 2;
        int titleHeight   = mc.font.lineHeight + 4;
        int paddingTop    = 6;
        int paddingBottom = 6;
        int windowHeight  = paddingTop + titleHeight
                + grouped.size() * lineHeight
                + (!mobEffectMap.isEmpty() ? lineHeight : 0)
                + mobEffectMap.size() * lineHeight
                + paddingBottom;

        int windowX = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int windowY = bottomFieldY - windowHeight;

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 300);

        gui.fill(windowX, windowY, windowX + fieldWidth, windowY + windowHeight, 0xCC000000);
        ArmorStatsBarRenderer.drawWindowWithTile(gui, ArmorStatsFieldRenderer.TEXTURE,
                windowX, windowY, fieldWidth, windowHeight);

        int textX = windowX + ArmorStatsBarRenderer.EDGE + 4;
        int textY = windowY + paddingTop;

        String itemName = foodItem != null
                ? new ItemStack(foodItem).getHoverName().getString()
                : foodKey.getPath();
        gui.drawString(mc.font, itemName,
                windowX + (fieldWidth - mc.font.width(itemName)) / 2, textY, 0xFFFFFF, false);
        textY += titleHeight;

        // DamageType строки с иконками
        for (Map.Entry<DamageType, Float> entry : grouped.entrySet()) {
            float pct = Math.min(entry.getValue(), 1.0f);

            int ticks = maxTicks.getOrDefault(entry.getKey(), 0);
            int seconds = ticks / 20;
            int minutes = seconds / 60;
            seconds = seconds % 60;
            String timeStr = minutes > 0
                    ? minutes + ":" + String.format("%02d", seconds)
                    : seconds + "s";

            ResourceLocation icon = new ResourceLocation(
                    "damagecore", "textures/gui/damage_types/" + entry.getKey().getDamageName() + "_damage.png");
            gui.blit(icon, textX, textY, 0, 0, iconSize, iconSize, iconSize, iconSize);

            String lineStr = String.format("%.0f", pct * 100) + "%  " + timeStr;
            gui.drawString(mc.font, lineStr, textX + iconSize + 3, textY, 0xEEEEEE, false);
            textY += lineHeight;
        }

        // Пустая строка-разделитель
        if (!mobEffectMap.isEmpty()) {
            textY += lineHeight;
        }

        // Mob effects с иконками
        for (MobEffectInstance inst : mobEffectMap.values()) {
            String effectName = inst.getEffect().getDisplayName().getString();

            int totalTicks = inst.getDuration();
            String timeStr;
            if (totalTicks == Integer.MAX_VALUE) {
                timeStr = "\u221E";
            } else {
                int sec = totalTicks / 20;
                int min = sec / 60;
                sec = sec % 60;
                timeStr = min > 0 ? min + ":" + String.format("%02d", sec) : sec + "s";
            }

            TextureAtlasSprite sprite = mc.getMobEffectTextures().get(inst.getEffect());
            if (sprite != null) {
                gui.blit(textX, textY - 1, 0, iconSize, iconSize, sprite);
            }
            gui.drawString(mc.font, effectName + "  " + timeStr, textX + iconSize + 3, textY, 0xEEEEEE, false);
            textY += lineHeight;
        }

        gui.pose().popPose();
    }

    public static void renderEntityDetails(GuiGraphics gui, InventoryScreen screen,
                                           int bottomFieldY, int fieldWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ResourceLocation entityKey = ArmorStatsHoverHandler.getHoveredEntity();
        if (entityKey == null) return;

        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            LivingEntity source = EffectSourceManager.getSource(mc.player, inst.getEffect());
            if (source != null) {
                ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(source.getType());
                if (entityKey.equals(key)) effects.add(inst);
            }
        }

        int iconSize     = 9;
        int lineHeight   = mc.font.lineHeight + 2;
        int titleHeight  = mc.font.lineHeight + 4;
        int paddingTop   = 6;
        int paddingBottom = 6;
        int windowHeight = paddingTop + titleHeight + effects.size() * lineHeight + paddingBottom;

        int windowX = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int windowY = bottomFieldY - windowHeight;

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 300);

        gui.fill(windowX, windowY, windowX + fieldWidth, windowY + windowHeight, 0xCC000000);
        ArmorStatsBarRenderer.drawWindowWithTile(gui, ArmorStatsFieldRenderer.TEXTURE,
                windowX, windowY, fieldWidth, windowHeight);

        int textX = windowX + ArmorStatsBarRenderer.EDGE + 4;
        int textY = windowY + paddingTop;

        // Заголовок — название сущности по центру
        String entityName = BuiltInRegistries.ENTITY_TYPE
                .get(entityKey).getDescription().getString();
        gui.drawString(mc.font, entityName,
                windowX + (fieldWidth - mc.font.width(entityName)) / 2, textY, 0xFFFFFF, false);
        textY += titleHeight;

        // Эффекты с иконками моб-эффектов и иконками типов урона
        for (MobEffectInstance inst : effects) {
            String effectName = inst.getEffect().getDisplayName().getString();

            int totalTicks = inst.getDuration();
            String timeStr;
            if (totalTicks == Integer.MAX_VALUE) {
                timeStr = "\u221E";
            } else {
                int seconds = totalTicks / 20;
                int minutes = seconds / 60;
                seconds = seconds % 60;
                timeStr = minutes > 0 ? minutes + ":" + String.format("%02d", seconds) : seconds + "s";
            }

            // Иконка моб-эффекта
            TextureAtlasSprite sprite = mc.getMobEffectTextures().get(inst.getEffect());
            if (sprite != null) {
                gui.blit(textX, textY - 1, 0, iconSize, iconSize, sprite);
            }
            gui.drawString(mc.font, effectName + "  " + timeStr, textX + iconSize + 3, textY, 0xEEEEEE, false);
            textY += lineHeight;
        }

        gui.pose().popPose();
    }
    private static Component line(String key, float value) {
        return Component.literal(
                Component.translatable(key).getString() + ": " + String.format("%.1f", value) + "%");
    }
}