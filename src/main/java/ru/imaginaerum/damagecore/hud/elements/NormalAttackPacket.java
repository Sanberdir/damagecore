package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.hud.elements.DrainStaminaPacket;

import java.util.function.Supplier;

public class NormalAttackPacket {

    public NormalAttackPacket() {}

    public static void encode(NormalAttackPacket packet, FriendlyByteBuf buf) {}

    public static NormalAttackPacket decode(FriendlyByteBuf buf) {
        return new NormalAttackPacket();
    }

    public static void handle(NormalAttackPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer attacker = ctx.get().getSender();
            if (attacker == null) return;

            // Дренаж стамины — шлём обратно клиенту
            ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> attacker),
                    new DrainStaminaPacket(4.0f)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}