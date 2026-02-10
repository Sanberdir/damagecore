package ru.imaginaerum.damagecore.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer;

import java.awt.*;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends Container> extends Screen {

    protected AbstractContainerScreenMixin(Component pTitle) {
        super(pTitle);
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