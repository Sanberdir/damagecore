package ru.imaginaerum.damagecore.library_extra_slots;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CombatModeSyncPacket {
    private final boolean combatMode;

    public CombatModeSyncPacket(boolean combatMode) { this.combatMode = combatMode; }

    public static void encode(CombatModeSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.combatMode);
    }

    public static CombatModeSyncPacket decode(FriendlyByteBuf buf) {
        return new CombatModeSyncPacket(buf.readBoolean());
    }

    public static void handle(CombatModeSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide() != net.minecraftforge.fml.LogicalSide.CLIENT) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            player.getCapability(ExtraSlotCapability.INSTANCE)
                    .ifPresent(data -> data.setCombatMode(msg.combatMode));
        });
        ctx.setPacketHandled(true);
    }
}