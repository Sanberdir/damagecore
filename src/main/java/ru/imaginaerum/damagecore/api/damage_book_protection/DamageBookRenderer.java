package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.resources.ResourceLocation;
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

        for (DamageType dt : DamageType.values()) {
            DamageResistance armorResistance = armorTotals.get(dt);
            float foodProtection = foodTotals.getOrDefault(dt, 0.0f);

            if ((armorResistance == null || (armorResistance.getFlat() <= 0 && armorResistance.getPercent() <= 0))
                    && foodProtection <= 0) {
                continue;
            }

            ResourceLocation icon = new ResourceLocation(
                    "damagecore",
                    "textures/gui/damage_types/" + dt.getDamageName() + "_damage.png"
            );

            gui.blit(icon, cursorX, cursorY, 0, 0,
                    ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            int totalFlat = 0;
            float totalPercent = 0.0f;

            if (armorResistance != null) {
                totalFlat = Math.round(armorResistance.getFlat());
                totalPercent += armorResistance.getPercent();
            }
            totalPercent += foodProtection;

            if (totalPercent > 1.0f) {
                totalPercent = 1.0f;
            }

            int totalPercentInt = Math.round(totalPercent * 100);

            StringBuilder textBuilder = new StringBuilder();
            int displayColor = PERMANENT_COLOR;

            if (totalFlat > 0) {
                textBuilder.append(totalFlat);
            }

            if (totalPercentInt > 0) {
                if (textBuilder.length() > 0) {
                    textBuilder.append(" ");
                }
                textBuilder.append(totalPercentInt).append("%");

                if (foodProtection > 0) {
                    displayColor = TEMPORARY_COLOR;
                }
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

            // ТУЛТИП ТОЛЬКО В ПЕРВОЙ ВКЛАДКЕ
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
                int totalPercentFromTooltip = armorPercentFromArmor + foodPercentInt;

                if (totalPercentFromTooltip > 100) {
                    totalPercentFromTooltip = 100;
                }

                Component totalLine = Component.translatable("damagecore.tooltip.total_protection",
                        totalPercentFromTooltip + "%");
                tooltipLines.add(totalLine.getVisualOrderText());

                tooltipLines.add(Component.literal("").getVisualOrderText());

                if (armorResistance != null && armorResistance.getPercent() > 0) {
                    int armorPercent = Math.round(armorResistance.getPercent() * 100);
                    Component armorLine = Component.translatable("damagecore.tooltip.armor_detail",
                            armorPercent + "%");
                    tooltipLines.add(armorLine.getVisualOrderText());
                }

                if (foodProtection > 0) {
                    int foodPercent = Math.round(foodProtection * 100);
                    Component foodLine = Component.translatable("damagecore.tooltip.food_effect_detail",
                            foodPercent + "%");
                    tooltipLines.add(foodLine.getVisualOrderText());

                    if (Minecraft.getInstance().player != null) {
                        var foodManager = FoodProtectionCapability.get(
                                Minecraft.getInstance().player);
                        if (foodManager != null) {
                            var effect = foodManager.getEffect(dt);
                            if (effect != null) {
                                int remainingSeconds = effect.getRemainingTicks() / 20;
                                Component timeLine = Component.translatable("damagecore.tooltip.time_remaining",
                                        remainingSeconds);
                                tooltipLines.add(timeLine.getVisualOrderText());
                            }
                        }
                    }
                }

                gui.renderTooltip(font, tooltipLines, mouseX, mouseY);
            }

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

        List<ItemStack> itemsToDisplay = new ArrayList<>();

        // === ТОЛЬКО ВРЕМЕННЫЕ ЭФФЕКТЫ (еда / зелья) ===
        var foodManager = FoodProtectionCapability.get(player);
        if (foodManager != null) {
            var effects = foodManager.getActiveEffects();

            Map<Item, List<FoodProtectionEffect>> grouped = new HashMap<>();

            for (List<FoodProtectionEffect> list : effects.values()) {
                for (FoodProtectionEffect effect : list) {
                    Item item = effect.getItem();

                    grouped
                            .computeIfAbsent(item, k -> new ArrayList<>())
                            .add(effect);
                }
            }

            for (Item item : grouped.keySet()) {
                itemsToDisplay.add(new ItemStack(item));
            }
        }


        final int START_X = tabX + 10;
        final int START_Y = tabY + 20;
        final int CELL_SPACING = 2;
        final int MAX_PER_ROW = 7;

        int x = START_X;
        int y = START_Y;
        int rowCount = 0;

        for (int i = 0; i < itemsToDisplay.size(); i++) {
            ItemStack stack = itemsToDisplay.get(i);

            if (rowCount >= MAX_PER_ROW) {
                rowCount = 0;
                x = START_X;
                y += 18 + CELL_SPACING;
            }

            // Ванильный слот
            gui.blit(
                    INVENTORY_LOCATION,
                    x,
                    y,
                    7,
                    83,
                    18,
                    18
            );

            gui.renderItem(stack, x + 1, y + 1);
            gui.renderItemDecorations(
                    Minecraft.getInstance().font,
                    stack,
                    x + 1,
                    y + 1
            );

            x += 18 + CELL_SPACING;
            rowCount++;

            // максимум 3 ряда
            if (i >= MAX_PER_ROW * 3 - 1) {
                if (itemsToDisplay.size() > MAX_PER_ROW * 3) {
                    gui.pose().pushPose();
                    gui.pose().scale(0.7f, 0.7f, 1f);
                    gui.drawString(
                            Minecraft.getInstance().font,
                            "...",
                            (int) ((x + 2) / 0.7f),
                            (int) ((y + 7) / 0.7f),
                            0xAAAAAA,
                            false
                    );
                    gui.pose().popPose();
                }
                break;
            }
        }

        // Нет активных эффектов
        if (itemsToDisplay.isEmpty()) {
            String text = Component.translatable("damagecore.effects.none").getString();
            gui.pose().pushPose();
            gui.pose().scale(0.8f, 0.8f, 1f);
            gui.drawString(
                    Minecraft.getInstance().font,
                    text,
                    (int) ((tabX + TAB_WIDTH / 2f - Minecraft.getInstance().font.width(text) * 0.4f) / 0.8f),
                    (int) ((START_Y + 40) / 0.8f),
                    0xAAAAAA,
                    false
            );
            gui.pose().popPose();
        }
    }


}