package ru.imaginaerum.damagecore.api.damage_book_protection.node_variant;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер серверной стороны SkillTree с поддержкой сохранения выбранных вариантов.
 */
public class ServerSkillTreeManager {

    private static final String TREE_TAG_KEY = "damagecore_skill_tree";

    /**
     * Получаем все ноды игрока. Если нет сохранённых данных — возвращаем пустой список.
     */
    public static List<SkillTreeNode> getPlayerSkillTreeNodes(ServerPlayer player) {
        List<SkillTreeNode> nodes = new ArrayList<>();

        CompoundTag treeTag = player.getPersistentData().getCompound(TREE_TAG_KEY);
        for (String nodeId : treeTag.getAllKeys()) {
            CompoundTag nodeTag = treeTag.getCompound(nodeId);

            // Создаём ноду с id и выбранным вариантом
            SkillTreeNode node = new SkillTreeNode(
                    nodeId,
                    null, // itemStack можно инициализировать из варианта позже
                    false, // locked
                    null,  // parentId
                    SkillTreeNode.Side.START
            );

            // Читаем выбранный вариант
            node.selectedOption = nodeTag.getInt("selectedOption");

            // TODO: при наличии вариантов: можно сразу применить выбранный вариант
            if (node.variants != null && !node.variants.isEmpty() && node.selectedOption >= 0) {
                node.applyVariant(node.selectedOption);
            }

            nodes.add(node);
        }

        return nodes;
    }

    /**
     * Получить конкретную ноду игрока по id.
     */
    public static SkillTreeNode getNode(ServerPlayer player, String nodeId) {
        for (SkillTreeNode n : getPlayerSkillTreeNodes(player)) {
            if (n.id.equals(nodeId)) return n;
        }
        return null;
    }

    /**
     * Сохраняем выбор варианта ноды в persistent data игрока.
     */
    public static void saveNodeOption(ServerPlayer player, SkillTreeNode node) {
        if (player == null || node == null) return;

        CompoundTag treeTag = player.getPersistentData().getCompound(TREE_TAG_KEY);
        CompoundTag nodeTag = treeTag.getCompound(node.id);

        nodeTag.putInt("selectedOption", node.selectedOption);

        treeTag.put(node.id, nodeTag);
        player.getPersistentData().put(TREE_TAG_KEY, treeTag);
    }
}