package ru.imaginaerum.damagecore.armor;

import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class DamageArmorModifier {
    private static final Map<ArmorMaterial, Map<ArmorItem.Type, Map<DamageType, Float>>> VANILLA_MODIFIERS = new HashMap<>();

    static {
        // Инициализируем модификаторы для ванильных материалов
        initializeVanillaModifiers();
    }

    private static void initializeVanillaModifiers() {
        // ЖЕЛЕЗНАЯ БРОНЯ
        Map<ArmorItem.Type, Map<DamageType, Float>> ironModifiers = new EnumMap<>(ArmorItem.Type.class);

        // Шлем железный
        ironModifiers.put(ArmorItem.Type.HELMET, Map.of(
                DamageType.SLASHING, 2.0f,
                DamageType.BLUDGEONING, 3.0f
        ));

        // Кираса железная
        ironModifiers.put(ArmorItem.Type.CHESTPLATE, Map.of(
                DamageType.PIERCING, 4.0f,
                DamageType.SLASHING, 3.0f,
                DamageType.BLUDGEONING, 2.0f
        ));

        // Поножи железные
        ironModifiers.put(ArmorItem.Type.LEGGINGS, Map.of(
                DamageType.PIERCING, 2.0f,
                DamageType.SLASHING, 2.0f
        ));

        // Ботинки железные
        ironModifiers.put(ArmorItem.Type.BOOTS, Map.of(
                DamageType.BLUDGEONING, 1.0f
        ));

        VANILLA_MODIFIERS.put(net.minecraft.world.item.ArmorMaterials.IRON, ironModifiers);

        // АЛМАЗНАЯ БРОНЯ
        Map<ArmorItem.Type, Map<DamageType, Float>> diamondModifiers = new EnumMap<>(ArmorItem.Type.class);

        diamondModifiers.put(ArmorItem.Type.HELMET, Map.of(
                DamageType.PIERCING, 3.0f,
                DamageType.SLASHING, 4.0f,
                DamageType.BLUDGEONING, 3.0f
        ));

        diamondModifiers.put(ArmorItem.Type.CHESTPLATE, Map.of(
                DamageType.PIERCING, 5.0f,
                DamageType.SLASHING, 4.0f,
                DamageType.BLUDGEONING, 3.0f,
                DamageType.FIRE, 2.0f
        ));

        diamondModifiers.put(ArmorItem.Type.LEGGINGS, Map.of(
                DamageType.PIERCING, 3.0f,
                DamageType.SLASHING, 3.0f,
                DamageType.FIRE, 1.0f
        ));

        diamondModifiers.put(ArmorItem.Type.BOOTS, Map.of(
                DamageType.BLUDGEONING, 2.0f,
                DamageType.LIGHTNING, 1.0f
        ));

        VANILLA_MODIFIERS.put(net.minecraft.world.item.ArmorMaterials.DIAMOND, diamondModifiers);

        // НЕЗЕРИТОВАЯ БРОНЯ
        Map<ArmorItem.Type, Map<DamageType, Float>> netheriteModifiers = new EnumMap<>(ArmorItem.Type.class);

        netheriteModifiers.put(ArmorItem.Type.HELMET, Map.of(
                DamageType.PIERCING, 4.0f,
                DamageType.SLASHING, 5.0f,
                DamageType.BLUDGEONING, 4.0f,
                DamageType.FIRE, 6.0f,
                DamageType.NECROTIC, 2.0f
        ));

        netheriteModifiers.put(ArmorItem.Type.CHESTPLATE, Map.of(
                DamageType.PIERCING, 6.0f,
                DamageType.SLASHING, 5.0f,
                DamageType.BLUDGEONING, 4.0f,
                DamageType.FIRE, 8.0f,
                DamageType.FORCE, 3.0f
        ));

        // Добавьте другие материалы по аналогии...
    }

    public static Map<DamageType, Float> getDamageResistances(ArmorMaterial material, ArmorItem.Type type) {
        Map<ArmorItem.Type, Map<DamageType, Float>> materialModifiers = VANILLA_MODIFIERS.get(material);
        if (materialModifiers != null) {
            return materialModifiers.getOrDefault(type, Map.of());
        }
        return Map.of();
    }

    public static float getDamageResistance(ArmorMaterial material, ArmorItem.Type type, DamageType damageType) {
        return getDamageResistances(material, type).getOrDefault(damageType, 0.0f);
    }

    public static boolean hasVanillaModifiers(ArmorMaterial material) {
        return VANILLA_MODIFIERS.containsKey(material);
    }
}