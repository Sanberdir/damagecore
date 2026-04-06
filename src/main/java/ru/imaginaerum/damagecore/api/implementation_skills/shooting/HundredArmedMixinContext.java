package ru.imaginaerum.damagecore.api.implementation_skills.shooting;

import net.minecraft.world.entity.LivingEntity;

public class HundredArmedMixinContext {
    public static final ThreadLocal<LivingEntity> currentEntity = new ThreadLocal<>();
}