package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.damage_book_protection.node_variant.SyncNodeVariantsPacket;
import ru.imaginaerum.damagecore.sounds.CustomSoundEvents;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Server-side handler for skill tree: learning + node variant persistence helpers.
 */
public final class SkillTreeServerHandler {
    private static final String ROOT_KEY = "damagecore_skilltree";
    private static final int REQUIRED_LEVELS = 5;

    private SkillTreeServerHandler() {}

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

            if (node.locked) return;

            Set<String> learned = getLearnedSet(player, treeId);

            // ИСПРАВЛЕНИЕ: Проверяем ВСЕХ родителей (должны быть изучены ВСЕ)
            for (String parentId : node.parentIds) {
                // Пропускаем "start" (корневой узел)
                if (parentId != null && !"start".equalsIgnoreCase(parentId)) {
                    // Если хотя бы один родитель не изучен - нельзя изучить узел
                    if (!learned.contains(parentId)) {
                        return; // Родитель не изучен - отказываем
                    }
                }
            }

            // Проверка на уже изученный узел
            if (!learned.add(nodeId)) return;

            // Проверка уровней опыта
            if (player.experienceLevel < REQUIRED_LEVELS) return;
            player.giveExperienceLevels(-REQUIRED_LEVELS);

            saveLearnedSet(player, treeId, learned);

            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncLearnedNodesPacket(treeId, new ArrayList<>(learned)));

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

                // изученные ноды
                List<String> learnedList = new ArrayList<>();
                ListTag lt = treeTag.getList("learned", 8);
                for (int i = 0; i < lt.size(); i++) learnedList.add(lt.getString(i));
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new SyncLearnedNodesPacket(treeId, learnedList));

                // варианты нод
                Map<String, Integer> variants = new HashMap<>();
                for (String varKey : treeTag.getAllKeys()) {
                    if (varKey.startsWith("node_variant_")) {
                        String nodeId = varKey.substring("node_variant_".length());
                        variants.put(nodeId, treeTag.getInt(varKey));
                    }
                }
                if (!variants.isEmpty()) {
                    System.out.println("Sending variants for tree " + treeId + ": " + variants); // ДОБАВИТЬ
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new SyncNodeVariantsPacket(treeId, variants));
                }
            } catch (NumberFormatException ignored) {}
        }
    }
}