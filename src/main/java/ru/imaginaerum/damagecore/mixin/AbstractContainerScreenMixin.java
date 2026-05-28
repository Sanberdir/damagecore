package ru.imaginaerum.damagecore.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.damage_book_protection.ISkillTreeAccessor;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer;
import ru.imaginaerum.damagecore.api.damage_book_protection.stats_field.ArmorStatsHoverHandler;

import java.awt.*;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends Container> extends Screen {

    protected AbstractContainerScreenMixin(Component pTitle) {
        super(pTitle);
    }
    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            at = @At("HEAD"), cancellable = true)
    private void damagecore$cancelTooltip(GuiGraphics gui, int mouseX, int mouseY, CallbackInfo ci) {
        if ((Object)this instanceof InventoryScreen screen) {
            if (ArmorStatsHoverHandler.isHoveringAnyCustomWindow(screen, mouseX, mouseY)) {
                // Только отменяем стандартный tooltip
                ci.cancel();
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void damagecore$clearHoveredSlot(GuiGraphics gui, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if ((Object)this instanceof InventoryScreen screen) {
            if (ArmorStatsHoverHandler.isHoveringAnyCustomWindow(screen, mouseX, mouseY)) {
                ((AbstractContainerScreenAccessor)this).setHoveredSlot(null);
            }
        }
    }

    @Inject(method = "isHovering", at = @At("HEAD"), cancellable = true)
    private void damagecore$cancelSlotHover(Slot slot, double mouseX, double mouseY,
                                            CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof InventoryScreen screen) {
            if (ArmorStatsHoverHandler.isHoveringAnyCustomWindow(screen, mouseX, mouseY)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void skillTree$mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        int guiLeft = ((AbstractContainerScreenAccessor)this).getLeftPos();
        int guiTop = ((AbstractContainerScreenAccessor)this).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor)this).damagecore$getImageWidth();

        int panelScreenX = guiLeft + imageWidth + 2;
        int panelScreenY = guiTop;

        boolean consumed = SkillTreeRenderer.mouseDragged((int)mouseX, (int)mouseY, button, panelScreenX, panelScreenY);
        if (consumed) {
            cir.setReturnValue(true);
        }
    }
}