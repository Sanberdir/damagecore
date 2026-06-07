package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffects;
import ru.imaginaerum.damagecore.hud.DamageCoreHudOverlay;

public class HealthBarElement {

    private static final int TEXTURE_BAR_WIDTH = 32;
    private static final int EDGE_WIDTH = 6;

    private static final int BAR_X = 47;
    private static final int BAR_Y = 17;
    private static final int BAR_W = 52;
    private static final int BAR_H = 6;

    private static final int TEXTURE_X = 0;
    private static final int TEXTURE_Y_EMPTY = 66;

    // Красная полоска
    private static final int FILLED_TEX_X      = 2;
    private static final int FILLED_TEX_Y      = 49;
    private static final int FILLED_TEX_W      = 28;
    private static final int FILLED_TEX_H      = 4;

    // Золотая полоска (absorption)
    private static final int GOLD_TEX_X        = 34;
    private static final int GOLD_TEX_Y        = 49;
    // Ширина и высота золотой — те же размеры на текстуре
    private static final int GOLD_TEX_W        = 28;
    private static final int GOLD_TEX_H        = 4;

    // Y-координаты эффектов (X берётся из FILLED_TEX_X / GOLD_TEX_X)
    private static final int FILLED_TEX_Y_FROZEN = 97;
    private static final int FILLED_TEX_X_FROZEN = 2;
    private static final int FILLED_TEX_X_WITHER = 33;
    private static final int FILLED_TEX_X_POISON = 66;

    private static final int HUD_TEXTURE_WIDTH  = 160;
    private static final int HUD_TEXTURE_HEIGHT = 208;
    private static float lastHealth = -1f;
    private static int damageAnimTick = 0;
    private static final int ANIM_PHASE_TICKS = 8; // сколько тиков каждая фаза
    public static void render(GuiGraphics gui, float healthPercent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float maxHealth    = mc.player.getMaxHealth();
        float currentHp    = mc.player.getHealth();
        float absorption   = mc.player.getAbsorptionAmount();
        float baseHealth   = 20f;

        boolean hasAbsorption = absorption > 0f;

        // Суммарное максимальное значение для растяжки полоски
        float totalMax = hasAbsorption ? maxHealth + absorption : maxHealth;

        float widthScale = totalMax / baseHealth;
        int scaledBarW   = Math.round(BAR_W * widthScale);
        long now = System.currentTimeMillis();
        if (lastHealth < 0f) lastHealth = currentHp;

        if (currentHp < lastHealth) {
            damageAnimTick = ANIM_PHASE_TICKS * 3; // запускаем 3 фазы
        }
        lastHealth = currentHp;

        if (damageAnimTick > 0) damageAnimTick--;

// Выбор текстуры пустой полоски
        int emptyTexX;
        if (damageAnimTick > ANIM_PHASE_TICKS * 2) {
            emptyTexX = 32;  // фаза 1
        } else if (damageAnimTick > ANIM_PHASE_TICKS) {
            emptyTexX = 64;  // фаза 2
        } else if (damageAnimTick > 0) {
            emptyTexX = 96;  // фаза 3
        } else {
            emptyTexX = TEXTURE_X; // обычная (0)
        }
        // Пустая полоска — растянута под всё (HP + absorption)
        renderEmptyBar(gui, BAR_X, BAR_Y, scaledBarW, BAR_H, emptyTexX, TEXTURE_Y_EMPTY);

        int filledBarW = scaledBarW - 4; // внутренняя ширина без краёв

        // --- Красная часть (текущее HP относительно maxHealth) ---
        int texX = FILLED_TEX_X;
        int texY = FILLED_TEX_Y;

        if (mc.player.isFreezing() || mc.player.getTicksFrozen() > 0) {
            texX = FILLED_TEX_X_FROZEN;
            texY = FILLED_TEX_Y_FROZEN;
        } else if (mc.player.hasEffect(MobEffects.WITHER)) {
            texX = FILLED_TEX_X_WITHER;
            texY = FILLED_TEX_Y_FROZEN; // Y=97
        } else if (mc.player.hasEffect(MobEffects.POISON)) {
            texX = FILLED_TEX_X_POISON;
            texY = FILLED_TEX_Y_FROZEN; // Y=97
        }

        // Ширина красной части = filledBarW * (currentHp / totalMax)
        int redW = Math.round(filledBarW * (currentHp / totalMax));

        if (redW > 0) {
            renderFilledBar(gui,
                    BAR_X + 2, BAR_Y + 1,
                    redW, BAR_H - 2,
                    texX, texY,
                    FILLED_TEX_W, FILLED_TEX_H,
                    filledBarW,
                    false);
        }

        // --- Золотая часть (absorption) правее красной ---
        if (hasAbsorption) {
            int goldW = Math.round(filledBarW * (absorption / totalMax));
            // Подгоняем, чтобы не вылезти за край из-за округления
            goldW = Math.min(goldW, filledBarW - redW);

            if (goldW > 0) {
                renderFilledBar(gui,
                        BAR_X + 2 + redW, BAR_Y + 1,
                        goldW, BAR_H - 2,
                        GOLD_TEX_X, GOLD_TEX_Y,
                        GOLD_TEX_W, GOLD_TEX_H,
                        filledBarW - redW,  // ← пространство доступное золотой
                        true);
            }
        }
    }

