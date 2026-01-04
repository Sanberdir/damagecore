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
public class Drunkenness3Handler {

    private static float prevWaveX = 0;
    private static float prevWaveY = 0;
    private static float prevWaveZ = 0;
    private static float prevShake = 0;
    private static float prevPixelScale = 1.0f;

    private static float prevPixelOffsetX = 0;
    private static float prevPixelOffsetY = 0;

    private static float prevStretch = 1.0f;

    private static float prevCameraShakeX = 0;
    private static float prevCameraShakeY = 0;
    private static float prevCameraRoll = 0;

    private static float swayTimer = 0;
    private static float prevSwayX = 0;
    private static float prevSwayZ = 0;

    private static final float SMOOTH_FACTOR = 0.25f;
    private static final float CAMERA_SMOOTH_FACTOR = 0.3f;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player != null && player.hasEffect(DCEffects.DRUNKENNESS_3.get())) {
            int amplifier = player.getEffect(DCEffects.DRUNKENNESS_3.get()).getAmplifier();

            if (player.hasEffect(MobEffects.CONFUSION)) {
                resetValues();
                return;
            }

            long time = System.currentTimeMillis();
            float intensity = (amplifier + 1) * 0.2f;
            float pixelIntensity = (amplifier + 1) * 0.3f;

            float targetWaveX = (float) Math.sin(time * 0.001) * intensity;
            float targetWaveY = (float) Math.cos(time * 0.001 + 100) * intensity * 0.5f;

            prevWaveX = lerp(prevWaveX, targetWaveX, SMOOTH_FACTOR);
            prevWaveY = lerp(prevWaveY, targetWaveY, SMOOTH_FACTOR);

            float targetShake = (float) Math.sin(time * 0.01) * intensity * 0.15f;
            prevShake = lerp(prevShake, targetShake, SMOOTH_FACTOR * 1.2f);

            PoseStack poseStack = event.getPoseStack();

            if (pixelIntensity > 0.1f) {
                float pixelWave = (float) Math.sin(time * 0.00045f) * 0.5f + 0.5f;
                float targetPixelScale = 1.0f - pixelIntensity * 0.12f * pixelWave;
                prevPixelScale = lerp(prevPixelScale, targetPixelScale, SMOOTH_FACTOR * 0.25f);
                poseStack.scale(prevPixelScale, prevPixelScale, prevPixelScale);
            } else {
                prevPixelScale = 1.0f;
            }

            // -----------------------
            // Медленнее в 4 раза и растянуть в 1.5×
            float circleTime = time * 0.000005f; // 4× медленнее
            float radius = pixelIntensity * 0.9f; // растянуто сильнее (раньше было 0.6)

            float targetOffsetX = Mth.cos(circleTime) * radius;
            float targetOffsetY = Mth.sin(circleTime) * radius;

            prevPixelOffsetX = lerp(prevPixelOffsetX, targetOffsetX, 0.06f);
            prevPixelOffsetY = lerp(prevPixelOffsetY, targetOffsetY, 0.06f);

            poseStack.translate(0.5f + prevPixelOffsetX, 0.5f + prevPixelOffsetY, 0);

            float scaleTime = circleTime * 0.6f;
            float rawTargetStretch = 1.0f - pixelIntensity * 0.9f * (float) Math.sin(scaleTime); // 1.5× растяжение
            float targetStretch = clamp(rawTargetStretch, 0.35f, 2.0f);

            prevStretch = lerp(prevStretch, targetStretch, 0.06f);
            poseStack.scale(prevStretch, prevStretch, 1.0f);

            poseStack.scale(1.0f + prevShake, 1.0f + prevShake, 1.0f);

            poseStack.translate(-0.5f - prevPixelOffsetX, -0.5f - prevPixelOffsetY, 0);
            // -----------------------

            poseStack.mulPose(Axis.XP.rotationDegrees(prevWaveY * 14.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(prevWaveX * 14.0f));

            float targetWaveZ = (float) Math.sin(time * 0.0015 + 50) * intensity * 0.3f;
            prevWaveZ = lerp(prevWaveZ, targetWaveZ, SMOOTH_FACTOR);
            poseStack.mulPose(Axis.ZP.rotationDegrees(prevWaveZ * 4.5f));

        } else {
            prevWaveX = lerp(prevWaveX, 0, SMOOTH_FACTOR * 0.5f);
            prevWaveY = lerp(prevWaveY, 0, SMOOTH_FACTOR * 0.5f);
            prevWaveZ = lerp(prevWaveZ, 0, SMOOTH_FACTOR * 0.5f);
            prevShake = lerp(prevShake, 0, SMOOTH_FACTOR * 0.5f);
            prevPixelScale = lerp(prevPixelScale, 1.0f, SMOOTH_FACTOR * 0.5f);
            prevPixelOffsetX = lerp(prevPixelOffsetX, 0, 0.06f);
            prevPixelOffsetY = lerp(prevPixelOffsetY, 0, 0.06f);
            prevStretch = lerp(prevStretch, 1.0f, 0.06f);
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player != null && player.hasEffect(DCEffects.DRUNKENNESS_3.get())) {
            int amplifier = player.getEffect(DCEffects.DRUNKENNESS_3.get()).getAmplifier();

            if (player.hasEffect(MobEffects.CONFUSION)) {
                prevCameraShakeX = 0;
                prevCameraShakeY = 0;
                prevCameraRoll = 0;
                return;
            }

            float intensity = (amplifier + 1) * 0.25f;
            long time = System.currentTimeMillis();

            float targetCameraShakeX = (float) Math.sin(time * 0.018) * intensity * 1.8f;
            float targetCameraShakeY = (float) Math.cos(time * 0.015) * intensity * 1.3f;
            float targetCameraRoll = (float) Math.sin(time * 0.014) * intensity * 4.0f;

            prevCameraShakeX = lerp(prevCameraShakeX, targetCameraShakeX, CAMERA_SMOOTH_FACTOR);
            prevCameraShakeY = lerp(prevCameraShakeY, targetCameraShakeY, CAMERA_SMOOTH_FACTOR);
            prevCameraRoll = lerp(prevCameraRoll, targetCameraRoll, CAMERA_SMOOTH_FACTOR);

            event.setYaw(event.getYaw() + prevCameraShakeX);
            event.setPitch(event.getPitch() + prevCameraShakeY);
            event.setRoll(event.getRoll() + prevCameraRoll);
        } else {
            prevCameraShakeX = lerp(prevCameraShakeX, 0, CAMERA_SMOOTH_FACTOR);
            prevCameraShakeY = lerp(prevCameraShakeY, 0, CAMERA_SMOOTH_FACTOR);
            prevCameraRoll = lerp(prevCameraRoll, 0, CAMERA_SMOOTH_FACTOR);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player != null) {
            if (event.player.hasEffect(DCEffects.DRUNKENNESS_3.get())) {
                int amplifier = event.player.getEffect(DCEffects.DRUNKENNESS_3.get()).getAmplifier();

                if (event.player.hasEffect(MobEffects.CONFUSION)) {
                    swayTimer = 0;
                    prevSwayX = 0;
                    prevSwayZ = 0;
                    return;
                }

                float intensity = (amplifier + 1) * 0.15f;
                swayTimer += 0.085f;

                float targetSwayX = Mth.sin(swayTimer * 0.45f) * intensity * 0.55f;
                float targetSwayZ = Mth.cos(swayTimer * 0.28f) * intensity * 0.55f;

                prevSwayX = lerp(prevSwayX, targetSwayX, 0.2f);
                prevSwayZ = lerp(prevSwayZ, targetSwayZ, 0.2f);

                if (event.player.onGround() && (event.player.xxa != 0 || event.player.zza != 0)) {
                    event.player.setPos(
                            event.player.getX() + prevSwayX * 0.8f,
                            event.player.getY(),
                            event.player.getZ() + prevSwayZ * 0.8f
                    );
                }
            } else {
                swayTimer = 0;
                prevSwayX = lerp(prevSwayX, 0, 0.15f);
                prevSwayZ = lerp(prevSwayZ, 0, 0.15f);
            }
        }
    }

    private static float lerp(float start, float end, float amount) {
        amount = Math.max(0, Math.min(1, amount));
        return start + (end - start) * amount;
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    private static void resetValues() {
        prevWaveX = 0;
        prevWaveY = 0;
        prevWaveZ = 0;
        prevShake = 0;
        prevPixelScale = 1.0f;
        prevPixelOffsetX = 0;
        prevPixelOffsetY = 0;
        prevStretch = 1.0f;
        prevCameraShakeX = 0;
        prevCameraShakeY = 0;
        prevCameraRoll = 0;
        swayTimer = 0;
        prevSwayX = 0;
        prevSwayZ = 0;
    }
}
