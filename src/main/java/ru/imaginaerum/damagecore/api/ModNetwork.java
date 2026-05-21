package ru.imaginaerum.damagecore.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import ru.imaginaerum.damagecore.api.damage_book_protection.LearnNodePacket;
import ru.imaginaerum.damagecore.api.damage_book_protection.RequestFullSyncPacket;
import ru.imaginaerum.damagecore.api.damage_book_protection.SyncLearnedNodesPacket;
import ru.imaginaerum.damagecore.api.damage_book_protection.SyncNodeLevelsPacket;
import ru.imaginaerum.damagecore.api.damage_book_protection.node_variant.SelectNodeVariantPacket;
import ru.imaginaerum.damagecore.api.damage_book_protection.node_variant.SelectVariantPacket;
import ru.imaginaerum.damagecore.api.damage_book_protection.node_variant.SyncNodeVariantsPacket;
import ru.imaginaerum.damagecore.api.implementation_skills.shooting.HundredArmedSyncPacket;
import ru.imaginaerum.damagecore.attack_packets.strong_attack.StrongAttackPacket;
import ru.imaginaerum.damagecore.events_tree.SyncTreeXpPacket;
import ru.imaginaerum.damagecore.hud.elements.DrainStaminaPacket;
import ru.imaginaerum.damagecore.hud.elements.NormalAttackPacket;
import ru.imaginaerum.damagecore.hud.net.ThirstDamagePacket;
import ru.imaginaerum.damagecore.library_damage.PacketSyncAttackType;
import ru.imaginaerum.damagecore.library_damage.PacketTypedAttack;

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
        CHANNEL.registerMessage(id++, SyncNodeVariantsPacket.class,
                SyncNodeVariantsPacket::encode,
                SyncNodeVariantsPacket::decode,
                SyncNodeVariantsPacket::handle);

        CHANNEL.registerMessage(id++,
                SyncNodeLevelsPacket.class,
                SyncNodeLevelsPacket::encode,
                SyncNodeLevelsPacket::decode,
                SyncNodeLevelsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(
                id++,
                HundredArmedSyncPacket.class,
                HundredArmedSyncPacket::encode,
                HundredArmedSyncPacket::decode,
                HundredArmedSyncPacket::handle
        );
        CHANNEL.registerMessage(
                id++,
                SelectNodeVariantPacket.class,
                SelectNodeVariantPacket::encode,
                SelectNodeVariantPacket::decode,
                SelectNodeVariantPacket::handle
        );
        CHANNEL.registerMessage(
                id++,
                SelectVariantPacket.class,
                SelectVariantPacket::encode,
                SelectVariantPacket::decode,
                SelectVariantPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++,
                SyncTreeXpPacket.class,
                SyncTreeXpPacket::encode,
                SyncTreeXpPacket::decode,
                SyncTreeXpPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
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
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++,
                SyncLearnedNodesPacket.class,
                SyncLearnedNodesPacket::encode,
                SyncLearnedNodesPacket::decode,
                SyncLearnedNodesPacket::handle);
        CHANNEL.registerMessage(
                id++,  // следующий свободный id
                ThirstDamagePacket.class,
                ThirstDamagePacket::toBytes,
                ThirstDamagePacket::new,
                ThirstDamagePacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER)

        );
        CHANNEL.registerMessage(
                id++,
                PacketSyncAttackType.class,
                PacketSyncAttackType::encode,
                PacketSyncAttackType::new,       // decode: конструктор из FriendlyByteBuf
                PacketSyncAttackType::handle
        );
        CHANNEL.registerMessage(
                /* следующий свободный id */ 2,
                PacketTypedAttack.class,
                PacketTypedAttack::encode,
                PacketTypedAttack::new,
                PacketTypedAttack::handle
        );
        // ─── Сильная атака (СКМ) ───────────────────────────────────────────
        CHANNEL.registerMessage(id++,
                StrongAttackPacket.class,
                StrongAttackPacket::encode,
                StrongAttackPacket::decode,
                StrongAttackPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, DrainStaminaPacket.class,
                DrainStaminaPacket::encode,
                DrainStaminaPacket::decode,
                DrainStaminaPacket::handle);

        CHANNEL.registerMessage(id++, NormalAttackPacket.class,
                NormalAttackPacket::encode,
                NormalAttackPacket::decode,
                NormalAttackPacket::handle);
    }
}