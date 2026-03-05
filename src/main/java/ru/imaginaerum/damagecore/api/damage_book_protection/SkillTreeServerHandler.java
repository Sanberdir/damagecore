package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer;
import ru.imaginaerum.damagecore.sounds.CustomSoundEvents;

import java.lang.reflect.Field;
import java.util.*;

public final class SkillTreeServerHandler {
    private static final String ROOT_KEY = "damagecore_skilltree";
    private static final int REQUIRED_LEVELS = 5;

    private SkillTreeServerHandler() {}

    /** Обработка запроса изучения ноды от клиента */
    public static void handleLearnRequest(ServerPlayer player, int treeId, String nodeId) {
        try {
            if (!SkillTreeRenderer.hasTreeForTab(treeId)) return;

            Object treeObj = getTreeObject(treeId);
            if (treeObj == null) return;

            Map<?, ?> nodes = getNodesMap(treeObj);
            if (nodes == null || !nodes.containsKey(nodeId)) return;

            SkillTreeNode node = (SkillTreeNode) nodes.get(nodeId);

            // НОВАЯ ПРОВЕРКА: если нода заблокирована - не изучаем
            if (node.locked) return; // заблокированные ноды нельзя изучить

            // Получаем/создаем серверный набор изученных нод
            Set<String> learned = getLearnedSet(player, treeId);

            // НОВАЯ ПРОВЕРКА: зависимость от родителя — нельзя изучить, если родитель не изучен
            String parentId = node.parentId;
            if (parentId != null && !"start".equalsIgnoreCase(parentId)) {
                if (!learned.contains(parentId)) {
                    // попытка изучить без изучения родителя — отклоняем
                    return;
                }
            }

            if (!learned.add(nodeId)) return; // уже изучено

            // Проверка уровней
            if (player.experienceLevel < REQUIRED_LEVELS) return;
            player.giveExperienceLevels(-REQUIRED_LEVELS);

            // Сохраняем в NBT
            saveLearnedSet(player, treeId, learned);

            // Отправляем клиенту обновлённый список
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncLearnedNodesPacket(treeId, new ArrayList<>(learned)));

            // Звук изучения
            player.playNotifySound(CustomSoundEvents.LEARNING_SKILL.get(), SoundSource.PLAYERS, 1.5f, 1f);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** Получаем объект дерева через рефлексию */
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

    /** Получаем Map<String, SkillTreeNode> из дерева */
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

    /** Получить набор изученных нод игрока для конкретного дерева */
    private static Set<String> getLearnedSet(ServerPlayer player, int treeId) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, mod);

        String tagName = "tree_" + treeId;
        Set<String> result = new HashSet<>();
        if (mod.contains(tagName)) {
            ListTag listTag = mod.getList(tagName, 8);
            for (int i = 0; i < listTag.size(); i++) result.add(listTag.getString(i));
        }
        return result;
    }

    /** Сохраняем изученные ноды игрока в NBT */
    private static void saveLearnedSet(ServerPlayer player, int treeId, Set<String> set) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, mod);

        ListTag listTag = new ListTag();
        for (String s : set) listTag.add(StringTag.valueOf(s));
        mod.put("tree_" + treeId, listTag);
        persisted.put(ROOT_KEY, mod);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Отправка полного прогресса игроку (при входе в мир) */
    public static void sendFullSyncToPlayer(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        if (mod == null) return;

        for (String key : mod.getAllKeys()) {
            if (!key.startsWith("tree_")) continue;
            try {
                int treeId = Integer.parseInt(key.substring(5));
                List<String> list = new ArrayList<>();
                ListTag lt = mod.getList(key, 8);
                for (int i = 0; i < lt.size(); i++) list.add(lt.getString(i));

                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new SyncLearnedNodesPacket(treeId, list));
            } catch (NumberFormatException ignored) {}
        }
    }
}