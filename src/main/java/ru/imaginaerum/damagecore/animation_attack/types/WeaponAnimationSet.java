package ru.imaginaerum.damagecore.animation_attack.types;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record WeaponAnimationSet(
        List<SwingAnimationEntry> swingAnimations,
        ResourceLocation strongAttack
) {
}