// src/main/java/ru/imaginaerum/damagecore/effect/effects/DrunkennessHandler.java
package ru.imaginaerum.damagecore.effect.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore; // Ваш главный класс мода
import ru.imaginaerum.damagecore.effect.DCEffects;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, value = Dist.CLIENT)
public class DrunkennessHandler {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Проверяем, что это нужная нам стадия рендеринга.
        // AFTER_SKY - идеальная стадия для искажения камеры.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        // Проверяем, есть ли у игрока эффект опьянения
        if (player != null && player.hasEffect(DCEffects.DRUNKENNESS_1.get())) {
            int amplifier = player.getEffect(DCEffects.DRUNKENNESS_1.get()).getAmplifier();

            // Если есть и эффект тошноты, не делаем ничего, чтобы избежать конфликта
            if (player.hasEffect(MobEffects.CONFUSION)) {
                return;
            }

            // Получаем время для анимации волны
            long time = System.currentTimeMillis();

            // Вычисляем интенсивность волны. Она зависит от уровня эффекта (amplifier)
            float intensity = (amplifier + 1) * 0.15f;

            // Создаем "волну" по осям X и Y (наклон)
            float waveX = (float) Math.sin(time * 0.001) * intensity;
            float waveY = (float) Math.cos(time * 0.001 + 100) * intensity * 0.5f;

            // Создаем "дрожание" (zoom/quake)
            float shake = (float) Math.sin(time * 0.01) * intensity * 0.1f;

            // Получаем PoseStack из события
            PoseStack poseStack = event.getPoseStack();

            // ВАЖНО: Мы не должны использовать pushPose() и popPose() здесь,
            // так как это изменит матрицу для всех последующих рендереров в этом кадре,
            // что может привести к ошибкам. Мы применяем трансформации напрямую к текущей матрице.
            // Это эквивалентно изменению матрицы вида (View Matrix).

            // Небольшое "приближение/отдаление" для дрожания
            poseStack.scale(1.0f + shake, 1.0f + shake, 1.0f);

            // Наклон по осям X и Y для создания волны
            poseStack.mulPose(Axis.XP.rotationDegrees(waveY * 10.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(waveX * 10.0f));
        }
    }
}