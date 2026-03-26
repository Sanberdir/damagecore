package ru.imaginaerum.damagecore.mixin;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(Gui.class)
public class HudNoIconPotionEffectsMixin {
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderEffects(GuiGraphics guiGraphics, CallbackInfo ci) {
        // Полностью отключаем рендеринг эффектов
        ci.cancel();
    }
}
