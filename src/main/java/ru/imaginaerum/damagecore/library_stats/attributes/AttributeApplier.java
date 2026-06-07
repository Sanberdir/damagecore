package ru.imaginaerum.damagecore.library_stats.attributes;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class AttributeApplier {

    private static final UUID LIVE_FORGE_UUID =
            UUID.nameUUIDFromBytes("damagecore:live_forge_bonus".getBytes());

    private static final String LIVE_FORGE_NAME = "damagecore.live_forge_bonus";

    public static void applyLiveForge(Player player, int level) {
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        // Убираем старый модификатор
        attr.removeModifier(LIVE_FORGE_UUID);

        if (level <= 0) return;

        // +1 HP за каждый уровень (1 единица = половина сердца)
        // Если хочешь полное сердце за уровень — замени level на level * 2
        attr.addPermanentModifier(new AttributeModifier(
                LIVE_FORGE_UUID,
                LIVE_FORGE_NAME,
                level/2,
                AttributeModifier.Operation.ADDITION
        ));
    }
}