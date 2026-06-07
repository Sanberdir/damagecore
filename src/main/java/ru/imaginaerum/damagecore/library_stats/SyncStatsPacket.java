package ru.imaginaerum.damagecore.library_stats;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncStatsPacket {

    private final int[] statValues;
    private final int[] pressCounts;
    private final int   totalXp;

    public SyncStatsPacket(IPlayerStats stats, int totalXp) {
        StatsType[] types = StatsType.values();
        this.statValues  = new int[types.length];
        this.pressCounts = new int[types.length];
        this.totalXp     = totalXp;
        for (int i = 0; i < types.length; i++) {
            this.statValues[i]  = stats.getStat(types[i]);
            this.pressCounts[i] = stats.getPressCount(types[i]);
        }
    }

    private SyncStatsPacket(int[] statValues, int[] pressCounts, int totalXp) {
        this.statValues  = statValues;
        this.pressCounts = pressCounts;
        this.totalXp     = totalXp;
    }

    public static void encode(SyncStatsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.statValues.length);
        for (int v : packet.statValues)  buf.writeVarInt(v);
        for (int c : packet.pressCounts) buf.writeVarInt(c);
        buf.writeVarInt(packet.totalXp);
    }

    public static SyncStatsPacket decode(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        int[] statValues  = new int[len];
        int[] pressCounts = new int[len];
        for (int i = 0; i < len; i++) statValues[i]  = buf.readVarInt();
        for (int i = 0; i < len; i++) pressCounts[i] = buf.readVarInt();
        int totalXp = buf.readVarInt();
        return new SyncStatsPacket(statValues, pressCounts, totalXp);
    }

    public static void handle(SyncStatsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            PlayerStatsCapability.get(player).ifPresent(stats -> {
                StatsType[] types = StatsType.values();
                for (int i = 0; i < types.length && i < packet.statValues.length; i++) {
                    stats.setStat(types[i], packet.statValues[i]);
                    stats.setPressCount(types[i], packet.pressCounts[i]);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}