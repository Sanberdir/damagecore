package ru.imaginaerum.damagecore.armor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = DamageCore.MODID)
public class DamageArmorModifier extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    private static DamageArmorModifier INSTANCE;

    private final Map<String, ArmorMaterialConfig> materialConfigs = new ConcurrentHashMap<>();
    private final Map<ArmorMaterial, Map<ArmorItem.Type, Map<DamageType, Float>>> cachedModifiers = new ConcurrentHashMap<>();

    public DamageArmorModifier() {
        super(GSON, "damage_armor_modifiers");
        INSTANCE = this;
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new DamageArmorModifier());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        materialConfigs.clear();
        cachedModifiers.clear();

        resources.forEach((resourceLocation, jsonElement) -> {
            try {
                ArmorMaterialConfig config = GSON.fromJson(jsonElement, ArmorMaterialConfig.class);
                materialConfigs.put(resourceLocation.getPath(), config);
                cacheMaterialModifiers(config);
            } catch (Exception e) {
                LOGGER.error("Failed to load armor modifiers from: {}", resourceLocation, e);
            }
        });

        // Загружаем стандартные модификаторы как fallback
        initializeDefaultModifiers();
    }

    private void cacheMaterialModifiers(ArmorMaterialConfig config) {
        Map<ArmorItem.Type, Map<DamageType, Float>> typeModifiers = new EnumMap<>(ArmorItem.Type.class);

        config.helmet.forEach((damageType, value) ->
                typeModifiers.put(ArmorItem.Type.HELMET, parseDamageTypes(config.helmet)));

        config.chestplate.forEach((damageType, value) ->
                typeModifiers.put(ArmorItem.Type.CHESTPLATE, parseDamageTypes(config.chestplate)));

        config.leggings.forEach((damageType, value) ->
                typeModifiers.put(ArmorItem.Type.LEGGINGS, parseDamageTypes(config.leggings)));

        config.boots.forEach((damageType, value) ->
                typeModifiers.put(ArmorItem.Type.BOOTS, parseDamageTypes(config.boots)));

        ArmorMaterial material = getMaterialByName(config.material);
        if (material != null) {
            cachedModifiers.put(material, typeModifiers);
        }
    }

    private Map<DamageType, Float> parseDamageTypes(Map<String, Float> source) {
        Map<DamageType, Float> result = new HashMap<>();
        if (source != null) {
            source.forEach((key, value) -> {
                try {
                    DamageType damageType = DamageType.valueOf(key.toUpperCase());
                    result.put(damageType, value);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Unknown damage type: {}", key);
                }
            });
        }
        return result;
    }

    private ArmorMaterial getMaterialByName(String materialName) {
        return switch (materialName.toLowerCase()) {
            case "leather" -> ArmorMaterials.LEATHER;
            case "chainmail" -> ArmorMaterials.CHAIN;
            case "iron" -> ArmorMaterials.IRON;
            case "gold", "golden" -> ArmorMaterials.GOLD;
            case "diamond" -> ArmorMaterials.DIAMOND;
            case "netherite" -> ArmorMaterials.NETHERITE;
            case "turtle" -> ArmorMaterials.TURTLE;
            default -> {
                LOGGER.warn("Unknown armor material: {}", materialName);
                yield null;
            }
        };
    }

    private void initializeDefaultModifiers() {
        // Эти значения будут использоваться если нет конфига в ресурспаке
        // или как fallback значения
        Map<ArmorItem.Type, Map<DamageType, Float>> defaultIron = new EnumMap<>(ArmorItem.Type.class);
        defaultIron.put(ArmorItem.Type.HELMET, Map.of(DamageType.SLASHING, 2.0f, DamageType.BLUDGEONING, 3.0f));
        defaultIron.put(ArmorItem.Type.CHESTPLATE, Map.of(DamageType.PIERCING, 4.0f, DamageType.SLASHING, 3.0f, DamageType.BLUDGEONING, 2.0f));
        defaultIron.put(ArmorItem.Type.LEGGINGS, Map.of(DamageType.PIERCING, 2.0f, DamageType.SLASHING, 2.0f));
        defaultIron.put(ArmorItem.Type.BOOTS, Map.of(DamageType.BLUDGEONING, 1.0f));

        if (!cachedModifiers.containsKey(ArmorMaterials.IRON)) {
            cachedModifiers.put(ArmorMaterials.IRON, defaultIron);
        }
    }

    public static Map<DamageType, Float> getDamageResistances(ArmorMaterial material, ArmorItem.Type type) {
        if (INSTANCE == null) return Map.of();

        Map<ArmorItem.Type, Map<DamageType, Float>> materialModifiers =
                INSTANCE.cachedModifiers.get(material);

        if (materialModifiers != null) {
            return materialModifiers.getOrDefault(type, Map.of());
        }
        return Map.of();
    }

    public static float getDamageResistance(ArmorMaterial material, ArmorItem.Type type, DamageType damageType) {
        return getDamageResistances(material, type).getOrDefault(damageType, 0.0f);
    }

    public static boolean hasModifiers(ArmorMaterial material) {
        return INSTANCE != null && INSTANCE.cachedModifiers.containsKey(material);
    }
}