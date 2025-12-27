package ru.imaginaerum.damagecore.effect.effects;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import ru.imaginaerum.damagecore.datagen.DamageTypesGenerator;
import ru.imaginaerum.damagecore.library_damage.Bleeding_2_DamageSource;
import ru.imaginaerum.damagecore.library_damage.DeathPoisonDamageSource;

public class Bleeding2Effect extends MobEffect {
    public Bleeding2Effect(MobEffectCategory category, int color) {
        super(category, color);
    }
     @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int interval = 25 >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;

        float damage = 1.5F;

        // Проверяем, доступен ли DamageType
        var registry = entity.level().registryAccess().registry(Registries.DAMAGE_TYPE);
        if (registry.isPresent()) {
            var damageTypeHolder = registry.get().getHolder(DamageTypesGenerator.BLEEDING_2);

            if (damageTypeHolder.isPresent()) {
                // Используем кастомный DamageType
                Entity sourceEntity = entity.getLastAttacker();
                if (sourceEntity == null) {
                    sourceEntity = entity;
                }

                DamageSource damageSource = new Bleeding_2_DamageSource(damageTypeHolder.get(), sourceEntity);
                entity.hurt(damageSource, damage);
                return;
            }
        }

        // Fallback на магический урон
        DamageSource fallbackSource;
        if (entity.getLastAttacker() != null) {
            fallbackSource = entity.damageSources().indirectMagic(entity.getLastAttacker(), entity);
        } else {
            fallbackSource = entity.damageSources().magic();
        }
        entity.hurt(fallbackSource, damage);
    }
}
