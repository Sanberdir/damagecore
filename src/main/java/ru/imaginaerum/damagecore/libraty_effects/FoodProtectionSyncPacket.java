package ru.imaginaerum.damagecore.libraty_effects;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FoodProtectionSyncPacket {

    private final CompoundTag data;

    public FoodProtectionSyncPacket(CompoundTag data) {
        this.data = data;
    }

    public static void encode(FoodProtectionSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeNbt(pkt.data);
    }

    public static FoodProtectionSyncPacket decode(FriendlyByteBuf buf) {
        return new FoodProtectionSyncPacket(buf.readNbt());
    }

    public static void handle(FoodProtectionSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            player.getCapability(FoodProtectionCapability.FOOD_PROTECTION)
                    .ifPresent(manager -> manager.load(pkt.data));
        });
        ctx.get().setPacketHandled(true);
    }
}