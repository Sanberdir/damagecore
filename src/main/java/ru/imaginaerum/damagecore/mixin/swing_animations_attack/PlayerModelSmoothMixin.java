package ru.imaginaerum.damagecore.mixin.swing_animations_attack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class PlayerModelSmoothMixin<T extends LivingEntity> {

    @Unique private float damagecore$oldRightArmX, damagecore$oldRightArmY, damagecore$oldRightArmZ;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    private void damagecore$smoothAnimation(T entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, CallbackInfo ci) {
        PlayerModel<?> model = (PlayerModel<?>)(Object)this;
        float smooth = 0.25f;
        ModelPart arm = model.rightArm;

        arm.xRot = Mth.lerp(smooth, damagecore$oldRightArmX, arm.xRot);
        arm.yRot = Mth.lerp(smooth, damagecore$oldRightArmY, arm.yRot);
        arm.zRot = Mth.lerp(smooth, damagecore$oldRightArmZ, arm.zRot);

        damagecore$oldRightArmX = arm.xRot;
        damagecore$oldRightArmY = arm.yRot;
        damagecore$oldRightArmZ = arm.zRot;
    }
}