package ru.imaginaerum.damagecore.api.implementation_skills.blocking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

import java.util.function.Consumer;

@Mod.EventBusSubscriber
public class ShieldDurabilityEvent {

    private static final String HALF_DAMAGE_KEY = "half_damage_accum";

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!SkillTreeServerHandler.isNodeLearned(player, "reliable_material")) return;

        ItemStack shield = player.getUseItem();
        if (shield.isEmpty() || !(shield.getItem() instanceof ShieldItem)) return;

        // Текущий блокированный урон
        float damage = event.getBlockedDamage();

        // Достаем накопленный дробный урон с NBT
        float acc = 0f;
        if (shield.hasTag() && shield.getTag().contains(HALF_DAMAGE_KEY)) {
            acc = shield.getTag().getFloat(HALF_DAMAGE_KEY);
        }

        // Добавляем половину урона
        acc += damage / 2f;

        // Отменяем стандартный урон
        event.setShieldTakesDamage(false);

        // Применяем целую часть урона
        int applied = (int) acc;
        if (applied > 0) {
            LivingEntity entity = event.getEntity();
            Consumer<LivingEntity> onBroken = e -> entity.broadcastBreakEvent(entity.getUsedItemHand());
            shield.hurtAndBreak(applied, entity, onBroken);

            acc -= applied; // оставляем дробную часть
        }

        // Сохраняем оставшуюся дробную часть обратно в NBT
        shield.getOrCreateTag().putFloat(HALF_DAMAGE_KEY, acc);
    }
}