    static void renderEmptyBar(GuiGraphics gui, int barX, int barY, int barW, int barH, int texX, int texY) {
        int drawX = barX;

        // Левый край
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY, EDGE_WIDTH, barH,
                texX, texY, EDGE_WIDTH, barH,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        drawX += EDGE_WIDTH;

        // Середина
        int midWidth = barW - EDGE_WIDTH * 2;
        int midTexX  = texX + EDGE_WIDTH;
        int midTexW  = TEXTURE_BAR_WIDTH - EDGE_WIDTH * 2;
        if (midWidth > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY, midWidth, barH,
                    midTexX, texY, midTexW, barH,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
            drawX += midWidth;
        }

        // Правый край
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY, EDGE_WIDTH, barH,
                texX + TEXTURE_BAR_WIDTH - EDGE_WIDTH, texY, EDGE_WIDTH, barH,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
    }

    /**
     * @param isRightSegment  true = золотая (правый сегмент), края рисуются иначе:
     *                        левый край не рисуется если примыкает к красной,
     *                        правый рисуется всегда как завершение.
     */
    static void renderFilledBar(GuiGraphics gui,
                                int barX, int barY,
                                int barW, int barH,
                                int texX, int texY,
                                int texW, int texH,
                                int maxFilledW,
                                boolean isRightSegment) {
        if (barW <= 0) return;

        int drawX = barX;
        int filledEdgeWidth = Math.min(EDGE_WIDTH, texW / 2);
        int midTexW = texW - filledEdgeWidth * 2;

        // Левый край: у золотой не рисуем (стыкуется с красной вплотную)
        if (!isRightSegment) {
            int leftW = Math.min(filledEdgeWidth, barW);
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY, leftW, barH,
                    texX, texY, leftW, texH,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
            drawX += leftW;
            barW  -= leftW;
            if (barW <= 0) return;
        }

        // Правый край золотой — резервируем место
        int rightW = 0;
        {
            // Правый скос только если сегмент доходит до правого края бара
            int midMaxScreenW = maxFilledW - filledEdgeWidth * (isRightSegment ? 1 : 2);
            rightW = Math.max(0, barW - midMaxScreenW);
            rightW = Math.min(rightW, filledEdgeWidth);
            rightW = Math.min(rightW, barW);
        }

        // Середина
        int midScreenW = barW - rightW;
        if (midScreenW > 0 && midTexW > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY, midScreenW, barH,
                    texX + filledEdgeWidth, texY, midTexW, texH,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
            drawX += midScreenW;
        }

        // Правый край
        if (rightW > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY, rightW, barH,
                    texX + texW - filledEdgeWidth, texY, rightW, texH,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        }
    }
}