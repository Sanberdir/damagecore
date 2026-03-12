package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.damage_book_protection.node_variant.SyncNodeVariantsPacket;
import ru.imaginaerum.damagecore.events_tree.SyncTreeXpPacket;
import ru.imaginaerum.damagecore.sounds.CustomSoundEvents;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Server-side handler for skill tree: learning + node variant persistence helpers.
 */
public final class SkillTreeServerHandler {
    private static final String ROOT_KEY = "damagecore_skilltree";
    private static final int REQUIRED_LEVELS = 5;
    // Добавить константы в SkillTreeServerHandler:
    private static final String XP_KEY = "tree_xp";
    private static final String LEVEL_KEY = "tree_level";
    private static final String NODE_LEVEL_PREFIX = "node_level_";

    private SkillTreeServerHandler() {}
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
                try {
                    int lvl = treeTag.getInt(key);
                    if (lvl > 0) result.put(nodeId, lvl);
                } catch (Exception ignored) {}
            }
        }
        return result;
    }
    /**
     * Возвращает объект дерева по ID (рефлексия)
     */
    public static Object getTreeById(int treeId) {
        return getTreeObject(treeId); // уже есть в классе
    }

    /**
     * Возвращает прогресс конкретной ноды для игрока в диапазоне 0..1
     */
    public static float getNodeProgress(ServerPlayer player, int treeId, String nodeId) {
        if (player == null || nodeId == null) return 0f;
        try {
            Map<String, SkillTreeNode> nodes = getNodesMap(getTreeById(treeId));
            if (nodes == null || !nodes.containsKey(nodeId)) return 0f;

            SkillTreeNode node = nodes.get(nodeId);
            Map<String, Integer> levels = getNodeLevels(player, treeId);
            int currentLevel = levels.getOrDefault(nodeId, 0);

            return Math.min(1f, (float) currentLevel / Math.max(1, node.maxLevel));
        } catch (Throwable t) {
            t.printStackTrace();
            return 0f;
        }
    }
    // Сохраняет уровень для конкретной ноды
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
    /** Возвращает Map всех деревьев (treeId -> дерево) через рефлексию */
    @SuppressWarnings("unchecked")
    public static Map<Integer, Object> getTreesMap() {
        try {
            Class<?> cls = Class.forName("ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer");
            Field treesField = cls.getDeclaredField("trees");
            treesField.setAccessible(true);
            Object obj = treesField.get(null);
            if (obj instanceof Map<?, ?> map) return (Map<Integer, Object>) map;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return Collections.emptyMap();
    }

    // ------------------------------
    // Сохранение выбранного варианта
    // ------------------------------
    public static void saveNodeVariant(ServerPlayer player, SkillTreeNode node, int treeId) {
        if (player == null || node == null) return;
        System.out.println("Saving variant for node " + node.id + " = " + node.selectedOption);
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, mod);

        CompoundTag treeTag = mod.getCompound("tree_" + treeId);
        mod.put("tree_" + treeId, treeTag);

        treeTag.putInt("node_variant_" + node.id, node.selectedOption);

        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    // ------------------------------
    // Изучение ноды
    // ------------------------------
    // Обновляем метод handleLearnRequest для проверки нескольких родителей

    public static void handleLearnRequest(ServerPlayer player, int treeId, String nodeId) {
        try {
            if (!SkillTreeRenderer.hasTreeForTab(treeId)) return;

            Object treeObj = getTreeObject(treeId);
            if (treeObj == null) return;

            Map<String, SkillTreeNode> nodes = getNodesMap(treeObj);
            if (nodes == null || !nodes.containsKey(nodeId)) return;

            SkillTreeNode node = nodes.get(nodeId);
            if (node == null) return;

            if (node.locked) return;

            // читаем текущие уровни на сервере
            Map<String, Integer> levels = getNodeLevels(player, treeId);
            int currentLevel = levels.getOrDefault(nodeId, 0);

            // не даём учиться выше maxLevel
            if (currentLevel >= node.maxLevel) return;

            // Проверяем всех родителей — каждый должен быть хотя бы level >= 1
            for (String parentId : node.parentIds) {
                if (parentId != null && !"start".equalsIgnoreCase(parentId)) {
                    int pLevel = levels.getOrDefault(parentId, 0);

                    // если отсутствует в сохранённых — пробуем взять значение из server-side node (если есть)
                    if (pLevel <= 0) {
                        SkillTreeNode pnode = nodes.get(parentId);
                        if (pnode != null) pLevel = pnode.level;
                    }

                    if (pLevel <= 0) {
                        // хотя бы один родитель не изучен — нельзя изучить
                        return;
                    }
                }
            }

            // Проверка уровня опыта
            if (player.experienceLevel < REQUIRED_LEVELS) return;
            player.giveExperienceLevels(-REQUIRED_LEVELS);

            // увеличиваем уровень и сохраняем
            int newLevel = Math.min(node.maxLevel, currentLevel + 1);
            saveNodeLevel(player, treeId, nodeId, newLevel);

            // Отправляем синхронизацию (можно отправить только изменённую ноду или весь map)
            Map<String, Integer> single = new HashMap<>();
            single.put(nodeId, newLevel);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncNodeLevelsPacket(treeId, single));

            player.playNotifySound(CustomSoundEvents.LEARNING_SKILL.get(), SoundSource.PLAYERS, 1.5f, 1f);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    // ------------------------------
    // Рефлексия для дерева
    // ------------------------------
    public static Object getTreeObject(int treeId) {
        try {
            Class<?> cls = Class.forName("ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer");
            Field treesField = cls.getDeclaredField("trees");
            treesField.setAccessible(true);
            Object treesObj = treesField.get(null);
            if (!(treesObj instanceof Map)) return null;
            return ((Map<?, ?>) treesObj).get(treeId);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getNodesMapAsObjects(Object treeObj) {
        Map<String, SkillTreeNode> nodes = getNodesMap(treeObj);
        if (nodes == null) return Collections.emptyMap();
        return new HashMap<>(nodes); // автоматически приведется к Map<String,Object>
    }
    @SuppressWarnings("unchecked")
    public static Map<String, SkillTreeNode> getNodesMap(Object treeObj) {
        if (treeObj == null) return null;
        try {
            Field nodesField = treeObj.getClass().getDeclaredField("nodes");
            nodesField.setAccessible(true);
            Object obj = nodesField.get(treeObj);
            if (obj instanceof Map) return (Map<String, SkillTreeNode>) obj;
        } catch (Throwable t) { t.printStackTrace(); }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static SkillTreeNode getNodeForPlayer(ServerPlayer player, String nodeId) {
        if (nodeId == null) return null;
        try {
            Class<?> cls = Class.forName("ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer");
            Field treesField = cls.getDeclaredField("trees");
            treesField.setAccessible(true);
            Object treesObj = treesField.get(null);
            if (!(treesObj instanceof Map)) return null;
            Map<?, ?> trees = (Map<?, ?>) treesObj;
            for (Object treeObj : trees.values()) {
                Map<String, SkillTreeNode> nodes = getNodesMap(treeObj);
                if (nodes == null) continue;
                SkillTreeNode n = nodes.get(nodeId);
                if (n != null) return n;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    // ------------------------------
    // Сохранение/чтение изученных нод
    // ------------------------------
    private static Set<String> getLearnedSet(ServerPlayer player, int treeId) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, mod);

        CompoundTag treeTag = mod.getCompound("tree_" + treeId);
        mod.put("tree_" + treeId, treeTag);

        Set<String> result = new HashSet<>();
        if (treeTag.contains("learned")) {
            ListTag listTag = treeTag.getList("learned", 8);
            for (int i = 0; i < listTag.size(); i++) result.add(listTag.getString(i));
        }
        return result;
    }

    private static void saveLearnedSet(ServerPlayer player, int treeId, Set<String> learned) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, mod);

        CompoundTag treeTag = mod.getCompound("tree_" + treeId);
        mod.put("tree_" + treeId, treeTag);

        ListTag list = new ListTag();
        for (String s : learned) list.add(StringTag.valueOf(s));
        treeTag.put("learned", list);

        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    // ------------------------------
    // Полная синхронизация прогресса
    // ------------------------------
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

                // --- уровни нод ---
                Map<String, Integer> levels = new HashMap<>();
                for (String tk : treeTag.getAllKeys()) {
                    if (tk.startsWith(NODE_LEVEL_PREFIX)) {
                        String nodeId = tk.substring(NODE_LEVEL_PREFIX.length());
                        int lvl = treeTag.getInt(tk);
                        if (lvl > 0) levels.put(nodeId, lvl);
                    }
                }
                if (!levels.isEmpty()) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new SyncNodeLevelsPacket(treeId, levels));
                }

                // --- варианты нод (как было) ---
                Map<String, Integer> variants = new HashMap<>();
                for (String varKey : treeTag.getAllKeys()) {
                    if (varKey.startsWith("node_variant_")) {
                        String nodeId = varKey.substring("node_variant_".length());
                        variants.put(nodeId, treeTag.getInt(varKey));
                    }
                }
                if (!variants.isEmpty()) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new SyncNodeVariantsPacket(treeId, variants));
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}