package ru.imaginaerum.damagecore.mixin.swing_animations_attack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.animation_attack.IExampleAnimatedPlayer;

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

        if (entity instanceof IExampleAnimatedPlayer animated && animated.damagecore$isHeadLookAtCameraActive()) {
            damagecore$applyHeadLookAtCamera(model, entity);
        }
    }

    @Unique
    private void damagecore$applyHeadLookAtCamera(PlayerModel<?> model, LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) return;

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        double eyeX = entity.getX();
        double eyeY = entity.getY() + entity.getEyeHeight();
        double eyeZ = entity.getZ();

        double dx = cameraPos.x - eyeX;
        double dy = cameraPos.y - eyeY;
        double dz = cameraPos.z - eyeZ;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        float targetPitch = (float) -(Mth.atan2(dy, horizontalDist) * (180D / Math.PI));

        float bodyYaw = entity.yBodyRot;
        float relativeYaw = Mth.wrapDegrees(targetYaw - bodyYaw);
        relativeYaw = Mth.clamp(relativeYaw, -75.0F, 75.0F);
        float relativePitch = Mth.clamp(targetPitch, -60.0F, 60.0F);

        model.head.yRot = relativeYaw * Mth.DEG_TO_RAD;
        model.head.xRot = relativePitch * Mth.DEG_TO_RAD;
    }
}