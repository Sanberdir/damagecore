package ru.imaginaerum.damagecore.mixin.inventory_screen;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.imaginaerum.damagecore.lybrary_extra_slots.ExtraSlotCapability;
import ru.imaginaerum.damagecore.lybrary_extra_slots.IExtraSlot;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getOffhandItem()Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack damagecore$fakeOffhandForRender(LocalPlayer player) {
        boolean combat = player.getCapability(ExtraSlotCapability.INSTANCE)
                .map(IExtraSlot::isCombatMode)
                .orElse(false);

        // вне боя рендерер "думает", что оффхенд пуст — рука/предмет от первого лица не рисуются;
        // реальный предмет в инвентаре/оффхенде игрока никуда не девается
        return combat ? player.getOffhandItem() : ItemStack.EMPTY;
    }
}