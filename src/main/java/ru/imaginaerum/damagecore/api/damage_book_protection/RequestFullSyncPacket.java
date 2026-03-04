package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// пакет пустой, просто запрос синхронизации
public class RequestFullSyncPacket {
    public RequestFullSyncPacket() {}

    public static void encode(RequestFullSyncPacket pkt, FriendlyByteBuf buf) {}

    public static RequestFullSyncPacket decode(FriendlyByteBuf buf) {
        return new RequestFullSyncPacket();
    }

    public static void handle(RequestFullSyncPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            SkillTreeServerHandler.sendFullSyncToPlayer(player);
        });
        ctx.setPacketHandled(true);
    }
}