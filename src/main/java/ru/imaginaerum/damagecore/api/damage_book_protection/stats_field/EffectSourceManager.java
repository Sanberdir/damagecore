package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class EffectSourceManager {

    private static final Map<UUID, Map<MobEffect, LivingEntity>> SOURCES = new HashMap<>();

    public static void setSource(LivingEntity target, MobEffect effect, LivingEntity source) {
        SOURCES
                .computeIfAbsent(target.getUUID(), k -> new HashMap<>())
                .put(effect, source);
    }

    public static LivingEntity getSource(LivingEntity target, MobEffect effect) {
        Map<MobEffect, LivingEntity> map = SOURCES.get(target.getUUID());
        if (map == null) return null;
        return map.get(effect);
    }

    public static Collection<LivingEntity> getAllSources(LivingEntity target) {
        Map<MobEffect, LivingEntity> map = SOURCES.get(target.getUUID());
        if (map == null) return List.of();
        return map.values();
    }
}
