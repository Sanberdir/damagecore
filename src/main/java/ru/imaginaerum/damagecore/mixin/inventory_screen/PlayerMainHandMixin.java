package ru.imaginaerum.damagecore.mixin.inventory_screen;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.lybrary_extra_slots.ExtraSlotCapability;

@Mixin(Player.class)
public abstract class PlayerMainHandMixin {

    private static final Logger DAMAGECORE_LOG = LogManager.getLogger("damagecore-combat");

    @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true)
    private void damagecore$getMainHand(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        if (slot != EquipmentSlot.MAINHAND) return;
        try {
            Player self = (Player) (Object) this;
            self.getCapability(ExtraSlotCapability.INSTANCE).ifPresent(data -> {
                if (data.isCombatMode()) {
                    cir.setReturnValue(data.getHandler().getStackInSlot(0));
                }
            });
        } catch (Throwable t) {
            DAMAGECORE_LOG.error("Exception in getItemBySlot mixin", t);
        }
    }

    @Inject(method = "setItemSlot", at = @At("HEAD"), cancellable = true)
    private void damagecore$setMainHand(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        if (slot != EquipmentSlot.MAINHAND) return;
        try {
            Player self = (Player) (Object) this;
            self.getCapability(ExtraSlotCapability.INSTANCE).ifPresent(data -> {
                if (data.isCombatMode()) {
                    data.getHandler().setStackInSlot(0, stack);
                    ci.cancel();
                }
            });
        } catch (Throwable t) {
            DAMAGECORE_LOG.error("Exception in setItemSlot mixin", t);
        }
    }
}