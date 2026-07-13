package ru.imaginaerum.damagecore.lybrary_extra_slots;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "damagecore", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModSetup {
    @SubscribeEvent
    public static void onRegisterCaps(RegisterCapabilitiesEvent event) {
        ExtraSlotCapability.register(event);
    }
}
