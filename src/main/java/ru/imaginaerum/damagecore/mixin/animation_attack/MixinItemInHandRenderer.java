package ru.imaginaerum.damagecore.mixin.animation_attack;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.animation_attack.PlayerAnimationController;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void onRenderArmWithItem(
            AbstractClientPlayer player,
            float partialTick, float pitch,
            InteractionHand hand, float swingProgress,
            ItemStack stack, float equippedProgress,
            PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, CallbackInfo ci
    ) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!PlayerAnimationController.isPlaying(player)) {
            return;
        }

        ci.cancel();
    }
}