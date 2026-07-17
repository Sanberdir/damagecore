package ru.imaginaerum.damagecore.mixin.inventory_screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.items.SlotItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.library_extra_slots.ExtraSlotCapability;
import ru.imaginaerum.damagecore.library_extra_slots.IExtraSlot;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {

    protected InventoryMenuMixin(MenuType<?> type, int id) { super(type, id); }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Inventory;ZLnet/minecraft/world/entity/player/Player;)V",
            at = @At("TAIL"))
    private void damagecore$addExtraSlot(Inventory inventory, boolean active, Player player, CallbackInfo ci) {
        IExtraSlot data = player.getCapability(ExtraSlotCapability.INSTANCE).orElse(null);
        if (data == null) return;

        this.addSlot(new SlotItemHandler(data.getHandler(), 0, 77, 8));
        this.addSlot(new SlotItemHandler(data.getHandler(), 1, 77, 26));
        this.addSlot(new SlotItemHandler(data.getHandler(), 2, 77, 44));
    }
}