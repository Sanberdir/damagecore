package ru.imaginaerum.damagecore.mixin.damage_poison_mixins;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;

@Mixin(MobEffectInstance.class)
public class PoisonTickMixin {

    @Shadow private MobEffect effect;
    @Shadow private int duration;

    @Unique
    private int damagecore_poisonSkipTicks = 0;

    @Inject(
            method = "applyEffect",   // метод, который дёргает applyEffectTick внутри себя
            at = @At("HEAD"),
            cancellable = true
    )
    private void damagecore_throttlePoisonTick(LivingEntity entity, CallbackInfo ci) {
        if (effect != MobEffects.POISON) return;
        if (!(entity instanceof Player player)) return;
        if (entity.level().isClientSide) return;

        int liveForce = PlayerStatsCapability.get(player)
                .map(s -> s.getStat(StatsType.LIVE_FORCE)).orElse(0);
        if (liveForce <= 0) return;

        // Каждый уровень LIVE_FORCE добавляет 1 пропущенный тик между тиками урона
        // т.е. liveForce=1 → урон каждые 2 вызова, liveForce=3 → каждые 4
        if (damagecore_poisonSkipTicks < liveForce) {
            damagecore_poisonSkipTicks++;
            ci.cancel(); // пропускаем этот тик урона
        } else {
            damagecore_poisonSkipTicks = 0; // пропускать больше не надо, урон проходит
        }
    }
}