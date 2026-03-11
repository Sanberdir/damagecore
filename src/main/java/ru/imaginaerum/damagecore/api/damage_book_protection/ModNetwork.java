package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import ru.imaginaerum.damagecore.api.damage_book_protection.node_variant.SelectVariantPacket;
import ru.imaginaerum.damagecore.api.damage_book_protection.node_variant.SyncNodeVariantsPacket;
import ru.imaginaerum.damagecore.events_tree.SyncTreeXpPacket;

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
        int packetId = 0;
// сервер → клиент
        CHANNEL.registerMessage(id++, SyncNodeVariantsPacket.class,
                SyncNodeVariantsPacket::encode,
                SyncNodeVariantsPacket::decode,
                SyncNodeVariantsPacket::handle);
        CHANNEL.registerMessage(
                packetId++,
                SelectVariantPacket.class,
                SelectVariantPacket::encode,
                SelectVariantPacket::decode,
                SelectVariantPacket::handle
        );
        // В методе init() добавить:
        CHANNEL.registerMessage(id++,
                SyncTreeXpPacket.class,
                SyncTreeXpPacket::encode,
                SyncTreeXpPacket::decode,
                SyncTreeXpPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)); // <- важно: с сервера на клиент
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