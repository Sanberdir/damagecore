package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.armor.DamageArmorModifier;
import ru.imaginaerum.damagecore.armor.DamageResistance;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.EnumMap;
import java.util.Map;

public final class DamageBookStateCollector {
    private DamageBookStateCollector() {}

    public static Map<DamageType, DamageResistance> collectTotalResistances(Player player) {
        Map<DamageType, DamageResistance> totals = new EnumMap<>(DamageType.class);
        if (player == null) return totals;

        for (ItemStack stack : player.getArmorSlots()) {
            if (!(stack.getItem() instanceof ArmorItem armorItem)) continue;

            Map<DamageType, DamageResistance> part =
                    DamageArmorModifier.getDamageResistances(armorItem.getMaterial(), armorItem.getType());

            for (Map.Entry<DamageType, DamageResistance> e : part.entrySet()) {
                DamageResistance dr = e.getValue();
                if (dr.getFlat() <= 0 && dr.getPercent() <= 0) continue;

                totals.merge(
                        e.getKey(),
                        new DamageResistance(dr.getFlat(), dr.getPercent()),
                        (a, b) -> new DamageResistance(
                                a.getFlat() + b.getFlat(),
                                Math.min(1.0f, a.getPercent() + b.getPercent())
                        )
                );
            }
        }
        return totals;
    }
}
