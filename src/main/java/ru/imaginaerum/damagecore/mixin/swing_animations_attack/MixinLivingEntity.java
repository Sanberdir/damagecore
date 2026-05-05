package ru.imaginaerum.damagecore.mixin.swing_animations_attack;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.SwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.animation_attack.SwordSwingAnimator;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(
            method = "swing(Lnet/minecraft/world/InteractionHand;)V",
            at = @At("HEAD")
    )
    private void damagecore$onSwing(InteractionHand hand, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!self.level().isClientSide()) return;
        if (!(self instanceof AbstractClientPlayer clientPlayer)) return;
        if (hand != InteractionHand.MAIN_HAND) return;
        if (!(clientPlayer.getMainHandItem().getItem() instanceof SwordItem)) return;

        SwordSwingAnimator.trigger(clientPlayer);
    }
}