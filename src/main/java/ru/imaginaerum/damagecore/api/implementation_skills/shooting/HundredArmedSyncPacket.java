package ru.imaginaerum.damagecore.api.implementation_skills.shooting;

import net.minecraft.network.FriendlyByteBuf;
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
        ctx.get().enqueueWork(() -> {
            ClientHundredArmedData.hasSkill = packet.hasSkill;
        });
        ctx.get().setPacketHandled(true);
    }
}