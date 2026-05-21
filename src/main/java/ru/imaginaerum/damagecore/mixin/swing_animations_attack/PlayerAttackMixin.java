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

        System.out.println("[DC] attack() called, isClientSide=" + self.level().isClientSide());

        if (self.level().isClientSide()) return;

        if (!(self instanceof ICurrentAttackType typeHolder)) {
            System.out.println("[DC] FAIL: player не реализует ICurrentAttackType");
            return;
        }

        if (!(self.getMainHandItem().getItem() instanceof IDamageCoreWeapon weapon)) {
            System.out.println("[DC] FAIL: предмет не IDamageCoreWeapon, item="
                    + self.getMainHandItem().getItem());
            return;
        }

        if (!(target instanceof LivingEntity living)) {
            System.out.println("[DC] FAIL: цель не LivingEntity");
            return;
        }

        DamageType type = typeHolder.damagecore$getCurrentAttackType();
        System.out.println("[DC] attackType=" + type);

        Map<DamageType, Double> damageMap = weapon.damagecore$getDamageMap();
        System.out.println("[DC] damageMap=" + damageMap);

        double damage = damageMap.getOrDefault(type, 0.0);
        System.out.println("[DC] damage для типа " + type + " = " + damage);

        if (damage <= 0) {
            System.out.println("[DC] FAIL: damage <= 0, урон не наносится");
            return;
        }

        ci.cancel();

        TypedDamageSource source = new TypedDamageSource(
                self.level().damageSources().playerAttack(self).typeHolder(),
                type,
                self
        );

        boolean hurt = living.hurt(source, (float) damage);
        System.out.println("[DC] living.hurt() вернул " + hurt + ", target=" + living);

        self.resetAttackStrengthTicker();
    }
}