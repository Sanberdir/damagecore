package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "damagecore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SkillTreeMouseHandler {

    @SubscribeEvent
    public static void onGuiMouseScroll(ScreenEvent.MouseScrolled event) {
        System.out.println("!!! GUI SCROLL DETECTED !!! Delta: " + event.getScrollDelta());

        Minecraft mc = Minecraft.getInstance();
        System.out.println("Current screen: " + mc.screen);

        int panelScreenX = 100;
        int panelScreenY = 100;

        boolean used = SkillTreeRenderer.mouseScrolled(
                (int) event.getMouseX(),
                (int) event.getMouseY(),
                event.getScrollDelta(),
                panelScreenX,
                panelScreenY
        );

        if (used) {
            event.setCanceled(true);
        }
    }
}
