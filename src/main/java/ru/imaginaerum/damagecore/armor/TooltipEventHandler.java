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

import java.util.Map;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TooltipEventHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (stack.getItem() instanceof ArmorItem armorItem) {
            // Получаем сопротивления
            Map<DamageType, DamageResistance> resistances = DamageArmorModifier.getDamageResistances(
                    armorItem.getMaterial(),
                    armorItem.getType()
            );

            if (!resistances.isEmpty()) {
                event.getToolTip().add(Component.literal(""));
                event.getToolTip().add(Component.literal("Защита от типов урона:")
                        .withStyle(ChatFormatting.GRAY));

                for (Map.Entry<DamageType, DamageResistance> entry : resistances.entrySet()) {
                    DamageResistance resistance = entry.getValue();
                    if (resistance.getFlat() > 0 || resistance.getPercent() > 0) {
                        String damageName = getTranslatedDamageName(entry.getKey());
                        String resistanceText = resistance.toString();

                        Component tooltipLine = Component.literal("  " + damageName + ": " + resistanceText)
                                .withStyle(ChatFormatting.BLUE);
                        event.getToolTip().add(tooltipLine);
                    }
                }
            }

            // Удаляем стандартную информацию о защите
            event.getToolTip().removeIf(component -> {
                String text = component.getString().toLowerCase();
                return text.contains("armor") ||
                        text.contains("броня") ||
                        text.contains("toughness") ||
                        text.contains("прочность") ||
                        text.matches(".*\\+\\s*\\d+.*(armor|броня).*");
            });
        }
    }

    private static String getTranslatedDamageName(DamageType damageType) {
        return switch (damageType) {
            case PIERCING -> "Колющий";
            case SLASHING -> "Режущий";
            case FIRE -> "Огненный";
            case COLD -> "Холодный";
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