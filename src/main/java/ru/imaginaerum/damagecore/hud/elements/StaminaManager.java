package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.Config;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class StaminaManager {

    public static final float BASE_STAMINA = 40f;

    public static float getMaxStamina() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return BASE_STAMINA;
        return PlayerStatsCapability.get(mc.player)
                .map(stats -> BASE_STAMINA + stats.getStat(StatsType.ENDURANCE) * 0.5f)
                .orElse(BASE_STAMINA);
    }

    private static final float DRAIN_SPRINT              = 0.2f;
    private static final float DRAIN_SHIELD_HOLD         = 0.02f;  // пассивный дрейн пока держишь щит
    private static final float DRAIN_SHIELD_HIT          = 6.0f;   // разовый дрейн при получении удара в щит
    private static final float REGEN_WALK                = 0.2f;
    private static final float REGEN_STAND               = 0.45f;

    private static final int EXHAUSTION_COOLDOWN         = 40;

    private static final float STAMINA_CRITICAL          = 4.0f;
    private static final float STAMINA_LOW               = 10.0f;

    private static final float SPEED_MULTIPLIER_CRITICAL = 0.3f;
    private static final float SPEED_MULTIPLIER_LOW      = 0.6f;

    private static final float DRAIN_BOAT                        = DRAIN_SPRINT / 4f;
    private static final float BOAT_SPEED_MULTIPLIER_CRITICAL    = 0.2f;
    private static final float BOAT_SPEED_MULTIPLIER_LOW         = 0.4f;
    private static final double ORIGINAL_BOAT_MAX_SPEED          = 1.2;
    public static boolean isExhausted() { return exhausted; }
    // --- Состояние ---
    private static float stamina       = getMaxStamina();
    private static boolean exhausted   = false;
    private static int exhaustionTimer = 0;
    private static double originalSpeed = -1;
    private static float getShieldStaminaMultiplier(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (SkillTreeServerHandler.isNodeLearned(serverPlayer, "shield_bearer")) {
                return 0.5f; // в 2 раза меньше расход
            }
        }
        return 1.0f;
    }
    public static float getStamina()    { return stamina; }
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (!Config.showStaminaHud) return;
        if (player.isCreative() || player.isSpectator()) return;

        // Только блокируем удар при истощении — дренаж идёт через сервер
        if (exhausted) {
            event.setCanceled(true);
        }
    }
    public static void drainFromServer(float amount) {
        if (exhausted) return;
        stamina -= amount;
        if (stamina <= 0f) {
            stamina = 0f;
            // triggerExhaustion требует Player — берём из Minecraft
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) triggerExhaustion(mc.player);
        }
    }
    // --- Удар в щит: разовый дрейн, при нуле стамины — щит сносится ---
    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!Config.showStaminaHud) return;
        if (stamina <= 0f || exhausted) {
            // стамины нет — щит пробивается, урон проходит
            event.setCanceled(true);
            triggerExhaustion(player);
            return;
        }

        float mult = getShieldStaminaMultiplier(player);
        stamina -= DRAIN_SHIELD_HIT * mult;
        if (stamina <= 0f) {
            stamina = 0f;
            triggerExhaustion(player);
        }
    }

    // --- Тик: скорость и лодка ---
    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!Config.showStaminaHud) return;
        if (player.isCreative() || player.isSpectator()) {
            stamina   = getMaxStamina();
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

    // --- Тик: трата/восстановление стамины ---
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.showStaminaHud) return;
        Minecraft mc     = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) return;

        boolean sprinting   = player.isSprinting();
        boolean moving      = player.getDeltaMovement().horizontalDistanceSqr() > 1e-6;
        boolean inBoat      = player.getVehicle() instanceof Boat;
        boolean blockingShield = isHoldingShield(player) && player.isBlocking();

        if (exhausted) {
            if (sprinting)      player.setSprinting(false);
            // Щит опускается — имитируем disable как от топора
            if (blockingShield) player.getCooldowns().addCooldown(
                    player.getUseItem().getItem(), 100
            );
            exhaustionTimer--;
            if (exhaustionTimer <= 0) exhausted = false;
            return;
        }

        if (inBoat) {
            // Лодка
            if (moving) {
                stamina -= DRAIN_BOAT;
                if (stamina <= 0f) stamina = 0f;
            } else {
                stamina = Math.min(stamina + REGEN_STAND, getMaxStamina());
            }
        } else if (sprinting) {
            // Бег
            stamina -= DRAIN_SPRINT;
            if (stamina <= 0f) {
                stamina = 0f;
                triggerExhaustion(player);
            }
        } else if (blockingShield) {
            // Держим щит — пассивный дрейн, регена нет
            float mult = getShieldStaminaMultiplier(player);
            stamina -= DRAIN_SHIELD_HOLD * mult;
            if (stamina <= 0f) {
                stamina = 0f;
                triggerExhaustion(player);
            }
        } else if (moving) {
            stamina = Math.min(stamina + REGEN_WALK, getMaxStamina());
        } else {
            stamina = Math.min(stamina + REGEN_STAND, getMaxStamina());
        }
    }

    // --- Вспомогательные ---

    private static boolean isHoldingShield(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof ShieldItem
                || player.getOffhandItem().getItem() instanceof ShieldItem;
    }

    private static void triggerExhaustion(Player player) {
        exhausted       = true;
        exhaustionTimer = EXHAUSTION_COOLDOWN;
        player.setSprinting(false);
    }

    private static void handleBoatSlowdown(Player player) {
        if (!(player.getVehicle() instanceof Boat boat)) return;

        Vec3 vel    = boat.getDeltaMovement();
        double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        float multiplier;
        if (stamina < STAMINA_CRITICAL)  multiplier = BOAT_SPEED_MULTIPLIER_CRITICAL;
        else if (stamina < STAMINA_LOW)  multiplier = BOAT_SPEED_MULTIPLIER_LOW;
        else                             return;

        if (hSpeed > 0.01) {
            double maxAllowed = ORIGINAL_BOAT_MAX_SPEED * multiplier;
            if (hSpeed > maxAllowed) {
                double scale = maxAllowed / hSpeed;
                boat.setDeltaMovement(vel.x * scale, vel.y, vel.z * scale);
            }
        }
    }
}