package ru.imaginaerum.damagecore.animation_attack;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

public class AnimationSetup {

    // Уникальный ключ слоя — по нему потом достаём ModifierLayer у игрока
    public static final ResourceLocation ATTACK_LAYER_KEY =
            new ResourceLocation("damagecore", "attack_layer");

    // Приоритет слоя (чем выше — тем приоритетнее над другими анимациями)
    public static final int ATTACK_LAYER_PRIORITY = 500;

    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                ATTACK_LAYER_KEY,
                ATTACK_LAYER_PRIORITY,
                (AbstractClientPlayer player) -> {
                    // Создаём пустой ModifierLayer для каждого игрока при его создании
                    return new ModifierLayer<>();
                }
        );
    }
}