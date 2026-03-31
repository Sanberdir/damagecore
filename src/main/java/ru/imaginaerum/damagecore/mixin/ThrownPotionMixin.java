package ru.imaginaerum.damagecore.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.damage_book_protection.stats_field.PotionTracker;

@Mixin(ThrowablePotionItem.class)
public class ThrownPotionMixin {

    @Inject(
            method = "use",
            at = @At("HEAD")
    )
    private void damagecore$onThrow(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {

        if (!level.isClientSide) return;

        ItemStack stack = player.getItemInHand(hand);
        PotionTracker.onPotionDrank(stack);
    }
}