package ru.imaginaerum.damagecore.library_damage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketTypedAttack {

    private final int targetId;
    private final DamageType attackType;

    public PacketTypedAttack(int targetId, DamageType type) {
        this.targetId = targetId;
        this.attackType = type;
    }

    public PacketTypedAttack(FriendlyByteBuf buf) {
        this.targetId = buf.readInt();
        this.attackType = buf.readEnum(DamageType.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(targetId);
        buf.writeEnum(attackType);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            if (!(sender.getMainHandItem().getItem() instanceof IDamageCoreWeapon weapon)) return;

            Entity target = sender.level().getEntity(targetId);
            if (!(target instanceof LivingEntity living)) return;

            double damage = weapon.damagecore$getDamageMap().getOrDefault(attackType, 0.0);
            if (damage <= 0) return;

            TypedDamageSource source = new TypedDamageSource(
                    sender.level().damageSources().playerAttack(sender).typeHolder(),
                    attackType,
                    sender
            );

            living.hurt(source, (float) damage);
            sender.resetAttackStrengthTicker();

            System.out.println("[DC] PacketTypedAttack: type=" + attackType
                    + " damage=" + damage + " target=" + living);
        });
        ctx.get().setPacketHandled(true);
    }
}