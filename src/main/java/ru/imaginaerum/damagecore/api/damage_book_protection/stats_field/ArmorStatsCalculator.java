package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.api.damage_book_protection.protection_helpers.*;
import ru.imaginaerum.damagecore.armor.DamageArmorModifier;
import ru.imaginaerum.damagecore.armor.DamageResistance;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;

import java.util.EnumMap;
import java.util.Map;

public class ArmorStatsCalculator {

    /**
     * Итоговая процентная защита игрока
     * (БРОНЯ + ЕДА)
     */
    public static Map<DamageType, Float> getPlayerTotalProtectionPercent(Player player) {
        Map<DamageType, Float> result = new EnumMap<>(DamageType.class);

        // ===== 1. БРОНЯ =====
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem armorItem) {

                Map<DamageType, DamageResistance> resistances =
                        DamageArmorModifier.getDamageResistances(
                                armorItem.getMaterial(),
                                armorItem.getType()
                        );

                for (Map.Entry<DamageType, DamageResistance> entry : resistances.entrySet()) {
                    DamageType type = entry.getKey();
                    float percent = entry.getValue().getPercent();

                    result.merge(type, percent, Float::sum);
                }
            }
        }

        // ===== 2. ЕДА =====
        FoodProtectionManager foodManager = FoodProtectionCapability.get(player);
        if (foodManager != null) {
            for (DamageType type : DamageType.values()) {
                float foodPercent = foodManager.getTotalProtectionPercent(type);
                if (foodPercent > 0) {
                    result.merge(type, foodPercent, Float::sum);
                }
            }
        }

        // ===== 3. ЗАЧАРОВАНИЯ =====
        for (DamageType type : DamageType.values()) {
            float enchantPercent = 0f;

            switch (type) {
                case FIRE -> enchantPercent +=
                        FireProtectionHelper.getEnchantProtectionPercent(player, type);

                case PIERCING -> enchantPercent +=
                        ProjectileProtectionHelper.getEnchantProtectionPercent(player, type);

                case BLUDGEONING -> {
                    enchantPercent += ExplosionProtectionHelper.getEnchantProtectionPercent(player, type);
                    enchantPercent += FallProtectionHelper.getEnchantProtectionPercent(player, type);
                }

                case SLASHING -> enchantPercent +=
                        ProtectionHelper.getEnchantProtectionPercent(player, type);
            }

            // Protection работает на ВСЕ физические типы
            if (type == DamageType.PIERCING
                    || type == DamageType.SLASHING
                    || type == DamageType.BLUDGEONING) {

                enchantPercent += ProtectionHelper.getEnchantProtectionPercent(player, type);
            }

            if (enchantPercent > 0) {
                result.merge(type, enchantPercent, Float::sum);
            }
        }

        // ===== 4. Ограничение =====
        result.replaceAll((type, value) -> Math.min(0.9f, value));

        return result;
    }

    /**
     * Только защита от брони (без еды)
     */
    public static Map<DamageType, Float> getArmorOnlyProtectionPercent(Player player) {
        Map<DamageType, Float> result = new EnumMap<>(DamageType.class);

        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem armorItem) {

                Map<DamageType, DamageResistance> resistances =
                        DamageArmorModifier.getDamageResistances(
                                armorItem.getMaterial(),
                                armorItem.getType()
                        );

                for (Map.Entry<DamageType, DamageResistance> entry : resistances.entrySet()) {
                    DamageType type = entry.getKey();
                    float percent = entry.getValue().getPercent();

                    result.merge(type, percent, Float::sum);
                }
            }
        }

        result.replaceAll((type, value) -> Math.min(0.9f, value));

        return result;
    }
}