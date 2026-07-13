package ru.imaginaerum.damagecore.lybrary_extra_slots;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModKeyMappings {

    public static final String CATEGORY = "key.categories.damagecore";
    public static KeyMapping COMBAT_MODE_KEY;

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        COMBAT_MODE_KEY = new KeyMapping(
                "key.damagecore.combat_mode",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                CATEGORY
        );
        event.register(COMBAT_MODE_KEY);
    }
}