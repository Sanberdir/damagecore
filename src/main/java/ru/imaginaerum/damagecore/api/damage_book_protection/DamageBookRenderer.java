package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantments;
import ru.imaginaerum.damagecore.armor.DamageResistance;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionEffect;

import java.util.*;

public final class DamageBookRenderer {
    public static final int TAB_WIDTH = 150;
    private static final int ICON_SIZE = 16;
    private static final int ITEM_SIZE = 20;
    private static final int TEMPORARY_COLOR = 0xFFFF00; // Жёлтый цвет для временных эффектов
    private static final int PERMANENT_COLOR = 0xFFFFFF; // Белый цвет для постоянной защиты
    private static final int CELL_SIZE = 18; // Размер ячейки с границей
    private static final int CELL_BORDER_COLOR = 0xFFFFFF; // Белый цвет границы
    // Защита от чар
    private static float getProtectionEnchantPercent(Player player, DamageType dt) {
        if (player == null) return 0f;

        if (dt != DamageType.PIERCING
                && dt != DamageType.SLASHING
                && dt != DamageType.BLUDGEONING) {
            return 0f;
        }

        int totalLevel = 0;

        for (ItemStack stack : player.getArmorSlots()) {
            totalLevel += stack.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION);
        }

        return totalLevel * 0.05f; // 5% за уровень
    }
    private static final ResourceLocation DAMAGE_BOOK_TAB =
            new ResourceLocation("damagecore", "textures/gui/container/creative_inventory/damage_book.png");

    private DamageBookRenderer() {}


    public static void renderMainTab(GuiGraphics gui, InventoryScreen screen, int tabX, int tabY) {
        gui.blit(DAMAGE_BOOK_TAB, tabX, tabY - 1, 0, 0, TAB_WIDTH,
                ((ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor) screen).damagecore$getImageHeight());
    }

    public static void renderSmallTabs(GuiGraphics gui, InventoryScreen screen, int tabX, int tabY, int selectedSmall) {
        int[] smallYOffsets = {3, 3 + 26 + 1};

        for (int i = 0; i < 2; i++) {
            boolean isActive = (i == selectedSmall);
            int renderX = tabX - 29;
            if (isActive) renderX -= 2;
            int renderY = tabY + smallYOffsets[i];
            int texX = isActive ? 188 : 153;
            int width = isActive ? 35 : 30;

            gui.blit(DAMAGE_BOOK_TAB, renderX, renderY, texX, 2, width, 26);

            if (i == 0) {
                ItemStack netherite_helmet = new ItemStack(Items.NETHERITE_HELMET);
                int itemX = renderX + (width - 16) / 2;
                int itemY = renderY + (26 - 16) / 2;

                gui.renderItem(netherite_helmet, itemX, itemY);
                gui.renderItemDecorations(Minecraft.getInstance().font, netherite_helmet, itemX, itemY);
            } else {
                ItemStack bread = new ItemStack(Items.BREAD);
                ItemStack potion = new ItemStack(Items.POTION);
                PotionUtils.setPotion(potion, Potions.STRENGTH);

                int overlap = -4;
                int itemWidth = 16;
                int totalWidth = itemWidth * 2 + overlap;
                int startX = renderX + (width - totalWidth) / 2 + 3;
                int itemY = renderY + (26 - 16) / 2;

                int breadX = startX;
                int potionX = startX + itemWidth + overlap;

                gui.renderItem(bread, breadX, itemY);
                gui.renderItemDecorations(Minecraft.getInstance().font, bread, breadX, itemY);

                gui.renderItem(potion, potionX, itemY);
                gui.renderItemDecorations(Minecraft.getInstance().font, potion, potionX, itemY);
            }
        }
    }

    // Рендер первой вкладки с данными о защите - С ТУЛТИПАМИ
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
                    && foodProtection <= 0 && vanillaPercent <= 0) {
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

                List<net.minecraft.util.FormattedCharSequence> tooltipLines = new ArrayList<>();

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



    protected static final ResourceLocation INVENTORY_LOCATION =
            new ResourceLocation("textures/gui/container/inventory.png");

    // Рендер второй вкладки (активные эффекты) - ПРОСТЫЕ ЯЧЕЙКИ В РЯД
    public static void renderActiveEffects(
            GuiGraphics gui,
            int tabX,
            int tabY,
            DamageBookStateCollector.ProtectionData protectionData,
            int mouseX,
            int mouseY
    ) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        List<ItemStack> allItemsToDisplay = new ArrayList<>();

        // ==== Эффекты из FoodProtectionCapability ====
        var foodManager = FoodProtectionCapability.get(player);
        if (foodManager != null) {
            var effects = foodManager.getActiveEffects();
            Map<Item, List<FoodProtectionEffect>> grouped = new HashMap<>();

            for (List<FoodProtectionEffect> list : effects.values()) {
                for (FoodProtectionEffect effect : list) {
                    grouped.computeIfAbsent(effect.getItem(), k -> new ArrayList<>()).add(effect);
                }
            }

            for (Item item : grouped.keySet()) {
                allItemsToDisplay.add(new ItemStack(item));
            }
        }
        // ==== Броня с Protection ====
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION) > 0) {
                allItemsToDisplay.add(armor.copy());
            }
        }
        // ==== Ванильные эффекты как зелья ====
        for (MobEffectInstance inst : player.getActiveEffects()) {
            if (inst.getEffect() == MobEffects.DAMAGE_RESISTANCE) continue;

            ItemStack potionStack = findPotionForEffect(inst);
            if (potionStack != null) {
                allItemsToDisplay.add(potionStack);
            }
        }

        final int START_X = tabX + 10;
        final int START_Y = tabY + 20;
        final int CELL_SPACING = 2;
        final int MAX_PER_ROW = 7;
        final int MAX_ROWS = 3;

        Font font = Minecraft.getInstance().font;

        int curX = START_X;
        int curY = START_Y;
        int col = 0;
        int row = 0;

        for (ItemStack stack : allItemsToDisplay) {
            if (col >= MAX_PER_ROW) {
                col = 0;
                curX = START_X;
                curY += 18 + CELL_SPACING;
                row++;
                if (row >= MAX_ROWS) break;
            }

            gui.blit(INVENTORY_LOCATION, curX, curY, 7, 83, 18, 18);
            gui.renderItem(stack, curX + 1, curY + 1);
            gui.renderItemDecorations(font, stack, curX + 1, curY + 1);

            curX += 18 + CELL_SPACING;
            col++;
        }

        // ==== Если вообще нет эффектов ====
        boolean hasResistance = player.getActiveEffects()
                .stream().anyMatch(e -> e.getEffect() == MobEffects.DAMAGE_RESISTANCE);

        if (allItemsToDisplay.isEmpty() && !hasResistance) {
            String text = Component.translatable("damagecore.effects.none").getString();
            gui.pose().pushPose();
            gui.pose().scale(0.8f, 0.8f, 1f);
            gui.drawString(
                    font,
                    text,
                    (int) ((tabX + TAB_WIDTH / 2f - font.width(text) * 0.4f) / 0.8f),
                    (int) ((START_Y + 40) / 0.8f),
                    0xAAAAAA,
                    false
            );
            gui.pose().popPose();
        }
    }

    private static ItemStack findPotionForEffect(MobEffectInstance effect) {
        for (Potion potion : BuiltInRegistries.POTION) {
            for (MobEffectInstance inst : potion.getEffects()) {
                if (inst.getEffect() == effect.getEffect()) {
                    ItemStack stack = new ItemStack(Items.POTION);
                    PotionUtils.setPotion(stack, potion);
                    return stack;
                }
            }
        }
        return null;
    }
}