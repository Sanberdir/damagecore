package ru.imaginaerum.damagecore.tab;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.item.DCItems;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativeTabHandler {

    @SubscribeEvent
    public static void addItemsToCreativeTab(BuildCreativeModeTabContentsEvent event) {

        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.getEntries().putBefore(
                    Items.COAL.getDefaultInstance(),
                    DCItems.BIRCH_LEAF.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {

            event.getEntries().putBefore(
                    Items.WOODEN_SHOVEL.getDefaultInstance(),
                    DCItems.CHLOROPHILOSYNTHETIC_BELT.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
            event.getEntries().putBefore(
                    DCItems.CHLOROPHILOSYNTHETIC_BELT.get().getDefaultInstance(),
                    DCItems.SCARLET_STAPLER_RING.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
            event.getEntries().putBefore(
                    DCItems.SCARLET_STAPLER_RING.get().getDefaultInstance(),
                    DCItems.HEAVY_BELT.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
            event.getEntries().putBefore(
                    DCItems.HEAVY_BELT.get().getDefaultInstance(),
                    DCItems.GOLDEN_RING.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
            event.getEntries().putBefore(
                    DCItems.GOLDEN_RING.get().getDefaultInstance(),
                    DCItems.BANDAGE.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
            event.getEntries().putBefore(
                    DCItems.BANDAGE.get().getDefaultInstance(),
                    DCItems.DECORATED_LEATHER_BELT.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
            event.getEntries().putBefore(
                    DCItems.DECORATED_LEATHER_BELT.get().getDefaultInstance(),
                    DCItems.GOLDEN_AMULET.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
            event.getEntries().putBefore(
                    DCItems.GOLDEN_AMULET.get().getDefaultInstance(),
                    DCItems.POTION_BAG.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
            event.getEntries().putBefore(
                    DCItems.POTION_BAG.get().getDefaultInstance(),
                    DCItems.POTION_BELT.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
            event.getEntries().putBefore(
                    DCItems.POTION_BELT.get().getDefaultInstance(),
                    DCItems.TRAVEL_BAG.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
            event.getEntries().putBefore(
                    DCItems.TRAVEL_BAG.get().getDefaultInstance(),
                    DCItems.QUIVER_OF_ARROWS.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
        }
    }
}