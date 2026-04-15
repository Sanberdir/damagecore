package ru.imaginaerum.damagecore.attack_packets;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import ru.imaginaerum.damagecore.DamageCore;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyBindings {

    public static KeyMapping STRONG_ATTACK_KEY;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        STRONG_ATTACK_KEY = new KeyMapping(
                "key.damagecore.strong_attack",  // ключ локализации
                GLFW.GLFW_KEY_LEFT_ALT,   // ALT left
                "key.category.damagecore"         // категория в Управлении
        );
        event.register(STRONG_ATTACK_KEY);
    }
}
