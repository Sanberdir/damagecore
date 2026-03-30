package ru.imaginaerum.damagecore.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.stats_field.EffectSourceManager;

@Mod.EventBusSubscriber(modid = "damagecore", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EffectSourceEvent {
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity target = event.getEntity();

        Entity sourceEntity = event.getEffectSource();
        if (!(sourceEntity instanceof LivingEntity source)) return;

        EffectSourceManager.setSource(target, event.getEffectInstance().getEffect(), source);
    }


}
