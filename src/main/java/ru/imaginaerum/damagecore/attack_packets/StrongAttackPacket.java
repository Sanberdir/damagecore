package ru.imaginaerum.damagecore.attack_packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class StrongAttackPacket {

    private static final float DAMAGE_MULTIPLIER = 2.0f;

    public StrongAttackPacket() {}

    public static void encode(StrongAttackPacket packet, FriendlyByteBuf buf) {
        // нет данных
    }

    public static StrongAttackPacket decode(FriendlyByteBuf buf) {
        return new StrongAttackPacket();
    }

    public static void handle(StrongAttackPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer attacker = ctx.get().getSender();
            if (attacker == null) return;

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
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        AABB box = player.getBoundingBox()
                .inflate(1.0);

        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e != player && e.isAlive() && e.isPickable()
        );

        LivingEntity closest = null;
        double minDist = Double.MAX_VALUE;

        for (LivingEntity e : candidates) {
            Vec3 toEntity = e.getBoundingBox().getCenter().subtract(eye).normalize();
            if (toEntity.dot(look) < 0.7) continue;

            double dist = e.distanceToSqr(player);
            if (dist < minDist) {
                minDist = dist;
                closest = e;
            }
        }

        return closest;
    }
}
