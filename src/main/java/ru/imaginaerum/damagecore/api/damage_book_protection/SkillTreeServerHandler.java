package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.damage_book_protection.node_variant.SyncNodeVariantsPacket;
import ru.imaginaerum.damagecore.events_tree.SkillTreeXpManager;
import ru.imaginaerum.damagecore.sounds.CustomSoundEvents;

import java.util.*;

public final class SkillTreeServerHandler {
    private SkillTreeServerHandler() {}

    private static final String ROOT_KEY = "damagecore_skilltree";
    private static final int REQUIRED_LEVELS = 5;
    private static final String NODE_LEVEL_PREFIX = "node_level_";

    // --------------------------------------------------
    // Публичные методы для внешнего использования
    // --------------------------------------------------

    public static boolean isNodeLearned(ServerPlayer player, String nodeId) {
        if (player == null || nodeId == null) return false;
        for (int treeId : SkillTreeServerRegistry.getAllTreeIds()) {
            if (getNodeLevels(player, treeId).getOrDefault(nodeId, 0) > 0) return true;
        }
        return false;
    }

    public static float getNodeProgress(ServerPlayer player, int treeId, String nodeId) {
        if (player == null || nodeId == null) return 0f;
        SkillTreeNode node = SkillTreeServerRegistry.getNode(treeId, nodeId);
        if (node == null) return 0f;
        int currentLevel = getNodeLevels(player, treeId).getOrDefault(nodeId, 0);
        return Math.min(1f, (float) currentLevel / Math.max(1, node.maxLevel));
    }

    public static SkillTreeNode getNodeForPlayer(ServerPlayer player, String nodeId) {
        if (nodeId == null) return null;
        for (int treeId : SkillTreeServerRegistry.getAllTreeIds()) {
            SkillTreeNode node = SkillTreeServerRegistry.getNode(treeId, nodeId);
            if (node != null) return node;
        }
        return null;
    }

    // --------------------------------------------------
    // Основная логика изучения ноды
    // --------------------------------------------------

    public static void handleLearnRequest(ServerPlayer player, int treeId, String nodeId) {
        if (player == null || nodeId == null) return;

        try {
            if (!SkillTreeServerRegistry.hasTree(treeId)) return;

            Map<String, SkillTreeNode> nodes = SkillTreeServerRegistry.getNodes(treeId);
            SkillTreeNode node = nodes.get(nodeId);
            if (node == null) return;

            // НЕ проверяем node.locked — на сервере lock это дефолтное состояние из JSON
            // Вместо этого проверяем родителей через NBT

            Map<String, Integer> levels = getNodeLevels(player, treeId);
            int currentLevel = levels.getOrDefault(nodeId, 0);
            if (currentLevel >= node.maxLevel) return;

            // Проверка уровня вкладки ДО сохранения
            int playerTreeLevel = SkillTreeXpManager.getLevel(player, treeId);
            if (node.getRequiredTreeLevel() > playerTreeLevel) return;

            // Проверка родителей через NBT (не через node.locked!)
            for (String parentId : node.parentIds) {
                if (parentId == null || "start".equalsIgnoreCase(parentId)) continue;
                if (levels.getOrDefault(parentId, 0) <= 0) return;
            }

            // Проверка уровней игрока
            if (player.experienceLevel < REQUIRED_LEVELS) return;
            player.giveExperienceLevels(-REQUIRED_LEVELS);

            // Сохраняем
            int newLevel = Math.min(node.maxLevel, currentLevel + 1);
            saveNodeLevel(player, treeId, nodeId, newLevel);
            unlockChildren(treeId, nodeId, player);

            // Синхронизируем с клиентом
            Map<String, Integer> single = new HashMap<>();
            single.put(nodeId, newLevel);
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncNodeLevelsPacket(treeId, single)
            );

            // Звук
            player.playNotifySound(
                    CustomSoundEvents.LEARNING_SKILL.get(),
                    SoundSource.PLAYERS, 1.5f, 1f
            );

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    private static void unlockChildren(int treeId, String learnedNodeId, ServerPlayer player) {
        Map<String, SkillTreeNode> nodes = SkillTreeServerRegistry.getNodes(treeId);
        if (nodes == null) return;

        Map<String, Integer> levels = getNodeLevels(player, treeId);
        int playerTreeLevel = SkillTreeXpManager.getLevel(player, treeId);

        for (SkillTreeNode candidate : nodes.values()) {
            if (!candidate.locked) continue;

            // Проверка requiredTreeLevel
            if (candidate.getRequiredTreeLevel() > playerTreeLevel) continue;

            // Проверяем все родители
            boolean allParentsLearned = true;
            for (String parentId : candidate.parentIds) {
                if (parentId == null || "start".equalsIgnoreCase(parentId)) continue;

                int parentLevel = levels.getOrDefault(parentId, 0);
                if (parentId.equals(learnedNodeId)) parentLevel = 1;

                if (parentLevel <= 0) {
                    allParentsLearned = false;
                    break;
                }
            }

            if (allParentsLearned) {
                candidate.locked = false;
            }
        }
    }
    // --------------------------------------------------
    // Сохранение варианта ноды
    // --------------------------------------------------

    public static void saveNodeVariant(ServerPlayer player, SkillTreeNode node, int treeId) {
        if (player == null || node == null) return;

        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        root.put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, mod);

        CompoundTag treeTag = mod.getCompound("tree_" + treeId);
        mod.put("tree_" + treeId, treeTag);

        treeTag.putInt("node_variant_" + node.id, node.selectedOption);

        mod.put("tree_" + treeId, treeTag);
        persisted.put(ROOT_KEY, mod);
        root.put(Player.PERSISTED_NBT_TAG, persisted);

        System.out.println("[Server] Saved variant: " + node.id + " = " + node.selectedOption);
    }

