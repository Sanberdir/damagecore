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
        float totalResistance = 0.0f;

        for (ItemStack stack : entity.getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem armorItem) {
                totalResistance += DamageArmorModifier.getDamageResistance(
                        armorItem.getMaterial(),
                        armorItem.getType(),
                        damageType
                );
            }
        }

        event.setAmount(Math.max(0.0f, incomingDamage - totalResistance));

        // 🧹 3. ОБЯЗАТЕЛЬНО чистим контекст
        DamageContext.clear(entity);
    }
}
