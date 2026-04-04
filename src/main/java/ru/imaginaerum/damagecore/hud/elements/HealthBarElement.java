package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ru.imaginaerum.damagecore.hud.DamageCoreHudOverlay;

public class HealthBarElement {

    private static final int TEXTURE_BAR_WIDTH = 32; // ширина исходной полоски на текстуре
    private static final int EDGE_WIDTH = 6;        // ширина краёв на текстуре

    private static final int BAR_X = 47; // экранная позиция
    private static final int BAR_Y = 17;
    private static final int BAR_W = 52; // новая ширина полоски на экране
    private static final int BAR_H = 6;  // высота полоски

    private static final int TEXTURE_X = 0;      // X на текстуре
    private static final int TEXTURE_Y_EMPTY = 66; // Y пустой полоски

    // Параметры для заполненной полоски
    private static final int FILLED_TEX_X = 2;    // X на текстуре (2)
    private static final int FILLED_TEX_Y = 49;   // Y на текстуре (49)
    private static final int FILLED_TEX_W = 28;   // ширина на текстуре (30 - 2 = 28)
    private static final int FILLED_TEX_H = 4;    // высота на текстуре (53 - 49 = 4)

    // Размер исходной текстуры HUD
    private static final int HUD_TEXTURE_WIDTH = 160;
    private static final int HUD_TEXTURE_HEIGHT = 208;

    public static void render(GuiGraphics gui, float healthPercent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float maxHealth = mc.player.getMaxHealth();
        float baseHealth = 20f; // базовое максимальное здоровье

        // Коэффициент растяжения полоски относительно базы
        float widthScale = Math.min(maxHealth / baseHealth, 2f); // ограничим x2 чтобы не вылезло за экран
        int scaledBarW = Math.round(BAR_W * widthScale);

        renderEmptyBar(gui, BAR_X, BAR_Y, scaledBarW, BAR_H, TEXTURE_X, TEXTURE_Y_EMPTY);

        int filledBarW = scaledBarW - 4;
        int filledW = Math.round(filledBarW * healthPercent);

        if (filledW > 0) {
            renderFilledBar(gui,
                    BAR_X + 2,
                    BAR_Y + 1,
                    filledW,
                    BAR_H - 2,
                    FILLED_TEX_X,
                    FILLED_TEX_Y,
                    filledBarW); // передаём актуальный максимум
        }
    }

    static void renderEmptyBar(GuiGraphics gui, int barX, int barY, int barW, int barH, int texX, int texY) {
        int drawX = barX;

        // Левая граница (без растяжки, 1:1)
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY,
                EDGE_WIDTH, barH,
                texX, texY,
                EDGE_WIDTH, barH,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        drawX += EDGE_WIDTH;

        // Средняя часть
        int midWidth = barW - EDGE_WIDTH * 2;
        int midTexX = texX + EDGE_WIDTH;
        int midTexW = TEXTURE_BAR_WIDTH - EDGE_WIDTH * 2;
        if (midWidth > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    midWidth, barH,
                    midTexX, texY,
                    midTexW, barH,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
            drawX += midWidth;
        }

        // Правая граница
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY,
                EDGE_WIDTH, barH,
                texX + TEXTURE_BAR_WIDTH - EDGE_WIDTH, texY,
                EDGE_WIDTH, barH,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
    }

    static void renderFilledBar(GuiGraphics gui, int barX, int barY, int barW, int barH, int texX, int texY, int maxFilledW) {
        if (barW <= 0) return;

        int drawX = barX;
        int filledEdgeWidth = Math.min(EDGE_WIDTH, FILLED_TEX_W / 2);
        int midTexW = FILLED_TEX_W - filledEdgeWidth * 2;

        int leftW = Math.min(filledEdgeWidth, barW);

        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY,
                leftW, barH,
                texX, texY,
                leftW, FILLED_TEX_H,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        drawX += leftW;
        barW  -= leftW;

        if (barW <= 0) return;

        int rightW = 0;
        if (leftW >= filledEdgeWidth) {
            int midMaxScreenW = maxFilledW - filledEdgeWidth * 2; // адаптивно под текущую ширину полоски
            rightW = Math.max(0, barW - midMaxScreenW);
            rightW = Math.min(rightW, filledEdgeWidth);
            rightW = Math.min(rightW, barW);
        }

        int midScreenW = barW - rightW;
        if (midScreenW > 0 && midTexW > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    midScreenW, barH,
                    texX + filledEdgeWidth, texY,
                    midTexW, FILLED_TEX_H,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
            drawX += midScreenW;
            barW  -= midScreenW;
        }

        if (rightW > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    rightW, barH,
                    texX + FILLED_TEX_W - filledEdgeWidth, texY,
                    rightW, FILLED_TEX_H,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        }
    }
}