package ru.imaginaerum.damagecore.api.damage_book_protection.node_variant;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.damage_book_protection.ModNetwork;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeNode;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SelectNodeVariantPacket {
    private final int treeId;
    private final String nodeId;
    private final int variant;

    public SelectNodeVariantPacket(int treeId, String nodeId, int variant) {
        this.treeId = treeId;
        this.nodeId = nodeId;
        this.variant = variant;
    }

    public static void encode(SelectNodeVariantPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.treeId);
        buf.writeUtf(pkt.nodeId);
        buf.writeInt(pkt.variant);
    }

    public static SelectNodeVariantPacket decode(FriendlyByteBuf buf) {
        return new SelectNodeVariantPacket(
                buf.readInt(),
                buf.readUtf(),
                buf.readInt()
        );
    }

    public static void handle(SelectNodeVariantPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            Map<String, SkillTreeNode> nodes = SkillTreeServerRegistry.getNodes(pkt.treeId);
            if (nodes == null || nodes.isEmpty()) return;

            SkillTreeNode node = nodes.get(pkt.nodeId);
            if (node == null) return;

            node.applyVariant(pkt.variant);
            SkillTreeServerHandler.saveNodeVariant(player, node, pkt.treeId);

            Map<String, Integer> sync = new HashMap<>();
            sync.put(pkt.nodeId, pkt.variant);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncNodeVariantsPacket(pkt.treeId, sync));

            System.out.println("[Server] Variant selected: " + pkt.nodeId + " = " + pkt.variant);
        });

        ctx.setPacketHandled(true);
    }
}
