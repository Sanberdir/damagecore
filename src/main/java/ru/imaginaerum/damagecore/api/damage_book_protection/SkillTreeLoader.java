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
     * Загружает JSON-файл дерева по пути внутри namespace (например "skill_tree/foo.json").
     * Возвращает Object[] где:
     *  - [0] = ItemStack tabIcon (ItemStack.EMPTY если не указан / не найден)
     *  - [1] = List<SkillTreeNode> list (список нод, возможно пустой)
     *
     * Формат ожидаемого JSON:
     * {
     *   "tabIcon": "minecraft:diamond_sword",   // опционально
     *   "nodes": [ { ... }, { ... } ]
     * }
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

                // --- читаем tabIcon (опционально) ---
                if (root.has("tabIcon") && root.get("tabIcon").isJsonPrimitive()) {
                    try {
                        String iconId = root.get("tabIcon").getAsString();
                        if (iconId != null && !iconId.isBlank()) {
                            ResourceLocation iconRL = new ResourceLocation(iconId);
                            Item maybe = ForgeRegistries.ITEMS.getValue(iconRL);
                            if (maybe != null) {
                                tabIconStack = new ItemStack(maybe);
                            } else {
                                tabIconStack = new ItemStack(Items.BARRIER); // явный фолбек
                            }
                        }
                    } catch (Exception ignored) {
                        tabIconStack = new ItemStack(Items.BARRIER);
                    }
                }

                // --- читаем nodes (массив) ---
                if (root.has("nodes") && root.get("nodes").isJsonArray()) {
                    JsonArray arr = root.getAsJsonArray("nodes");
                    for (JsonElement el : arr) {
                        if (!el.isJsonObject()) continue;
                        JsonObject obj = el.getAsJsonObject();

                        String id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();
                        String itemStr = obj.has("item") ? obj.get("item").getAsString() : null;
                        boolean lock = obj.has("lock") && obj.get("lock").getAsBoolean();
                        String parent = obj.has("parent") ? obj.get("parent").getAsString() : "start";
                        String sideStr = obj.has("side") ? obj.get("side").getAsString().toUpperCase(Locale.ROOT) : "RIGHT";

                        // Получаем Item через ForgeRegistries
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

                        SkillTreeNode node = new SkillTreeNode(id, new ItemStack(item), lock, parent, side);

                        // optional grid positions: gridX / gridY (integers)
                        if (obj.has("gridX") && obj.has("gridY")) {
                            try {
                                int gx = obj.get("gridX").getAsInt();
                                int gy = obj.get("gridY").getAsInt();
                                node.setGridPos(gx, gy);
                            } catch (Exception ignored) {}
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