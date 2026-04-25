package ru.imaginaerum.damagecore.mixin.animation_attack;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.animation_attack.PlayerAnimationController;

@Mixin(LivingEntity.class)
public class MixinLivingEntitySwing {

    @Shadow public float attackAnim;
    @Shadow public float oAttackAnim;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof AbstractClientPlayer player)) return;
        if (!self.level().isClientSide()) return;

        if (PlayerAnimationController.isPlaying(player)) {
            // Блокируем vanilla swing progression полностью
            attackAnim = 0f;
            oAttackAnim = 0f;
        }
    }
}