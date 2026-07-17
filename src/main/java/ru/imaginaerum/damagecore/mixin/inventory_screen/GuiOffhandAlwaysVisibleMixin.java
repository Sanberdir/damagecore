package ru.imaginaerum.damagecore.mixin.inventory_screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.imaginaerum.damagecore.library_extra_slots.ExtraSlotCapability;
import ru.imaginaerum.damagecore.library_extra_slots.IExtraSlot;

@Mixin(Gui.class)
public abstract class GuiOffhandAlwaysVisibleMixin {
    @Redirect(
            method = "renderHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
                    ordinal = 1
            )
    )
    private void damagecore$blockSelectedSlotFrame(GuiGraphics guiGraphics,
                                                   ResourceLocation tex,
                                                   int x, int y,
                                                   int u, int v,
                                                   int w, int h,
                                                   float partialTick,
                                                   GuiGraphics original) {

        Player player = Minecraft.getInstance().player;

        boolean combat = player != null &&
                player.getCapability(ExtraSlotCapability.INSTANCE)
                        .map(IExtraSlot::isCombatMode)
                        .orElse(false);

        // 👉 В боевом режиме НЕ рисуем рамку выбранного слота
        if (combat && u == 0 && v == 22 && w == 24 && h == 22) {
            return;
        }

        guiGraphics.blit(tex, x, y, u, v, w, h);
    }
    @Redirect(
            method = "renderHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0)
    )
    private boolean damagecore$alwaysShowOffhandBackground(ItemStack itemStack) {
        // ordinal=0 — это именно проверка перед отрисовкой ФОНА оффхенд-ячейки.
        // Всегда возвращаем false ("не пусто"), чтобы ваниль рисовала рамку
        // независимо от того, есть предмет в реальной руке или нет.
        return false;
    }
}