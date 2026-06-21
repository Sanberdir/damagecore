package ru.imaginaerum.damagecore.mixin.suffocation_mixins;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;

@Mixin(LivingEntity.class)
public class LivingEntitySuffocationMixin {

    @Unique
    private int damagecore_drownHoldTicks = 0;

    @Unique
    private int damagecore_wallHoldTicks = 0;

    @Inject(
            method = "hurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void damagecore_setSuffocationDamageType(
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity entity = (LivingEntity)(Object)this;

        if (!(entity instanceof IHasDamageType holder)) return;

        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)) {
            holder.setLastDamageType(DamageType.SUFFOCATION);
        }

        // Задержка урона от стены через Endurance
        if (source.is(DamageTypes.IN_WALL) && entity instanceof Player player) {
            if (entity.level().isClientSide) return;

            int enduranceLevel = PlayerStatsCapability.get(player)
                    .map(s -> s.getStat(StatsType.ENDURANCE)).orElse(0);
            if (enduranceLevel <= 0) return;

            int delay = enduranceLevel * 5;

            if (damagecore_wallHoldTicks < delay) {
                damagecore_wallHoldTicks++;
                cir.setReturnValue(false);
                cir.cancel();
            } else {
                damagecore_wallHoldTicks = 0;
            }
        }
    }

    @Inject(method = "baseTick", at = @At("HEAD"))
    private void damagecore_delayAirDamage(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (entity.level().isClientSide) return;

        if (entity.getAirSupply() >= 0) {
            damagecore_drownHoldTicks = 0;
            return;
        }

        if (!(entity instanceof Player player)) return;

        int enduranceLevel = PlayerStatsCapability.get(player)
                .map(s -> s.getStat(StatsType.ENDURANCE)).orElse(0);
        if (enduranceLevel <= 0) return;

        int delay = enduranceLevel * 5;

        // airSupply == -19 значит следующий тик ваниль доведёт до -20 и нанесёт урон
        // Перехватываем заранее
        if (entity.getAirSupply() == -19) {
            if (damagecore_drownHoldTicks < delay) {
                // Удерживаем на -18, чтобы ваниль не дошла до -20 в этом тике
                entity.setAirSupply(-18);
                damagecore_drownHoldTicks++;
            } else {
                damagecore_drownHoldTicks = 0;
            }
        }
    }

}