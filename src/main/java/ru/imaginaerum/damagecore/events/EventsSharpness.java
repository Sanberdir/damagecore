package ru.imaginaerum.damagecore.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;

@Mod.EventBusSubscriber(modid = DamageCore.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventsSharpness {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof IDamageCoreWeapon)) return;

        int sharpness = EnchantmentHelper.getItemEnchantmentLevel(
                Enchantments.SHARPNESS, stack
        );
        if (sharpness <= 0) return;

        double bonus = 1.25 * sharpness;

        event.getToolTip().add(
                Component.literal(
                        "Острота: +" + String.format("%.1f", bonus) + " (режущий)"
                ).withStyle(ChatFormatting.DARK_PURPLE)
        );
    }
}
