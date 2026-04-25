package ru.imaginaerum.damagecore.animation_attack;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;

public class PlayerAnimationController {

    @SuppressWarnings("unchecked")
    private static ModifierLayer<IAnimation> getLayer(AbstractClientPlayer player) {
        var layerObj = PlayerAnimationAccess.getPlayerAssociatedData(player)
                .get(AnimationSetup.ATTACK_LAYER_KEY);

        if (!(layerObj instanceof ModifierLayer<?> raw)) return null;
        return (ModifierLayer<IAnimation>) raw;
    }

    public static void playAttack(AbstractClientPlayer player, AttackAnimationData data) {
        var layer = getLayer(player);
        if (layer == null) return;

        var anim = PlayerAnimationRegistry.getAnimation(data.getAnimationId());
        if (anim == null) return;

        var built = anim.mutableCopy().build();

        var playerAnim = new CustomAnimationPlayer(built, data.getTimeline());

        layer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(3, Ease.INOUTSINE),
                playerAnim
        );
    }

    public static void tick(AbstractClientPlayer player) {
        var layer = getLayer(player);
        if (layer == null) return;

        var anim = layer.getAnimation();

        if (anim instanceof CustomAnimationPlayer custom) {

            // 👉 ВАЖНО: событие удара
            custom.handleEvents(0, () -> {
                performHit(player);
            });

            if (!custom.isInfluencing(0)) {
                layer.replaceAnimationWithFade(
                        AbstractFadeModifier.standardFadeIn(3, Ease.INOUTSINE),
                        null
                );
            }
        }
    }

    private static void performHit(AbstractClientPlayer player) {
        // 🔥 здесь ты можешь вызвать свою систему урона
        System.out.println("HIT FRAME!");

        // пример:
        // player.attack(target);
    }

    public static boolean isPlaying(AbstractClientPlayer player) {
        var layer = getLayer(player);
        if (layer == null) return false;

        return layer.getAnimation() instanceof CustomAnimationPlayer;
    }
}