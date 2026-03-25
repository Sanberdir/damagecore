package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;

import java.util.Map;

public final class ArmorStatsFieldRenderer {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation("damagecore", "textures/gui/container/creative_inventory/armor_statistic_field.png");

    private static final int TEX_W = 200;
    private static final int TEX_H = 20;
    private static final int EDGE = 2;

    private ArmorStatsFieldRenderer() {
    }

    public static boolean isButtonHovered(double mouseX, double mouseY, InventoryScreen screen) {
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop = ((AbstractContainerScreenAccessor) screen).getTopPos();

        int x = guiLeft + 62;
        int y = guiTop + 10;

        return mouseX >= x && mouseX < x + 10 &&
                mouseY >= y && mouseY < y + 9;
    }

    public static void render(GuiGraphics gui, InventoryScreen screen) {
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int drawW = imageWidth;
        int drawH = 20;

        int topFieldY = guiTop - drawH + 1;
        int bottomFieldY = guiTop + 165;

        // Рисуем обе полоски
        drawStretchBar(gui, TEXTURE, guiLeft, topFieldY, drawW, drawH);
        drawStretchBar(gui, TEXTURE, guiLeft, bottomFieldY, drawW, drawH);

        // ===== РЕНДЕР СТАТОВ В ВЕРХНЕЙ ПОЛОСКЕ =====
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Map<DamageType, Float> protections =
                ArmorStatsCalculator.getPlayerTotalProtectionPercent(mc.player);

        int iconSize = 8;
        float scale = 2f / 3f; // уменьшаем цифры в 1.5 раза
        int startX = guiLeft + 6;
// Центр полоски по вертикали
        int yTop = topFieldY + (drawH - iconSize) / 2;

        int x = startX; // стартовая позиция
        for (Map.Entry<DamageType, Float> entry : protections.entrySet()) {
            DamageType type = entry.getKey();
            float percent = entry.getValue();

            // Иконка типа урона
            ResourceLocation icon = new ResourceLocation(
                    "damagecore",
                    "textures/gui/damage_types/" + type.getDamageName() + "_damage.png"
            );

            // Рисуем иконку
            gui.blit(icon, x, yTop, 0, 0, iconSize, iconSize, iconSize, iconSize);

            // Рисуем цифру процента справа от иконки, масштабируем
            gui.pose().pushPose();
            int percentY = yTop + (iconSize / 2) - (int)(mc.font.lineHeight * scale / 2);
            gui.pose().translate(x + iconSize + 1, percentY, 0);
            gui.pose().scale(scale, scale, 1.0f);
            gui.drawString(mc.font, (int)(percent * 100) + "%", 0, 0, 0xFFFFFF, false);
            gui.pose().popPose();

            // Смещаем x на ширину блока + 4 пикселя для следующего блока
            int textWidth = (int)(mc.font.width((int)(percent * 100) + "%") * scale);
            x += iconSize + 1 + textWidth + 4;
        }
    }

    private static void drawStretchBar(GuiGraphics gui, ResourceLocation tex,
                                       int x, int y, int width, int height) {
        int middleWidth = width - EDGE * 2;

        // левый край
        gui.blit(tex, x, y, 0, 0, EDGE, height, TEX_W, TEX_H);

        // середина
        gui.blit(tex, x + EDGE, y, EDGE, 0, middleWidth, height, TEX_W, TEX_H);

        // правый край
        gui.blit(tex, x + EDGE + middleWidth, y,
                TEX_W - EDGE, 0,
                EDGE, height,
                TEX_W, TEX_H);
    }
}