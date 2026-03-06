package ru.imaginaerum.damagecore.api.damage_book_protection.node_variant;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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

            // Читаем parentIds из NBT, если они там сохранены
            List<String> parentIds = new ArrayList<>();
            if (nodeTag.contains("parentIds")) {
                ListTag parentsList = nodeTag.getList("parentIds", 8); // 8 = String
                for (int i = 0; i < parentsList.size(); i++) {
                    parentIds.add(parentsList.getString(i));
                }
            } else if (nodeTag.contains("parentId")) {
                // Для обратной совместимости
                parentIds.add(nodeTag.getString("parentId"));
            } else {
                parentIds.add("start");
            }

            // Читаем side из NBT
            SkillTreeNode.Side side = SkillTreeNode.Side.START;
            if (nodeTag.contains("side")) {
                try {
                    side = SkillTreeNode.Side.valueOf(nodeTag.getString("side"));
                } catch (Exception ignored) {}
            }

            // Создаём ноду с правильным конструктором (список родителей)
            SkillTreeNode node = new SkillTreeNode(
                    nodeId,
                    ItemStack.EMPTY, // itemStack временно пустой, заполнится из варианта
                    false, // locked
                    parentIds,
                    side
            );

            // Читаем выбранный вариант
            node.selectedOption = nodeTag.getInt("selectedOption");

            // Читаем и восстанавливаем варианты, если они сохранены
            if (nodeTag.contains("variants")) {
                ListTag variantsList = nodeTag.getList("variants", 10); // 10 = Compound
                for (int i = 0; i < variantsList.size(); i++) {
                    CompoundTag variantTag = variantsList.getCompound(i);
                    String variantId = variantTag.getString("id");
                    // Здесь нужно восстановить ItemStack из NBT
                    // Это сложнее, возможно, стоит хранить только ID предмета
                    ItemStack stack = ItemStack.EMPTY; // Временно

                    SkillTreeNode.Variant variant = new SkillTreeNode.Variant(variantId, stack);
                    node.variants.add(variant);
                    node.options.add(stack);
                }
            }

            // Применяем выбранный вариант, если есть
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