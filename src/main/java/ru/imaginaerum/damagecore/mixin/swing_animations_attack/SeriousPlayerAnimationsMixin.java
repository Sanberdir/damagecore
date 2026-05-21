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
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.PacketSyncAttackType;

import com.mojang.authlib.GameProfile;

import static dev.kosmx.playerAnim.core.util.Ease.INOUTSINE;

@Mixin(AbstractClientPlayer.class)
public abstract class SeriousPlayerAnimationsMixin extends Player
        implements IExampleAnimatedPlayer, torsoPosGetter, ICurrentAttackType {

    public SeriousPlayerAnimationsMixin(Level level, BlockPos pos, float yRot, GameProfile profile) {
        super(level, pos, yRot, profile);
    }

    @Unique
    private DamageType currentAttackType = DamageType.SLASHING;

    @Override
    public void damagecore$setCurrentAttackType(DamageType type) {
        this.currentAttackType = type;
    }

    @Override
    public DamageType damagecore$getCurrentAttackType() {
        return this.currentAttackType;
    }

    @Unique
    private int pendingSwingIndex = 0;

    @Unique
    private final ModifierLayer<IAnimation> swordSwingContainer = new ModifierLayer<>();

    @Unique
    private KeyframeAnimation[] swing_anims;

    @Unique
    private KeyframeAnimation strong_attack;

    @Unique
    private HoldLastFramePlayer currentSwingPlayer;

    @Unique
    private int swingIndex = 0;

    @Unique
    private boolean initialized = false;

    @Unique
    private boolean swordSwingRequested = false;

    @Unique
    private boolean strongAttackRequested = false;

    @Unique
    private boolean preventNextSwing = false;

    @Unique
    private DamageType damagecore$getAttackTypeForIndex(int index) {
        return switch (index) {
            case 1 -> DamageType.PIERCING; // fa_2
            default -> DamageType.SLASHING; // fa_1 и fa_3
        };
    }

    @Override
    public void requestSwordSwing() {
        if (preventNextSwing) { preventNextSwing = false; return; }

        pendingSwingIndex = swingIndex;
        DamageType type = damagecore$getAttackTypeForIndex(pendingSwingIndex);

        // ❌ убрать: ModNetwork.CHANNEL.sendToServer(new PacketSyncAttackType(type));

        // Сохраняем тип локально (для чтения в LocalPlayerAttackMixin)
        this.currentAttackType = type;

        swordSwingRequested = true;
    }

    @Override
    public boolean consumeSwordSwingRequest() {
        if (swordSwingRequested) {
            swordSwingRequested = false;
            return true;
        }
        return false;
    }

    @Override
    public void requestStrongAttack() {
        preventNextSwing = true;
        strongAttackRequested = true;
    }

    @Override
    public boolean consumeStrongAttackRequest() {
        if (strongAttackRequested) {
            strongAttackRequested = false;
            return true;
        }
        return false;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void damagecore$tick(CallbackInfo ci) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;

        if (!initialized) {
            swing_anims = new KeyframeAnimation[] {
                    PlayerAnimationRegistry.getAnimation(new ResourceLocation("damagecore", "fa_1")),
                    PlayerAnimationRegistry.getAnimation(new ResourceLocation("damagecore", "fa_2")),
                    PlayerAnimationRegistry.getAnimation(new ResourceLocation("damagecore", "fa_3"))
            };

            strong_attack =
                    PlayerAnimationRegistry.getAnimation(new ResourceLocation("damagecore", "strong_attack"));

            PlayerAnimationAccess.getPlayerAnimLayer(self)
                    .addAnimLayer(10, swordSwingContainer);

            initialized = true;
        }

        // STRONG ATTACK
        if (consumeStrongAttackRequest() && strong_attack != null) {
            swordSwingContainer.setAnimation(null);
            swordSwingContainer.replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(0, INOUTSINE),
                    new KeyframeAnimationPlayer(strong_attack)
            );
            return;
        }

        // NORMAL SWING
        if (consumeSwordSwingRequest() && swing_anims != null) {
            int idx = pendingSwingIndex;
            System.out.println("[TICK] играем idx=" + idx + ", swingIndex=" + swingIndex);

            KeyframeAnimation anim = swing_anims[idx];

            // следующий удар по кругу: 0 -> 1 -> 2 -> 0
            swingIndex = (idx + 1) % swing_anims.length;

            if (anim != null) {
                swordSwingContainer.setAnimation(null);
                currentSwingPlayer = new HoldLastFramePlayer(anim);

                // Сброс комбо только после ПОСЛЕДНЕЙ анимации
                if (idx == swing_anims.length - 1) {
                    currentSwingPlayer.setOnExpired(() -> swingIndex = 0);
                }

                swordSwingContainer.replaceAnimationWithFade(
                        AbstractFadeModifier.standardFadeIn(0, INOUTSINE),
                        currentSwingPlayer
                );
            }
        }
    }

    @Override public ModifierLayer<IAnimation> seriousplayeranimations_getModAnimation() { return null; }
    @Override public Vec3f getTorsoPos() { return null; }
    @Override public Vec3f getTorsoRotation() { return null; }

    @Override public void disableArms(boolean b) {}
    @Override public void disableLeftArmB(boolean b) {}
    @Override public void disableRightArmB(boolean b) {}
    @Override public void disableMainArmB(boolean b) {}
    @Override public void disableOffArmB(boolean b) {}
    @Override public void disableAnimationB(boolean b) {}
    @Override public void disableOverlayB(boolean b) {}
    @Override public void armPosMain(net.minecraft.client.model.HumanoidModel.ArmPose pos) {}
    @Override public void armPosOff(net.minecraft.client.model.HumanoidModel.ArmPose pos) {}
}