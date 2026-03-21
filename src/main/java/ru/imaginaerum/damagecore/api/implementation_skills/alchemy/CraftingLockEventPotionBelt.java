package ru.imaginaerum.damagecore.api.implementation_skills.alchemy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

@Mod.EventBusSubscriber
public class CraftingLockEventPotionBelt {
    private static final ResourceLocation POTION_BAG =
            new ResourceLocation("damagecore", "potion_bag");

    private static final ResourceLocation POTION_BELT =
            new ResourceLocation("damagecore", "potion_belt");

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        if (!(player.containerMenu instanceof CraftingMenu menu)) return;

        boolean learned = SkillTreeServerHandler.isNodeLearned(player, "alchemy_belt");
        if (learned) return;

        Slot resultSlot = menu.slots.get(0);
        ItemStack result = resultSlot.getItem();
        if (result.isEmpty()) return;

        ResourceLocation id = result.getItem().builtInRegistryHolder().key().location();

        if (POTION_BAG.equals(id) || POTION_BELT.equals(id)) {
            resultSlot.set(ItemStack.EMPTY);
            menu.broadcastChanges();
        }
    }
}
