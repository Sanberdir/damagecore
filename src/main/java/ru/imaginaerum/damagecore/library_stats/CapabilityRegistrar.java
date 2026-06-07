package ru.imaginaerum.damagecore.library_stats;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Добавь этот @SubscribeEvent в свой класс, подписанный на MOD шину,
 * или зарегистрируй его через FMLJavaModLoadingContext.get().getModEventBus()
 */
@Mod.EventBusSubscriber(modid = "damagecore", bus = Mod.EventBusSubscriber.Bus.MOD)
public class CapabilityRegistrar {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerStats.class);
    }
}
