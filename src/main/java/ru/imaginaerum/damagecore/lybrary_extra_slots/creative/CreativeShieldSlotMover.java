package ru.imaginaerum.damagecore.lybrary_extra_slots.creative;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "damagecore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CreativeShieldSlotMover {
    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof CreativeModeInventoryScreen screen) {

            for (Slot slot : screen.getMenu().slots) {

                System.out.println(
                        "Slot: index=" + slot.getSlotIndex()
                                + " container=" + slot.container.getClass().getName()
                                + " x=" + slot.x
                                + " y=" + slot.y
                );
            }
        }
    }

}