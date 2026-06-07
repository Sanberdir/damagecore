package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.events_tree.SkillTreeXpManager;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.SyncStatsPacket;

import java.util.function.Supplier;

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

            SkillTreeXpManager.loadFromPersistentData(player);
            SkillTreeServerHandler.sendFullSyncToPlayer(player);

            // ✅ Синхронизируем статы и XP при первом открытии
            PlayerStatsCapability.get(player).ifPresent(stats ->
                    ModNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new SyncStatsPacket(stats, player.totalExperience)
                    )
            );
        });
        ctx.setPacketHandled(true);
    }
}