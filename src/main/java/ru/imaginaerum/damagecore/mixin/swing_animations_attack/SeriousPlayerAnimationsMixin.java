package ru.imaginaerum.damagecore.mixin.swing_animations_attack;

import com.mojang.authlib.GameProfile;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.animation_attack.IExampleAnimatedPlayer;
import ru.imaginaerum.damagecore.animation_attack.torsoPosGetter;

import static dev.kosmx.playerAnim.core.util.Ease.INOUTSINE;

@Mixin(AbstractClientPlayer.class)
public abstract class SeriousPlayerAnimationsMixin extends Player
        implements IExampleAnimatedPlayer, torsoPosGetter {

    public SeriousPlayerAnimationsMixin(Level level, BlockPos pos,
                                        float yRot, GameProfile profile) {
        super(level, pos, yRot, profile);
    }

    @Unique
    private final dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier disableRightItem =
            new dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier(partName -> {

                if (partName.equals("rightItem")) {
                    return java.util.Optional.of(
                            new dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier.PartModifier(
                                    new Vec3f(-1, 0, 0),
                                    new Vec3f(0, 0, 0)
                            )
                    );
                }

                return java.util.Optional.empty();
            });

    @Overwrite
    public boolean isSpectator() {
        return false;
    }

    @Overwrite
    public boolean isCreative() {
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // Sword swing animation
    // ─────────────────────────────────────────────────────────────

    @Unique
    private final ModifierLayer<IAnimation> swordSwingContainer =
            new ModifierLayer<>();

    @Unique
    private KeyframeAnimation sword_swing = null;

    @Unique
    private boolean swordSwingRequested = false;

    // ─────────────────────────────────────────────────────────────
    // Strong attack animation
    // ─────────────────────────────────────────────────────────────



    @Unique
    private KeyframeAnimation strong_attack = null;

    @Unique
    private boolean strongAttackRequested = false;

    // ─────────────────────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────────────────────

    @Unique
    private boolean animationsInitialized = false;

    // Блокирует обычный swing после strong attack
    @Unique
    private boolean preventNextSwordSwing = false;

    // ─────────────────────────────────────────────────────────────
    // Sword swing request
    // ─────────────────────────────────────────────────────────────

    @Override
    public void requestSwordSwing() {

        if (this.preventNextSwordSwing) {
            this.preventNextSwordSwing = false;
            return;
        }

        this.swordSwingRequested = true;
    }

    @Override
    public boolean consumeSwordSwingRequest() {
        if (this.swordSwingRequested) {
            this.swordSwingRequested = false;
            return true;
        }

        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // Strong attack request
    // ─────────────────────────────────────────────────────────────

    @Override
    public void requestStrongAttack() {
        this.preventNextSwordSwing = true;
        this.strongAttackRequested = true;
    }

    @Override
    public boolean consumeStrongAttackRequest() {
        if (this.strongAttackRequested) {
            this.strongAttackRequested = false;
            return true;
        }

        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // Tick
    // ─────────────────────────────────────────────────────────────

    @Inject(method = "tick", at = @At("HEAD"))
    private void damagecore$onTick(CallbackInfo ci) {

        AbstractClientPlayer self =
                (AbstractClientPlayer) (Object) this;

        if (!animationsInitialized) {

            sword_swing = PlayerAnimationRegistry.getAnimation(
                    new ResourceLocation(
                            "damagecore",
                            "sword_swing"
                    )
            );

            strong_attack = PlayerAnimationRegistry.getAnimation(
                    new ResourceLocation(
                            "damagecore",
                            "strong_attack"
                    )
            );

            PlayerAnimationAccess.getPlayerAnimLayer(self)
                    .addAnimLayer(10, swordSwingContainer);

            swordSwingContainer.addModifierLast(disableRightItem);

            animationsInitialized = true;
        }

        // Strong attack
        if (consumeStrongAttackRequest() && strong_attack != null) {

            swordSwingContainer.replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(
                            0,
                            INOUTSINE
                    ),
                    new KeyframeAnimationPlayer(strong_attack)
            );

            return;
        }

        // Normal attack
        if (consumeSwordSwingRequest() && sword_swing != null) {

            swordSwingContainer.replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(
                            0,
                            INOUTSINE
                    ),
                    new KeyframeAnimationPlayer(sword_swing)
            );
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Interface stubs
    // ─────────────────────────────────────────────────────────────

    @Override
    public ModifierLayer<IAnimation> seriousplayeranimations_getModAnimation() {
        return null;
    }

    @Override
    public Vec3f getTorsoPos() {
        return null;
    }

    @Override
    public Vec3f getTorsoRotation() {
        return null;
    }

    @Override
    public void disableArms(boolean b) {}

    @Override
    public void disableLeftArmB(boolean b) {}

    @Override
    public void disableRightArmB(boolean b) {}

    @Override
    public void disableMainArmB(boolean b) {}

    @Override
    public void disableOffArmB(boolean b) {}

    @Override
    public void disableAnimationB(boolean b) {}

    @Override
    public void disableOverlayB(boolean b) {}

    @Override
    public void armPosMain(HumanoidModel.ArmPose pos) {}

    @Override
    public void armPosOff(HumanoidModel.ArmPose pos) {}
}