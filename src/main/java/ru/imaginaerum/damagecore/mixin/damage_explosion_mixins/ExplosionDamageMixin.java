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
        // targetEntity — это та самая сущность, которой наносят урон (жертва)
        if (targetEntity instanceof LivingEntity living) {
            // проставим тип уронa на жертве (если она реализует интерфейс)
            if (living instanceof IHasDamageType has) {
                has.setLastDamageType(DamageType.FORCE);
            }
            // положим в DamageContext реальный урон (amount)
            DamageContext.add(living, DamageType.FORCE, amount);

        }

        // очень важно: вызвать оригинальный метод, чтобы урон применился
        return targetEntity.hurt(source, amount);
    }
}
