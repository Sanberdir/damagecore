package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ru.imaginaerum.damagecore.hud.DamageCoreHudOverlay;

public class StaminaBarElement {

    private static final int TEXTURE_BAR_WIDTH = 104;
    private static final int EDGE_WIDTH = 6;

    private static final int BAR_X = 38;
    private static final int BAR_Y = 33;
    private static final int BAR_W = 52;
    private static final int BAR_H = 6;

    private static final int TEXTURE_X = 38;
    private static final int TEXTURE_Y_FULL = 97;
    private static final int TEXTURE_Y_EMPTY = 193;

    private static float stamina = 40f;
    public static final float MAX_STAMINA = 40f;

    public static void update(Minecraft mc) {
        if (mc.player == null) return;

        boolean sprinting = mc.player.isSprinting();
        boolean moving = mc.player.zza != 0 || mc.player.xxa != 0;

        boolean isInMovingBoat = false;
        if (mc.player.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat boat) {
            isInMovingBoat = boat.getDeltaMovement().horizontalDistanceSqr() > 0.001;
        }

        if ((sprinting && moving) || isInMovingBoat) {
            stamina -= isInMovingBoat ? 0.03f : 0.1f;
        } else {
            stamina += moving ? 0.04f : 0.16f;
        }

        stamina = Math.max(0, Math.min(MAX_STAMINA, stamina));
    }

    public static float getStamina() {
        return stamina;
    }

    public static void render(GuiGraphics gui) {
        int filledWidth = Math.max(0, Math.min(BAR_W, (int)(BAR_W * stamina / MAX_STAMINA)));

        // Пустая полоска
        renderBar(gui, BAR_X, BAR_Y, BAR_W, BAR_H, TEXTURE_X, TEXTURE_Y_EMPTY);

        // Заполнение
        if (filledWidth > 0) {
            renderBarFilled(gui, BAR_X, BAR_Y, BAR_W, BAR_H, filledWidth, TEXTURE_X, TEXTURE_Y_FULL);
        }
    }

    static void renderBar(GuiGraphics gui, int barX, int barY, int barW, int barH, int texX, int texY) {
        int remaining = barW;
        int drawX = barX;

        int left = Math.min(EDGE_WIDTH, remaining);
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, drawX, barY, texX, texY, left, barH, 160, 208);
        drawX += left; remaining -= left;

        if (remaining > EDGE_WIDTH) {
            int mid = remaining - EDGE_WIDTH;
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, drawX, barY, texX + EDGE_WIDTH, texY, mid, barH, 160, 208);
            drawX += mid; remaining -= mid;
        }

        if (remaining > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, drawX, barY,
                    texX + TEXTURE_BAR_WIDTH - EDGE_WIDTH, texY, remaining, barH, 160, 208);
        }
    }

    static void renderBarFilled(GuiGraphics gui, int barX, int barY, int barW, int barH,
                                int filledWidth, int texX, int texY) {
        int fillX = barX;
        int width = filledWidth;

        int left = Math.min(EDGE_WIDTH, width);
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, fillX, barY, texX, texY, left, barH, 160, 208);
        fillX += left; width -= left;

        if (width > 0) {
            int mid = Math.min(width, barW - EDGE_WIDTH * 2);
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, fillX, barY, texX + EDGE_WIDTH, texY, mid, barH, 160, 208);
            fillX += mid; width -= mid;
        }

        if (width > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, fillX, barY,
                    texX + TEXTURE_BAR_WIDTH - EDGE_WIDTH, texY,
                    Math.min(width, EDGE_WIDTH), barH, 160, 208);
        }
    }
}