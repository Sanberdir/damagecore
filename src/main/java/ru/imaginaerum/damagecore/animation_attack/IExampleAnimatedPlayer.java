package ru.imaginaerum.damagecore.animation_attack;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import net.minecraft.client.model.HumanoidModel;

public interface IExampleAnimatedPlayer {

    ModifierLayer<IAnimation> seriousplayeranimations_getModAnimation();

    void requestSwordSwing();
    boolean consumeSwordSwingRequest();
    void requestStrongAttack();
    boolean consumeStrongAttackRequest();
    void disableArms(boolean b);
    void disableLeftArmB(boolean b);
    void disableRightArmB(boolean b);
    void disableMainArmB(boolean b);
    void disableOffArmB(boolean b);
    void disableAnimationB(boolean b);
    void disableOverlayB(boolean b);
    boolean damagecore$isHeadLookAtCameraActive(); // новое


    void armPosMain(HumanoidModel.ArmPose pos);
    void armPosOff(HumanoidModel.ArmPose pos);
}