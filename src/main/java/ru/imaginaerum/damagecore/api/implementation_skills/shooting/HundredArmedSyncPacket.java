package ru.imaginaerum.damagecore.api.implementation_skills.shooting;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HundredArmedSyncPacket {

    private final boolean hasSkill;

    public HundredArmedSyncPacket(boolean hasSkill) {
        this.hasSkill = hasSkill;
    }

    public static HundredArmedSyncPacket decode(FriendlyByteBuf buf) {
        return new HundredArmedSyncPacket(buf.readBoolean());
    }

    public static void encode(HundredArmedSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.hasSkill);
    }

    public static void handle(HundredArmedSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                // ИСПРАВЛЕНО: вместо прямого обращения к ClientHundredArmedData используем прокси,
                // чтобы JVM не загружала @OnlyIn(CLIENT) класс на выделенном сервере.
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        HundredArmedClientProxy.apply(packet.hasSkill)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}