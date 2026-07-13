package ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.api.ModNetwork;

@Mod.EventBusSubscriber(modid = DamageCore.MODID)
public final class EffectSourceTracker {

    private EffectSourceTracker() {}

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) return;

        // Ищем последнего атаковавшего через vanilla combat tracker
        LivingEntity lastHurtBy = player.getLastHurtByMob();
        if (lastHurtBy == null) return;

        // Исключаем игрока как источника — для зелий игрока уже есть WATCHED_THROWN/DRINK
        EntityType<?> sourceType = lastHurtBy.getType();
        if (sourceType == EntityType.PLAYER) return;

        MobEffect effect = event.getEffectInstance().getEffect();

        ModNetwork.sendToClient(
                new SyncEffectSourcePayload(effect, sourceType),
                player
        );
    }
}