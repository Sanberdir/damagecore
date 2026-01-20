package ru.imaginaerum.damagecore.armor;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.EnumMap;
import java.util.Map;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, DamageCore.MODID);

    private static final Map<DamageType, RegistryObject<Attribute>> RESISTANCE_ATTRIBUTES =
            new EnumMap<>(DamageType.class);

    static {
        // Регистрируем атрибуты для всех типов урона
        registerAttribute(DamageType.PIERCING);
        registerAttribute(DamageType.SLASHING);
        registerAttribute(DamageType.FIRE);
        registerAttribute(DamageType.FORCE);
        registerAttribute(DamageType.LUMINOUS_RADIANT);
        registerAttribute(DamageType.NECROTIC);
        registerAttribute(DamageType.LIGHTNING);
        registerAttribute(DamageType.POISON);
        registerAttribute(DamageType.SOUNDER);
        registerAttribute(DamageType.PSY);
        registerAttribute(DamageType.BLUDGEONING);
    }

    private static void registerAttribute(DamageType damageType) {
        String attributeName = damageType.getDamageName() + "_resistance";
        String translationKey = "attribute.damagecore." + attributeName;

        RegistryObject<Attribute> attribute = ATTRIBUTES.register(
                attributeName,
                () -> new RangedAttribute(
                        translationKey,
                        0.0,
                        0.0,
                        1024.0
                ).setSyncable(true)
        );

        RESISTANCE_ATTRIBUTES.put(damageType, attribute);
    }

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }

    public static Attribute getResistanceAttribute(DamageType damageType) {
        RegistryObject<Attribute> attribute = RESISTANCE_ATTRIBUTES.get(damageType);
        if (attribute == null) {
            throw new IllegalArgumentException("No attribute registered for damage type: " + damageType);
        }
        return attribute.get();
    }

    // Геттеры для конкретных атрибутов (если нужны)
    public static RegistryObject<Attribute> getPiercingResistance() {
        return RESISTANCE_ATTRIBUTES.get(DamageType.PIERCING);
    }

    public static RegistryObject<Attribute> getSlashingResistance() {
        return RESISTANCE_ATTRIBUTES.get(DamageType.SLASHING);
    }

    public static RegistryObject<Attribute> getBludgeoningResistance() {
        return RESISTANCE_ATTRIBUTES.get(DamageType.BLUDGEONING);
    }

}