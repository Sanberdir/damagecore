package ru.imaginaerum.damagecore.api.implementation_skills.alchemy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

@Mod.EventBusSubscriber
public class PotionMasterPickupEvent {

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!SkillTreeServerHandler.isNodeLearned(player, "potion_master")) return;

        ItemStack pickedUp = event.getItem().getItem();
        if (!(pickedUp.getItem() instanceof PotionItem)) return;

        // Сообщаем Mixin'у, кто сейчас подбирает — до любой логики merge
        PotionMasterContext.pickingUpPlayer.set(player.getUUID());
        try {
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack existing = inventory.getItem(i);
                if (existing.isEmpty()) continue;
                if (!ItemStack.isSameItemSameTags(existing, pickedUp)) continue;

                int maxStack = SkillTreeServerHandler.getNodeLevel(player, "potion_master") * 3;

                int canAdd = maxStack - existing.getCount();

                int toAdd = Math.min(canAdd, pickedUp.getCount());
                existing.grow(toAdd);
                pickedUp.shrink(toAdd);

                if (pickedUp.isEmpty()) {
                    event.getItem().discard();
                    event.setCanceled(true);
                }
                return;
            }
        } finally {
            // Всегда чистим — даже если выброшено исключение
            PotionMasterContext.pickingUpPlayer.remove();
        }
    }
}