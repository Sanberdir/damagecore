package ru.imaginaerum.damagecore.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import ru.imaginaerum.damagecore.DamageCore;

public class DamageTypesGenerator {
    public static final ResourceKey<DamageType> DEATH_POISON =
            ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(DamageCore.MODID, "death_poison"));

    public static final ResourceKey<DamageType> BLEEDING_1 =
            ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(DamageCore.MODID, "bleeding_1"));
    public static final ResourceKey<DamageType> BLEEDING_2 =
            ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(DamageCore.MODID, "bleeding_2"));
    public static final ResourceKey<DamageType> BLEEDING_3 =
            ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(DamageCore.MODID, "bleeding_3"));

    public static void bootstrap(BootstapContext<DamageType> context) {
        context.register(DEATH_POISON,
                new DamageType("death_poison",
                        DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                        0.1F, // exhaustion (истощение)
                        DamageEffects.HURT // эффект при получении урона
                ));
        context.register(BLEEDING_1,
                new DamageType("bleeding_1",
                        DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                        0.1F, // exhaustion (истощение)
                        DamageEffects.HURT // эффект при получении урона
                ));
        context.register(BLEEDING_2,
                new DamageType("bleeding_2",
                        DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                        0.1F, // exhaustion (истощение)
                        DamageEffects.HURT // эффект при получении урона
                ));
        context.register(BLEEDING_3,
                new DamageType("bleeding_3",
                        DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                        0.1F, // exhaustion (истощение)
                        DamageEffects.HURT // эффект при получении урона
                ));
    }
}