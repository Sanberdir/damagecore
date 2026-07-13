package ru.imaginaerum.damagecore.libraty_effects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Убран импорт Minecraft!

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
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        FoodProtectionClientProxy.apply(pkt.data)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}