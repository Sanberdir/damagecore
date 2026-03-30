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

    // Позиция всего элемента на экране
    private static final int ELEMENT_X = 26;
    private static final int ELEMENT_Y = 40;

    // Значок (иконка голода) — фон
    private static final int ICON_SRC_X  = 0;
    private static final int ICON_SRC_Y  = 12;
    private static final int ICON_W      = 18;
    private static final int ICON_H      = 20;

    // Заполненная часть голода (уменьшается сверху вниз)
    private static final int FILL_SRC_X  = 2;
    private static final int FILL_SRC_Y  = 12;
    private static final int FILL_W      = 16;
    private static final int FILL_H      = 10;

    // Пустая часть голода (фон под заполнением)
    private static final int EMPTY_SRC_X = 0;
    private static final int EMPTY_SRC_Y = 38;
    private static final int EMPTY_W     = 16;
    private static final int EMPTY_H     = 10;

    // Насыщенность (поверх заполненной, тоже сверху вниз)
    private static final int SAT_SRC_X   = 16;
    private static final int SAT_SRC_Y   = 38;
    private static final int SAT_W       = 16;
    private static final int SAT_H       = 10;

    // Смещение полоски относительно иконки
    private static final int BAR_OFFSET_X = 2;
    private static final int BAR_OFFSET_Y = 0; // верх полоски совпадает с верхом иконки

    public static void render(GuiGraphics gui, Minecraft mc) {
        if (mc.player == null) return;

        FoodData food = mc.player.getFoodData();
        float hunger     = food.getFoodLevel();       // 0..20
        float saturation = food.getSaturationLevel(); // 0..20 (обычно)
        float maxHunger  = 20f;
        float maxSat     = 20f;

        int screenX = ELEMENT_X;
        int screenY = ELEMENT_Y;

        // 1. Рисуем иконку-фон (значок голода целиком)
        gui.blit(TEXTURE,
                screenX, screenY,
                ICON_SRC_X, ICON_SRC_Y,
                ICON_W, ICON_H,
                TEXTURE_W, TEXTURE_H);

        int barX = screenX + BAR_OFFSET_X;
        int barY = screenY + BAR_OFFSET_Y;

        // 2. Пустая полоска (фон) — всегда полная
        gui.blit(TEXTURE,
                barX, barY,
                EMPTY_SRC_X, EMPTY_SRC_Y,
                EMPTY_W, EMPTY_H,
                TEXTURE_W, TEXTURE_H);

        // 3. Заполненная часть голода — уменьшается СВЕРХУ ВНИЗ
        //    Полная высота = FILL_H. При hunger=20 рисуем все 10px.
        //    При hunger=10 рисуем 5px снизу, срезаем 5px сверху.
        int fillH = Math.round(FILL_H * (hunger / maxHunger));
        if (fillH > 0) {
            int cut = FILL_H - fillH; // сколько срезаем сверху
            gui.blit(TEXTURE,
                    barX,              barY + cut,   // экранная Y смещается вниз
                    FILL_SRC_X,        FILL_SRC_Y + cut, // UV тоже смещается вниз
                    FILL_W,            fillH,
                    TEXTURE_W,         TEXTURE_H);
        }

        // 4. Насыщенность — поверх заполненной, тоже уменьшается сверху вниз
        //    Насыщенность не превышает hunger визуально
        float satClamped = Math.min(saturation, maxSat);
        int satH = Math.round(SAT_H * (satClamped / maxSat));
        if (satH > 0) {
            int cut = SAT_H - satH;
            gui.blit(TEXTURE,
                    barX,            barY + cut,
                    SAT_SRC_X,       SAT_SRC_Y + cut,
                    SAT_W,           satH,
                    TEXTURE_W,       TEXTURE_H);
        }
    }
}