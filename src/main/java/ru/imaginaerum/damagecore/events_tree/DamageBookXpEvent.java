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
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

import java.util.*;

/**
 * Наполнение бутылки водой даёт XP дереву "alchemy" и восстанавливает здоровье
 */
@Mod.EventBusSubscriber
public class DamageBookXpEvent {

    private static final Map<Player, Integer> fillCount = new HashMap<>();
    private static final float HP_PER_LEARNED_NODE = 1.0f;

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Level level = player.level();

        // Проверяем, держит ли игрок бутылку
        ItemStack itemStack = event.getItemStack();
        if (itemStack.getItem() != Items.GLASS_BOTTLE) return;

        // Выполняем рейтрейс чтобы проверить, смотрит ли игрок на воду
        BlockHitResult rayTrace = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);

        if (rayTrace.getType() != HitResult.Type.BLOCK) return;

        BlockPos clickedPos = rayTrace.getBlockPos();

        // Проверяем, есть ли вода в этой позиции
        if (!level.getFluidState(clickedPos).is(FluidTags.WATER)) return;

        // Проверяем, можно ли взаимодействовать
        if (!level.mayInteract(player, clickedPos)) return;

        // Проверяем, не на стороне клиента ли мы
        if (level.isClientSide) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // 🔹 Получаем ID дерева alchemy
        int alchemyTreeId = findAlchemyTreeIdOrFallback();
        if (alchemyTreeId == 0) return;

        // Увеличиваем счётчик наполнений
        int count = fillCount.getOrDefault(player, 0) + 1;
        fillCount.put(player, count);

        // Даём XP в дерево alchemy
        SkillTreeXpManager.addXp(serverPlayer, alchemyTreeId, 1);

        // Получаем выученные ноды и восстанавливаем HP
        Set<String> learned = getLearnedSetFromPlayer(serverPlayer, alchemyTreeId);
        if (!learned.isEmpty()) {
            serverPlayer.heal(learned.size() * HP_PER_LEARNED_NODE);
        }

        // Сброс после 3 наполнений
        if (count >= 3) {
            fillCount.put(player, 0);
        }
    }

    /**
     * Вспомогательный метод для рейтрейса (исправленная версия)
     */
    private static BlockHitResult getPlayerPOVHitResult(Level level, Player player, ClipContext.Fluid fluidMode) {
        // Стандартная дистанция взаимодействия в Minecraft
        double reachDistance = player.isCreative() ? 5.0 : 4.5;

        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 viewVector = player.getViewVector(1.0F);
        Vec3 targetPosition = eyePosition.add(viewVector.x * reachDistance, viewVector.y * reachDistance, viewVector.z * reachDistance);

        ClipContext context = new ClipContext(
                eyePosition,
                targetPosition,
                ClipContext.Block.OUTLINE,
                fluidMode,
                player
        );

        return level.clip(context);
    }

    private static int findAlchemyTreeIdOrFallback() {
        try {
            Map<Integer, Object> trees = SkillTreeServerHandler.getTreesMap();
            if (trees == null || trees.isEmpty()) return 0;

            for (Map.Entry<Integer, Object> e : trees.entrySet()) {
                Integer treeId = e.getKey();
                Object treeObj = e.getValue();
                Map<String, ?> nodes = SkillTreeServerHandler.getNodesMap(treeObj);
                if (nodes == null) continue;
                for (String nodeId : nodes.keySet()) {
                    if (nodeId != null && nodeId.toLowerCase(Locale.ROOT).contains("alchemy")) {
                        return treeId;
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return 0;
    }

    private static Set<String> getLearnedSetFromPlayer(ServerPlayer player, int treeId) {
        try {
            var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            var mod = persisted.getCompound("damagecore_skilltree");
            var treeTag = mod.getCompound("tree_" + treeId);

            Set<String> result = new HashSet<>();
            if (treeTag.contains("learned")) {
                var listTag = treeTag.getList("learned", 8);
                for (int i = 0; i < listTag.size(); i++) result.add(listTag.getString(i));
            }
            return result;
        } catch (Throwable t) {
            t.printStackTrace();
            return Collections.emptySet();
        }
    }
}