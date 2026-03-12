package ru.imaginaerum.damagecore.events_tree;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeNode;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

import java.util.*;

@Mod.EventBusSubscriber
public class AddXPAlchemy {

    private static final Map<Player, Integer> fillCount = new HashMap<>();
    private static final float HP_PER_LEARNED_NODE = 1.0f;

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Level level = player.level();

        ItemStack itemStack = event.getItemStack();
        if (itemStack.getItem() != Items.GLASS_BOTTLE) return;

        BlockHitResult rayTrace = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (rayTrace.getType() != HitResult.Type.BLOCK) return;

        BlockPos clickedPos = rayTrace.getBlockPos();
        if (!level.getFluidState(clickedPos).is(FluidTags.WATER)) return;
        if (!level.mayInteract(player, clickedPos)) return;

        if (level.isClientSide) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        int alchemyTreeId = findAlchemyTreeIdOrFallback();
        if (alchemyTreeId != 0) {
            return;
        }



        // Добавляем XP
        SkillTreeXpManager.addXp(serverPlayer, alchemyTreeId, 1);

        int count = fillCount.getOrDefault(player, 0) + 1;
        fillCount.put(player, count);

        // Лечение
        Object treeObj = SkillTreeServerHandler.getTreeById(alchemyTreeId);
        if (treeObj != null) {
            Map<String, SkillTreeNode> nodes = SkillTreeServerHandler.getNodesMap(treeObj);
            if (nodes != null && !nodes.isEmpty()) {
                float totalHeal = 0f;
                for (Map.Entry<String, SkillTreeNode> e : nodes.entrySet()) {
                    String nodeId = e.getKey();
                    SkillTreeNode node = e.getValue();
                    float progress = SkillTreeServerHandler.getNodeProgress(serverPlayer, alchemyTreeId, nodeId);
                    if (node.isLearned()) progress = 1.0f;
                    if (progress > 0f) totalHeal += progress * HP_PER_LEARNED_NODE;
                }
                if (totalHeal > 0f) {
                    serverPlayer.heal(totalHeal);
                }
            }
        }

        if (count >= 3) fillCount.put(player, 0);
    }

    private static BlockHitResult getPlayerPOVHitResult(Level level, Player player, ClipContext.Fluid fluidMode) {
        double reachDistance = player.isCreative() ? 5.0 : 4.5;
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 viewVector = player.getViewVector(1.0F);
        Vec3 targetPosition = eyePosition.add(viewVector.x * reachDistance, viewVector.y * reachDistance, viewVector.z * reachDistance);

        ClipContext context = new ClipContext(eyePosition, targetPosition, ClipContext.Block.OUTLINE, fluidMode, player);
        return level.clip(context);
    }

    private static int findAlchemyTreeIdOrFallback() {
        final int FALLBACK_ID = -1; // Явный ID заглушки

        try {
            Map<Integer, Object> trees = SkillTreeServerHandler.getTreesMap();
            if (trees == null || trees.isEmpty()) {
                return FALLBACK_ID;
            }

            for (Map.Entry<Integer, Object> e : trees.entrySet()) {
                Integer treeId = e.getKey();
                Object treeObj = e.getValue();
                var nodesMap = SkillTreeServerHandler.getNodesMap(treeObj);
                if (nodesMap == null) continue;

                for (String nodeId : nodesMap.keySet()) {
                    if (nodeId != null && nodeId.toLowerCase(Locale.ROOT).contains("alchemy")) {
                        return treeId; // Нашли реальное дерево
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        // Если не нашли ни одного дерева
        return FALLBACK_ID;
    }
}