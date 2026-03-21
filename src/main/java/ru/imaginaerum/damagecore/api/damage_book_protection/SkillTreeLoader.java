package ru.imaginaerum.damagecore.api.damage_book_protection;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SkillTreeLoader {
    private SkillTreeLoader() {}

    private static final Gson GSON = new Gson();

    /**
     * Загружает JSON-файл дерева.
     * Поддерживает:
     * - множественных родителей (parents)
     * - максимальный уровень узла (maxLevel)
     * - начальный уровень узла (level)
     * - варианты выбора (variants)
     * - явные grid координаты
     */
    @SuppressWarnings("unchecked")
    public static Object[] loadFromResource(String pathInNamespace) {
        List<SkillTreeNode> result = new ArrayList<>();
        ItemStack tabIconStack = ItemStack.EMPTY;

        try {
            ResourceLocation rl = new ResourceLocation("damagecore", pathInNamespace);
            Optional<Resource> optResource = Minecraft.getInstance().getResourceManager().getResource(rl);

            if (optResource.isEmpty()) {
                System.err.println("Resource not found: " + pathInNamespace);
                return new Object[] { tabIconStack, result };
            }

            Resource resource = optResource.get();
            try (InputStream is = resource.open();
                 Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                JsonElement rootEl = JsonParser.parseReader(reader);
                if (!rootEl.isJsonObject()) {
                    System.err.println("Invalid JSON: root is not an object in " + pathInNamespace);
                    return new Object[] { tabIconStack, result };
                }

                JsonObject root = rootEl.getAsJsonObject();

                // Читаем tabIcon (поддержка как строки, так и объекта)
                if (root.has("tabIcon")) {
                    tabIconStack = parseItemStack(root.get("tabIcon"));
                }

                // Читаем nodes
                if (root.has("nodes") && root.get("nodes").isJsonArray()) {
                    JsonArray arr = root.getAsJsonArray("nodes");
                    for (JsonElement el : arr) {
                        if (!el.isJsonObject()) continue;
                        JsonObject obj = el.getAsJsonObject();

                        // Обязательные поля
                        String id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();

                        // Предмет узла
                        ItemStack itemStack = ItemStack.EMPTY;
                        if (obj.has("item")) {
                            itemStack = parseItemStack(obj.get("item"));
                        }

                        // Блокировка (по умолчанию false)
                        boolean lock = obj.has("lock") && obj.get("lock").getAsBoolean();

                        // --- Чтение родителей (поддержка множественных) ---
                        List<String> parentIds = new ArrayList<>();

                        if (obj.has("parents") && obj.get("parents").isJsonArray()) {
                            // Новый формат: массив родителей
                            JsonArray parentsArray = obj.getAsJsonArray("parents");
                            for (JsonElement pEl : parentsArray) {
                                if (pEl.isJsonPrimitive()) {
                                    parentIds.add(pEl.getAsString());
                                }
                            }
                        } else if (obj.has("parent") && obj.get("parent").isJsonPrimitive()) {
                            // Старый формат: один родитель (для обратной совместимости)
                            parentIds.add(obj.get("parent").getAsString());
                        } else {
                            // Нет родителя - корневой узел (добавляем "start" для совместимости)
                            parentIds.add("start");
                        }

                        // Сторона расположения
                        String sideStr = obj.has("side") ? obj.get("side").getAsString().toUpperCase(Locale.ROOT) : "RIGHT";
                        SkillTreeNode.Side side;
                        try {
                            side = SkillTreeNode.Side.valueOf(sideStr);
                        } catch (Exception ex) {
                            side = SkillTreeNode.Side.RIGHT;
                        }

                        // Создаем узел с несколькими родителями
                        SkillTreeNode node = new SkillTreeNode(id, itemStack, lock, parentIds, side);

                        // ===== ВАЖНО: загружаем максимальный уровень =====
                        if (obj.has("maxLevel")) {
                            try {
                                int maxLevel = obj.get("maxLevel").getAsInt();
                                node.setMaxLevel(Math.max(1, maxLevel)); // минимум 1
                            } catch (Exception e) {
                                System.err.println("Invalid maxLevel for node " + id + ": " + e.getMessage());
                            }
                        }
                        if (obj.has("requiredTreeLevel")) {
                            try {
                                int req = obj.get("requiredTreeLevel").getAsInt();
                                node.setRequiredTreeLevel(Math.max(0, req));
                            } catch (Exception e) {
                                System.err.println("Invalid requiredTreeLevel for node " + id + ": " + e.getMessage());
                            }
                        }
                        // ===== ВАЖНО: загружаем начальный уровень =====
                        if (obj.has("level")) {
                            try {
                                int level = obj.get("level").getAsInt();
                                node.setLevel(level); // setLevel сам ограничит через maxLevel
                            } catch (Exception e) {
                                System.err.println("Invalid level for node " + id + ": " + e.getMessage());
                            }
                        }

                        // Явные grid позиции (опционально)
                        if (obj.has("gridX") && obj.has("gridY")) {
                            try {
                                int gx = obj.get("gridX").getAsInt();
                                int gy = obj.get("gridY").getAsInt();
                                node.setGridPos(gx, gy);
                            } catch (Exception ignored) {}
                        }

                        // ===== Загрузка вариантов (variants) =====
                        if (obj.has("variants") && obj.get("variants").isJsonArray()) {
                            JsonArray vars = obj.getAsJsonArray("variants");
                            for (JsonElement ve : vars) {
                                if (ve.isJsonObject()) {
                                    // Полный формат: объект с id и item
                                    JsonObject vo = ve.getAsJsonObject();

                                    String variantId = vo.has("id") ? vo.get("id").getAsString() : UUID.randomUUID().toString();
                                    ItemStack variantStack = ItemStack.EMPTY;

                                    if (vo.has("item")) {
                                        variantStack = parseItemStack(vo.get("item"));
                                    }

                                    if (!variantStack.isEmpty()) {
                                        SkillTreeNode.Variant variant = new SkillTreeNode.Variant(variantId, variantStack);
                                        node.variants.add(variant);
                                        node.options.add(variantStack);
                                    }

                                } else if (ve.isJsonPrimitive()) {
                                    // Простой формат: строка с ID предмета
                                    String itemId = ve.getAsString();
                                    ItemStack variantStack = parseItemStackFromString(itemId);

                                    if (!variantStack.isEmpty()) {
                                        // Создаем ID на основе предмета
                                        String variantId = itemId.replace(':', '_').replace('/', '_');
                                        SkillTreeNode.Variant variant = new SkillTreeNode.Variant(variantId, variantStack);
                                        node.variants.add(variant);
                                        node.options.add(variantStack);
                                    }
                                }
                            }
                        }

                        result.add(node);
                    }
                }
            }

        } catch (Exception ex) {
            System.err.println("Failed to load skill tree from " + pathInNamespace + ": " + ex.getMessage());
            ex.printStackTrace();
        }

        return new Object[] { tabIconStack, result };
    }

    /**
     * Парсит ItemStack из JsonElement (поддержка строки и объекта)
     */
    private static ItemStack parseItemStack(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return ItemStack.EMPTY;
        }

        if (element.isJsonPrimitive()) {
            // Простой формат: "minecraft:diamond"
            String itemId = element.getAsString();
            return parseItemStackFromString(itemId);

        } else if (element.isJsonObject()) {
            // Сложный формат: {"item": "minecraft:diamond", "count": 3}
            JsonObject obj = element.getAsJsonObject();

            if (!obj.has("item")) {
                return ItemStack.EMPTY;
            }

            String itemId = obj.get("item").getAsString();
            ItemStack stack = parseItemStackFromString(itemId);

            if (obj.has("count")) {
                try {
                    stack.setCount(obj.get("count").getAsInt());
                } catch (Exception ignored) {}
            }

            // Здесь можно добавить загрузку NBT, если нужно
            // if (obj.has("nbt")) { ... }

            return stack;
        }

        return ItemStack.EMPTY;
    }

    /**
     * Парсит ItemStack из строки с ID предмета
     */
    private static ItemStack parseItemStackFromString(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return ItemStack.EMPTY;
        }

        try {
            ResourceLocation itemRL = new ResourceLocation(itemId);
            Item maybe = ForgeRegistries.ITEMS.getValue(itemRL);

            if (maybe != null && maybe != Items.AIR) {
                return new ItemStack(maybe);
            } else {
                System.err.println("Unknown item: " + itemId);
                return new ItemStack(Items.BARRIER); // Заглушка для неизвестных предметов
            }
        } catch (Exception e) {
            System.err.println("Failed to parse item: " + itemId + " - " + e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    /**
     * Утилита для загрузки только узлов (для обратной совместимости)
     */
    public static List<SkillTreeNode> loadNodes(String pathInNamespace) {
        Object[] result = loadFromResource(pathInNamespace);
        if (result != null && result.length > 1 && result[1] instanceof List) {
            return (List<SkillTreeNode>) result[1];
        }
        return new ArrayList<>();
    }
}