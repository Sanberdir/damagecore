package ru.imaginaerum.damagecore.libraty_effects;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.*;

public class FoodProtectionReloadListener extends SimpleJsonResourceReloadListener {
    public static final Map<Item, List<Effect>> EFFECTS = new HashMap<>();

    public FoodProtectionReloadListener() {
        super(new Gson(), "food_protection");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler) {
        EFFECTS.clear();

        for (JsonElement element : jsons.values()) {
            JsonObject json = element.getAsJsonObject();

            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(json.get("item").getAsString()));
            if (item == null) continue;

            List<Effect> effects = new ArrayList<>();
            JsonArray array = json.getAsJsonArray("effects");

            boolean overrideVanilla = json.has("override_vanilla_effects") && json.get("override_vanilla_effects").getAsBoolean();
            List<ResourceLocation> removeEffects = new ArrayList<>();
            if (json.has("remove_effects")) {
                JsonArray rem = json.getAsJsonArray("remove_effects");
                for (JsonElement r : rem) {
                    removeEffects.add(new ResourceLocation(r.getAsString()));
                }
            }

            for (JsonElement e : array) {
                JsonObject o = e.getAsJsonObject();
                DamageType dtype = DamageType.valueOf(o.get("damage_type").getAsString());
                float prot = o.get("protection").getAsFloat();
                int duration = o.get("duration").getAsInt();

                effects.add(new Effect(dtype, prot, duration, overrideVanilla, removeEffects));
            }

            EFFECTS.put(item, effects);
        }
    }

    public record Effect(
            DamageType damageType,
            float protection,
            int duration,
            boolean overrideVanilla,
            List<ResourceLocation> removeEffects
    ) {}
}
