package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.gui.GuiGraphics;

public class ManaBarElement {

    private static final int BAR_X = 45;
    private static final int BAR_Y = 25;
    private static final int BAR_W = 26;
    private static final int BAR_H = 6;

    private static final int TEXTURE_X = 45;
    private static final int TEXTURE_Y_FULL = 89;
    private static final int TEXTURE_Y_EMPTY = 185;

    private static float mana = 10f;
    public static final float MAX_MANA = 10f;

    public static float getMana() { return mana; }
    public static void setMana(float value) { mana = Math.max(0, Math.min(MAX_MANA, value)); }

    public static void render(GuiGraphics gui) {
        int filledWidth = Math.max(0, Math.min(BAR_W, (int)(BAR_W * mana / MAX_MANA)));

        StaminaBarElement.renderBar(gui, BAR_X, BAR_Y, BAR_W, BAR_H, TEXTURE_X, TEXTURE_Y_EMPTY);

        if (filledWidth > 0) {
            StaminaBarElement.renderBarFilled(gui, BAR_X, BAR_Y, BAR_W, BAR_H, filledWidth, TEXTURE_X, TEXTURE_Y_FULL);
        }
    }
}