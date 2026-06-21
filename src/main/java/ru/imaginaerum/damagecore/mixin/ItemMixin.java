package ru.imaginaerum.damagecore.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;
import ru.imaginaerum.damagecore.library_damage.WeaponDamageData;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Mixin(Item.class)
public abstract class ItemMixin {

    @Inject(
            method = "appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V",
            at = @At("RETURN")
    )
    private void damagecore$addDamageTypeTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        if (stack.getItem() instanceof IDamageCoreWeapon weapon) {
            Map<DamageType, Double> map = weapon.damagecore$getDamageMap();

            if (!map.isEmpty()) {
                // Заголовок
                tooltip.add(
                        Component.translatable("damagecore.possible_damage")
                                .withStyle(ChatFormatting.GRAY)
                );

                // Типы урона с отступом
                for (Map.Entry<DamageType, Double> e : map.entrySet()) {
                    ChatFormatting color = switch (e.getKey()) {
                        case PIERCING, SLASHING, BLUDGEONING -> ChatFormatting.GREEN;
                        case FIRE, BLEEDING                            -> ChatFormatting.RED;
                        case COLD                            -> ChatFormatting.AQUA;
                        case LIGHTNING                       -> ChatFormatting.YELLOW;
                        case NECROTIC                        -> ChatFormatting.DARK_PURPLE;
                        case POISON                          -> ChatFormatting.DARK_GREEN;
                        case LUMINOUS_RADIANT                -> ChatFormatting.WHITE;
                        case PSY                             -> ChatFormatting.LIGHT_PURPLE;
                        case SOUNDER                         -> ChatFormatting.BLUE;
                        case SUFFOCATION                     -> ChatFormatting.DARK_GRAY;
                    };

                    tooltip.add(
                            Component.literal(" ").append(
                                    Component.translatable(
                                            "damagecore.damage." + e.getKey().getDamageName(),
                                            String.format("%.1f", e.getValue())
                                    ).withStyle(color)
                            )
                    );
                }
            }

            // Скорость атаки
            Item item = stack.getItem();
            WeaponDamageData data = DamageCore.WEAPON_DAMAGE_MANAGER.getDamageData(item);
            if (data != null && data.hasAttackSpeed()) {
                tooltip.add(
                        Component.translatable(
                                "damagecore.attack_speed",
                                String.format("%.1f", data.getAttackSpeed())
                        ).withStyle(ChatFormatting.BLUE)
                );
            }
        }
    }
}