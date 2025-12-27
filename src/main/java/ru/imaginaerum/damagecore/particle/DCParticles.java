package ru.imaginaerum.damagecore.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.imaginaerum.damagecore.DamageCore;

public class DCParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, DamageCore.MODID);

    public static final RegistryObject<SimpleParticleType> STUN =
            PARTICLE_TYPES.register("stun", () -> new SimpleParticleType(true));
}
