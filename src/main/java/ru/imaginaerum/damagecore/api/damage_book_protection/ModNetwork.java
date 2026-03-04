package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class ModNetwork {

    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    private static int id = 0;

    public static void init() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation("damagecore", "main"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );
//        ModNetwork.CHANNEL.sendToServer(new RequestFullSyncPacket());
        CHANNEL.registerMessage(id++,
                LearnNodePacket.class,
                LearnNodePacket::encode,
                LearnNodePacket::decode,
                LearnNodePacket::handle);
        CHANNEL.registerMessage(id++,
                RequestFullSyncPacket.class,
                RequestFullSyncPacket::encode,
                RequestFullSyncPacket::decode,
                RequestFullSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)); // <- важно
        CHANNEL.registerMessage(id++,
                SyncLearnedNodesPacket.class,
                SyncLearnedNodesPacket::encode,
                SyncLearnedNodesPacket::decode,
                SyncLearnedNodesPacket::handle);
    }
}