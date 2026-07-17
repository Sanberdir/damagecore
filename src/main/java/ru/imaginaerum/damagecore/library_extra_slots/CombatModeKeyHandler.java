package ru.imaginaerum.damagecore.library_extra_slots;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.ModNetwork;

@Mod.EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT)
public class CombatModeKeyHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // consumeClick() отдаёт true один раз за нажатие, не зависает при удержании
        while (ModKeyMappings.COMBAT_MODE_KEY.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new CombatModeTogglePacket());
        }
    }
}