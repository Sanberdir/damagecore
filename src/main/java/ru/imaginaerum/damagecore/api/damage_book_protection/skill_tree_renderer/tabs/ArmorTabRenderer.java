package ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer.tabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.armor.DamageArmorModifier;
import ru.imaginaerum.damagecore.armor.DamageResistance;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;
import ru.imaginaerum.damagecore.api.damage_book_protection.protection_helpers.*;

import java.util.*;

public final class ArmorTabRenderer {

    private ArmorTabRenderer() {}

    public static void render(GuiGraphics gui,
                              int areaX,
                              int areaY,
                              Minecraft mc,
                              int mouseX,
                              int mouseY) {

        final int leftPadding = 6;
        final int topPadding  = 4;
        final int lineHeight  = mc.font.lineHeight + 2;

        int textX = areaX + leftPadding;
        int y     = areaY + topPadding;

        Player player = mc.player;
        if (player == null) return;

        Map<DamageType, Float> armorFlat = new EnumMap<>(DamageType.class);
        Map<DamageType, Float> armorPercent = new EnumMap<>(DamageType.class);

        boolean hasAnyArmor = false;

        for (ItemStack stack : player.getArmorSlots()) {
            if (!(stack.getItem() instanceof ArmorItem armorItem)) continue;
            hasAnyArmor = true;

            var resistances =
                    DamageArmorModifier.getDamageResistances(
                            armorItem.getMaterial(),
                            armorItem.getType()
                    );

            for (var e : resistances.entrySet()) {
                DamageType type = e.getKey();
                DamageResistance res = e.getValue();

                armorFlat.merge(type, res.getFlat(), Float::sum);
                armorPercent.merge(type, res.getPercent(), Float::sum);
            }
        }

        Map<DamageType, Float> enchantPercent = new EnumMap<>(DamageType.class);
        for (DamageType type : DamageType.values()) {
            float p = getTotalEnchantProtection(player, type);
            if (p > 0) enchantPercent.put(type, p);
        }

        Map<DamageType, DamageResistance> totals = new EnumMap<>(DamageType.class);
        for (DamageType type : DamageType.values()) {
            float flat = armorFlat.getOrDefault(type, 0f);
            float percent = Math.min(1f,
                    armorPercent.getOrDefault(type, 0f)
                            + enchantPercent.getOrDefault(type, 0f));

            if (flat > 0 || percent > 0) {
                totals.put(type, new DamageResistance(flat, percent));
            }
        }

        DamageType hovered = null;

        if (hasAnyArmor && !totals.isEmpty()) {
            gui.drawString(mc.font,
                    Component.translatable("damagecore.armor_tab.header"),
                    textX, y, 0xFFFFFF, true);

            y += lineHeight + 2;

            for (var entry : totals.entrySet()) {
                DamageType type = entry.getKey();
                String value = entry.getValue().toString();

                Component line = Component.translatable(getKey(type))
                        .append(Component.literal(": " + value));

                gui.drawString(mc.font, line, textX, y, 0xFFFFFF, true);

                int w = mc.font.width(line);

                if (mouseX >= textX && mouseX <= textX + w
                        && mouseY >= y && mouseY < y + lineHeight) {
                    hovered = type;
                }

                y += lineHeight;
            }
        }

        if (hovered != null) {
            float af = armorFlat.getOrDefault(hovered, 0f);
            float ap = armorPercent.getOrDefault(hovered, 0f);
            float ep = enchantPercent.getOrDefault(hovered, 0f);

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable(getKey(hovered)));

            if (af > 0 || ap > 0) {
                tooltip.add(Component.translatable(
                        "damagecore.tooltip.armor",
                        format(af, ap)
                ));
            }

            if (ep > 0) {
                tooltip.add(Component.literal("§e" + (ep * 100) + "%"));
            }

            gui.renderTooltip(mc.font, tooltip, Optional.empty(), mouseX, mouseY);
        }

        // =========================
        // ✔ ВОЗВРАТ РАБОЧЕГО ИММУНИТЕТА
        // =========================

        int lf = PlayerStatsCapability.get(player)
                .map(s -> s.getStat(StatsType.LIVE_FORCE))
                .orElse(0);

        int en = PlayerStatsCapability.get(player)
                .map(s -> s.getStat(StatsType.ENDURANCE))
                .orElse(0);

        boolean hasImmunity = lf > 0 || en > 0;

        if (hasImmunity) {
            y += 6;

            gui.drawString(mc.font,
                    Component.translatable("damagecore.immunity_tab.header"),
                    textX, y, 0xFFFFFF, true);

            y += lineHeight + 2;

            if (lf > 0) {
                for (DamageType type : new DamageType[]{
                        DamageType.BLEEDING,
                        DamageType.FIRE,
                        DamageType.COLD,
                        DamageType.POISON
                }) {
                    Component line = Component.translatable(getKey(type))
                            .append(Component.literal(": " + lf));

                    gui.drawString(mc.font, line, textX, y, 0xEEEEEE, true);
                    y += lineHeight;
                }
            }

            if (en > 0) {
                Component line = Component.translatable(getKey(DamageType.SUFFOCATION))
                        .append(Component.literal(": " + en));

                gui.drawString(mc.font, line, textX, y, 0xEEEEEE, true);
                y += lineHeight;
            }
        }
    }

    private static String format(float flat, float percent) {
        if (flat > 0 && percent > 0) return flat + " + " + (percent * 100) + "%";
        if (flat > 0) return String.valueOf(flat);
        if (percent > 0) return (percent * 100) + "%";
        return "0";
    }

    private static float getTotalEnchantProtection(Player player, DamageType type) {
        float sum = 0f;
        sum += ProtectionHelper.getEnchantProtectionPercent(player, type);

        switch (type) {
            case FIRE -> sum += FireProtectionHelper.getEnchantProtectionPercent(player, type);
            case PIERCING -> sum += ProjectileProtectionHelper.getEnchantProtectionPercent(player, type);
            case BLUDGEONING -> {
                sum += ExplosionProtectionHelper.getEnchantProtectionPercent(player, type);
                sum += FallProtectionHelper.getEnchantProtectionPercent(player, type);
            }
        }
        return sum;
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