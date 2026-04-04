package ru.imaginaerum.damagecore.mixin.tree_mixins.alchemy;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;
import ru.imaginaerum.damagecore.api.implementation_skills.alchemy.PotionMasterContext;

import java.util.UUID;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void onGetMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        if (!((Item)(Object)this instanceof PotionItem)) return;

        // Контекст подбора
        UUID pickingUUID = PotionMasterContext.pickingUpPlayer.get();
        if (pickingUUID != null) {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            var player = server.getPlayerList().getPlayer(pickingUUID);
            if (player != null && SkillTreeServerHandler.isNodeLearned(player, "potion_master")) {
                cir.setReturnValue(16);
            }
            return;
        }

        // Контекст GUI (двойной клик, shift-клик)
        UUID menuUUID = PotionMasterContext.menuPlayer.get();
        if (menuUUID != null) {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            var player = server.getPlayerList().getPlayer(menuUUID);
            if (player != null && SkillTreeServerHandler.isNodeLearned(player, "potion_master")) {
                cir.setReturnValue(16);
            }
        }
    }
}