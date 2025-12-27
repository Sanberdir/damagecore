package ru.imaginaerum.damagecore.effect.effects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.effect.DCEffects;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BleedingHandler {

    private static final int BLEEDING_2_DURATION_TICKS = 15 * 20; // 15 секунд

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        // Проверяем Кровотечение I
        MobEffectInstance bleeding1 = entity.getEffect(DCEffects.BLEEDING_1.get());
        if (bleeding1 != null) {
            // Эффект должен длиться 20 секунд. Начальная длительность - 400 тиков.
            // Когда прошло 20 секунд, оставшаяся длительность будет 0.
            // Проверяем, что эффект вот-вот закончится.
            if (bleeding1.getDuration() <= 1) {
                // Заменяем на Кровотечение II со стандартной длительностью 15 секунд
                replaceBleedingEffect(entity, bleeding1, DCEffects.BLEEDING_2.get(), BLEEDING_2_DURATION_TICKS);
            }
            return; // Если есть Кровотечение I, другие проверки не нужны
        }

        // Проверяем Кровотечение II
        MobEffectInstance bleeding2 = entity.getEffect(DCEffects.BLEEDING_2.get());
        if (bleeding2 != null) {
            // Эффект должен длиться 15 секунд. Начальная длительность - 300 тиков.
            if (bleeding2.getDuration() <= 1) {
                // Заменяем на Кровотечение III с "бесконечной" длительностью
                replaceBleedingEffect(entity, bleeding2, DCEffects.BLEEDING_3.get(), Integer.MAX_VALUE);
            }
        }
    }

    /**
     * Безопасно заменяет один эффект кровотечения на другой, сохраняя усилитель и другие свойства.
     *
     * @param entity Сущность, на которую накладывается эффект.
     * @param oldEffect Старый экземпляр эффекта для получения свойств.
     * @param newEffect Новый эффект для наложения.
     * @param newDuration Новая длительность эффекта в тиках.
     */
    private static void replaceBleedingEffect(LivingEntity entity, MobEffectInstance oldEffect, net.minecraft.world.effect.MobEffect newEffect, int newDuration) {
        // Снимаем старый эффект
        entity.removeEffect(oldEffect.getEffect());

        // Накладываем новый эффект, сохраняя усилитель и другие флаги
        entity.addEffect(new MobEffectInstance(
                newEffect,
                newDuration,
                oldEffect.getAmplifier(),
                oldEffect.isAmbient(),
                oldEffect.isVisible(),
                oldEffect.showIcon()
        ));
    }
}