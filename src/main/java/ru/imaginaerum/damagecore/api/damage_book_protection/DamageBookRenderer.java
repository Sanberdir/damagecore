package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.armor.DamageResistance;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.Map;

public final class DamageBookRenderer {
    public static final int TAB_WIDTH = 150;
    private static final int ICON_SIZE = 16;

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

    public static void renderDamageIconsAndTexts(
            GuiGraphics gui,
            int tabX,
            int tabY,
            Map<DamageType, DamageResistance> totals,
            int mouseX,
            int mouseY
    ) {
        if (totals == null || totals.isEmpty()) return;

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
            DamageResistance dr = totals.get(dt);
            if (dr == null) continue;

            ResourceLocation icon = new ResourceLocation(
                    "damagecore",
                    "textures/gui/damage_types/" + dt.getDamageName() + "_damage.png"
            );

            gui.blit(icon, cursorX, cursorY, 0, 0,
                    ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            int flat = Math.round(dr.getFlat());
            int percent = Math.round(dr.getPercent() * 100);

            String text = "";
            if (flat > 0) text += flat;
            if (percent > 0) {
                if (!text.isEmpty()) text += " ";
                text += percent + "%";
            }

            if (!text.isEmpty()) {
                gui.pose().pushPose();
                gui.pose().scale(0.7f, 0.7f, 1.0f);
                gui.drawString(
                        font,
                        text,
                        (int) ((cursorX + ICON_SIZE + 2) / 0.7f),
                        (int) ((cursorY + 6) / 0.7f),
                        0xFFFFFF,
                        false
                );
                gui.pose().popPose();
            }

            // ---------- TOOLTIP ----------
            int hoverX2 = cursorX + ICON_SIZE + TEXT_AREA;
            int hoverY2 = cursorY + ICON_SIZE;

            if (mouseX >= cursorX && mouseX < hoverX2 &&
                    mouseY >= cursorY && mouseY < hoverY2) {

                java.util.List<net.minecraft.util.FormattedCharSequence> tooltip =
                        java.util.List.of(
                                // Верхняя строка: "Пробивание урон", "Огненный урон" и т.д.
                                Component.translatable("damagecore.damage_type." + dt.getDamageName())
                                        .append(Component.translatable("damagecore.damage_type.damage"))
                                        .getVisualOrderText(),

                                // Броня
                                Component.translatable("damagecore.tooltip.armor", percent + "%")
                                        .getVisualOrderText(),

                                // Зелья (заглушка 0)
                                Component.translatable("damagecore.tooltip.potion", 0 + "%")
                                        .getVisualOrderText(),

                                // Пища (заглушка 0)
                                Component.translatable("damagecore.tooltip.food", 0 + "%")
                                        .getVisualOrderText()
                        );

                gui.renderTooltip(font, tooltip, mouseX, mouseY);
            }
            // ----------------------------

            index++;
            if (index % ICONS_PER_ROW == 0) {
                cursorX = START_X;
                cursorY += ROW_SPACING;
            } else {
                cursorX += ICON_SIZE + 2 + TEXT_AREA + GAP_AFTER_TEXT;
            }
        }
    }


}
