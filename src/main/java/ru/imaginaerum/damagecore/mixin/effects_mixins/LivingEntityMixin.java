package ru.imaginaerum.damagecore.mixin.effects_mixins;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Полностью отключает ванильное магическое снижение урона
 * (включая эффект Resistance) внутри getDamageAfterMagicAbsorb.
 *
 * Броня НЕ затрагивается — только эффекты/магия.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(
            method = "getDamageAfterMagicAbsorb",
            at = @At("HEAD"),
            cancellable = true
    )
    private void damagecore$disableVanillaResistance(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Float> cir
    ) {
        // Просто возвращаем исходный урон без магического снижения
        cir.setReturnValue(amount);
    }
}
