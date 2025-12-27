package ru.imaginaerum.damagecore.particle;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.particle.particles.StunParticle;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RegisterParticlesEvent {
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSideHandler {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Вызвать на КЛИЕНТСКОЙ стороне
        }

        @SubscribeEvent
        public static void registerParticleFactories(final RegisterParticleProvidersEvent event) {

            Minecraft.getInstance().particleEngine.register(DCParticles.STUN.get(),
                    StunParticle.Provider::new);
        }
    }
}
