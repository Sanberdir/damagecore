package ru.imaginaerum.damagecore.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TooltipEventHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (!(stack.getItem() instanceof ArmorItem armorItem)) return;

        Map<DamageType, DamageResistance> resistances = DamageArmorModifier.getDamageResistances(
                armorItem.getMaterial(),
                armorItem.getType()
        );

        // Собираем процентные бонусы от чар этого конкретного предмета
        Map<DamageType, Float> enchantPercents = new EnumMap<>(DamageType.class);
        for (DamageType type : DamageType.values()) {
            float p = getItemEnchantProtection(stack, type);
            if (p > 0) enchantPercents.put(type, p);
        }

        // Объединяем материал + чары
        Map<DamageType, DamageResistance> combined = new EnumMap<>(DamageType.class);
        Set<DamageType> allTypes = new HashSet<>(resistances.keySet());
        allTypes.addAll(enchantPercents.keySet());

        for (DamageType type : allTypes) {
            DamageResistance base = resistances.getOrDefault(type, new DamageResistance(0, 0));
            float extraPercent = enchantPercents.getOrDefault(type, 0f);
            float totalPercent = Math.min(1f, base.getPercent() + extraPercent);
            combined.put(type, new DamageResistance(base.getFlat(), totalPercent));
        }

        if (!combined.isEmpty()) {
            event.getToolTip().add(Component.literal(""));
            event.getToolTip().add(Component.literal("Защита от типов урона:")
                    .withStyle(ChatFormatting.GRAY));

            for (Map.Entry<DamageType, DamageResistance> entry : combined.entrySet()) {
                DamageResistance res = entry.getValue();
                if (res.getFlat() > 0 || res.getPercent() > 0) {
                    String damageName = getTranslatedDamageName(entry.getKey());
                    boolean hasEnchant = enchantPercents.containsKey(entry.getKey());
                    ChatFormatting color = hasEnchant ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
                    Component line = Component.literal("  " + damageName + ": " + res)
                            .withStyle(color);
                    event.getToolTip().add(line);
                }
            }
        }

        event.getToolTip().removeIf(component -> {
            String text = component.getString().toLowerCase();
            return text.contains("armor") || text.contains("броня") ||
                    text.contains("toughness") || text.contains("прочность") ||
                    text.matches(".*\\+\\s*\\d+.*(armor|броня).*");
        });
    }

    private static float getItemEnchantProtection(ItemStack stack, DamageType type) {
        // Создаём временный список из одного предмета чтобы переиспользовать хелперы
        // Хелперы принимают Player, поэтому проще продублировать логику прямо здесь

        var enchants = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack);
        float sum = 0f;

        // Protection — физические типы
        if (type == DamageType.PIERCING || type == DamageType.SLASHING || type == DamageType.BLUDGEONING) {
            int lvl = enchants.getOrDefault(
                    net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION, 0);
            if (lvl > 0) sum += lvl * 0.04f; // ваша формула из ProtectionHelper
        }

        switch (type) {
            case FIRE -> {
                int lvl = enchants.getOrDefault(
                        net.minecraft.world.item.enchantment.Enchantments.FIRE_PROTECTION, 0);
                if (lvl > 0) sum += lvl * 0.08f; // ваша формула из FireProtectionHelper
            }
            case PIERCING -> {
                int lvl = enchants.getOrDefault(
                        net.minecraft.world.item.enchantment.Enchantments.PROJECTILE_PROTECTION, 0);
                if (lvl > 0) sum += lvl * 0.08f;
            }
            case BLUDGEONING -> {
                int lvl1 = enchants.getOrDefault(
                        net.minecraft.world.item.enchantment.Enchantments.BLAST_PROTECTION, 0);
                if (lvl1 > 0) sum += lvl1 * 0.08f;
                int lvl2 = enchants.getOrDefault(
                        net.minecraft.world.item.enchantment.Enchantments.FALL_PROTECTION, 0);
                if (lvl2 > 0) sum += lvl2 * 0.08f;
            }
            default -> {}
        }

        return sum;
    }

    private static String getTranslatedDamageName(DamageType damageType) {
        return switch (damageType) {
            case PIERCING -> "Колющий";
            case SLASHING -> "Режущий";
            case FIRE -> "Огненный";
            case COLD -> "Холодный";
            case BLEEDING -> "Кровотечение";
            case SUFFOCATION -> "Удушье";
            case LUMINOUS_RADIANT -> "Лучистый";
            case NECROTIC -> "Некротический";
            case LIGHTNING -> "Молния";
            case POISON -> "Ядовитый";
            case SOUNDER -> "Звуковой";
            case PSY -> "Психический";
            case BLUDGEONING -> "Дробящий";
        };
    }
}