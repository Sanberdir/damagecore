package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class StaminaManager {

    public static final float MAX_STAMINA = 40f;

    private static final float DRAIN_SPRINT = 0.2f;
    private static final float REGEN_WALK   = 0.3f;
    private static final float REGEN_STAND  = 0.8f;

    private static final int EXHAUSTION_COOLDOWN = 40;

    private static final float STAMINA_CRITICAL = 4.0f;
    private static final float STAMINA_LOW      = 10.0f;

    private static final float SPEED_MULTIPLIER_CRITICAL = 0.3f;
    private static final float SPEED_MULTIPLIER_LOW      = 0.6f;

    private static final float DRAIN_BOAT = DRAIN_SPRINT / 4f;

    private static final float BOAT_SPEED_MULTIPLIER_CRITICAL = 0.2f;
    private static final float BOAT_SPEED_MULTIPLIER_LOW      = 0.4f;
    private static final double ORIGINAL_BOAT_MAX_SPEED       = 1.2;

    // --- Состояние ---
    private static float stamina = MAX_STAMINA;
    private static boolean exhausted = false;
    private static int exhaustionTimer = 0;
    private static double originalSpeed = -1;

    public static float getStamina()    { return stamina; }
    public static boolean isExhausted() { return exhausted; }

    public static void resetSpeed(Player player) {
        if (originalSpeed == -1 || player == null) return;
        var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) speedAttribute.setBaseValue(originalSpeed);
    }

    // --- Тик: скорость и лодка (LivingEvent — есть доступ к Player) ---
    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.isCreative()|| player.isSpectator()) {
            stamina = MAX_STAMINA;
            exhausted = false;
            return;
        }
        var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        if (originalSpeed == -1) originalSpeed = speedAttribute.getBaseValue();

        if (stamina < STAMINA_CRITICAL) {
            speedAttribute.setBaseValue(originalSpeed * SPEED_MULTIPLIER_CRITICAL);
        } else if (stamina < STAMINA_LOW) {
            speedAttribute.setBaseValue(originalSpeed * SPEED_MULTIPLIER_LOW);
        } else {
            speedAttribute.setBaseValue(originalSpeed);
        }

        handleBoatSlowdown(player);
    }

    // --- Тик: трата/восстановление стамины (ClientTickEvent — есть LocalPlayer) ---
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) return;

        boolean sprinting = player.isSprinting();
        boolean moving    = player.getDeltaMovement().horizontalDistanceSqr() > 1e-6;
        boolean inBoat    = player.getVehicle() instanceof Boat;

        if (exhausted) {
            if (sprinting) player.setSprinting(false);
            exhaustionTimer--;
            if (exhaustionTimer <= 0) exhausted = false;
            return;
        }

        if (inBoat) {
            if (moving) {
                stamina -= DRAIN_BOAT;
                if (stamina <= 0f) stamina = 0f;
            } else {
                stamina = Math.min(stamina + REGEN_STAND, MAX_STAMINA);
            }
        } else if (sprinting) {
            stamina -= DRAIN_SPRINT;
            if (stamina <= 0f) {
                stamina = 0f;
                exhausted = true;
                exhaustionTimer = EXHAUSTION_COOLDOWN;
                player.setSprinting(false);
            }
        } else if (moving) {
            stamina = Math.min(stamina + REGEN_WALK, MAX_STAMINA);
        } else {
            stamina = Math.min(stamina + REGEN_STAND, MAX_STAMINA);
        }
    }

    private static void handleBoatSlowdown(Player player) {
        if (!(player.getVehicle() instanceof Boat boat)) return;

        Vec3 vel = boat.getDeltaMovement();
        double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        float multiplier;
        if (stamina < STAMINA_CRITICAL)     multiplier = BOAT_SPEED_MULTIPLIER_CRITICAL;
        else if (stamina < STAMINA_LOW)     multiplier = BOAT_SPEED_MULTIPLIER_LOW;
        else                                return;

        if (hSpeed > 0.01) {
            double maxAllowed = ORIGINAL_BOAT_MAX_SPEED * multiplier;
            if (hSpeed > maxAllowed) {
                double scale = maxAllowed / hSpeed;
                boat.setDeltaMovement(vel.x * scale, vel.y, vel.z * scale);
            }
        }
    }
}