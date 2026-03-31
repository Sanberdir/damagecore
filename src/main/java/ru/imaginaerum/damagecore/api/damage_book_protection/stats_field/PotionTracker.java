package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;

import java.util.*;

public final class PotionTracker {

    private PotionTracker() {}

    // ключ — ResourceLocation зелья (из PotionUtils), значение — оригинальный стек
    private static final Map<ResourceLocation, ItemStack> ACTIVE_POTIONS = new LinkedHashMap<>();

    /**
     * Вызывать из mixin при употреблении зелья игроком.
     */
    public static void onPotionDrank(ItemStack stack) {
        if (stack.isEmpty()) return;
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);

        // Временно для отладки:
        System.out.println("[PotionTracker] onPotionDrank called, item="
                + stack.getItem() + ", effects=" + effects.size());

        if (effects.isEmpty()) return;

        ResourceLocation key = buildKey(stack);
        System.out.println("[PotionTracker] key=" + key);
        ACTIVE_POTIONS.put(key, stack.copy());
    }

    /**
     * Вызывать каждый тик/кадр для очистки истёкших зелий.
     * Зелье считается активным, пока хотя бы один его эффект есть у игрока.
     */
    public static void cleanup(net.minecraft.world.entity.player.Player player) {
        ACTIVE_POTIONS.entrySet().removeIf(entry -> {
            List<MobEffectInstance> potionEffects = PotionUtils.getMobEffects(entry.getValue());
            for (MobEffectInstance pe : potionEffects) {
                if (player.hasEffect(pe.getEffect())) return false; // ещё активен
            }
            return true; // все эффекты истекли — убираем
        });
    }

    public static Map<ResourceLocation, ItemStack> getActivePotions() {
        return ACTIVE_POTIONS;
    }

    private static ResourceLocation buildKey(ItemStack stack) {
        // Используем ResourceLocation зелья из реестра
        net.minecraft.world.item.alchemy.Potion potion = PotionUtils.getPotion(stack);
        ResourceLocation potionRl =
                net.minecraft.core.registries.BuiltInRegistries.POTION.getKey(potion);

        // Добавляем суффикс по типу предмета (potion / splash_potion / lingering_potion)
        String itemSuffix = stack.is(Items.SPLASH_POTION) ? "_splash"
                : stack.is(Items.LINGERING_POTION) ? "_lingering"
                : "";

        return new ResourceLocation(
                potionRl.getNamespace(),
                potionRl.getPath() + itemSuffix
        );
    }
}