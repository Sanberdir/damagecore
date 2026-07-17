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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.animation_attack.HoldLastFramePlayer;
import ru.imaginaerum.damagecore.animation_attack.ICurrentAttackType;
import ru.imaginaerum.damagecore.animation_attack.IExampleAnimatedPlayer;
import ru.imaginaerum.damagecore.animation_attack.torsoPosGetter;
import ru.imaginaerum.damagecore.animation_attack.types.SwingAnimationEntry;
import ru.imaginaerum.damagecore.animation_attack.types.WeaponAnimationManager;
import ru.imaginaerum.damagecore.animation_attack.types.WeaponAnimationSet;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_weapon_types.WeaponType;
import ru.imaginaerum.damagecore.library_weapon_types.WeaponTypeManager;

import static dev.kosmx.playerAnim.core.util.Ease.INOUTSINE;

@Mixin(AbstractClientPlayer.class)
public abstract class SeriousPlayerAnimationsMixin extends Player
        implements IExampleAnimatedPlayer, torsoPosGetter, ICurrentAttackType {

    public SeriousPlayerAnimationsMixin(Level level, BlockPos pos, float yRot, GameProfile profile) {
        super(level, pos, yRot, profile);
    }

    @Unique private DamageType currentAttackType = DamageType.SLASHING;
    @Unique private int pendingSwingIndex = 0, swingIndex = 0;
    @Unique private final ModifierLayer<IAnimation> swordSwingContainer = new ModifierLayer<>();
    @Unique private HoldLastFramePlayer currentSwingPlayer;
    @Unique private boolean initialized = false, swordSwingRequested = false, strongAttackRequested = false, preventNextSwing = false;
    @Unique private boolean pendingReturnToIdle = false;
    @Override public void damagecore$setCurrentAttackType(DamageType type) { currentAttackType = type; }
    @Override public DamageType damagecore$getCurrentAttackType() { return currentAttackType; }
    @Unique private boolean attackInProgress = false;
    @Unique private WeaponType damagecore$currentWeaponType = null;
    @Unique private SwingAnimationEntry[] damagecore$swingEntries = new SwingAnimationEntry[0];
    @Unique private KeyframeAnimation[] swing_anims = new KeyframeAnimation[0];
    @Unique private KeyframeAnimation strong_attack;
    @Unique private boolean headLookAtCameraActive = false;

    @Override
    public boolean damagecore$isHeadLookAtCameraActive() {
        return headLookAtCameraActive;
    }
    @Unique
    private void damagecore$refreshAnimationsIfNeeded(AbstractClientPlayer self) {
        ResourceLocation itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getKey(self.getMainHandItem().getItem());
        WeaponType type = itemId != null ? WeaponTypeManager.INSTANCE.getType(itemId) : null;

        if (type == damagecore$currentWeaponType) return;
        damagecore$currentWeaponType = type;

        WeaponAnimationSet set = WeaponAnimationManager.INSTANCE.getAnimationSet(type);
        if (set == null) {
            damagecore$swingEntries = new SwingAnimationEntry[0];
            swing_anims = new KeyframeAnimation[0];
            strong_attack = null;
            swingIndex = 0;
            return;
        }

        damagecore$swingEntries = set.swingAnimations().toArray(new SwingAnimationEntry[0]);
        swing_anims = new KeyframeAnimation[damagecore$swingEntries.length];
        for (int i = 0; i < damagecore$swingEntries.length; i++) {
            swing_anims[i] = PlayerAnimationRegistry.getAnimation(damagecore$swingEntries[i].animation());
        }
        strong_attack = set.strongAttack() != null
                ? PlayerAnimationRegistry.getAnimation(set.strongAttack())
                : null;

        swingIndex = swing_anims.length > 0 ? swingIndex % swing_anims.length : 0;
    }
    @Unique
    private DamageType damagecore$getAttackTypeForIndex(int index) {
        if (index < 0 || index >= damagecore$swingEntries.length) return DamageType.SLASHING;
        return damagecore$swingEntries[index].damageType();
    }

    @Override
    public void requestSwordSwing() {
        if (attackInProgress) return;
        if (preventNextSwing) { preventNextSwing = false; return; }
        pendingSwingIndex = swingIndex;
        currentAttackType = damagecore$getAttackTypeForIndex(pendingSwingIndex);
        swordSwingRequested = true;
    }

    @Override
    public boolean consumeSwordSwingRequest() {
        if (!swordSwingRequested) return false;
        swordSwingRequested = false;
        return true;
    }

    @Override
    public void requestStrongAttack() {
        if (attackInProgress) return;
        preventNextSwing = true;
        strongAttackRequested = true;
    }

    @Override
    public boolean consumeStrongAttackRequest() {
        if (!strongAttackRequested) return false;
        strongAttackRequested = false;
        return true;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void damagecore$tick(CallbackInfo ci) {
        AbstractClientPlayer self = (AbstractClientPlayer)(Object)this;

        if (pendingReturnToIdle) {
            pendingReturnToIdle = false;
            swingIndex = 0;
            headLookAtCameraActive = false; // сброс
            swordSwingContainer.replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(8, INOUTSINE),
                    null
            );
        }

        if (!initialized) {
            PlayerAnimationAccess.getPlayerAnimLayer(self).addAnimLayer(10, swordSwingContainer);
            initialized = true;
        }

        damagecore$refreshAnimationsIfNeeded(self);

        if (consumeStrongAttackRequest() && strong_attack != null) {
            headLookAtCameraActive = false; // сильная атака не смотрит в камеру
            swordSwingContainer.replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(0, INOUTSINE),
                    new KeyframeAnimationPlayer(strong_attack)
            );
            return;
        }

        if (consumeSwordSwingRequest() && swing_anims.length > 0) {
            int idx = pendingSwingIndex;
            if (idx >= swing_anims.length) idx = 0;

            KeyframeAnimation anim = swing_anims[idx];
            swingIndex = (idx + 1) % swing_anims.length;

            headLookAtCameraActive = damagecore$swingEntries[idx].headLookAtCamera(); // новое

            if (anim != null) {
                attackInProgress = true;
                currentSwingPlayer = new HoldLastFramePlayer(anim);
                currentSwingPlayer.setOnAnimationDone(() -> attackInProgress = false);
                currentSwingPlayer.setOnExpired(() -> pendingReturnToIdle = true);

                swordSwingContainer.replaceAnimationWithFade(
                        AbstractFadeModifier.standardFadeIn(0, INOUTSINE),
                        currentSwingPlayer
                );
            }
        }
    }

    @Override public ModifierLayer<IAnimation> seriousplayeranimations_getModAnimation() { return swordSwingContainer; }

    @Override public Vec3f getTorsoPos() { return null; }
    @Override public Vec3f getTorsoRotation() { return null; }

    @Override public void disableArms(boolean b) {}
    @Override public void disableLeftArmB(boolean b) {}
    @Override public void disableRightArmB(boolean b) {}
    @Override public void disableMainArmB(boolean b) {}
    @Override public void disableOffArmB(boolean b) {}
    @Override public void disableAnimationB(boolean b) {}
    @Override public void disableOverlayB(boolean b) {}
    @Override public void armPosMain(HumanoidModel.ArmPose pos) {}
    @Override public void armPosOff(HumanoidModel.ArmPose pos) {}
}