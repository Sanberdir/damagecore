package ru.imaginaerum.damagecore.armor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = DamageCore.MODID)
public class DamageResistanceHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        // 🔥 1. ПРИОРИТЕТ — тип из контекста (яд, эффекты)
        DamageType damageType = DamageContext.getLast(entity);

        // 2. Если эффекта не было — обычная логика
        if (damageType == null) {
            damageType = DamageHelper.getDamageType(event.getSource());
        }

        if (damageType == null) return;

        float incomingDamage = event.getAmount();

        // Собираем все защиты от каждой части брони
        List<DamageResistance> allResistances = new ArrayList<>();

        for (ItemStack stack : entity.getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem armorItem) {
                DamageResistance resistance = DamageArmorModifier.getDamageResistance(
                        armorItem.getMaterial(),
                        armorItem.getType(),
                        damageType
                );
                if (resistance.getFlat() > 0 || resistance.getPercent() > 0) {
                    allResistances.add(resistance);
                }
            }
        }

        // Добавляем защиту от эффектов еды (только для игроков)
        float foodProtectionPercent = 0.0f;
        if (entity instanceof Player player) {
            FoodProtectionManager foodManager = FoodProtectionCapability.get(player);
            if (foodManager != null) {
                foodProtectionPercent = foodManager.getTotalProtectionPercent(damageType);
            }
        }

        // Применяем защиты в порядке: сначала вся абсолютная, потом вся процентная
        float totalFlat = 0f;
        float totalPercent = 0f;

        if (!allResistances.isEmpty()) {
            // Суммируем абсолютную защиту
            totalFlat = allResistances.stream()
                    .map(DamageResistance::getFlat)
                    .reduce(0f, Float::sum);

            // Суммируем процентную защиту от брони
            totalPercent = allResistances.stream()
                    .map(DamageResistance::getPercent)
                    .reduce(0f, Float::sum);
        }

        // Добавляем защиту от еды (только процентная)
        totalPercent += foodProtectionPercent;

        // Ограничиваем суммарную процентную защиту максимум 90%
        totalPercent = Math.min(1.0f, totalPercent);

        // Применяем формулу: (Урон - Абсолютная защита) * (1 - Процентная защита)
        float afterFlat = Math.max(0, incomingDamage - totalFlat);
        float finalDamage = afterFlat * (1 - totalPercent);

        event.setAmount(finalDamage);

        // 🧹 3. ОБЯЗАТЕЛЬНО чистим контекст
        DamageContext.clear(entity);
    }
}