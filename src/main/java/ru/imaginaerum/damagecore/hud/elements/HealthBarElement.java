package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.gui.GuiGraphics;
import ru.imaginaerum.damagecore.hud.DamageCoreHudOverlay;

public class HealthBarElement {

    private static final int TEXTURE_BAR_WIDTH = 32; // ширина исходной полоски на текстуре
    private static final int EDGE_WIDTH = 6;        // ширина краёв на текстуре

    private static final int BAR_X = 45; // экранная позиция
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
        renderEmptyBar(gui, BAR_X, BAR_Y, BAR_W, BAR_H, TEXTURE_X, TEXTURE_Y_EMPTY);

        int filledBarW = BAR_W - 4;
        int filledW = Math.round(filledBarW * healthPercent); // сколько пикселей заполнено

        if (filledW > 0) {
            renderFilledBar(gui,
                    BAR_X + 2,
                    BAR_Y + 1,
                    filledW,
                    BAR_H - 2,
                    FILLED_TEX_X,
                    FILLED_TEX_Y);
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

    static void renderFilledBar(GuiGraphics gui, int barX, int barY, int barW, int barH, int texX, int texY) {
        int drawX = barX;
        int filledEdgeWidth = Math.min(EDGE_WIDTH, FILLED_TEX_W / 2);

        // Левый край — рисуем только если хватает места
        int leftW = Math.min(filledEdgeWidth, barW);
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY,
                leftW, barH,
                texX, texY,
                leftW, FILLED_TEX_H,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        drawX += leftW;
        barW -= leftW;

        // Средняя часть
        int midTexW = FILLED_TEX_W - filledEdgeWidth * 2;
        int midScreenW = Math.max(0, barW - filledEdgeWidth); // оставляем место под правый край
        if (midScreenW > 0 && midTexW > 0) {
            // Обрезаем текстуру пропорционально
            int clampedMidScreenW = Math.min(midScreenW, barW);
            int clampedMidTexW = Math.round((float) midTexW * clampedMidScreenW / (FILLED_TEX_W - filledEdgeWidth * 2 > 0 ? FILLED_TEX_W - filledEdgeWidth * 2 : 1));
            clampedMidTexW = Math.max(1, Math.min(clampedMidTexW, midTexW));

            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    clampedMidScreenW, barH,
                    texX + filledEdgeWidth, texY,
                    midTexW, FILLED_TEX_H,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
            drawX += clampedMidScreenW;
            barW -= clampedMidScreenW;
        }

        // Правый край — только если осталось место
        if (barW >= filledEdgeWidth) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    filledEdgeWidth, barH,
                    texX + FILLED_TEX_W - filledEdgeWidth, texY,
                    filledEdgeWidth, FILLED_TEX_H,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        } else if (barW > 0) {
            // Частичный правый край
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    barW, barH,
                    texX + FILLED_TEX_W - filledEdgeWidth, texY,
                    barW, FILLED_TEX_H,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        }
    }
}