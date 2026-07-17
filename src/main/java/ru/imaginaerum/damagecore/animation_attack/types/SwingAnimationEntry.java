package ru.imaginaerum.damagecore.animation_attack.types;

import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.library_damage.DamageType;

public record SwingAnimationEntry(
        ResourceLocation animation,
        DamageType damageType,
        boolean headLookAtCamera
) {
}