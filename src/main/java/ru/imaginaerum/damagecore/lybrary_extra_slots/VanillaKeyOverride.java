package ru.imaginaerum.damagecore.lybrary_extra_slots;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT)
public class VanillaKeyOverride {

    private static boolean applied = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (applied || event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        // снимаем стандартный биндинг swap-offhand с клавиши F
        mc.options.keySwapOffhand.setKey(InputConstants.UNKNOWN);
        mc.options.keySwapOffhand.setDown(false);
        mc.options.save();

        applied = true;
    }

}