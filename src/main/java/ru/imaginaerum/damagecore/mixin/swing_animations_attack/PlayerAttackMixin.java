package ru.imaginaerum.damagecore.mixin.swing_animations_attack;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.animation_attack.ICurrentAttackType;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;
import ru.imaginaerum.damagecore.library_damage.TypedDamageSource;

import java.util.Map;

@Mixin(Player.class)
public class PlayerAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void damagecore$attack(Entity target, CallbackInfo ci) {

        Player self = (Player)(Object)this;

        if (self.level().isClientSide()) return;

        if (!(self instanceof ICurrentAttackType typeHolder)) {
            return;
        }

        if (!(self.getMainHandItem().getItem() instanceof IDamageCoreWeapon weapon)) {

            return;
        }

        if (!(target instanceof LivingEntity living)) {
            return;
        }

        DamageType type = typeHolder.damagecore$getCurrentAttackType();

        Map<DamageType, Double> damageMap = weapon.damagecore$getDamageMap();

        double damage = damageMap.getOrDefault(type, 0.0);

        if (damage <= 0) {
            return;
        }

        ci.cancel();

        self.resetAttackStrengthTicker();
    }
}