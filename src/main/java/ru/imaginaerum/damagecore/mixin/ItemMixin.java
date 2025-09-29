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
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;

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
            for (Map.Entry<DamageType, Double> e : map.entrySet()) {
                ChatFormatting color;
                switch (e.getKey()) {
                    case PIERCING -> color = ChatFormatting.DARK_GREEN;
                    case SLASHING -> color = ChatFormatting.DARK_GREEN;
                    case BLUDGEONING -> color = ChatFormatting.DARK_GREEN;
                    case FIRE -> color = ChatFormatting.DARK_RED;
                    default -> color = ChatFormatting.WHITE;
                }
                tooltip.add(
                        Component.translatable(
                                "damagecore.damage." + e.getKey().getDamageName(), // ключ для перевода
                                String.format("%.1f", e.getValue())              // параметр %s для числа урона
                        ).withStyle(color)
                );
            }
        }
    }
}
