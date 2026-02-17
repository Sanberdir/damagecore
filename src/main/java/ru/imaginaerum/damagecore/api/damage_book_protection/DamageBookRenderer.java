package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

public final class DamageBookRenderer {

    public static final int TAB_WIDTH = 150;

    // ---- СХЕМА ВКЛАДОК ----
    public static final int MIDDLE_TABS = 8;
    public static final int SIDE_TABS = 2;
    public static final int TABS_PER_ROW = MIDDLE_TABS + SIDE_TABS;

    public static int bottomLeft()  { return 0; }
    public static int bottomRight() { return TABS_PER_ROW - 1; }
    public static int bottomMiddle(int i) { return 1 + i; }

    public static int topLeft() { return TABS_PER_ROW; }
    public static int topRight() { return TABS_PER_ROW * 2 - 1; }
    public static int topMiddle(int i) { return TABS_PER_ROW + 1 + i; }

    public static int selectedBottomTab = 0;

    private static final ResourceLocation DAMAGE_BOOK_TAB =
            new ResourceLocation("damagecore", "textures/gui/container/creative_inventory/damage_book.png");

    private static final ResourceLocation DAMAGE_CORE_INTERFACE =
            new ResourceLocation("damagecore", "textures/gui/container/creative_inventory/damage_core_interface.png");

    private DamageBookRenderer() {}

    // ---------- вычисление позиций ----------

    public static int calcMiddleGap(int panelLeft, int panelWidth, int tabW) {
        int startX = panelLeft + tabW;
        int endX = panelLeft + panelWidth - tabW;
        return (endX - startX - tabW * MIDDLE_TABS) / (MIDDLE_TABS - 1);
    }

    public static int calcMiddleX(int i, int panelLeft, int panelWidth, int tabW) {
        int startX = panelLeft + tabW;
        int gap = calcMiddleGap(panelLeft, panelWidth, tabW);
        return startX + i * (tabW + gap) + 1;
    }

    // ---------- левая книга ----------

    public static void renderMainTab(GuiGraphics gui, InventoryScreen screen, int tabX, int tabY) {
        gui.blit(DAMAGE_BOOK_TAB, tabX, tabY - 1, 0, 0, TAB_WIDTH,
                ((ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor) screen).damagecore$getImageHeight());
    }

    public static void renderSmallTabs(GuiGraphics gui, InventoryScreen screen, int tabX, int tabY, int selectedSmall) {
        int[] smallYOffsets = {3, 30};

        for (int i = 0; i < 2; i++) {
            boolean active = (i == selectedSmall);
            int renderX = tabX - 29 - (active ? 2 : 0);
            int renderY = tabY + smallYOffsets[i];
            int texX = active ? 188 : 153;
            int width = active ? 35 : 30;

            gui.blit(DAMAGE_BOOK_TAB, renderX, renderY, texX, 2, width, 26);

            if (i == 0) {
                ItemStack helm = new ItemStack(Items.NETHERITE_HELMET);
                gui.renderItem(helm, renderX + (width - 16)/2, renderY + 5);
            } else {
                ItemStack bread = new ItemStack(Items.BREAD);
                ItemStack potion = new ItemStack(Items.POTION);
                PotionUtils.setPotion(potion, Potions.STRENGTH);

                gui.renderItem(bread, renderX + 5, renderY + 5);
                gui.renderItem(potion, renderX + 13, renderY + 5);
            }
        }
    }

    // ---------- правая панель ----------

    public static void renderRightInterface(
            GuiGraphics gui,
            InventoryScreen screen,
            int x, int y,
            int mouseX,
            int mouseY
    ) {
        int PANEL_W = 289;
        int PANEL_H = 166;
        int TAB_W = 28;

        int panelLeft = x + 2;
        int panelTop = y;

        gui.blit(DAMAGE_CORE_INTERFACE, panelLeft, panelTop, 179, 0, PANEL_W, PANEL_H, 512, 512);

        int TAB_Y = panelTop + 163;
        int TOP_Y = panelTop - 25;

        // ===== НИЖНИЙ РЯД =====

        drawSideTab(gui, panelLeft, TAB_Y, TAB_W, bottomLeft(), true, 0, 0);
        drawMiddleRow(gui, panelLeft, PANEL_W, TAB_Y, TAB_W, true);
        drawSideTab(gui, panelLeft + PANEL_W - TAB_W, TAB_Y, TAB_W, bottomRight(), true, 56, 1);

        // ===== ВЕРХНИЙ РЯД =====

        drawSideTab(gui, panelLeft, TOP_Y, TAB_W, topLeft(), false, 89, 1);
        drawMiddleRow(gui, panelLeft, PANEL_W, TOP_Y, TAB_W, false);
        drawSideTab(gui, panelLeft + PANEL_W - TAB_W, TOP_Y, TAB_W, topRight(), false, 145, 1);

        SkillTreeRenderer.render(gui, screen, panelLeft, panelTop, mouseX, mouseY);
    }

    private static void drawMiddleRow(GuiGraphics gui, int panelLeft, int panelW, int y, int tabW, boolean bottom) {
        for (int i = 0; i < MIDDLE_TABS; i++) {
            int id = bottom ? bottomMiddle(i) : topMiddle(i);
            if (!SkillTreeRenderer.hasTreeForTab(id)) continue;

            int x = calcMiddleX(i, panelLeft, panelW, tabW);
            boolean active = selectedBottomTab == id;

            int v = active ? (bottom ? 204 : 203) : 175;
            int h = active ? 32 : 25;
            int u = bottom ? 28 : 117;

            int yOffset;

            if (active) {
                // активные уже были подправлены ранее
                yOffset = bottom ? -1 : -3;
            } else {
                // idle:
                // нижние как были
                // верхние — на 1px выше
                yOffset = bottom ? 2 : 1;
            }

            gui.blit(DAMAGE_CORE_INTERFACE, x, y + yOffset, u, v, tabW, h, 512, 512);
        }
    }



    private static void drawSideTab(GuiGraphics gui, int x, int y, int tabW, int id, boolean bottom, int u, int idleYOffset) {
        if (!SkillTreeRenderer.hasTreeForTab(id)) return;

        boolean active = selectedBottomTab == id;

        int v = active
                ? (bottom ? 204 : 203)
                : (bottom ? 173 + idleYOffset : 175);

        int h = active ? 32 : 27;

        int yOffset;
        if (active) {
            yOffset = bottom ? -1 : -3;   // ↓ нижние ниже, верхние выше
        } else {
            yOffset = idleYOffset;
        }

        gui.blit(DAMAGE_CORE_INTERFACE, x, y + yOffset, u, v, tabW, h, 512, 512);
    }


    public static void setBottomTab(int tab) {
        selectedBottomTab = tab;
    }
}
