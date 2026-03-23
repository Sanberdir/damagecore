package ru.imaginaerum.damagecore.hud;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
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

    // Множители скорости для лодки
    private static final float BOAT_SPEED_MULTIPLIER_CRITICAL = 0.2f;  // 40% от обычной скорости лодки
    private static final float BOAT_SPEED_MULTIPLIER_LOW = 0.4f;       // 70% от обычной скорости лодки

    // Для хранения оригинальной скорости игрока
    private static double originalSpeed = -1;

    // Для хранения оригинальной максимальной скорости лодки
    private static double originalBoatMaxSpeed = 1.2; // Стандартная скорость лодки в Minecraft

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

        // Обработка замедления лодки
        handleBoatSlowdown(player, currentStamina);
    }

    private static void handleBoatSlowdown(Player player, float currentStamina) {
        // Проверяем, находится ли игрок в лодке
        if (!(player.getVehicle() instanceof Boat boat)) return;

        // Получаем текущую скорость лодки
        Vec3 currentVelocity = boat.getDeltaMovement();
        double currentHorizontalSpeed = Math.sqrt(
                currentVelocity.x * currentVelocity.x +
                        currentVelocity.z * currentVelocity.z
        );

        // Определяем множитель замедления
        float speedMultiplier = 1.0f;

        if (currentStamina < STAMINA_CRITICAL) {
            speedMultiplier = BOAT_SPEED_MULTIPLIER_CRITICAL;
        } else if (currentStamina < STAMINA_LOW) {
            speedMultiplier = BOAT_SPEED_MULTIPLIER_LOW;
        } else {
            // Если стамина в норме, не применяем замедление
            return;
        }

        // Применяем замедление только если лодка движется
        if (currentHorizontalSpeed > 0.01) {
            // Ограничиваем максимальную скорость лодки
            double maxAllowedSpeed = originalBoatMaxSpeed * speedMultiplier;

            if (currentHorizontalSpeed > maxAllowedSpeed) {
                // Замедляем лодку до разрешенной скорости
                double scale = maxAllowedSpeed / currentHorizontalSpeed;
                boat.setDeltaMovement(
                        currentVelocity.x * scale,
                        currentVelocity.y,
                        currentVelocity.z * scale
                );
            }
        }
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