package ru.imaginaerum.damagecore.library_stats;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.EnumMap;
import java.util.Map;

public class PlayerStats implements IPlayerStats, INBTSerializable<CompoundTag> {

    private final Map<StatsType, Integer> stats      = new EnumMap<>(StatsType.class);
    private final Map<StatsType, Integer> pressCounts = new EnumMap<>(StatsType.class);

    public PlayerStats() {
        for (StatsType type : StatsType.values()) {
            stats.put(type, type.getDefaultValue());
            pressCounts.put(type, 0);
        }
    }

    @Override
    public int getStat(StatsType type) {
        return stats.getOrDefault(type, type.getDefaultValue());
    }

    @Override
    public void setStat(StatsType type, int value) {
        stats.put(type, Math.max(0, value));
    }

    @Override
    public int getPressCount(StatsType type) {
        return pressCounts.getOrDefault(type, 0);
    }

    @Override
    public void setPressCount(StatsType type, int count) {
        pressCounts.put(type, Math.max(0, count));
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag statsTag  = new CompoundTag();
        CompoundTag countTag  = new CompoundTag();
        for (StatsType type : StatsType.values()) {
            statsTag.putInt(type.getId(), stats.getOrDefault(type, type.getDefaultValue()));
            countTag.putInt(type.getId(), pressCounts.getOrDefault(type, 0));
        }
        tag.put("stats",  statsTag);
        tag.put("counts", countTag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        CompoundTag statsTag  = tag.getCompound("stats");
        CompoundTag countTag  = tag.getCompound("counts");
        for (StatsType type : StatsType.values()) {
            stats.put(type, statsTag.contains(type.getId())
                    ? statsTag.getInt(type.getId())
                    : type.getDefaultValue());
            pressCounts.put(type, countTag.contains(type.getId())
                    ? countTag.getInt(type.getId())
                    : 0);
        }
    }
}