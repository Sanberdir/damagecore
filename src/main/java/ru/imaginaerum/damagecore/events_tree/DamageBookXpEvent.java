package ru.imaginaerum.damagecore.events_tree;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import ru.imaginaerum.damagecore.api.damage_book_protection.DamageBookRenderer;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

import java.util.*;

/**
 * ПКМ по блоку даёт XP именно дереву "alchemy" и восстанавливает здоровье.
 */
@Mod.EventBusSubscriber
public class DamageBookXpEvent {

    private static final Map<Player, Integer> clickCount = new HashMap<>();
    private static final float HP_PER_LEARNED_NODE = 1.0f;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        int count = clickCount.getOrDefault(player, 0) + 1;
        clickCount.put(player, count);

        // 🔹 строго дерево "alchemy"
        int alchemyTreeId = findAlchemyTreeIdOrFallback();

        // даём XP в дерево alchemy через менеджер
        SkillTreeXpManager.addXp(serverPlayer, alchemyTreeId, 1);

        // получаем выученные ноды и восстанавливаем HP
        Set<String> learned = getLearnedSetFromPlayer(serverPlayer, alchemyTreeId);
        if (!learned.isEmpty()) {
            serverPlayer.heal(learned.size() * HP_PER_LEARNED_NODE);
        }

        // сброс после 3 кликов
        if (count >= 3) clickCount.put(player, 0);
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