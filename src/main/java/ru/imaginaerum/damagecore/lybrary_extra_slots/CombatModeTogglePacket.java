package ru.imaginaerum.damagecore.lybrary_extra_slots;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.lybrary_extra_slots.ExtraSlotCapability;

import java.util.function.Supplier;

public class CombatModeTogglePacket {

    public CombatModeTogglePacket() {}

    public static void encode(CombatModeTogglePacket msg, FriendlyByteBuf buf) {}

    public static CombatModeTogglePacket decode(FriendlyByteBuf buf) {
        return new CombatModeTogglePacket();
    }

    public static void handle(CombatModeTogglePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            player.getCapability(ExtraSlotCapability.INSTANCE).ifPresent(data -> {
                boolean newState = !data.isCombatMode();
                data.setCombatMode(newState);

                // клиент должен знать о смене режима, иначе рендер руки и предсказание будут врать
                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new CombatModeSyncPacket(newState)
                );
            });
        });
        ctx.setPacketHandled(true);
    }
}