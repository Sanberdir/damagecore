package ru.imaginaerum.damagecore.mixin.event_trees;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.implementation_skills.AlchemyXpHandler;

@Mixin(BrewingStandMenu.class)
public class BrewingStandMixin {

    @Inject(method = "quickMoveStack", at = @At("RETURN"))
    private void damagecore$onShiftTake(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        handle(player);
    }

    private void handle(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;

        AlchemyXpHandler.tryGiveXp(sp);
    }
}