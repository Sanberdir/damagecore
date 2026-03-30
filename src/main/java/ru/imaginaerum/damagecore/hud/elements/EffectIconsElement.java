package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EffectIconsElement {

    private static final ResourceLocation EFFECT_LINE_TEXTURE =
            new ResourceLocation("damagecore", "textures/hud/effect_line.png");

    // Текстура: 63x4 пикселя
    private static final int TEXTURE_FULL_W = 63;
    private static final int TEXTURE_H     = 4;
    private static final int EDGE_W        = 5;   // левый и правый край

    // Один слот под эффект: иконка (10) + отступ между иконками (3)
    private static final int ICON_SIZE = 10;
    private static final int ICON_GAP  = 3;

    // Координаты полосы
// Координаты полосы
    private static final int BAR_Y = 11;
    private static final int ICON_Y = BAR_Y - ICON_SIZE + 2; // иконки над полосой с отступом 1px
    private static final int BAR_START_X = 40;


    public static void render(GuiGraphics gui, Minecraft mc) {
        if (mc.player == null) return;

        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        if (effects.isEmpty()) return;

        List<MobEffectInstance> list = new ArrayList<>(effects);
        int count = list.size();

        // Ширина полосы: края + слоты для всех эффектов (последний без правого gap)
        // Содержимое = count иконок + (count-1) gap + отступы внутри краёв
        // Полоса охватывает: EDGE_W + count*ICON_SIZE + (count-1)*ICON_GAP + EDGE_W
        int innerW = count * ICON_SIZE + (count - 1) * ICON_GAP;
        int barW   = EDGE_W + innerW + EDGE_W;

        // --- Рисуем полосу (9-slice по горизонтали, высота 4) ---
        renderBar(gui, BAR_START_X, BAR_Y, barW);

        // --- Рисуем иконки эффектов ---
        int iconDrawX = BAR_START_X + EDGE_W;
        for (MobEffectInstance effectInstance : list) {
            TextureAtlasSprite sprite = mc.getMobEffectTextures().get(effectInstance.getEffect());
            if (sprite != null) {
                gui.blit(iconDrawX, ICON_Y, 0, ICON_SIZE, ICON_SIZE, sprite);
            }
            iconDrawX += ICON_SIZE + ICON_GAP;
        }
    }

    /**
     * Растягивает полосу: левый край (5px) | середина (stretch) | правый край (5px).
     * Текстура: 63x4, U-layout: [0..4] left edge, [5..57] mid tile, [58..62] right edge.
     */
    private static void renderBar(GuiGraphics gui, int x, int y, int totalW) {
        if (totalW <= 0) return;

        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, EFFECT_LINE_TEXTURE);
        com.mojang.blaze3d.systems.RenderSystem.setShader(
                net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

        com.mojang.blaze3d.vertex.BufferBuilder buf = com.mojang.blaze3d.vertex.Tesselator
                .getInstance().getBuilder();
        buf.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);

        org.joml.Matrix4f mat = gui.pose().last().pose();

        float tw = TEXTURE_FULL_W; // 63f
        float th = TEXTURE_H;      // 4f

        // UV границы в [0..1]
        float uMidL = EDGE_W / tw;           // 5/63
        float uMidR = (tw - EDGE_W) / tw;    // 58/63
        float uRightL = (tw - EDGE_W) / tw;  // 58/63
        float uRightR = 1f;
        float vT = 0f;
        float vB = 1f;

        int midW  = totalW - EDGE_W * 2;
        int x0    = x;
        int x1    = x + EDGE_W;
        int x2    = x1 + Math.max(0, midW);
        int x3    = x2 + EDGE_W;
        int yT    = y;
        int yB    = y + TEXTURE_H;

        // Левый край
        addQuad(buf, mat, x0, x1, yT, yB,  0f,     uMidL, vT, vB);
        // Середина — UV от uMidL до uMidR независимо от ширины → растяжение
        if (midW > 0) {
            addQuad(buf, mat, x1, x2, yT, yB, uMidL, uMidR, vT, vB);
        }
        // Правый край
        addQuad(buf, mat, x2, x3, yT, yB, uRightL, uRightR, vT, vB);

        com.mojang.blaze3d.vertex.Tesselator.getInstance().end();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private static void addQuad(com.mojang.blaze3d.vertex.BufferBuilder buf,
                                org.joml.Matrix4f mat,
                                int x0, int x1, int yT, int yB,
                                float u0, float u1, float v0, float v1) {
        buf.vertex(mat, x0, yB, 0).uv(u0, v1).endVertex();
        buf.vertex(mat, x1, yB, 0).uv(u1, v1).endVertex();
        buf.vertex(mat, x1, yT, 0).uv(u1, v0).endVertex();
        buf.vertex(mat, x0, yT, 0).uv(u0, v0).endVertex();
    }
}