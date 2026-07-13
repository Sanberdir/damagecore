package ru.imaginaerum.damagecore.mixin.inventory_screen.key_mixins;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBindsList.KeyEntry.class)
public class HideOffhandKeyMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hideOffhandKey(GuiGraphics graphics, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {

        var entry = (KeyBindsList.KeyEntry)(Object)this;

        // достаём KeyMapping через рефлексию/геттер (зависит от маппингов)
        KeyMapping key = ((AccessorKeyEntry)entry).getKey();

        if (key == Minecraft.getInstance().options.keySwapOffhand) {
            ci.cancel(); // просто не рисуем строку
        }
    }
}