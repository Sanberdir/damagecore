package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ru.imaginaerum.damagecore.hud.DrainStaminaClientProxy;

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
                // ИСПРАВЛЕНО: вместо прямой ссылки на StaminaManager используем прокси-класс.
                // DistExecutor гарантирует, что DrainStaminaClientProxy (и через него
                // StaminaManager с Minecraft.getInstance()) загрузится ТОЛЬКО на клиенте.
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        DrainStaminaClientProxy.drain(packet.amount)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}