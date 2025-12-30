package ru.imaginaerum.damagecore.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.imaginaerum.damagecore.DamageCore;

public class DCItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DamageCore.MODID);

    public static final RegistryObject<Item> BIRCH_LEAF = ITEMS.register("birch_leaf",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SCARLET_STAPLER_RING = ITEMS.register("scarlet_stapler_ring",
            () -> new Item(new Item.Properties()));
}
