package ru.imaginaerum.damagecore.events_tree;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.imaginaerum.damagecore.api.damage_book_protection.DamageBookRenderer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncTreeXpPacket {
    public final Map<Integer, Integer> treeXp;
    public final Map<Integer, Integer> treeLevel;

    public SyncTreeXpPacket(Map<Integer, Integer> treeXp, Map<Integer, Integer> treeLevel) {
        this.treeXp = treeXp;
        this.treeLevel = treeLevel;
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
        for (int i = 0; i < xpSize; i++) {
            xp.put(buf.readInt(), buf.readInt());
        }

        int levelSize = buf.readInt();
        for (int i = 0; i < levelSize; i++) {
            level.put(buf.readInt(), buf.readInt());
        }

        return new SyncTreeXpPacket(xp, level);
    }

    public static void handle(SyncTreeXpPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // Очищаем и применяем полученные данные на клиенте
            DamageBookRenderer.clearXpData();
            for (var entry : pkt.treeXp.entrySet()) {
                DamageBookRenderer.setXp(entry.getKey(), entry.getValue());
            }
            for (var entry : pkt.treeLevel.entrySet()) {
                DamageBookRenderer.setLevel(entry.getKey(), entry.getValue());
            }
        });
        ctx.setPacketHandled(true);
    }
}