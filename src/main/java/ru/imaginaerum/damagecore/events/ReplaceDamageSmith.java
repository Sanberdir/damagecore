package ru.imaginaerum.damagecore.events;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.library_damage.DamageContext;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.Map;

@Mod.EventBusSubscriber(modid = "damagecore")
public class ReplaceDamageSmith {

    /**
     * Здесь мы читаем накопленную map и логируем все типы урона отдельно.
     * После логирования контекст удаляется через consumeMap.
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        Map<DamageType, Float> breakdown = DamageContext.consumeMap(target);
        // контекст уже очищен consumeMap
    }

    /**
     * Здесь мы добавляем бонус Smite как отдельный вклад в breakdown,
     * и одновременно прибавляем его к суммарному event.amount — без вызова entity.hurt.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker)) return;

        LivingEntity target = event.getEntity();
        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty()) return;

        int smiteLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, weapon);

        // Smite действует только по нежити (как в ваниле)
        if (smiteLevel <= 0 || target.getMobType() != net.minecraft.world.entity.MobType.UNDEAD) {
            return;
        }

        float smiteBonus = 2.5f * smiteLevel;

        // 1) Просто добавляем бонус к общему количеству урона в event
        event.setAmount(event.getAmount() + smiteBonus);

        // 2) И аккумулируем breakdown по типу LUMINOUS_RADIANT
        DamageContext.add(target, DamageType.LUMINOUS_RADIANT, smiteBonus);

        // Если нужно: также можно добавить базовый тип/часть урона в breakdown,
        // но это зависит от того, где ты определяешь базовый DamageType.
        // Например, если до этого уже что-то установило базовый тип — не трогай.
    }
}