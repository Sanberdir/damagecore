package ru.imaginaerum.damagecore.mixin.animation_attack;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.animation_attack.AttackAnimationManager;
import ru.imaginaerum.damagecore.animation_attack.PlayerAnimationController;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    @Inject(
            method = "swing",
            at = @At("HEAD"),
            cancellable = false
    )
    private void onSwing(InteractionHand hand, CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer)(Object)this;

        // Только главная рука
        if (hand != InteractionHand.MAIN_HAND) return;

        ItemStack held = self.getItemInHand(hand);

        AttackAnimationManager.getAnimation(held).ifPresent(animId -> {
            // Запускаем только на клиенте
            if (self.level().isClientSide()) {
                PlayerAnimationController.playAttack(self, animId);
            }
        });
    }
}
