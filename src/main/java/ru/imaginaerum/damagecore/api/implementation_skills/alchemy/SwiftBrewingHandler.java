package ru.imaginaerum.damagecore.api.implementation_skills.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
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

        for (ServerPlayer player : serverLevel.players()) {
            int nodeLevel = SkillTreeServerHandler.getNodeLevel(player, NODE_ID); // уровень 0-3
            if (nodeLevel <= 0) continue;

          BlockPos playerPos = player.blockPosition();
            for (int dx = -8; dx <= 8; dx++) {
                for (int dy = -4; dy <= 4; dy++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        BlockPos checkPos = playerPos.offset(dx, dy, dz);
                       BlockEntity be =
                                serverLevel.getBlockEntity(checkPos);
                        if (be instanceof BrewingStandBlockEntity brewingStand) {
                            accelerateBrewing(brewingStand, nodeLevel);
                        }
                    }
                }
            }
        }
    }

    private static void accelerateBrewing(
          BrewingStandBlockEntity brewingStand,
            int nodeLevel) {
        try {
            Field brewTimeField = BrewingStandBlockEntity.class
                    .getDeclaredField("brewTime");
            brewTimeField.setAccessible(true);
            int brewTime = brewTimeField.getInt(brewingStand);

            if (brewTime > 0) {
                int newTime = Math.max(1, brewTime - nodeLevel); // уровень 1 = -1, уровень 2 = -2, уровень 3 = -3
                brewTimeField.setInt(brewingStand, newTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}