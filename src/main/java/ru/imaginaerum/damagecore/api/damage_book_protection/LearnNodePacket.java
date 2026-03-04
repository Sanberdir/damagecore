package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import net.minecraft.server.level.ServerPlayer;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

public class LearnNodePacket {
    public final int treeId;
    public final String nodeId;

    public LearnNodePacket(int treeId, String nodeId) {
        this.treeId = treeId;
        this.nodeId = nodeId;
    }

    public static void encode(LearnNodePacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.treeId);
        buf.writeUtf(pkt.nodeId);
    }

    public static LearnNodePacket decode(FriendlyByteBuf buf) {
        int tid = buf.readInt();
        String nid = buf.readUtf(32767);
        return new LearnNodePacket(tid, nid);
    }

    public static void handle(LearnNodePacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return; // should not be null for client->server

            // Server-side handling (in separate helper)
            SkillTreeServerHandler.handleLearnRequest(player, pkt.treeId, pkt.nodeId);
        });
        ctx.setPacketHandled(true);
    }
}