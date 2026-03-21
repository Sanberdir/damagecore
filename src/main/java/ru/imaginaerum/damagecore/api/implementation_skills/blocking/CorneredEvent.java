package ru.imaginaerum.damagecore.api.implementation_skills.blocking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

@Mod.EventBusSubscriber
public class CorneredEvent {

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!SkillTreeServerHandler.isNodeLearned(player, "cornered")) return;

        ItemStack shield = player.getUseItem();
        if (shield.isEmpty() || !(shield.getItem() instanceof ShieldItem)) return;

        int currentDamage = shield.getDamageValue();
        int maxDamage = shield.getMaxDamage();

        // Примерный урон щиту
        int damageToShield = (int)Math.ceil(event.getBlockedDamage());

        // Проверяем сломается ли щит
        if (currentDamage + damageToShield >= maxDamage) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST,
                    20 * 20,
                    1
            ));
        }
    }
}