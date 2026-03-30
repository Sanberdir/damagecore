package ru.imaginaerum.damagecore.hud;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class HudCancelOverlays {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        String path = event.getOverlay().id().getPath();

        if (path.equals("player_health")) {
            event.setCanceled(true);
        }
        // Полоска голода
        if (path.equals("food_level")) {
            event.setCanceled(true);
        }
        // Насыщенность (отображается поверх голода)
        if (path.equals("saturation_level")) {
            event.setCanceled(true);
        }
    }
}