    // --------------------------------------------------
    // Полная синхронизация при входе игрока
    // --------------------------------------------------

    public static void sendFullSyncToPlayer(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        if (mod == null) return;

        for (String key : mod.getAllKeys()) {
            if (!key.startsWith("tree_")) continue;
            try {
                int treeId = Integer.parseInt(key.substring(5));
                CompoundTag treeTag = mod.getCompound(key);

                // Уровни нод
                Map<String, Integer> levels = new HashMap<>();
                for (String tk : treeTag.getAllKeys()) {
                    if (tk.startsWith(NODE_LEVEL_PREFIX)) {
                        String nid = tk.substring(NODE_LEVEL_PREFIX.length());
                        int lvl = treeTag.getInt(tk);
                        if (lvl > 0) levels.put(nid, lvl);
                    }
                }
                if (!levels.isEmpty()) {
                    ModNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new SyncNodeLevelsPacket(treeId, levels)
                    );
                }

                // Варианты нод
                Map<String, Integer> variants = new HashMap<>();
                for (String varKey : treeTag.getAllKeys()) {
                    if (varKey.startsWith("node_variant_")) {
                        String nid = varKey.substring("node_variant_".length());
                        variants.put(nid, treeTag.getInt(varKey));
                    }
                }

                // Применяем варианты на сервере
                Map<String, SkillTreeNode> nodes = SkillTreeServerRegistry.getNodes(treeId);
                for (Map.Entry<String, Integer> entry : variants.entrySet()) {
                    SkillTreeNode node = nodes.get(entry.getKey());
                    if (node != null) node.applyVariant(entry.getValue());
                }

                // Отправляем варианты клиенту
                if (!variants.isEmpty()) {
                    ModNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new SyncNodeVariantsPacket(treeId, variants)
                    );
                }

            } catch (NumberFormatException ignored) {}
        }
    }

    // --------------------------------------------------
    // NBT helpers
    // --------------------------------------------------

    private static Map<String, Integer> getNodeLevels(ServerPlayer player, int treeId) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, mod);

        CompoundTag treeTag = mod.getCompound("tree_" + treeId);
        mod.put("tree_" + treeId, treeTag);

        Map<String, Integer> result = new HashMap<>();
        for (String key : treeTag.getAllKeys()) {
            if (key.startsWith(NODE_LEVEL_PREFIX)) {
                String nodeId = key.substring(NODE_LEVEL_PREFIX.length());
                int lvl = treeTag.getInt(key);
                if (lvl > 0) result.put(nodeId, lvl);
            }
        }
        return result;
    }

    private static void saveNodeLevel(ServerPlayer player, int treeId, String nodeId, int level) {
        if (player == null || nodeId == null) return;

        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, mod);

        CompoundTag treeTag = mod.getCompound("tree_" + treeId);
        mod.put("tree_" + treeId, treeTag);

        treeTag.putInt(NODE_LEVEL_PREFIX + nodeId, Math.max(0, level));

        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
}