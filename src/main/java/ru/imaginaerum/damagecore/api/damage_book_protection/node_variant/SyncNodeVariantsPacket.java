package ru.imaginaerum.damagecore.api.damage_book_protection.node_variant;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record SyncNodeVariantsPacket(int treeId, Map<String, Integer> variants) {

    public static void encode(SyncNodeVariantsPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.treeId);
        buf.writeInt(pkt.variants.size());
        for (var entry : pkt.variants.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    public static SyncNodeVariantsPacket decode(FriendlyByteBuf buf) {
        int treeId = buf.readInt();
        int size = buf.readInt();
        Map<String, Integer> variants = new HashMap<>();
        for (int i = 0; i < size; i++) {
            variants.put(buf.readUtf(), buf.readInt());
        }
        return new SyncNodeVariantsPacket(treeId, variants);
    }

    public static void handle(SyncNodeVariantsPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                // ИСПРАВЛЕНО: вместо прямого вызова SkillTreeClientSync используем прокси,
                // чтобы JVM не загружала клиентский класс на выделенном сервере.
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        SyncNodeVariantsClientProxy.apply(pkt.treeId(), pkt.variants())
                )
        );
        ctx.get().setPacketHandled(true);
    }
}