package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DrainStaminaPacket {

    private final float amount;

    public DrainStaminaPacket(float amount) {
        this.amount = amount;
    }

    public static void encode(DrainStaminaPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.amount);
    }

    public static DrainStaminaPacket decode(FriendlyByteBuf buf) {
        return new DrainStaminaPacket(buf.readFloat());
    }

    public static void handle(DrainStaminaPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        StaminaManager.drainFromServer(packet.amount)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}