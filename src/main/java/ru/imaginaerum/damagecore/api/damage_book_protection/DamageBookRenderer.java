package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class DamageBookRenderer {
    public static final int TAB_WIDTH = 150;
    public static int selectedBottomTab = 0;
    // Защита от чар (левая вкладка)
    private static final ResourceLocation DAMAGE_BOOK_TAB =
            new ResourceLocation("damagecore", "textures/gui/container/creative_inventory/damage_book.png");

    // Правая интерфейсная текстура (в том же png указали область)
    private static final ResourceLocation DAMAGE_CORE_INTERFACE =
            new ResourceLocation("damagecore", "textures/gui/container/creative_inventory/damage_core_interface.png");

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

    /**
     * Рисует правую интерфейсную панель из текстуры damage_core_interface.
     * Пользователь предоставил область: X=179..468, Y=0..166 (ширина=289, высота=166).
     */
    public static void renderRightInterface(
            GuiGraphics gui,
            InventoryScreen screen,
            int x, int y,
            int mouseX,
            int mouseY
    ) {
        int PANEL_W = 289;
        int PANEL_H = 166;

        int panelLeft = x + 2;
        int panelTop  = y;

        // Основная панель
        gui.blit(DAMAGE_CORE_INTERFACE, panelLeft, panelTop, 179, 0, PANEL_W, PANEL_H, 512, 512);

        // Нижние вкладки
        int TAB_W = 28;
        int TAB_Y = panelTop + 163;

        // Левая вкладка (0) - показываем только если есть дерево
        if (SkillTreeRenderer.hasTreeForTab(0)) {
            int leftTabX = panelLeft;
            int leftTabY = TAB_Y + (selectedBottomTab == 0 ? -1 : 0);
            int leftU = 0;
            int leftV = (selectedBottomTab == 0 ? 204 : 173);
            int leftH = (selectedBottomTab == 0 ? 32 : 27);
            gui.blit(DAMAGE_CORE_INTERFACE, leftTabX, leftTabY, leftU, leftV, TAB_W, leftH, 512, 512);
        }

        // Правая вкладка (11) - показываем только если есть дерево
        if (SkillTreeRenderer.hasTreeForTab(11)) {
            int rightTabX = panelLeft + PANEL_W - TAB_W;
            int rightTabY = TAB_Y + (selectedBottomTab == 11 ? -1 : 1);
            int rightU = 56;
            int rightV = (selectedBottomTab == 11 ? 204 : 174);
            int rightH = (selectedBottomTab == 11 ? 32 : 27);
            gui.blit(DAMAGE_CORE_INTERFACE, rightTabX, rightTabY, rightU, rightV, TAB_W, rightH, 512, 512);
        }

        // Средние вкладки (1..8) - всегда 8 позиций, но показываем только если есть дерево
        int middleTabsCount = 8;
        int middleTabWidth = 28;
        int gapCount = middleTabsCount - 1;
        int startX = panelLeft + TAB_W;
        int endX = panelLeft + PANEL_W - TAB_W;
        int totalSpace = endX - startX;
        int gap = (totalSpace - middleTabWidth * middleTabsCount) / gapCount;

        int middleU = 28;
        int middleInactiveV = 175;
        int middleActiveV = 204;
        int middleH_inactive = 25;
        int middleH_active = 32;

        for (int i = 0; i < middleTabsCount; i++) {
            int tabId = i + 1; // ID от 1 до 8

            // Рисуем вкладку только если есть дерево для этого ID
            if (SkillTreeRenderer.hasTreeForTab(tabId)) {
                int drawX = startX + i * (middleTabWidth + gap) + 1;
                boolean isActive = (selectedBottomTab == tabId);
                int drawY = TAB_Y + (isActive ? -1 : 2);
                int srcV = isActive ? middleActiveV : middleInactiveV;
                int srcH = isActive ? middleH_active : middleH_inactive;

                gui.blit(DAMAGE_CORE_INTERFACE, drawX, drawY, middleU, srcV, middleTabWidth, srcH, 512, 512);
            }
        }

        // =====================
        // Верхние вкладки (зеркально нижним, индексы 12..)
        // =====================

        int TOP_TAB_BASE_Y = panelTop - 27 + 2;
        int topLeftIndex = 12;

        // Левая верхняя (12) - показываем только если есть дерево
        if (SkillTreeRenderer.hasTreeForTab(12)) {
            int topLeftX = panelLeft;
            int topLeftY = TOP_TAB_BASE_Y + (selectedBottomTab == 12 ? -3 : 1);
            int topLeftU = 89;
            int topLeftV = (selectedBottomTab == 12 ? 203 : 175);
            int topLeftH = (selectedBottomTab == 12 ? 32 : 26);
            gui.blit(DAMAGE_CORE_INTERFACE, topLeftX, topLeftY, topLeftU, topLeftV, TAB_W, topLeftH, 512, 512);
        }

        // Правая верхняя (23)
        int topRightIndex = 23;
        if (SkillTreeRenderer.hasTreeForTab(23)) {
            int topRightX = panelLeft + PANEL_W - TAB_W;
            int topRightY = TOP_TAB_BASE_Y + (selectedBottomTab == 23 ? -3 : 1);
            int topRightU = 145;
            int topRightV = (selectedBottomTab == 23 ? 203 : 175);
            int topRightH = (selectedBottomTab == 23 ? 32 : 27);
            gui.blit(DAMAGE_CORE_INTERFACE, topRightX, topRightY, topRightU, topRightV, TAB_W, topRightH, 512, 512);
        }

        // Средние верхние (13..20) - зеркально нижним
        int topMiddleTabsCount = 8;
        int topStartX = panelLeft + TAB_W;
        int topEndX = panelLeft + PANEL_W - TAB_W;
        int topTotalSpace = topEndX - topStartX;
        int topGap = (topTotalSpace - middleTabWidth * topMiddleTabsCount) / (topMiddleTabsCount - 1);

        int topMiddleU = 117;
        int topMiddleInactiveV = 175;
        int topMiddleActiveV = 203;
        int topMiddleH_inactive = 25;
        int topMiddleH_active = 32;

        for (int i = 0; i < topMiddleTabsCount; i++) {
            int idx = topLeftIndex + 1 + i; // ID от 13 до 20

            // Рисуем вкладку только если есть дерево для этого ID
            if (SkillTreeRenderer.hasTreeForTab(idx)) {
                int drawX = topStartX + i * (middleTabWidth + topGap) + 1;
                int drawY = TOP_TAB_BASE_Y + (selectedBottomTab == idx ? -3 : 1);
                int srcV = (selectedBottomTab == idx ? topMiddleActiveV : topMiddleInactiveV);
                int srcH = (selectedBottomTab == idx ? topMiddleH_active : topMiddleH_inactive);

                gui.blit(DAMAGE_CORE_INTERFACE, drawX, drawY, topMiddleU, srcV, middleTabWidth, srcH, 512, 512);
            }
        }

        // Рендер дерева навыков
        SkillTreeRenderer.render(gui, screen, panelLeft, panelTop, mouseX, mouseY);
    }

    public static void setBottomTab(int tab) {
        selectedBottomTab = tab;
    }
}