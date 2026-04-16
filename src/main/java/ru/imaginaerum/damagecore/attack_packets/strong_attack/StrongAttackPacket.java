package ru.imaginaerum.damagecore.attack_packets.strong_attack;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.hud.elements.DrainStaminaPacket;

import java.util.List;
import java.util.function.Supplier;

public class StrongAttackPacket {

    private static final float DAMAGE_MULTIPLIER = 2.0f;
    protected static final long COOLDOWN_MS = 800L; // 1.5 секунды

    // Кулдаун по UUID игрока — не слетает при переподключении за сессию
    private static final java.util.Map<java.util.UUID, Long> lastAttackTime =
            new java.util.concurrent.ConcurrentHashMap<>();

    public StrongAttackPacket() {}

    public static void encode(StrongAttackPacket packet, FriendlyByteBuf buf) {}

    public static StrongAttackPacket decode(FriendlyByteBuf buf) {
        return new StrongAttackPacket();
    }

    public static void handle(StrongAttackPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer attacker = ctx.get().getSender();
            if (attacker == null) return;

            long now = System.currentTimeMillis();
            long last = lastAttackTime.getOrDefault(attacker.getUUID(), 0L);
            if (now - last < COOLDOWN_MS) return;
            lastAttackTime.put(attacker.getUUID(), now);

            // Дренаж стамины — всегда, даже если цели нет
            ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> attacker),
                    new DrainStaminaPacket(6.0f)
            );

            LivingEntity target = findTarget(attacker);
            if (target == null) return;

            float baseDamage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float strongDamage = baseDamage * DAMAGE_MULTIPLIER;

            target.hurt(
                    attacker.damageSources().playerAttack(attacker),
                    strongDamage
            );

            Vec3 dir = target.position().subtract(attacker.position()).normalize();
            target.knockback(0.5, -dir.x, -dir.z);
        });
        ctx.get().setPacketHandled(true);
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 eye  = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double reach = 3.0;
        Vec3 end  = eye.add(look.scale(reach));

        // Проверяем блоки на пути
        BlockHitResult blockHit = player.level().clip(new ClipContext(
                eye, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        double blockDist = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation().distanceTo(eye)
                : reach;

        // Ищем entity в конусе взгляда
        AABB searchBox = player.getBoundingBox().inflate(reach);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> e != player && e.isAlive() && e.isPickable()
        );

        LivingEntity closest = null;
        double minDist = Double.MAX_VALUE;

        for (LivingEntity e : candidates) {
            Vec3 toEntity = e.getBoundingBox().getCenter().subtract(eye).normalize();
            if (toEntity.dot(look) < 0.97) continue;

            double dist = e.getBoundingBox().getCenter().distanceTo(eye);

            // Цель должна быть ближе блока на пути
            if (dist > blockDist) continue;

            if (dist < minDist) {
                minDist = dist;
                closest = e;
            }
        }

        return closest;
    }
}