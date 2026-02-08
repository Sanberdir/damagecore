package ru.imaginaerum.damagecore.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;

@Mixin(PowderSnowBlock.class)
public class ColdDamagePowderSnowMixin {

    @Inject(
            method = "entityInside",
            at = @At("HEAD")
    )
    private void damagecore$entityInside(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {

        if (!(entity instanceof LivingEntity living)) return;

        // помечаем следующий урон как COLD
        if (living instanceof IHasDamageType has) {
            has.setLastDamageType(DamageType.COLD);
        }

        // можно добавить в контекст — как ты делаешь для огня
        DamageContext.add(living, DamageType.COLD, 1.0F);
    }
}
