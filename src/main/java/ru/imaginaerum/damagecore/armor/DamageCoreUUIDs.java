package ru.imaginaerum.damagecore.armor;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ArmorItem;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.EnumMap;
import java.util.UUID;

public class DamageCoreUUIDs {
    private static final EnumMap<ArmorItem.Type, EnumMap<DamageType, UUID>> ARMOR_UUIDS = new EnumMap<>(ArmorItem.Type.class);

    static {
        // Инициализируем UUID для каждого сочетания типа брони и типа урона
        for (ArmorItem.Type armorType : ArmorItem.Type.values()) {
            EnumMap<DamageType, UUID> damageMap = new EnumMap<>(DamageType.class);
            for (DamageType damageType : DamageType.values()) {
                String uuidString = generateUUIDString(armorType, damageType);
                damageMap.put(damageType, UUID.fromString(uuidString));
            }
            ARMOR_UUIDS.put(armorType, damageMap);
        }
    }

    private static String generateUUIDString(ArmorItem.Type armorType, DamageType damageType) {
        // Генерируем детерминированные UUID на основе типа брони и типа урона
        String base = armorType.getName() + "_" + damageType.getDamageName();
        byte[] bytes = base.getBytes();

        // Преобразуем в UUID формат (8-4-4-4-12)
        long mostSigBits = 0;
        long leastSigBits = 0;

        for (int i = 0; i < 8; i++) {
            mostSigBits = (mostSigBits << 8) | (bytes[i % bytes.length] & 0xff);
        }

        for (int i = 8; i < 16; i++) {
            leastSigBits = (leastSigBits << 8) | (bytes[i % bytes.length] & 0xff);
        }

        // Устанавливаем версию UUID (4) и variant (2)
        mostSigBits = (mostSigBits & 0xFFFF0FFFFFFFFFL) | 0x4000L; // version 4
        leastSigBits = (leastSigBits & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L; // variant 2

        return String.format("%016x-%04x-%04x-%04x-%012x",
                mostSigBits >>> 32,
                (mostSigBits >>> 16) & 0xFFFF,
                mostSigBits & 0xFFFF,
                leastSigBits >>> 48,
                leastSigBits & 0xFFFFFFFFFFFFL);
    }

    public static UUID getUUIDFor(ArmorItem.Type armorType, DamageType damageType) {
        return ARMOR_UUIDS.get(armorType).get(damageType);
    }

    public static Attribute getAttributeForDamageType(DamageType damageType) {
        // Используем метод из ModAttributes вместо прямого доступа
        return ModAttributes.getResistanceAttribute(damageType);
    }
}