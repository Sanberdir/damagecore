package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.armor.DamageArmorModifier;
import ru.imaginaerum.damagecore.armor.DamageResistance;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.EnumMap;
import java.util.Map;

public class ArmorStatsCalculator {

    public static Map<DamageType, Float> getPlayerTotalProtectionPercent(Player player) {
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

        // Ограничиваем максимум 90%
        result.replaceAll((type, value) -> Math.min(0.9f, value));

        return result;
    }
}