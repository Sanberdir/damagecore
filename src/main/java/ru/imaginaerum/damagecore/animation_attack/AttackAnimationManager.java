package ru.imaginaerum.damagecore.animation_attack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class AttackAnimationManager {

    private static final Map<Item, AttackAnimationData> ITEM_ANIMATIONS = new HashMap<>();
    private static final List<TagEntry> TAG_ANIMATIONS = new ArrayList<>();

    private record TagEntry(TagKey<Item> tag, AttackAnimationData data) {}

    public static void register(Item item, AttackAnimationData data) {
        ITEM_ANIMATIONS.put(item, data);
    }

    public static void register(TagKey<Item> tag, AttackAnimationData data) {
        TAG_ANIMATIONS.add(new TagEntry(tag, data));
    }

    public static Optional<AttackAnimationData> get(ItemStack stack) {
        AttackAnimationData exact = ITEM_ANIMATIONS.get(stack.getItem());
        if (exact != null) return Optional.of(exact);

        for (TagEntry entry : TAG_ANIMATIONS) {
            if (stack.is(entry.tag())) {
                return Optional.of(entry.data());
            }
        }

        return Optional.empty();
    }
}