package ru.imaginaerum.damagecore.events;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;
import net.minecraft.world.effect.MobEffectInstance;

import ru.imaginaerum.damagecore.effect.DCEffects;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BleedingApplyHandler {

    private static final int BLEEDING_DURATION = 15 * 20; // 15 секунд в тиках

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();

        if (entity.level().isClientSide) return;

        boolean shouldCheck = false;

        if (source.getDirectEntity() instanceof AbstractArrow) {
            shouldCheck = true;
        }

        String msgId = source.getMsgId();
        if (msgId.equals("cactus") || msgId.equals("sweetBerryBush")) {
            shouldCheck = true;
        }

        if (shouldCheck) {
            // Шанс от 5% до 10%
            float chance = 0.05f + entity.level().random.nextFloat() * 0.05f;
            if (entity.level().random.nextFloat() < chance) {
                entity.addEffect(new MobEffectInstance(
                        DCEffects.BLEEDING_1.get(),
                        BLEEDING_DURATION,
                        0,
                        false,
                        true
                ));
            }
        }
    }
}