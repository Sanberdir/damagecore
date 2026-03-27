package ru.imaginaerum.damagecore.hud;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class HudCancelOverlays {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        // Убираем сердца
        if (event.getOverlay().id().getPath().equals("player_health")) {
            event.setCanceled(true);
        }
    }
}