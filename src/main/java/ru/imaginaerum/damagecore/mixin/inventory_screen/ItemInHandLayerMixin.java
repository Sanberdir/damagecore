package ru.imaginaerum.damagecore.mixin.inventory_screen;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.imaginaerum.damagecore.lybrary_extra_slots.ExtraSlotCapability;
import ru.imaginaerum.damagecore.lybrary_extra_slots.IExtraSlot;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getOffhandItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack damagecore$hideOffhandThirdPerson(LivingEntity entity) {
        if (entity instanceof Player player) {
            boolean combat = player.getCapability(ExtraSlotCapability.INSTANCE)
                    .map(IExtraSlot::isCombatMode)
                    .orElse(false);
            if (!combat) return ItemStack.EMPTY;
        }
        return entity.getOffhandItem();
    }
}