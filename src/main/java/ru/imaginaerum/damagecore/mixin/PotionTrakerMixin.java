package ru.imaginaerum.damagecore.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.damage_book_protection.stats_field.PotionTracker;

// --- Мixin 1: обычное зелье ---
@Mixin(PotionItem.class)
public class PotionTrakerMixin {

    @Inject(
            method = "finishUsingItem",
            at = @At("HEAD")
    )
    private void damagecore$onPotionFinished(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            CallbackInfoReturnable<ItemStack> cir) {

        if (!level.isClientSide) return;
        if (!(entity instanceof Player)) return;

        PotionTracker.onPotionDrank(stack);
    }
}