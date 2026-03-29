package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Collection;

public class EffectIconsElement {

    private static final ResourceLocation EFFECT_ICON_TEXTURE =
            new ResourceLocation("damagecore", "textures/hud/effect_icon.png");

    private static final int START_X = 39;
    private static final int START_Y = 5;
    private static final int ICON_SIZE = 10;
    private static final int ICON_STEP = 13;

    public static void render(GuiGraphics gui, Minecraft mc) {
        if (mc.player == null) return;

        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        if (effects.isEmpty()) return;

        int index = 0;
        for (MobEffectInstance effectInstance : effects) {
            int iconX = START_X + index * ICON_STEP;

            // Рамка
            gui.blit(EFFECT_ICON_TEXTURE, iconX, START_Y,
                    0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            // Спрайт эффекта по центру
            TextureAtlasSprite sprite = mc.getMobEffectTextures().get(effectInstance.getEffect());
            if (sprite != null) {
                gui.blit(iconX, START_Y, 0, ICON_SIZE, ICON_SIZE, sprite);
            }

            index++;
        }
    }
}