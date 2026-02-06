package ru.imaginaerum.damagecore.mixin.damage_explosion_mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;

@Mixin(Explosion.class)
public class ExplosionDamageMixin {

    @Redirect(
            method = "explode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean damagecore$onEntityHurt(Entity targetEntity, DamageSource source, float amount) {
        if (targetEntity instanceof LivingEntity living) {
            if (living instanceof IHasDamageType has) {
                // Можно запомнить последний урон как комбинированный тип — либо хранить список, либо как специальный "multi" тип
                // Здесь просто пример: запишем BLUDGEONING (основной) и добавим FIRE в DamageContext
                has.setLastDamageType(DamageType.BLUDGEONING);
            }

            // Добавляем оба типа в DamageContext
            DamageContext.add(living, DamageType.BLUDGEONING, amount * 0.5f); // половина урона BLUDGEONING
            DamageContext.add(living, DamageType.FIRE, amount * 0.5f);         // половина урона FIRE
        }

        return targetEntity.hurt(source, amount);
    }
}
