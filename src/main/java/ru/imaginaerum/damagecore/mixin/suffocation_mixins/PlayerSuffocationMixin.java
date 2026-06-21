package ru.imaginaerum.damagecore.mixin.suffocation_mixins;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;

@Mixin(Player.class)
public class PlayerSuffocationMixin {

    @ModifyVariable(
            method = "aiStep",
            at = @At("STORE"),
            ordinal = 0
    )
    private int damagecore_modifySuffocationInterval(int original) {
        Player player = (Player)(Object)this;

        int enduranceLevel = PlayerStatsCapability.get(player)
                .map(s -> s.getStat(StatsType.ENDURANCE)).orElse(0);

        if (enduranceLevel <= 0) return original;

        return original + enduranceLevel * 5;
    }
}