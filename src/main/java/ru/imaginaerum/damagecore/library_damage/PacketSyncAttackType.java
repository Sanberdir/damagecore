package ru.imaginaerum.damagecore.library_damage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.imaginaerum.damagecore.animation_attack.ICurrentAttackType;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.function.Supplier;

public class PacketSyncAttackType {

    private final DamageType attackType;

    public PacketSyncAttackType(DamageType type) {
        this.attackType = type;
    }

    public PacketSyncAttackType(FriendlyByteBuf buf) {
        this.attackType = buf.readEnum(DamageType.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(attackType);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            System.out.println("[DC] PacketSyncAttackType получен, sender=" + sender
                    + ", type=" + attackType);

            if (sender == null) {
                System.out.println("[DC] FAIL: sender == null");
                return;
            }

            if (sender instanceof ICurrentAttackType typeHolder) {
                typeHolder.damagecore$setCurrentAttackType(attackType);
                System.out.println("[DC] Тип установлен на сервере: " + attackType);
            } else {
                System.out.println("[DC] FAIL: ServerPlayer не реализует ICurrentAttackType");
            }
        });
        ctx.get().setPacketHandled(true);
    }
}