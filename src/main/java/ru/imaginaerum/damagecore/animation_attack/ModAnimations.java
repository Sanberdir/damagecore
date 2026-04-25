package ru.imaginaerum.damagecore.animation_attack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;

public class ModAnimations {

    public static final ResourceLocation OVERHEAD_SLASH =
            new ResourceLocation("damagecore", "attack_overhead");

    // Вызови это в FMLCommonSetupEvent
    public static void register() {

        // ⚔️ Таймлайн как в Better Combat
        AnimationTimeline timeline = new AnimationTimeline()
                .add(0.25f, AnimationEvent.Type.HIT)       // момент удара
                .add(0.33f, AnimationEvent.Type.SWING)     // продолжение
                .add(0.5f, AnimationEvent.Type.RECOVERY);  // откат

        // 🧠 Связываем анимацию + поведение
        AttackAnimationData data =
                new AttackAnimationData(OVERHEAD_SLASH, timeline);

        // ✅ Регистрируем по тегу
        AttackAnimationManager.register(ItemTags.SWORDS, data);

        // 👉 пример для конкретного предмета
        // AttackAnimationManager.register(Items.DIAMOND_SWORD, data);
    }
}