package ru.imaginaerum.damagecore.api.implementation_skills.shooting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

import java.util.Random;

@Mod.EventBusSubscriber
public class FatalShotEvent {

    private static final float CRIT_CHANCE = 0.30f; // 30% шанс крита
    private static final float CRIT_MULTIPLIER = 1.5f; // х1.5 урона при крите
    private static final Random random = new Random();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // Источник урона — стрела
        Entity directEntity = event.getSource().getDirectEntity();
        if (!(directEntity instanceof AbstractArrow arrow)) return;

        // Владелец стрелы — игрок
        Entity owner = arrow.getOwner();
        if (!(owner instanceof ServerPlayer player)) return;

        // Проверка ноды
        if (!SkillTreeServerHandler.isNodeLearned(player, "fatal_shot")) return;

        // Бросок на крит
        if (random.nextFloat() < CRIT_CHANCE) {
            float originalDamage = event.getAmount();
            float newDamage = originalDamage * CRIT_MULTIPLIER;
            event.setAmount(newDamage);
        }
    }
}