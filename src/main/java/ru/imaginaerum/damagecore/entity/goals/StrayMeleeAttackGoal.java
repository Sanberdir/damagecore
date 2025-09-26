package ru.imaginaerum.damagecore.entity.goals;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Stray;

public class StrayMeleeAttackGoal extends MeleeAttackGoal {
    private final Stray stray;

    public StrayMeleeAttackGoal(Stray stray, double speedModifier, boolean pauseWhenIdle) {
        super(stray, speedModifier, pauseWhenIdle);
        this.stray = stray;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distanceSquared) {
        super.checkAndPerformAttack(target, distanceSquared);

        // если реально ударил
        if (this.mob.swinging && target.isAlive()) {
            // накладываем замедление, как у стрел
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    200, // 10 секунд (200 тиков)
                    0    // уровень эффекта (0 = Slowness I)
            ));
        }
    }
}
