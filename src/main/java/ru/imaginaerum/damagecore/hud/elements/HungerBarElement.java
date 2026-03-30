package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodData;

public class HungerBarElement {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("damagecore", "textures/hud/requirement_line.png");

    private static final int TEXTURE_W = 80;
    private static final int TEXTURE_H = 48;

    // Значок голода — фон
    private static final int ICON_SRC_X = 0;
    private static final int ICON_SRC_Y = 12;
    private static final int ICON_W     = 18;
    private static final int ICON_H     = 20;

    // Заполненная часть (убывает сверху вниз)
    private static final int FILL_SRC_X = 2;
    private static final int FILL_SRC_Y = 12;
    private static final int FILL_W     = 16;
    private static final int FILL_H     = 10;

    // Пустая часть
    private static final int EMPTY_SRC_X = 0;
    private static final int EMPTY_SRC_Y = 38;
    private static final int EMPTY_W     = 16;
    private static final int EMPTY_H     = 10;

    // Насыщенность
    private static final int SAT_SRC_X = 16;
    private static final int SAT_SRC_Y = 38;
    private static final int SAT_W     = 16;
    private static final int SAT_H     = 10;

    private static final int BAR_OFFSET_X = 2;
    private static final int BAR_OFFSET_Y = 0;

    public static void render(GuiGraphics gui, Minecraft mc) {
        if (mc.player == null) return;

        FoodData food = mc.player.getFoodData();
        float hunger     = food.getFoodLevel();
        float saturation = food.getSaturationLevel();
        float maxHunger  = 20f;
        float maxSat     = 20f;

        // Привязка к строке сердечек: y = screenH - 49 (ванильный ряд HP)
        // Иконка слева от сердечек: hotbarLeft - ICON_W - 4 (жажда) - ICON_W - 2 (еда)
        int screenH    = mc.getWindow().getGuiScaledHeight();
        int screenW    = mc.getWindow().getGuiScaledWidth();
        int hotbarLeft = screenW / 2 - 50;
        int heartsY    = screenH - 49; // ванильный Y ряда сердечек

        // Еда — вторая иконка от края хотбара (жажда будет ещё левее)
        // жажда занимает ICON_W=17 + 2px зазор = 19px
        int screenX = hotbarLeft - 19 - ICON_W; // ~hotbarLeft - 39
        int screenY = heartsY;

        // 1. Иконка-фон
        gui.blit(TEXTURE,
                screenX, screenY,
                ICON_SRC_X, ICON_SRC_Y,
                ICON_W, ICON_H,
                TEXTURE_W, TEXTURE_H);

        int barX = screenX + BAR_OFFSET_X;
        int barY = screenY + BAR_OFFSET_Y;

        // 2. Пустая полоска
        gui.blit(TEXTURE,
                barX, barY,
                EMPTY_SRC_X, EMPTY_SRC_Y,
                EMPTY_W, EMPTY_H,
                TEXTURE_W, TEXTURE_H);

        // 3. Заполненная часть — убывает сверху вниз
        int fillH = Math.round(FILL_H * (hunger / maxHunger));
        if (fillH > 0) {
            int cut = FILL_H - fillH;
            gui.blit(TEXTURE,
                    barX,        barY + cut,
                    FILL_SRC_X,  FILL_SRC_Y + cut,
                    FILL_W,      fillH,
                    TEXTURE_W,   TEXTURE_H);
        }

        // 4. Насыщенность поверх
        float satClamped = Math.min(saturation, maxSat);
        int satH = Math.round(SAT_H * (satClamped / maxSat));
        if (satH > 0) {
            int cut = SAT_H - satH;
            gui.blit(TEXTURE,
                    barX,       barY + cut,
                    SAT_SRC_X,  SAT_SRC_Y + cut,
                    SAT_W,      satH,
                    TEXTURE_W,  TEXTURE_H);
        }
    }
}