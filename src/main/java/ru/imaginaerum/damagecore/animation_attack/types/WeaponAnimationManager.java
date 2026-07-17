package ru.imaginaerum.damagecore.animation_attack.types;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_weapon_types.WeaponType;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class WeaponAnimationManager implements net.minecraft.server.packs.resources.PreparableReloadListener {

    public static final WeaponAnimationManager INSTANCE = new WeaponAnimationManager();
    private static final Gson GSON = new Gson();

    private final Map<WeaponType, WeaponAnimationSet> animationSets = new HashMap<>();

    public WeaponAnimationSet getAnimationSet(WeaponType type) {
        if (type == null) return null;
        return animationSets.get(type);
    }

    @Override
    public CompletableFuture<Void> reload(
            PreparationBarrier barrier,
            ResourceManager manager,
            ProfilerFiller prepProfiler,
            ProfilerFiller applyProfiler,
            Executor prepExecutor,
            Executor applyExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Map<WeaponType, WeaponAnimationSet> loaded = new HashMap<>();

            Map<ResourceLocation, Resource> resources =
                    manager.listResources("weapon_animations", loc -> loc.getPath().endsWith(".json"));

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try (var reader = new InputStreamReader(
                        entry.getValue().open(), StandardCharsets.UTF_8)) {

                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (!json.has("type") || !json.has("swing_animations")) continue;

                    String typeName = json.get("type").getAsString().toUpperCase();
                    WeaponType type;
                    try {
                        type = WeaponType.valueOf(typeName);
                    } catch (IllegalArgumentException e) {
                        System.err.println("[WeaponAnimations] Unknown weapon type: " + typeName
                                + " in " + entry.getKey());
                        continue;
                    }

                    List<SwingAnimationEntry> swings = new ArrayList<>();
                    JsonArray swingArray = json.getAsJsonArray("swing_animations");
                    for (var el : swingArray) {
                        JsonObject obj = el.getAsJsonObject();
                        if (!obj.has("animation")) continue;

                        ResourceLocation animLoc = new ResourceLocation(obj.get("animation").getAsString());

                        DamageType damageType = DamageType.SLASHING;
                        if (obj.has("damage_type")) {
                            try {
                                damageType = DamageType.valueOf(obj.get("damage_type").getAsString().toUpperCase());
                            } catch (IllegalArgumentException e) {
                                System.err.println("[WeaponAnimations] Unknown damage type in " + entry.getKey());
                            }
                        }

                        boolean headLookAtCamera = obj.has("head_look_at_camera")
                                && obj.get("head_look_at_camera").getAsBoolean();

                        swings.add(new SwingAnimationEntry(animLoc, damageType, headLookAtCamera));
                    }

                    ResourceLocation strongAttack = json.has("strong_attack")
                            ? new ResourceLocation(json.get("strong_attack").getAsString())
                            : null;

                    loaded.put(type, new WeaponAnimationSet(swings, strongAttack));

                } catch (Exception e) {
                    System.err.println("[WeaponAnimations] Failed to load " + entry.getKey());
                    e.printStackTrace();
                }
            }
            return loaded;
        }, prepExecutor).thenCompose(barrier::wait).thenAcceptAsync(loaded -> {
            animationSets.clear();
            animationSets.putAll(loaded);
        }, applyExecutor);
    }
}