package ru.imaginaerum.damagecore.mixin.inventory_screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.library_extra_slots.ExtraSlotCapability;
import ru.imaginaerum.damagecore.library_extra_slots.IExtraSlot;

@Mixin(Inventory.class)
public class InventoryHotbarLockMixin {

    @Shadow public Player player;

    @Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true)
    private void damagecore$lockScroll(double direction, CallbackInfo ci) {

        boolean combat = this.player != null &&
                this.player.getCapability(ExtraSlotCapability.INSTANCE)
                        .map(IExtraSlot::isCombatMode)
                        .orElse(false);

        if (combat) {
            ci.cancel(); // ❌ блок прокрутки хотбара
        }
    }
}