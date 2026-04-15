package ru.imaginaerum.damagecore.attack_packets.strong_attack;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.attack_packets.KeyBindings;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StrongAttackClientHandler {

    private static boolean wasPressed = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        boolean isPressed = KeyBindings.STRONG_ATTACK_KEY.isDown();

        if (isPressed && !wasPressed) {
            ModNetwork.CHANNEL.sendToServer(new StrongAttackPacket());
        }

        wasPressed = isPressed;
    }
}
