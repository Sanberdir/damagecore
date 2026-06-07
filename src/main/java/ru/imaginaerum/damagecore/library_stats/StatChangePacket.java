package ru.imaginaerum.damagecore.library_stats;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.library_stats.attributes.AttributeApplier;

import java.util.function.Supplier;

public class StatChangePacket {

    private final StatsType type;
    private final boolean increment;

    public StatChangePacket(StatsType type, boolean increment) {
        this.type = type;
        this.increment = increment;
    }

    public static void encode(StatChangePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.type.getId());
        buf.writeBoolean(packet.increment);
    }

    public static StatChangePacket decode(FriendlyByteBuf buf) {
        StatsType type = StatsType.fromId(buf.readUtf());
        boolean increment = buf.readBoolean();
        return new StatChangePacket(type, increment);
    }

    private static int getServerXp(ServerPlayer player) {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;

        int xpToNext;
        if (level >= 30) {
            xpToNext = 112 + (level - 30) * 9;
        } else if (level >= 15) {
            xpToNext = 37 + (level - 15) * 5;
        } else {
            xpToNext = 7 + level * 2;
        }

        int totalForLevel;
        if (level >= 32) {
            totalForLevel = (int)(4.5 * level * level - 162.5 * level + 2220);
        } else if (level >= 17) {
            totalForLevel = (int)(2.5 * level * level - 40.5 * level + 360);
        } else {
            totalForLevel = level * level + 6 * level;
        }

        return totalForLevel + (int)(progress * xpToNext);
    }

    private static void removeXp(ServerPlayer player, int cost) {
        int remaining = cost;

        while (remaining > 0) {
            if (player.experienceProgress > 0f) {
                int levelCost = player.getXpNeededForNextLevel();
                int progressXp = (int)(player.experienceProgress * levelCost);

                if (progressXp >= remaining) {
                    player.experienceProgress -= (float) remaining / levelCost;
                    remaining = 0;
                } else {
                    remaining -= progressXp;
                    player.experienceProgress = 0f;
                }
            }

            if (remaining > 0 && player.experienceLevel > 0) {
                player.experienceLevel--;
                int levelCost = player.getXpNeededForNextLevel();
                player.experienceProgress = 1f;

                int progressXp = (int)(player.experienceProgress * levelCost);
                if (progressXp >= remaining) {
                    player.experienceProgress -= (float) remaining / levelCost;
                    remaining = 0;
                } else {
                    remaining -= progressXp;
                    player.experienceProgress = 0f;
                }
            } else {
                break;
            }
        }

        player.totalExperience = getServerXp(player);
    }

    public static void handle(StatChangePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || packet.type == null) return;

            PlayerStatsCapability.get(player).ifPresent(stats -> {
                if (packet.increment) {
                    if (stats.isMaxLevel(packet.type)) return;

                    int cost = stats.getNextCost(packet.type);
                    int actualXp = getServerXp(player);
                    if (actualXp < cost) return;

                    removeXp(player, cost);
                    stats.setStat(packet.type, stats.getStat(packet.type) + 1);
                    stats.setPressCount(packet.type, stats.getPressCount(packet.type) + 1);

                } else {
                    if (stats.getPressCount(packet.type) <= 0) return;

                    int refund = stats.getRefundCost(packet.type);
                    stats.setStat(packet.type, stats.getStat(packet.type) - 1);
                    stats.setPressCount(packet.type, stats.getPressCount(packet.type) - 1);
                    player.giveExperiencePoints(refund);
                    player.totalExperience = getServerXp(player);
                }

                if (packet.type == StatsType.LIVE_FORGE) {
                    AttributeApplier.applyLiveForge(player, stats.getStat(StatsType.LIVE_FORGE));

                    float newMax = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
                    if (player.getHealth() > newMax) {
                        player.setHealth(newMax);
                    }
                }

                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new SyncStatsPacket(stats, getServerXp(player))
                );
            });
        });
        ctx.get().setPacketHandled(true);
    }
}