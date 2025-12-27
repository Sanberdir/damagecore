package ru.imaginaerum.damagecore.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.effect.effects.*;

public class DCEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, DamageCore.MODID);

    public static final RegistryObject<MobEffect> STUNNING = MOB_EFFECTS.register("stunning",
            () -> new StunningEffect(MobEffectCategory.HARMFUL, 0x7d746d));

    public static final RegistryObject<MobEffect> DEATH_POISON = MOB_EFFECTS.register("death_poison",
            () -> new DeathPoisonEffect(MobEffectCategory.HARMFUL, 0x556832));


    public static final RegistryObject<MobEffect> BLEEDING_1 = MOB_EFFECTS.register("bleeding_1",
            () -> new BleedingEffect(MobEffectCategory.HARMFUL, 0xAA2232));
    public static final RegistryObject<MobEffect> BLEEDING_2 = MOB_EFFECTS.register("bleeding_2",
            () -> new Bleeding2Effect(MobEffectCategory.HARMFUL, 0xAA2232));
    public static final RegistryObject<MobEffect> BLEEDING_3 = MOB_EFFECTS.register("bleeding_3",
            () -> new Bleeding3Effect(MobEffectCategory.HARMFUL, 0xAA2232));
}