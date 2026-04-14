package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ru.imaginaerum.damagecore.Config;
import ru.imaginaerum.damagecore.hud.DamageCoreHudOverlay;

public class StaminaBarElement {

    private static final int TEXTURE_BAR_WIDTH = 32;
    private static final int EDGE_WIDTH = 6;

    private static final int BAR_X = 40;
    private static final int BAR_Y = 33;
    private static final int BAR_W = 52;
    private static final int BAR_H = 6;

    private static final int TEXTURE_X = 0;
    private static final int TEXTURE_Y_EMPTY = 72;

    // Заполненная полоска: X2 Y61 по X30 Y65
    private static final int FILLED_TEX_X = 2;
    private static final int FILLED_TEX_Y = 61;
    private static final int FILLED_TEX_W = 28; // 30 - 2
    private static final int FILLED_TEX_H = 4;  // 65 - 61

    private static final int HUD_TEXTURE_WIDTH = 160;
    private static final int HUD_TEXTURE_HEIGHT = 208;

    private static final int TEXTURE_X_FLASH = 32;  // координаты второй текстуры
    private static final int TEXTURE_Y_FLASH = 78;
    private static final int FLASH_DURATION  = 3;
    private static float lastStamina  = -1f;  // прошлое значение стамины
    private static int   flashTicks   = 0;    // сколько тиков мигать осталось
    private static long  lastGameTime = -1L;  // для отсчёта тиков

    public static void render(GuiGraphics gui) {
        if (!Config.showStaminaHud) return;

        float stamina = StaminaManager.getStamina();

        long now = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime()
                : System.currentTimeMillis() / 50;

        if (lastGameTime != now) {
            lastGameTime = now;

            if (lastStamina >= 0 && stamina < lastStamina) {
                flashTicks = FLASH_DURATION;
            }
            if (flashTicks > 0) flashTicks--;

            lastStamina = stamina;
        }

        boolean useAlt = flashTicks > 0;

        int emptyTexX = useAlt ? TEXTURE_X_FLASH : TEXTURE_X;
        int emptyTexY = useAlt ? TEXTURE_Y_FLASH : TEXTURE_Y_EMPTY;

        renderEmptyBar(gui, BAR_X, BAR_Y, BAR_W, BAR_H, emptyTexX, emptyTexY);

        float fraction = stamina / StaminaManager.MAX_STAMINA;
        int filledW = Math.round((BAR_W - 4) * fraction);

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
        int maxFilledW = BAR_W - 4;

        // Левый край
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

        // Сколько пикселей правого скоса уже "вошло"
        // Скос начинает появляться когда до maxFilledW остаётся <= filledEdgeWidth пикселей
        int currentTotal = (drawX - barX) + barW; // уже нарисовано + осталось
        int distanceFromEnd = maxFilledW - (leftW + barW);
        int rightEdgeVisible = Math.max(0, Math.min(filledEdgeWidth, filledEdgeWidth - distanceFromEnd));

        // Середина
        int midScreenW = Math.max(0, barW - rightEdgeVisible);
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

        // Правый скос — рисуем ровно rightEdgeVisible пикселей из текстуры
        if (rightEdgeVisible > 0 && barW > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    rightEdgeVisible, barH,
                    texX + FILLED_TEX_W - filledEdgeWidth, texY,
                    rightEdgeVisible, FILLED_TEX_H,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        }
    }
}