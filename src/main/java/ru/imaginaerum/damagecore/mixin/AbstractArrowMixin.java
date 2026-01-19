package ru.imaginaerum.damagecore.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreSource;

@Mixin(AbstractArrow.class)
public class AbstractArrowMixin {
    // колющий урон 2, как в ваниле
    @ModifyArg(
            method = "onHitEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            ),
            index = 0
    )
    private DamageSource damagecore$replaceArrowDamageSource(DamageSource original) {

        IDamageCoreSource dcSource = (IDamageCoreSource) original;
        dcSource.damagecore$setDamageType(DamageType.PIERCING);

        return original;
    }
}
