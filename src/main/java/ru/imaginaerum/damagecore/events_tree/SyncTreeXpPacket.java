package ru.imaginaerum.damagecore.events_tree;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncTreeXpPacket {
    public final Map<Integer, Integer> treeXp;
    public final Map<Integer, Integer> treeLevel;

    public SyncTreeXpPacket(Map<Integer, Integer> treeXp, Map<Integer, Integer> treeLevel) {
        this.treeXp = treeXp != null ? new HashMap<>(treeXp) : new HashMap<>();
        this.treeLevel = treeLevel != null ? new HashMap<>(treeLevel) : new HashMap<>();
    }

    public SyncTreeXpPacket(int singleTreeId, int singleXp) {
        Map<Integer,Integer> xp = new HashMap<>();
        xp.put(singleTreeId, singleXp);
        this.treeXp = xp;
        this.treeLevel = new HashMap<>();
    }

    public static SyncTreeXpPacket single(int treeId, int xp) {
        return new SyncTreeXpPacket(treeId, xp);
    }

    public static void encode(SyncTreeXpPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.treeXp.size());
        for (var entry : pkt.treeXp.entrySet()) {
            buf.writeInt(entry.getKey());
            buf.writeInt(entry.getValue());
        }
        buf.writeInt(pkt.treeLevel.size());
        for (var entry : pkt.treeLevel.entrySet()) {
            buf.writeInt(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    public static SyncTreeXpPacket decode(FriendlyByteBuf buf) {
        Map<Integer, Integer> xp = new HashMap<>();
        Map<Integer, Integer> level = new HashMap<>();
        int xpSize = buf.readInt();
        for (int i = 0; i < xpSize; i++) xp.put(buf.readInt(), buf.readInt());
        int levelSize = buf.readInt();
        for (int i = 0; i < levelSize; i++) level.put(buf.readInt(), buf.readInt());
        return new SyncTreeXpPacket(xp, level);
    }

    public static void handle(SyncTreeXpPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        SyncTreeXpClientProxy.apply(pkt)
                )
        );
        ctxSupplier.get().setPacketHandled(true);
    }
}