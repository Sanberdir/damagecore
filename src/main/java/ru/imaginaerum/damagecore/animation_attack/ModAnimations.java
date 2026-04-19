package ru.imaginaerum.damagecore.animation_attack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

public class ModAnimations {

    public static final ResourceLocation OVERHEAD_SLASH =
            new ResourceLocation("damagecore", "attack_overhead");

    // Вызови это в FMLCommonSetupEvent
    public static void register() {
        AttackAnimationManager.register(ItemTags.SWORDS, OVERHEAD_SLASH);
        // Позже добавишь свои предметы:
        // AttackAnimationManager.register(ModItems.MY_SWORD.get(), OVERHEAD_SLASH);
    }
}