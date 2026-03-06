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

    public static void loadAllTrees(String folderPath) {
        System.out.println("SkillTreeLoader.loadAllTrees() called");
        SkillTreeRenderer.loadAllTrees(folderPath);
    }

    private static final Gson GSON = new Gson();

    /**
     * Загружает JSON-файл дерева.
     * Новый формат поддерживает "parents" (массив) на замену "parent".
     */
    @SuppressWarnings("unchecked")
    public static Object[] loadFromResource(String pathInNamespace) {
        List<SkillTreeNode> result = new ArrayList<>();
        ItemStack tabIconStack = ItemStack.EMPTY;

        try {
            ResourceLocation rl = new ResourceLocation("damagecore", pathInNamespace);
            Optional<Resource> optResource = Minecraft.getInstance().getResourceManager().getResource(rl);

            if (optResource.isEmpty()) {
                return new Object[] { tabIconStack, result };
            }

            Resource resource = optResource.get();
            try (InputStream is = resource.open();
                 Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                JsonElement rootEl = JsonParser.parseReader(reader);
                if (!rootEl.isJsonObject()) {
                    return new Object[] { tabIconStack, result };
                }

                JsonObject root = rootEl.getAsJsonObject();

                // Читаем tabIcon
                if (root.has("tabIcon") && root.get("tabIcon").isJsonPrimitive()) {
                    try {
                        String iconId = root.get("tabIcon").getAsString();
                        if (iconId != null && !iconId.isBlank()) {
                            ResourceLocation iconRL = new ResourceLocation(iconId);
                            Item maybe = ForgeRegistries.ITEMS.getValue(iconRL);
                            tabIconStack = maybe != null ? new ItemStack(maybe) : new ItemStack(Items.BARRIER);
                        }
                    } catch (Exception ignored) {
                        tabIconStack = new ItemStack(Items.BARRIER);
                    }
                }

                // Читаем nodes
                if (root.has("nodes") && root.get("nodes").isJsonArray()) {
                    JsonArray arr = root.getAsJsonArray("nodes");
                    for (JsonElement el : arr) {
                        if (!el.isJsonObject()) continue;
                        JsonObject obj = el.getAsJsonObject();

                        String id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();
                        String itemStr = obj.has("item") ? obj.get("item").getAsString() : null;
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
                            // Нет родителя - корневой узел
                            parentIds.add("start");
                        }

                        String sideStr = obj.has("side") ? obj.get("side").getAsString().toUpperCase(Locale.ROOT) : "RIGHT";

                        // Получаем Item
                        Item item = Items.AIR;
                        if (itemStr != null && !itemStr.isBlank()) {
                            try {
                                ResourceLocation itemRL = new ResourceLocation(itemStr);
                                Item maybe = ForgeRegistries.ITEMS.getValue(itemRL);
                                if (maybe != null) item = maybe;
                            } catch (Exception ignored) {}
                        }

                        SkillTreeNode.Side side;
                        try {
                            side = SkillTreeNode.Side.valueOf(sideStr);
                        } catch (Exception ex) {
                            side = SkillTreeNode.Side.RIGHT;
                        }

                        // Создаем узел с несколькими родителями
                        SkillTreeNode node = new SkillTreeNode(id, new ItemStack(item), lock, parentIds, side);

                        // grid позиции
                        if (obj.has("gridX") && obj.has("gridY")) {
                            try {
                                int gx = obj.get("gridX").getAsInt();
                                int gy = obj.get("gridY").getAsInt();
                                node.setGridPos(gx, gy);
                            } catch (Exception ignored) {}
                        }

                        // Варианты
                        if (obj.has("variants") && obj.get("variants").isJsonArray()) {
                            JsonArray vars = obj.getAsJsonArray("variants");
                            for (JsonElement ve : vars) {
                                if (!ve.isJsonObject()) continue;
                                JsonObject vo = ve.getAsJsonObject();
                                if (!vo.has("item")) continue;

                                String variantId = vo.has("id") ? vo.get("id").getAsString() : UUID.randomUUID().toString();
                                String variantItemStr = vo.get("item").getAsString();
                                if (variantItemStr == null || variantItemStr.isBlank()) continue;

                                try {
                                    ResourceLocation itemRL = new ResourceLocation(variantItemStr);
                                    Item maybe = ForgeRegistries.ITEMS.getValue(itemRL);
                                    if (maybe != null) {
                                        SkillTreeNode.Variant variant = new SkillTreeNode.Variant(variantId, new ItemStack(maybe));
                                        node.variants.add(variant);
                                        node.options.add(variant.stack);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }

                        result.add(node);
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new Object[] { tabIconStack, result };
    }
}