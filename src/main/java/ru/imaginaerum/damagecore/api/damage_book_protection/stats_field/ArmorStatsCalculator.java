package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.armor.DamageArmorModifier;
import ru.imaginaerum.damagecore.armor.DamageResistance;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ArmorStatsCalculator {

    public static Map<DamageType, Float> getPlayerTotalProtectionPercent(Player player) {
        return getPlayerTotalProtectionPercent(player, ItemStack.EMPTY);
    }

    public static Map<DamageType, Float> getPlayerTotalProtectionPercent(Player player, ItemStack hoveredArmor) {
        Map<DamageType, Float> result = new EnumMap<>(DamageType.class);
        List<ItemStack> armorSet = getEffectiveArmor(player, hoveredArmor);

        // 1) Броня
        for (ItemStack stack : armorSet) {
            if (stack.getItem() instanceof ArmorItem armorItem) {
                Map<DamageType, DamageResistance> resistances =
                        DamageArmorModifier.getDamageResistances(
                                armorItem.getMaterial(),
                                armorItem.getType()
                        );

                for (Map.Entry<DamageType, DamageResistance> entry : resistances.entrySet()) {
                    result.merge(entry.getKey(), entry.getValue().getPercent(), Float::sum);
                }
            }
        }

        // 2) Еда
        FoodProtectionManager foodManager = FoodProtectionCapability.get(player);
        if (foodManager != null) {
            for (DamageType type : DamageType.values()) {
                float foodPercent = foodManager.getTotalProtectionPercent(type);
                if (foodPercent > 0) {
                    result.merge(type, foodPercent, Float::sum);
                }
            }
        }

        // 3) Зачарования — важно считать по armorSet, а не по player.getArmorSlots()
        addEnchantProtection(armorSet, result);

        // 4) Кап
        result.replaceAll((type, value) -> Math.min(0.9f, value));
        return result;
    }

    public static Map<DamageType, Float> getPlayerTotalProtectionPercentWithoutHover(Player player) {
        return getPlayerTotalProtectionPercent(player, ItemStack.EMPTY);
    }

    public static Map<DamageType, Float> getArmorOnlyProtectionPercent(Player player) {
        return getArmorOnlyProtectionPercent(player, ItemStack.EMPTY);
    }

    public static Map<DamageType, Float> getArmorOnlyProtectionPercent(Player player, ItemStack hoveredArmor) {
        Map<DamageType, Float> result = new EnumMap<>(DamageType.class);
        List<ItemStack> armorSet = getEffectiveArmor(player, hoveredArmor);

        for (ItemStack stack : armorSet) {
            if (stack.getItem() instanceof ArmorItem armorItem) {
                Map<DamageType, DamageResistance> resistances =
                        DamageArmorModifier.getDamageResistances(
                                armorItem.getMaterial(),
                                armorItem.getType()
                        );

                for (Map.Entry<DamageType, DamageResistance> entry : resistances.entrySet()) {
                    result.merge(entry.getKey(), entry.getValue().getPercent(), Float::sum);
                }
            }
        }

        result.replaceAll((type, value) -> Math.min(0.9f, value));
        return result;
    }

    private static List<ItemStack> getEffectiveArmor(Player player, ItemStack hoveredArmor) {
        List<ItemStack> result = new ArrayList<>(4);

        net.minecraft.world.entity.EquipmentSlot hoveredSlot = null;
        if (!hoveredArmor.isEmpty() && hoveredArmor.getItem() instanceof ArmorItem hoveredArmorItem) {
            hoveredSlot = hoveredArmorItem.getEquipmentSlot();
        }

        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (slot.getType() != net.minecraft.world.entity.EquipmentSlot.Type.ARMOR) continue;

            ItemStack equipped = player.getItemBySlot(slot);

            if (hoveredSlot != null && slot == hoveredSlot) {
                result.add(hoveredArmor);
            } else {
                result.add(equipped);
            }
        }

        return result;
    }

    /**
     * Здесь нужно считать чары именно по armorSet.
     * Если у тебя уже есть отдельные helper-классы,
     * перенеси их логику сюда или добавь overload'ы с List<ItemStack>.
     */
    private static void addEnchantProtection(List<ItemStack> armorSet, Map<DamageType, Float> result) {
        int protection = 0;
        int fireProtection = 0;
        int projectileProtection = 0;
        int blastProtection = 0;
        int featherFalling = 0;

        for (ItemStack stack : armorSet) {
            if (stack.isEmpty()) continue;

            net.minecraft.nbt.ListTag enchantments = stack.getEnchantmentTags();
            if (enchantments == null) continue;

            for (int i = 0; i < enchantments.size(); i++) {
                net.minecraft.nbt.CompoundTag tag = enchantments.getCompound(i);
                String id = tag.getString("id");
                int lvl = tag.getInt("lvl");

                switch (id) {
                    case "minecraft:protection" -> protection += lvl;
                    case "minecraft:fire_protection" -> fireProtection += lvl;
                    case "minecraft:projectile_protection" -> projectileProtection += lvl;
                    case "minecraft:blast_protection" -> blastProtection += lvl;
                    case "minecraft:feather_falling" -> featherFalling += lvl;
                }
            }
        }

        // Vanilla формулы (примерные, но близкие)
        float protectionPercent = protection * 0.04f;
        float firePercent = fireProtection * 0.08f;
        float projectilePercent = projectileProtection * 0.08f;
        float blastPercent = blastProtection * 0.08f;
        float fallPercent = featherFalling * 0.12f;

        if (protectionPercent > 0) {
            result.merge(DamageType.PIERCING, protectionPercent, Float::sum);
            result.merge(DamageType.SLASHING, protectionPercent, Float::sum);
            result.merge(DamageType.BLUDGEONING, protectionPercent, Float::sum);
        }

        if (firePercent > 0) {
            result.merge(DamageType.FIRE, firePercent, Float::sum);
        }

        if (projectilePercent > 0) {
            result.merge(DamageType.PIERCING, projectilePercent, Float::sum);
        }

        if (blastPercent > 0) {
            result.merge(DamageType.BLUDGEONING, blastPercent, Float::sum);
        }

        if (fallPercent > 0) {
            result.merge(DamageType.BLUDGEONING, fallPercent, Float::sum);
        }
    }
}