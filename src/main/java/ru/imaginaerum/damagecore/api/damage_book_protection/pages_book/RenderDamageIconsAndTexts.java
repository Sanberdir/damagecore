package ru.imaginaerum.damagecore.api.damage_book_protection.pages_book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import ru.imaginaerum.damagecore.api.damage_book_protection.*;
import ru.imaginaerum.damagecore.api.damage_book_protection.protection_helpers.*;
import ru.imaginaerum.damagecore.armor.DamageResistance;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RenderDamageIconsAndTexts {
    private static final int ICON_SIZE = 16;
    private static final int TEMPORARY_COLOR = 0xFFFF00; // Жёлтый цвет для временных эффектов
    private static final int PERMANENT_COLOR = 0xFFFFFF; // Белый цвет для постоянной защиты
    public static void renderDamageIconsAndTexts(
            GuiGraphics gui,
            int tabX,
            int tabY,
            DamageBookStateCollector.ProtectionData protectionData,
            int mouseX,
            int mouseY
    ) {
        if (protectionData == null) return;

        Map<DamageType, DamageResistance> armorTotals = protectionData.armorResistances;
        Map<DamageType, Float> foodTotals = protectionData.foodProtection;

        final int START_X = tabX + 14;
        final int START_Y = tabY + 15;
        final int TEXT_AREA = 22;
        final int GAP_AFTER_TEXT = 2;
        final int ICONS_PER_ROW = 3;
        final int ROW_SPACING = ICON_SIZE + 4;

        int cursorX = START_X;
        int cursorY = START_Y;
        int index = 0;

        Font font = Minecraft.getInstance().font;
        Player player = Minecraft.getInstance().player;

        for (DamageType dt : DamageType.values()) {
            DamageResistance armorResistance = armorTotals.get(dt);
            float foodProtection = foodTotals.getOrDefault(dt, 0.0f);

            // ==== Временная защита от зелий / ванильного Resistance ====
            float vanillaPercent = 0f;
            float enchantPercent = getProtectionEnchantPercent(player, dt);
            if (player != null) {
                for (MobEffectInstance inst : player.getActiveEffects()) {
                    if (inst.getEffect() == MobEffects.DAMAGE_RESISTANCE) {
                        int level = inst.getAmplifier() + 1;
                        float percent = 0.10f * level;

                        // проверка по типу урона
                        if (dt == DamageType.PIERCING || dt == DamageType.SLASHING
                                || dt == DamageType.BLUDGEONING) {
                            vanillaPercent += percent;
                        }
                    }
                }
            }

            if ((armorResistance == null || (armorResistance.getFlat() <= 0 && armorResistance.getPercent() <= 0))
                    && foodProtection <= 0
                    && vanillaPercent <= 0
                    && enchantPercent <= 0) {
                continue;
            }

            // ==== Иконка урона ====
            ResourceLocation icon = new ResourceLocation(
                    "damagecore",
                    "textures/gui/damage_types/" + dt.getDamageName() + "_damage.png"
            );
            gui.blit(icon, cursorX, cursorY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            // ==== Суммарная защита ====
            int totalFlat = armorResistance != null ? Math.round(armorResistance.getFlat()) : 0;
            float totalPercent = (armorResistance != null ? armorResistance.getPercent() : 0f)
                    + foodProtection + vanillaPercent + enchantPercent;

            if (totalPercent > 1.0f) totalPercent = 1.0f;
            int totalPercentInt = Math.round(totalPercent * 100);

            // ==== Цвет: если есть временная защита — жёлтый ====
            boolean hasTemporary = foodProtection > 0 || vanillaPercent > 0 || enchantPercent > 0;
            int displayColor = hasTemporary ? TEMPORARY_COLOR : PERMANENT_COLOR;

            // ==== Формирование текста рядом с иконкой ====
            StringBuilder textBuilder = new StringBuilder();
            if (totalFlat > 0) textBuilder.append(totalFlat);
            if (totalPercentInt > 0) {
                if (textBuilder.length() > 0) textBuilder.append(" ");
                textBuilder.append(totalPercentInt).append("%");
            }

            String text = textBuilder.toString();
            if (!text.isEmpty()) {
                gui.pose().pushPose();
                gui.pose().scale(0.7f, 0.7f, 1.0f);
                gui.drawString(
                        font,
                        text,
                        (int) ((cursorX + ICON_SIZE + 2) / 0.7f),
                        (int) ((cursorY + 6) / 0.7f),
                        displayColor,
                        false
                );
                gui.pose().popPose();
            }

            // ==== Tooltip при наведении ====
            int hoverX2 = cursorX + ICON_SIZE + TEXT_AREA;
            int hoverY2 = cursorY + ICON_SIZE;

            if (mouseX >= cursorX && mouseX < hoverX2 &&
                    mouseY >= cursorY && mouseY < hoverY2) {

                List<FormattedCharSequence> tooltipLines = new ArrayList<>();

                tooltipLines.add(
                        Component.translatable("damagecore.damage_type." + dt.getDamageName())
                                .append(Component.translatable("damagecore.damage_type.damage"))
                                .getVisualOrderText()
                );

                int armorPercentFromArmor = armorResistance != null ?
                        Math.round(armorResistance.getPercent() * 100) : 0;
                int foodPercentInt = Math.round(foodProtection * 100);
                int vanillaPercentInt = Math.round(vanillaPercent * 100);
                int enchantPercentInt = Math.round(enchantPercent * 100);

                int totalPercentFromTooltip =
                        armorPercentFromArmor
                                + foodPercentInt
                                + vanillaPercentInt
                                + enchantPercentInt;

                if (totalPercentFromTooltip > 100) totalPercentFromTooltip = 100;

// ---- детали по источникам ----

                if (armorResistance != null && armorResistance.getPercent() > 0) {
                    tooltipLines.add(Component.translatable(
                            "damagecore.tooltip.armor_detail",
                            armorPercentFromArmor + "%"
                    ).getVisualOrderText());
                }

                if (foodProtection > 0) {
                    tooltipLines.add(Component.translatable(
                            "damagecore.tooltip.food_effect_detail",
                            foodPercentInt + "%"
                    ).getVisualOrderText());
                }

                if (vanillaPercentInt > 0) {
                    tooltipLines.add(Component.translatable(
                            "damagecore.tooltip.potion_effect_detail",
                            vanillaPercentInt + "%"
                    ).getVisualOrderText());
                }

                if (enchantPercentInt > 0) {
                    tooltipLines.add(Component.translatable(
                            "damagecore.tooltip.enchant_protection_detail",
                            enchantPercentInt + "%"
                    ).getVisualOrderText());
                }

// ---- пустая строка ----
                tooltipLines.add(Component.literal("").getVisualOrderText());

// ---- ИТОГО ВНИЗУ ----
                tooltipLines.add(Component.translatable(
                        "damagecore.tooltip.total_protection",
                        totalPercentFromTooltip + "%"
                ).getVisualOrderText());


                gui.renderTooltip(font, tooltipLines, mouseX, mouseY);
            }

            // ==== Позиционирование следующей иконки ====
            index++;
            if (index % ICONS_PER_ROW == 0) {
                cursorX = START_X;
                cursorY += ROW_SPACING;
            } else {
                cursorX += ICON_SIZE + 2 + TEXT_AREA + GAP_AFTER_TEXT;
            }
        }
    }
    private static float getProtectionEnchantPercent(Player player, DamageType dt) {
        float normal = ProtectionHelper
                .getEnchantProtectionPercent(player, dt);

        float fire = FireProtectionHelper
                .getEnchantProtectionPercent(player, dt);
        float fall = FallProtectionHelper
                .getEnchantProtectionPercent(player, dt);
        float projectile = ProjectileProtectionHelper
                .getEnchantProtectionPercent(player, dt);
        float explosion = ExplosionProtectionHelper
                .getEnchantProtectionPercent(player, dt);

        return normal + fire + fall + projectile + explosion;
    }
}
