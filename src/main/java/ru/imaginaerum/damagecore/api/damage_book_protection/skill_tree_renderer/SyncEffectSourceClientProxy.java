package ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SyncEffectSourceClientProxy {
    public static void apply(MobEffect effect, EntityType<?> sourceType) {
        PotionTrackingClient.registerMobEffectFromServer(effect, sourceType);
    }
}