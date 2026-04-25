package ru.imaginaerum.damagecore.animation_attack;

import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

public class AnimationSetup {

    public static final ResourceLocation ATTACK_LAYER_KEY =
            new ResourceLocation("damagecore", "attack_layer");

    public static final int ATTACK_LAYER_PRIORITY = 2000;

    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                ATTACK_LAYER_KEY,
                ATTACK_LAYER_PRIORITY,
                (AbstractClientPlayer player) -> new ModifierLayer<>()
        );
    }
}