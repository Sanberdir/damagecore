package ru.imaginaerum.damagecore.api.damage_book_protection;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SkillTreeLoader {
    private SkillTreeLoader() {}

    private static final Gson GSON = new Gson();


    public static List<SkillTreeNode> loadFromResource(String pathInNamespace) {
        List<SkillTreeNode> result = new ArrayList<>();

        try {
            ResourceLocation rl = new ResourceLocation("damagecore", pathInNamespace);
            Optional<Resource> optResource = Minecraft.getInstance().getResourceManager().getResource(rl);

            if (optResource.isEmpty()) return result;

            Resource resource = optResource.get();
            try (InputStream is = resource.open(); // <--- открываем InputStream
                 Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonArray()) return result;
                JsonArray arr = root.getAsJsonArray();

                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();

                    String id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();
                    String itemStr = obj.has("item") ? obj.get("item").getAsString() : null;
                    boolean lock = obj.has("lock") && obj.get("lock").getAsBoolean();
                    String parent = obj.has("parent") ? obj.get("parent").getAsString() : "start";
                    String sideStr = obj.has("side") ? obj.get("side").getAsString().toUpperCase(Locale.ROOT) : "RIGHT";

                    // Попытка получить Item через ForgeRegistries
                    Item item = Items.AIR;
                    if (itemStr != null && !itemStr.isBlank()) {
                        try {
                            ResourceLocation itemRL = new ResourceLocation(itemStr);
                            Item maybe = ForgeRegistries.ITEMS.getValue(itemRL);
                            if (maybe != null) item = maybe;
                        } catch (Exception ignored) {
                        }
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

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }
}
