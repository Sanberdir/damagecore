package ru.imaginaerum.damagecore.mixin.tree_mixins.alchemy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.api.implementation_skills.alchemy.PotionMasterContext;

@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {

    @Inject(method = "clicked", at = @At("HEAD"))
    private void onClickedHead(int slotId, int button, ClickType clickType,
                               Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer sp) {
            PotionMasterContext.menuPlayer.set(sp.getUUID());
        }
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void onClickedReturn(int slotId, int button, ClickType clickType,
                                 Player player, CallbackInfo ci) {
        PotionMasterContext.menuPlayer.remove();
    }
}