package ru.imaginaerum.damagecore.api.implementation_skills.shooting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

import java.util.UUID;

@Mod.EventBusSubscriber
public class RangerAttributeEvent {

    // Уникальный UUID для модификатора скорости
    private static final UUID SPEED_MOD_UUID = UUID.fromString("e1c2f7f5-8c3a-4b21-90ed-12b3c91a90a1");
    private static final double SPEED_BONUS = 1; // +20% к скорости

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // Проверка ноды ranger
        if (!SkillTreeServerHandler.isNodeLearned(player, "ranger")) {
            removeSpeedModifier(player);
            return;
        }

        ItemStack activeItem = player.getUseItem();
        boolean pullingBow = !activeItem.isEmpty() &&
                (activeItem.getItem() instanceof BowItem || activeItem.getItem() instanceof CrossbowItem) &&
                player.isUsingItem();

        if (pullingBow) {
            addSpeedModifier(player);
        } else {
            removeSpeedModifier(player);
        }
    }

    private static void addSpeedModifier(ServerPlayer player) {
        if (player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(SPEED_MOD_UUID) == null) {
            AttributeModifier mod = new AttributeModifier(SPEED_MOD_UUID, "Ranger bow speed bonus", SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_TOTAL);
            player.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(mod);
        }
    }

    private static void removeSpeedModifier(ServerPlayer player) {
        if (player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(SPEED_MOD_UUID) != null) {
            player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPEED_MOD_UUID);
        }
    }
}