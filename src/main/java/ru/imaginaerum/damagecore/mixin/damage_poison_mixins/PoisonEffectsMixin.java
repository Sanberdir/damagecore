package ru.imaginaerum.damagecore.mixin.damage_poison_mixins;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import javax.annotation.Nullable;

import ru.imaginaerum.damagecore.api.IHasDamageType;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;

@Mixin(MobEffect.class)
public class PoisonEffectsMixin {

    @Redirect(
            method = "applyEffectTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean damagecore$onPoisonEffectTick(LivingEntity entity, DamageSource source, float amount) {
        MobEffect effect = (MobEffect)(Object)this;

        // Обрабатываем обычный Poison эффект
        if (effect == MobEffects.POISON && entity.getHealth() > 1.0F) {
            return handlePoisonDamage(entity, source, amount);
        }



        // Обрабатываем Harm эффект в applyEffectTick
        if ((effect == MobEffects.HARM && !entity.isInvertedHealAndHarm()) ||
                (effect == MobEffects.HEAL && entity.isInvertedHealAndHarm())) {
            return handlePoisonDamage(entity, source, amount);
        }

        return entity.hurt(source, amount);
    }

    @Redirect(
            method = "applyInstantenousEffect",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean damagecore$onInstantEffect(LivingEntity target, DamageSource source, float amount) {
        MobEffect effect = (MobEffect)(Object)this;

        // Обрабатываем мгновенный Harm эффект
        if ((effect == MobEffects.HARM && !target.isInvertedHealAndHarm()) ||
                (effect == MobEffects.HEAL && target.isInvertedHealAndHarm())) {
            return handlePoisonDamage(target, source, amount);
        }

        return target.hurt(source, amount);
    }

    private boolean handlePoisonDamage(LivingEntity entity, DamageSource source, float amount) {
        // Устанавливаем тип урона POISON на жертве
        if (entity instanceof IHasDamageType hasDamageType) {
            hasDamageType.setLastDamageType(DamageType.POISON);
        }

        // Сохраняем информацию об уроне в DamageContext
        DamageContext.add(entity, DamageType.POISON, amount);

        // Вызываем оригинальный метод
        return entity.hurt(source, amount);
    }
}