package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class DamageBookRenderer {
    public static final int TAB_WIDTH = 150;
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
    public static void renderRightInterface(GuiGraphics gui, InventoryScreen screen, int x, int y) {
        // 1) Основная правая панель (как было)
        int u = 179;       // X координата в текстуре DAMAGE_CORE_INTERFACE, откуда брать область панели
        int v = 0;         // Y координата в текстуре DAMAGE_CORE_INTERFACE, откуда брать область панели
        int width = 289;   // Ширина области текстуры, которая будет отрисована (пиксели)
        int height = 166;  // Высота области текстуры, которая будет отрисована (пиксели)

        gui.blit(
                DAMAGE_CORE_INTERFACE, // текстура
                x + 2,                 // экранная X позиция, где рисуем левый верхний угол панели
                y,                     // экранная Y позиция, где рисуем левый верхний угол панели
                u,                     // X на текстуре (см. выше)
                v,                     // Y на текстуре (см. выше)
                width,                 // ширина блока для отрисовки
                height,                // высота блока для отрисовки
                512, 512               // полный размер текстуры (для нормализации UV координат)
        );

        // 2) Наложение маленькой под-текстуры (src: X0..28, Y173..200) внутрь панели
        int srcExtraU = 0;          // X координата верхнего левого угла под-текстуры внутри файла PNG
        int srcExtraV = 173;        // Y координата верхнего левого угла под-текстуры внутри файла PNG
        int extraWidth = 28;        // Ширина под-текстуры (28 пикселей)
        int extraHeight = 27;       // Высота под-текстуры (200 - 173 = 27 пикселей)

        int destExtraX = x + 2;     // X экранная позиция для верхнего левого угла под-текстуры
        int destExtraY = y + 163;   // Y экранная позиция для верхнего левого угла под-текстуры

        gui.blit(
                DAMAGE_CORE_INTERFACE, // текстура
                destExtraX,            // экранная X позиция
                destExtraY,            // экранная Y позиция
                srcExtraU,             // X координата на текстуре
                srcExtraV,             // Y координата на текстуре
                extraWidth,            // ширина блока для отрисовки
                extraHeight,           // высота блока для отрисовки
                512, 512               // полный размер текстуры
        );
        // panelScreenX = x + 2; panelScreenY = y;
        int panelScreenX = x + 2;
        int panelScreenY = y;

// (1) — загрузка (в реальном коде лучше вызывать однократно; здесь для наглядности)
        SkillTreeRenderer.load("skill_tree/skill_tree.json");

// (2) — отрисовка (передаём mouse координаты из render вызова)
        SkillTreeRenderer.render(gui, screen, panelScreenX, panelScreenY, (int) Minecraft.getInstance().mouseHandler.xpos(), (int) Minecraft.getInstance().mouseHandler.ypos());

    }


}