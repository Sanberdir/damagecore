package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ru.imaginaerum.damagecore.hud.DamageCoreHudOverlay;

public class HealthBarElement {

    private static final int TEXTURE_BAR_WIDTH = 104;
    private static final int EDGE_WIDTH = 6;

    private static final int BAR_X = 45;
    private static final int BAR_Y = 17;
    private static final int BAR_H = 6;
    private static final int BASE_BAR_W = 52;
    private static final float BASE_MAX_HEALTH = 20f;

    private static final int TEXTURE_X = 45;
    private static final int TEXTURE_Y_EMPTY = 177;
    private static final int TEXTURE_Y_HEALTH = 81;
    private static final int TEXTURE_Y_ABSORPTION = 113;

    public static void render(GuiGraphics gui, Minecraft mc) {
        float health = mc.player.getHealth();
        float maxHealth = mc.player.getMaxHealth();
        float absorption = mc.player.getAbsorptionAmount();

        int barW = (int)(BASE_BAR_W * (maxHealth / BASE_MAX_HEALTH));
        int healthWidth = Math.max(0, Math.min(barW, (int)(barW * health / maxHealth)));
        int absorptionWidth = Math.max(0, Math.min(barW, (int)(barW * absorption / maxHealth)));
        boolean hasAbsorption = absorptionWidth > 0;

        // Пустая полоска
        StaminaBarElement.renderBar(gui, BAR_X, BAR_Y, barW, BAR_H, TEXTURE_X, TEXTURE_Y_EMPTY);

        // Красная HP
        if (healthWidth > 0) {
            renderHealth(gui, barW, healthWidth, hasAbsorption);
        }

        // Жёлтая Absorption
        if (hasAbsorption) {
            renderAbsorption(gui, barW, healthWidth, absorptionWidth);
        }
    }

    private static void renderHealth(GuiGraphics gui, int barW, int healthWidth, boolean hasAbsorption) {
        int fillX = BAR_X;
        int width = healthWidth;

        int left = Math.min(EDGE_WIDTH, width);
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, fillX, BAR_Y, TEXTURE_X, TEXTURE_Y_HEALTH, left, BAR_H, 160, 208);
        fillX += left; width -= left;

        if (width > 0) {
            int mid = Math.min(width, barW - EDGE_WIDTH * 2);
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, fillX, BAR_Y, TEXTURE_X + EDGE_WIDTH, TEXTURE_Y_HEALTH, mid, BAR_H, 160, 208);
            fillX += mid; width -= mid;
        }

        if (width > 0) {
            if (!hasAbsorption) {
                gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, fillX, BAR_Y,
                        TEXTURE_X + TEXTURE_BAR_WIDTH - EDGE_WIDTH, TEXTURE_Y_HEALTH,
                        Math.min(width, EDGE_WIDTH), BAR_H, 160, 208);
            } else {
                gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, fillX, BAR_Y,
                        TEXTURE_X + EDGE_WIDTH, TEXTURE_Y_HEALTH, width, BAR_H, 160, 208);
            }
        }
    }

    private static void renderAbsorption(GuiGraphics gui, int barW, int healthWidth, int absorptionWidth) {
        int goldStartX = BAR_X + healthWidth;
        int rightSkewStart = BAR_X + barW - EDGE_WIDTH;
        int absorptionEnd = goldStartX + absorptionWidth;

        int leftLen = (goldStartX == BAR_X) ? Math.min(EDGE_WIDTH, absorptionWidth) : 0;
        int rightLen = 0;

        if (absorptionEnd > rightSkewStart) {
            rightLen = Math.min(EDGE_WIDTH, absorptionEnd - rightSkewStart);
            rightLen = Math.min(rightLen, absorptionWidth - leftLen);
        }

        int midLen = Math.max(0, absorptionWidth - leftLen - rightLen);
        int drawX = goldStartX;

        if (leftLen > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, drawX, BAR_Y, TEXTURE_X, TEXTURE_Y_ABSORPTION, leftLen, BAR_H, 160, 208);
            drawX += leftLen;
        }
        if (midLen > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, drawX, BAR_Y, TEXTURE_X + EDGE_WIDTH, TEXTURE_Y_ABSORPTION, midLen, BAR_H, 160, 208);
            drawX += midLen;
        }
        if (rightLen > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, drawX, BAR_Y,
                    TEXTURE_X + TEXTURE_BAR_WIDTH - EDGE_WIDTH + (EDGE_WIDTH - rightLen),
                    TEXTURE_Y_ABSORPTION, rightLen, BAR_H, 160, 208);
        }
    }
}