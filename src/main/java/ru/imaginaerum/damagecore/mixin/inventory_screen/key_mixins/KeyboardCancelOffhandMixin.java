package ru.imaginaerum.damagecore.mixin.inventory_screen.key_mixins;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardCancelOffhandMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void blockSwapOffhand(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        if (mc.options.keySwapOffhand.matches(key, scancode)) {
            ci.cancel();
        }
    }
}
