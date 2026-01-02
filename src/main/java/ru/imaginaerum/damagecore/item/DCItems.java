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
    public static final RegistryObject<Item> CHLOROPHILOSYNTHETIC_BELT = ITEMS.register("chlorophilosynthetic_belt",
            () -> new Item(new Item.Properties()));
}
