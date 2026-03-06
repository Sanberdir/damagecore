package ru.imaginaerum.damagecore.api.damage_book_protection.node_variant;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeNode;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

import java.util.function.Supplier;

public class SelectVariantPacket {
    public final int treeId;       // <--- добавляем
    public final String nodeId;
    public final int variantIndex;

    public SelectVariantPacket(int treeId, String nodeId, int variantIndex) {
        this.treeId = treeId;
        this.nodeId = nodeId;
        this.variantIndex = variantIndex;
    }

    public static void encode(SelectVariantPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.treeId);
        buf.writeUtf(pkt.nodeId);
        buf.writeInt(pkt.variantIndex);
    }

    public static SelectVariantPacket decode(FriendlyByteBuf buf) {
        return new SelectVariantPacket(buf.readInt(), buf.readUtf(32767), buf.readInt());
    }

    public static void handle(SelectVariantPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            SkillTreeNode node = SkillTreeServerHandler.getNodeForPlayer(player, pkt.nodeId);
            if (node != null && pkt.variantIndex >= 0 && pkt.variantIndex < node.options.size()) {
                node.applyVariant(pkt.variantIndex);

                // Теперь передаём treeId
                SkillTreeServerHandler.saveNodeVariant(player, node, pkt.treeId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}