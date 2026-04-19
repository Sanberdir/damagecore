package ru.imaginaerum.damagecore.animation_attack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AttackAnimationManager {

    private static final Map<Item, ResourceLocation> ITEM_ANIMATIONS = new HashMap<>();
    private static final List<TagEntry> TAG_ANIMATIONS = new ArrayList<>();

    private record TagEntry(TagKey<Item> tag, ResourceLocation animation) {}

    // Регистрация конкретного предмета
    public static void register(Item item, ResourceLocation animationId) {
        ITEM_ANIMATIONS.put(item, animationId);
    }

    // Регистрация тега
    public static void register(TagKey<Item> tag, ResourceLocation animationId) {
        TAG_ANIMATIONS.add(new TagEntry(tag, animationId));
    }

    public static Optional<ResourceLocation> getAnimation(ItemStack stack) {
        // Конкретный предмет имеет приоритет над тегом
        ResourceLocation exact = ITEM_ANIMATIONS.get(stack.getItem());
        if (exact != null) return Optional.of(exact);

        for (TagEntry entry : TAG_ANIMATIONS) {
            if (stack.is(entry.tag())) {
                return Optional.of(entry.animation());
            }
        }

        return Optional.empty();
    }

    public static boolean hasAnimation(ItemStack stack) {
        return getAnimation(stack).isPresent();
    }
}