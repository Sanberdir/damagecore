package ru.imaginaerum.damagecore.mixin.enchanted;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ru.imaginaerum.damagecore.api.IHasDamageType;
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
            return; // не наша сущность — не лезем
        }

        DamageType type = holder.getLastDamageType();
        if (type == null) {
            return;
        }

        // Только физика
        if (type != DamageType.PIERCING
                && type != DamageType.SLASHING
                && type != DamageType.BLUDGEONING) {
            cir.setReturnValue(damage); // vanilla protection отключён
            return;
        }

        int level = EnchantmentHelper.getEnchantmentLevel(
                Enchantments.ALL_DAMAGE_PROTECTION,
                entity
        );

        if (level <= 0) {
            cir.setReturnValue(damage);
            return;
        }

        float reduction = level * 0.05f;
        float result = damage * (1.0f - reduction);

        cir.setReturnValue(result);
    }
}
