package ru.imaginaerum.damagecore.hud.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ThirstDamagePacket {

    public ThirstDamagePacket() {}

    public ThirstDamagePacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.hurt(player.damageSources().starve(), 1f);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}