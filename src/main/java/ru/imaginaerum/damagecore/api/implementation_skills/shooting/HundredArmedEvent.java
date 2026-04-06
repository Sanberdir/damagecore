package ru.imaginaerum.damagecore.api.implementation_skills.shooting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.damage_book_protection.ModNetwork;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class HundredArmedEvent {

    private static final Map<UUID, Boolean> lastKnownState = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        UUID playerId = player.getUUID();
        boolean hasSkill = SkillTreeServerHandler.isNodeLearned(player, "hundred_armed");

        boolean previous = lastKnownState.getOrDefault(playerId, false);
        if (previous != hasSkill) {
            lastKnownState.put(playerId, hasSkill);
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new HundredArmedSyncPacket(hasSkill)
            );
        }
    }
}