// src/main/java/ru/imaginaerum/damagecore/mixin/ItemStackMixin.java
package ru.imaginaerum.damagecore.mixin.tree_mixins.alchemy;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;
import ru.imaginaerum.damagecore.api.implementation_skills.alchemy.PotionMasterContext;

import java.util.UUID;

// ItemStackMixin.java
@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void onGetMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (!(self.getItem() instanceof PotionItem)) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Случай 1: стак уже в инвентаре — ищем по ссылке
        for (var player : server.getPlayerList().getPlayers()) {
            if (isStackInInventory(player, self)) {
                if (SkillTreeServerHandler.isNodeLearned(player, "potion_master")) {
                    cir.setReturnValue(16);
                }
                return;
            }
        }

        // Случай 2: стак — entity на земле, его подбирает игрок с навыком
        UUID pickingUUID = PotionMasterContext.pickingUpPlayer.get();
        if (pickingUUID == null) return;

        var pickingPlayer = server.getPlayerList().getPlayer(pickingUUID);
        if (pickingPlayer != null && SkillTreeServerHandler.isNodeLearned(pickingPlayer, "potion_master")) {
            cir.setReturnValue(16);
        }
    }

    private boolean isStackInInventory(Player player, ItemStack stack) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i) == stack) return true;
        }
        return false;
    }
}