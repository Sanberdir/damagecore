package ru.imaginaerum.damagecore.api.damage_book_protection.protection_helpers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import ru.imaginaerum.damagecore.library_damage.DamageType;

public final class ProtectionHelper {
    // central value — меняй здесь, и все потребители увидят изменение
    public static float PROTECTION_PER_LEVEL = 0.05f; // 5% за уровень

    private ProtectionHelper() {}

    public static float getProtectionPerLevel() {
        return PROTECTION_PER_LEVEL;
    }

    /** Возвращает защиту от чар (в долях, 0..1) для конкретной сущности и типа урона. */
    public static float getEnchantProtectionPercent(LivingEntity entity, DamageType type) {
        if (entity == null) return 0f;
        if (type != DamageType.PIERCING
                && type != DamageType.SLASHING
                && type != DamageType.BLUDGEONING) {
            return 0f;
        }

        // EnchantmentHelper.getEnchantmentLevel возвращает суммарный уровень чар по всей экипировке/предметам
        int totalLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, entity);
        return totalLevel * getProtectionPerLevel();
    }
}
