// src/main/java/ru/imaginaerum/damagecore/effect/effects/Drunkenness2Handler.java
package ru.imaginaerum.damagecore.effect.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.effect.DCEffects;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, value = Dist.CLIENT)
public class Drunkenness2Handler { // Изменено с DrunkennessHandler на Drunkenness2Handler

    private static float swayTimer = 0;
    private static float swayX = 0;
    private static float swayZ = 0;

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
        if (player != null && player.hasEffect(DCEffects.DRUNKENNESS_2.get())) {
            int amplifier = player.getEffect(DCEffects.DRUNKENNESS_2.get()).getAmplifier();

            // Если есть и эффект тошноты, не делаем ничего, чтобы избежать конфликта
            if (player.hasEffect(MobEffects.CONFUSION)) {
                return;
            }

            // Получаем время для анимации волны
            long time = System.currentTimeMillis();

            // Вычисляем интенсивность эффектов
            float intensity = (amplifier + 1) * 0.2f; // Увеличена интенсивность
            float pixelIntensity = (amplifier + 1) * 0.3f; // Отдельная интенсивность для пикселей

            // Создаем "волну" по осям X и Y (наклон)
            float waveX = (float) Math.sin(time * 0.001) * intensity;
            float waveY = (float) Math.cos(time * 0.001 + 100) * intensity * 0.5f;

            // Создаем "дрожание" (zoom/quake)
            float shake = (float) Math.sin(time * 0.01) * intensity * 0.15f;

            // Получаем PoseStack из события
            PoseStack poseStack = event.getPoseStack();

            // Применяем пикселизацию через масштабирование и сжатие
            if (pixelIntensity > 0.1f) {
                float pixelScale = 1.0f - pixelIntensity * 0.1f;
                // Создаем эффект низкого разрешения
                poseStack.scale(pixelScale, pixelScale, pixelScale);
            }

            // Небольшое "приближение/отдаление" для дрожания
            poseStack.scale(1.0f + shake, 1.0f + shake, 1.0f);

            // Наклон по осям X и Y для создания волны
            poseStack.mulPose(Axis.XP.rotationDegrees(waveY * 15.0f)); // Увеличена сила наклона
            poseStack.mulPose(Axis.YP.rotationDegrees(waveX * 15.0f));

            // Добавляем дополнительный наклон по Z для эффекта "заваливания"
            float waveZ = (float) Math.sin(time * 0.0015 + 50) * intensity * 0.3f;
            poseStack.mulPose(Axis.ZP.rotationDegrees(waveZ * 5.0f));
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player != null && player.hasEffect(DCEffects.DRUNKENNESS_2.get())) {
            int amplifier = player.getEffect(DCEffects.DRUNKENNESS_2.get()).getAmplifier();

            if (player.hasEffect(MobEffects.CONFUSION)) {
                return;
            }

            float intensity = (amplifier + 1) * 0.25f;
            long time = System.currentTimeMillis();

            // Добавляем дрожание камеры
            float cameraShakeX = (float) Math.sin(time * 0.02) * intensity * 2.0f;
            float cameraShakeY = (float) Math.cos(time * 0.017) * intensity * 1.5f;

            event.setYaw(event.getYaw() + cameraShakeX);
            event.setPitch(event.getPitch() + cameraShakeY);
            event.setRoll(event.getRoll() + (float) Math.sin(time * 0.015) * intensity * 5.0f);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Добавляем шатание при ходьбе
        if (event.phase == TickEvent.Phase.END && event.player != null) {
            if (event.player.hasEffect(DCEffects.DRUNKENNESS_2.get())) {
                int amplifier = event.player.getEffect(DCEffects.DRUNKENNESS_2.get()).getAmplifier();

                if (event.player.hasEffect(MobEffects.CONFUSION)) {
                    swayTimer = 0;
                    swayX = 0;
                    swayZ = 0;
                    return;
                }

                float intensity = (amplifier + 1) * 0.15f;

                // Увеличиваем таймер
                swayTimer += 0.1f;

                // Вычисляем смещение для шатания
                swayX = Mth.sin(swayTimer * 0.5f) * intensity * 0.6f;
                swayZ = Mth.cos(swayTimer * 0.3f) * intensity * 0.6f;

                // Применяем смещение к позиции игрока
                // Используем onGround() вместо isOnGround() для совместимости с версией Minecraft
                if (event.player.onGround() && (event.player.xxa != 0 || event.player.zza != 0)) {
                    // Только при движении
                    event.player.setPos(
                            event.player.getX() + swayX,
                            event.player.getY(),
                            event.player.getZ() + swayZ
                    );
                }
            } else {
                swayTimer = 0;
                swayX = 0;
                swayZ = 0;
            }
        }
    }
}