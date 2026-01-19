package ru.imaginaerum.damagecore.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.entities.DCEntityTagList;

@Mod.EventBusSubscriber(modid = "damagecore")
public class ArthropodsDamageEvent {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        LivingEntity target = event.getEntity();
        ItemStack weapon = attacker.getMainHandItem();

        int baneLevel = EnchantmentHelper.getItemEnchantmentLevel(
                Enchantments.BANE_OF_ARTHROPODS,
                weapon
        );

        if (baneLevel <= 0) return;

        if (!DCEntityTagList.ARTHROPOD.matches(target)) return;

        float bonusDamage = baneLevel * 2.5f;
        event.setAmount(event.getAmount() + bonusDamage);
    }
}
