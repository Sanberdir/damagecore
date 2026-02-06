package ru.imaginaerum.damagecore.mixin;

import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public class EnchantmentSharpnessHelperMixin {

    @Inject(
            method = "getDamageBonus",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void damagecore$disableSharpnessBonus(
            ItemStack stack,
            MobType mobType,
            CallbackInfoReturnable<Float> cir
    ) {
        int sharpness = EnchantmentHelper.getItemEnchantmentLevel(
                Enchantments.SHARPNESS, stack
        );

        if (sharpness > 0) {
            // Полностью вырубаем ванильный бонус
            cir.setReturnValue(0.0F);
        }
    }
}
