package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SyncLearnedNodesPacket {
    public final int treeId;
    public final List<String> learnedIds;

    public SyncLearnedNodesPacket(int treeId, List<String> learnedIds) {
        this.treeId = treeId;
        this.learnedIds = learnedIds;
    }

    public static void encode(SyncLearnedNodesPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.treeId);
        buf.writeInt(pkt.learnedIds.size());
        for (String s : pkt.learnedIds) buf.writeUtf(s);
    }

    public static SyncLearnedNodesPacket decode(FriendlyByteBuf buf) {
        int treeId = buf.readInt();
        int size = buf.readInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(buf.readUtf(32767));
        return new SyncLearnedNodesPacket(treeId, list);
    }

    public static void handle(SyncLearnedNodesPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // client thread
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            SkillTreeClientSync.applyLearnedNodes(pkt.treeId, pkt.learnedIds);
        });
        ctx.setPacketHandled(true);
    }
}