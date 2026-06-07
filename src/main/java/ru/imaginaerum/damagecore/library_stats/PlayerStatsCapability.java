package ru.imaginaerum.damagecore.library_stats;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = "damagecore")
public class PlayerStatsCapability {

    public static final Capability<IPlayerStats> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation ID =
            new ResourceLocation("damagecore", "player_stats");

    public static LazyOptional<IPlayerStats> get(Player player) {
        return player.getCapability(CAPABILITY);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) return;
        event.addCapability(ID, new Provider());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player player   = event.getEntity();

        original.reviveCaps();
        original.getCapability(CAPABILITY).ifPresent(oldStats ->
                player.getCapability(CAPABILITY).ifPresent(newStats -> {
                    for (StatsType type : StatsType.values()) {
                        newStats.setStat(type, oldStats.getStat(type));
                        // ✅ Копируем pressCount
                        newStats.setPressCount(type, oldStats.getPressCount(type));
                    }
                })
        );
        original.invalidateCaps();
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {

        private final PlayerStats instance = new PlayerStats();
        private final LazyOptional<IPlayerStats> optional = LazyOptional.of(() -> instance);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(
                @NotNull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return instance.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            instance.deserializeNBT(tag);
        }
    }
}