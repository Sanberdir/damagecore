package ru.imaginaerum.damagecore.animation_attack;

import net.minecraft.client.player.AbstractClientPlayer;

public class SwordSwingAnimator {

    public static void trigger(AbstractClientPlayer player) {
        if (player instanceof IExampleAnimatedPlayer animated) {
            animated.requestSwordSwing();
        }
    }
}