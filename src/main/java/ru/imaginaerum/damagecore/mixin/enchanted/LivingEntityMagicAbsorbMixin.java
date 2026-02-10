package ru.imaginaerum.damagecore.mixin.enchanted;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.api.damage_book_protection.protection_helpers.ProtectionHelper;
import ru.imaginaerum.damagecore.library_damage.DamageType;

@Mixin(LivingEntity.class)
public class LivingEntityMagicAbsorbMixin {

    @Inject(
            method = "getDamageAfterMagicAbsorb",
            at = @At("HEAD"),
            cancellable = true
    )
    private void damagecore_overrideProtection(
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Float> cir
    ) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (!(entity instanceof IHasDamageType holder)) {
            return;
        }
        DamageType type = holder.getLastDamageType();
        if (type == null) {
            return;
        }
        // только физические типы — остальное без Protection
        if (type != DamageType.PIERCING
                && type != DamageType.SLASHING
                && type != DamageType.BLUDGEONING) {
            cir.setReturnValue(damage);
            return;
        }
        float reduction = ProtectionHelper
                .getEnchantProtectionPercent(entity, type);

        if (reduction <= 0f) {
            cir.setReturnValue(damage);
            return;
        }
        if (reduction > 1f) reduction = 1f;

        cir.setReturnValue(damage * (1.0f - reduction));
    }

}