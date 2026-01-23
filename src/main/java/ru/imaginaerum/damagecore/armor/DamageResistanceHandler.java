package ru.imaginaerum.damagecore.armor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;

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

        // Применяем защиты в порядке: сначала вся абсолютная, потом вся процентная
        if (!allResistances.isEmpty()) {
            // Суммируем абсолютную защиту
            float totalFlat = allResistances.stream()
                    .map(DamageResistance::getFlat)
                    .reduce(0f, Float::sum);

            // Суммируем процентную защиту (максимум 90%)
            float totalPercent = allResistances.stream()
                    .map(DamageResistance::getPercent)
                    .reduce(0f, Float::sum);
            totalPercent = Math.min(1.0f, totalPercent); // Ограничиваем максимум 90%

            // Применяем формулу: (Урон - Абсолютная защита) * (1 - Процентная защита)
            float afterFlat = Math.max(0, incomingDamage - totalFlat);
            float finalDamage = afterFlat * (1 - totalPercent);

            event.setAmount(finalDamage);

        }

        // 🧹 3. ОБЯЗАТЕЛЬНО чистим контекст
        DamageContext.clear(entity);
    }
}