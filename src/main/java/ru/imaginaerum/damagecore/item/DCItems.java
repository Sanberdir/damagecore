package ru.imaginaerum.damagecore.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.item.custom.ScarletStaplerRing;

public class DCItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DamageCore.MODID);

    public static final RegistryObject<Item> BIRCH_LEAF = ITEMS.register("birch_leaf",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SCARLET_STAPLER_RING = ITEMS.register("scarlet_stapler_ring",
            () -> new ScarletStaplerRing(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> HEAVY_BELT = ITEMS.register("heavy_belt",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GOLDEN_RING = ITEMS.register("golden_ring",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BANDAGE = ITEMS.register("bandage",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DECORATED_LEATHER_BELT = ITEMS.register("decorated_leather_belt",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GOLDEN_AMULET = ITEMS.register("golden_amulet",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> POTION_BAG = ITEMS.register("potion_bag",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> POTION_BELT = ITEMS.register("potion_belt",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TRAVEL_BAG = ITEMS.register("travel_bag",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CHLOROPHILOSYNTHETIC_BELT = ITEMS.register("chlorophilosynthetic_belt",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> QUIVER_OF_ARROWS = ITEMS.register("quiver_of_arrows",
            () -> new Item(new Item.Properties()));
}
