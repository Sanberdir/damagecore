package ru.imaginaerum.damagecore.api.implementation_skills.alchemy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeNode;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

@Mod.EventBusSubscriber
public class AdeptInsightEvent {

    // Вариант 0: усиление лечебного зелья
    @SubscribeEvent
    public static void onPlayerHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SkillTreeNode node = SkillTreeServerHandler.getNodeForPlayer(player, "adept_insight");
        if (node == null || node.selectedOption != 0) return;

        ItemStack stack = player.getUseItem();
        if (!stack.isEmpty() && stack.getItem() == Items.POTION) {
            if (stack.getTag() != null && "minecraft:healing".equals(stack.getTag().getString("Potion"))) {
                event.setAmount(event.getAmount() * 2f);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SkillTreeNode node = SkillTreeServerHandler.getNodeForPlayer(player, "adept_insight");
        if (node == null || node.selectedOption != 1) return;

        ItemStack stack = player.getUseItem();
        if (!stack.isEmpty() && stack.getItem() == Items.POTION) {
            if (stack.getTag() != null && "minecraft:harming".equals(stack.getTag().getString("Potion"))) {
                event.setAmount(event.getAmount() * 2f);
            }
        }
    }
    @SubscribeEvent
    public static void onPotionFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        SkillTreeNode node = SkillTreeServerHandler.getNodeForPlayer(player, "adept_insight");
        if (node == null || node.selectedOption != 2) return;

        ItemStack stack = event.getItem();

        // Только обычные/длительные зелья с баффами
        if (!stack.isEmpty() && PotionUtils.getPotion(stack) != Potions.EMPTY) {
            for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
                if (effect.getDuration() <= 0) continue; // instant эффекты пропускаем

                MobEffectInstance extended = new MobEffectInstance(
                        effect.getEffect(),
                        effect.getDuration() * 2,
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.isVisible(),
                        effect.showIcon()
                );

                player.addEffect(extended);
            }
        }
    }
}