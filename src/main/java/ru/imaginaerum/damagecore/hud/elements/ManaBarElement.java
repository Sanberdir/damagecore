package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.gui.GuiGraphics;
import ru.imaginaerum.damagecore.hud.DamageCoreHudOverlay;

public class ManaBarElement {

    private static final int TEXTURE_BAR_WIDTH = 32;
    private static final int EDGE_WIDTH = 6;

    private static final int BAR_X = 45;
    private static final int BAR_Y = 25;
    private static final int BAR_W = 26; // половина от 52
    private static final int BAR_H = 6;

    private static final int TEXTURE_X = 0;
    private static final int TEXTURE_Y_EMPTY = 72;

    // Заполненная: X2 Y55 по X30 Y59
    private static final int FILLED_TEX_X = 2;
    private static final int FILLED_TEX_Y = 55;
    private static final int FILLED_TEX_W = 28; // 30 - 2
    private static final int FILLED_TEX_H = 4;  // 59 - 55

    private static final int HUD_TEXTURE_WIDTH = 160;
    private static final int HUD_TEXTURE_HEIGHT = 208;

    private static float mana = 1.0f;


    public static void render(GuiGraphics gui) {
        renderEmptyBar(gui, BAR_X, BAR_Y, BAR_W, BAR_H, TEXTURE_X, TEXTURE_Y_EMPTY);

        int filledBarW = BAR_W - 4;
        int filledW = Math.round(filledBarW * mana);

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

        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY,
                EDGE_WIDTH, barH,
                texX, texY,
                EDGE_WIDTH, barH,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        drawX += EDGE_WIDTH;

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

        int leftW = Math.min(filledEdgeWidth, barW);
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY,
                leftW, barH,
                texX, texY,
                leftW, FILLED_TEX_H,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        drawX += leftW;
        barW -= leftW;

        int midTexW = FILLED_TEX_W - filledEdgeWidth * 2;
        int midScreenW = Math.max(0, barW - filledEdgeWidth);
        if (midScreenW > 0 && midTexW > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    midScreenW, barH,
                    texX + filledEdgeWidth, texY,
                    midTexW, FILLED_TEX_H,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
            drawX += midScreenW;
            barW -= midScreenW;
        }

        if (barW >= filledEdgeWidth) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    filledEdgeWidth, barH,
                    texX + FILLED_TEX_W - filledEdgeWidth, texY,
                    filledEdgeWidth, FILLED_TEX_H,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        } else if (barW > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    barW, barH,
                    texX + FILLED_TEX_W - filledEdgeWidth, texY,
                    barW, FILLED_TEX_H,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        }
    }
}