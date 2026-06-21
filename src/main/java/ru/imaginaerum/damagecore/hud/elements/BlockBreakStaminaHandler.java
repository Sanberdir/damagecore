package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.api.ModNetwork;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlockBreakStaminaHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new DrainStaminaPacket(1.0f)
        );
    }
}