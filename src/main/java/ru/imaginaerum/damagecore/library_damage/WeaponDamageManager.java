package ru.imaginaerum.damagecore.library_damage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class WeaponDamageManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private final Map<Item, WeaponDamageData> weaponData = new HashMap<>();

    public WeaponDamageManager() {
        super(GSON, "weapon_damage");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> prepared, @NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
        weaponData.clear();

        System.out.println("=== DAMAGECORE RELOAD START ===");
        System.out.println("Found " + prepared.size() + " JSON files");

        for (Map.Entry<ResourceLocation, JsonElement> entry : prepared.entrySet()) {
            try {
                ResourceLocation id = entry.getKey();
                String path = id.getPath();

                System.out.println("Processing file: " + path + " (full path: " + id + ")");

                // ПРАВИЛЬНОЕ извлечение имени предмета из пути
                // Файл: weapon_damage/iron_sword.json
                // path = "weapon_damage/iron_sword.json"
                String itemName = extractItemNameFromPath(path);

                System.out.println("Looking for item: '" + itemName + "'");

                // Ищем предмет
                Item item = findItem(itemName);

                if (item != null) {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                    JsonObject json = entry.getValue().getAsJsonObject();
                    WeaponDamageData data = WeaponDamageData.fromJson(json);
                    weaponData.put(item, data);
                    System.out.println("SUCCESS: Loaded damage data for " + itemId + " -> " + data.getDamageMap());
                } else {
                    System.out.println("FAILED: Item '" + itemName + "' not found in registry!");
                }
            } catch (Exception e) {
                System.out.println("Failed to parse weapon damage data: " + entry.getKey());
                e.printStackTrace();
            }
        }

        System.out.println("=== DAMAGECORE RELOAD COMPLETE ===");
        System.out.println("Loaded weapon damage data for " + weaponData.size() + " items");

        // Дополнительный дебаг - покажем все загруженные предметы
        System.out.println("All loaded weapon data:");
        weaponData.forEach((item, data) -> {
            System.out.println("  " + BuiltInRegistries.ITEM.getKey(item) + " -> " + data.getDamageMap());
        });
    }

    private String extractItemNameFromPath(String path) {
        // path = "weapon_damage/iron_sword.json"
        String[] parts = path.split("/");
        if (parts.length > 1) {
            String fileName = parts[parts.length - 1]; // "iron_sword.json"
            // Убираем .json
            if (fileName.endsWith(".json")) {
                return fileName.substring(0, fileName.length() - 5); // "iron_sword"
            }
            return fileName;
        }
        return path;
    }

    private Item findItem(String itemName) {
        // Пробуем minecraft неймспейс
        ResourceLocation minecraftId = new ResourceLocation("minecraft", itemName);
        Item item = BuiltInRegistries.ITEM.get(minecraftId);
        if (item != null) {
            System.out.println("Found item in minecraft: " + minecraftId);
            return item;
        }

        System.out.println("Item '" + itemName + "' not found in minecraft namespace");
        return null;
    }

    public WeaponDamageData getDamageData(Item item) {
        return weaponData.get(item);
    }

    public boolean hasCustomDamage(Item item) {
        return weaponData.containsKey(item) && !weaponData.get(item).isEmpty();
    }
}