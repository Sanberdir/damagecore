package ru.imaginaerum.damagecore.api.implementation_skills.alchemy;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerRegistry;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "damagecore")
public class SwiftBrewingHandler {

    private static final String NODE_ID = "swift_brewing";

    @SubscribeEvent
    public static void onBrewingStandTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        int treeId = -1;
        for (int id : SkillTreeServerRegistry.getAllTreeIds()) {
            if (SkillTreeServerRegistry.getNode(id, NODE_ID) != null) {
                treeId = id;
                break;
            }
        }
        if (treeId == -1) return;

        final int finalTreeId = treeId;

        // Идём от игроков — ищем зелеварки рядом с игроками у которых есть навык
        for (ServerPlayer player : serverLevel.players()) {
            if (SkillTreeServerHandler.getNodeProgress(player, finalTreeId, NODE_ID) <= 0f) continue;

            // Ищем зелеварки в радиусе 8 блоков от игрока
            net.minecraft.core.BlockPos playerPos = player.blockPosition();
            for (int dx = -8; dx <= 8; dx++) {
                for (int dy = -4; dy <= 4; dy++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        net.minecraft.core.BlockPos checkPos = playerPos.offset(dx, dy, dz);
                        net.minecraft.world.level.block.entity.BlockEntity be =
                                serverLevel.getBlockEntity(checkPos);
                        if (be instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity brewingStand) {
                            accelerateBrewing(brewingStand);
                        }
                    }
                }
            }
        }
    }


    private static void accelerateBrewing(
            net.minecraft.world.level.block.entity.BrewingStandBlockEntity brewingStand) {
        try {
            Field brewTimeField = net.minecraft.world.level.block.entity.BrewingStandBlockEntity.class
                    .getDeclaredField("brewTime");
            brewTimeField.setAccessible(true);
            int brewTime = brewTimeField.getInt(brewingStand);

            if (brewTime > 0) {
                int newTime = brewTime - 2; // ускоряем на 2 доп. тика
                if (newTime < 1) newTime = 1;
                brewTimeField.setInt(brewingStand, newTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}