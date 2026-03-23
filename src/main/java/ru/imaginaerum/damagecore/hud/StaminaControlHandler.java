package ru.imaginaerum.damagecore.hud;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class StaminaControlHandler {

    // Минимальный уровень стамины для возможности бега
    private static final float MIN_STAMINA_FOR_SPRINT = 5.0f;

    // Пороги замедления
    private static final float STAMINA_CRITICAL = 2.0f;  // Критический уровень
    private static final float STAMINA_LOW = 5.0f;       // Низкий уровень

    // Множители скорости (чем меньше значение, тем медленнее)
    private static final float SPEED_MULTIPLIER_CRITICAL = 0.3f;  // 30% от обычной скорости
    private static final float SPEED_MULTIPLIER_LOW = 0.6f;       // 60% от обычной скорости

    // Для хранения оригинальной скорости игрока
    private static double originalSpeed = -1;

    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        float currentStamina = DamageCoreHudOverlay.getStamina();

        // Получаем атрибут скорости игрока
        var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        // Сохраняем оригинальную скорость при первом запуске
        if (originalSpeed == -1) {
            originalSpeed = speedAttribute.getBaseValue();
        }

        // Применяем замедление в зависимости от уровня стамины
        if (currentStamina < STAMINA_CRITICAL) {
            // Критически низкая стамина - сильное замедление
            double newSpeed = originalSpeed * SPEED_MULTIPLIER_CRITICAL;
            speedAttribute.setBaseValue(newSpeed);

            // Также отключаем бег при таком низком уровне
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
        }
        else if (currentStamina < STAMINA_LOW) {
            // Низкая стамина - умеренное замедление
            double newSpeed = originalSpeed * SPEED_MULTIPLIER_LOW;
            speedAttribute.setBaseValue(newSpeed);
        }
        else {
            // Стамина в норме - восстанавливаем скорость
            speedAttribute.setBaseValue(originalSpeed);
        }

        // Визуальный эффект (можно раскомментировать при наличии нужных методов)
        // if (currentStamina < STAMINA_LOW && player.level().isClientSide()) {
        //     applyVisualEffect(player, currentStamina);
        // }
    }

    // Опционально: метод для сброса скорости при выходе из игры
    public static void resetSpeed(Player player) {
        if (originalSpeed != -1 && player != null) {
            var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                speedAttribute.setBaseValue(originalSpeed);
            }
        }
    }
}