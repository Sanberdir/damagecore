package ru.imaginaerum.damagecore.events_tree;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.damage_book_protection.ModNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkillTreeXpManager {
    private static final Map<ServerPlayer, Map<Integer, Integer>> playerTreeXp = new ConcurrentHashMap<>();
    private static final Map<ServerPlayer, Map<Integer, Integer>> playerTreeLevel = new ConcurrentHashMap<>();

    private static final String ROOT_KEY = "damagecore_skilltree";
    private static final String XP_KEY = "tree_xp";
    private static final String LEVEL_KEY = "tree_level";

// В классе SkillTreeXpManager обновляем метод addXp

    public static void addXp(ServerPlayer player, int treeId, int amount) {
        Map<Integer, Integer> xpMap = playerTreeXp.computeIfAbsent(player, k -> new HashMap<>());
        Map<Integer, Integer> levelMap = playerTreeLevel.computeIfAbsent(player, k -> new HashMap<>());

        int currentXp = xpMap.getOrDefault(treeId, 0) + amount;
        int currentLevel = levelMap.getOrDefault(treeId, 0);

        // Повышаем уровень пока хватает XP, используя прогрессивную шкалу
        int xpRequired = getXpRequiredForLevel(currentLevel);

        while (currentXp >= xpRequired) {
            currentXp -= xpRequired;
            currentLevel++;
            xpRequired = getXpRequiredForLevel(currentLevel);
        }

        xpMap.put(treeId, currentXp);
        levelMap.put(treeId, currentLevel);

        // Сохраняем в persistentData
        saveToPersistentData(player, treeId, currentXp, currentLevel);

        // Отправляем обновление клиенту
        syncToClient(player);

        System.out.println("[SkillTreeXpManager] Player " + player.getName().getString() +
                " tree " + treeId + " XP: " + currentXp + "/" + xpRequired +
                " Level: " + currentLevel);
    }

    // Добавляем те же методы расчета XP
    private static int getXpRequiredForLevel(int level) {
        if (level < 0) return 0;
        return (int) Math.floor(BASE_XP_PER_LEVEL * Math.pow(XP_GROWTH_FACTOR, level));
    }

    // Добавляем константы (можно вынести в общий класс)
    private static final int BASE_XP_PER_LEVEL = 3;
    private static final double XP_GROWTH_FACTOR = 1.5;

    public static void loadFromPersistentData(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag mod = persisted.getCompound(ROOT_KEY);

        Map<Integer, Integer> xpMap = new HashMap<>();
        Map<Integer, Integer> levelMap = new HashMap<>();

        if (mod.contains(XP_KEY)) {
            CompoundTag xpTag = mod.getCompound(XP_KEY);
            for (String key : xpTag.getAllKeys()) {
                try {
                    xpMap.put(Integer.parseInt(key), xpTag.getInt(key));
                } catch (NumberFormatException ignored) {}
            }
        }

        if (mod.contains(LEVEL_KEY)) {
            CompoundTag levelTag = mod.getCompound(LEVEL_KEY);
            for (String key : levelTag.getAllKeys()) {
                try {
                    levelMap.put(Integer.parseInt(key), levelTag.getInt(key));
                } catch (NumberFormatException ignored) {}
            }
        }

        playerTreeXp.put(player, xpMap);
        playerTreeLevel.put(player, levelMap);

        // Отправляем клиенту
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncTreeXpPacket(xpMap, levelMap));
    }

    private static void saveToPersistentData(ServerPlayer player, int treeId, int xp, int level) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);

        CompoundTag mod = persisted.getCompound(ROOT_KEY);
        persisted.put(ROOT_KEY, mod);

        CompoundTag xpTag = mod.getCompound(XP_KEY);
        CompoundTag levelTag = mod.getCompound(LEVEL_KEY);

        xpTag.putInt(String.valueOf(treeId), xp);
        levelTag.putInt(String.valueOf(treeId), level);

        mod.put(XP_KEY, xpTag);
        mod.put(LEVEL_KEY, levelTag);
    }

    private static void syncToClient(ServerPlayer player) {
        Map<Integer, Integer> xpMap = playerTreeXp.getOrDefault(player, new HashMap<>());
        Map<Integer, Integer> levelMap = playerTreeLevel.getOrDefault(player, new HashMap<>());

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncTreeXpPacket(xpMap, levelMap));
    }

    public static void removePlayer(ServerPlayer player) {
        playerTreeXp.remove(player);
        playerTreeLevel.remove(player);
    }
}