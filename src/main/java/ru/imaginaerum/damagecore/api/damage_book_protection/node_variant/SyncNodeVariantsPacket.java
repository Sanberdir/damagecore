package ru.imaginaerum.damagecore.api.damage_book_protection.node_variant;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeClientSync;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeNode;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record SyncNodeVariantsPacket(int treeId, Map<String, Integer> variants) {

    // Сериализация
    public static void encode(SyncNodeVariantsPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.treeId);
        buf.writeInt(pkt.variants.size());
        for (var entry : pkt.variants.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    // Десериализация
    public static SyncNodeVariantsPacket decode(FriendlyByteBuf buf) {
        int treeId = buf.readInt();
        int size = buf.readInt();
        Map<String, Integer> variants = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            int sel = buf.readInt();
            variants.put(id, sel);
        }
        return new SyncNodeVariantsPacket(treeId, variants);
    }

    // Обработка на клиенте
    public static void handle(SyncNodeVariantsPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        System.out.println("Received variants packet on client for tree " + pkt.treeId()); // ДОБАВИТЬ
        ctx.get().enqueueWork(() -> {
            SkillTreeClientSync.applyVariants(pkt.treeId(), pkt.variants());
        });
        ctx.get().setPacketHandled(true);
    }
}