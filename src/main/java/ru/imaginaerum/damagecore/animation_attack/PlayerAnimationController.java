package ru.imaginaerum.damagecore.animation_attack;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class PlayerAnimationController {

    /**
     * Запускает анимацию атаки для игрока.
     * animId — ResourceLocation файла в assets/damagecore/player_animation/
     */
    @SuppressWarnings("unchecked")
    public static void playAttack(AbstractClientPlayer player, ResourceLocation animId) {
        // Получаем слой этого игрока по ключу
        var layerObj = PlayerAnimationAccess.getPlayerAssociatedData(player)
                .get(AnimationSetup.ATTACK_LAYER_KEY);

        if (!(layerObj instanceof ModifierLayer<?> rawLayer)) return;

        ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) rawLayer;

        // Загружаем KeyframeAnimation из реестра (файл из player_animation/)
        var keyframeAnim = PlayerAnimationRegistry.getAnimation(animId);
        if (keyframeAnim == null) return;

        // Запускаем с fade-in в 2 тика для плавности
        layer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(2, Ease.LINEAR),
                new KeyframeAnimationPlayer(keyframeAnim)
        );
    }

    /**
     * Останавливает анимацию с fade-out
     */
    @SuppressWarnings("unchecked")
    public static void stopAttack(AbstractClientPlayer player) {
        var layerObj = PlayerAnimationAccess.getPlayerAssociatedData(player)
                .get(AnimationSetup.ATTACK_LAYER_KEY);

        if (!(layerObj instanceof ModifierLayer<?> rawLayer)) return;

        ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) rawLayer;

        layer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(3, Ease.LINEAR),
                null // null = убрать анимацию
        );
    }

    /**
     * Проверяет, играет ли сейчас атак-анимация
     */
    @SuppressWarnings("unchecked")
    public static boolean isPlaying(AbstractClientPlayer player) {
        var layerObj = PlayerAnimationAccess.getPlayerAssociatedData(player)
                .get(AnimationSetup.ATTACK_LAYER_KEY);

        if (!(layerObj instanceof ModifierLayer<?> rawLayer)) return false;

        ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) rawLayer;
        return layer.getAnimation() != null && layer.isActive();
    }
}