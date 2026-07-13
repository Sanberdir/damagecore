package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

// Убраны импорты Minecraft и LocalPlayer!

public class SyncNodeLevelsPacket {
    public final int treeId;
    public final Map<String, Integer> levels;

    public SyncNodeLevelsPacket(int treeId, Map<String, Integer> levels) {
        this.treeId = treeId;
        this.levels = levels != null ? new HashMap<>(levels) : new HashMap<>();
    }

    public static void encode(SyncNodeLevelsPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.treeId);
        buf.writeInt(pkt.levels.size());
        for (Map.Entry<String, Integer> e : pkt.levels.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeInt(e.getValue());
        }
    }

    public static SyncNodeLevelsPacket decode(FriendlyByteBuf buf) {
        int treeId = buf.readInt();
        int size = buf.readInt();
        Map<String, Integer> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(32767), buf.readInt());
        }
        return new SyncNodeLevelsPacket(treeId, map);
    }

    public static void handle(SyncNodeLevelsPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        SyncNodeLevelsClientProxy.apply(pkt.treeId, pkt.levels)
                )
        );
        ctxSupplier.get().setPacketHandled(true);
    }
}