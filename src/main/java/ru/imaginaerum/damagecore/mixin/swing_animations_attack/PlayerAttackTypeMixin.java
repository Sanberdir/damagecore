package ru.imaginaerum.damagecore.mixin.swing_animations_attack;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ru.imaginaerum.damagecore.animation_attack.ICurrentAttackType;
import ru.imaginaerum.damagecore.library_damage.DamageType;

// Базовый Player — работает и на клиенте и на сервере
@Mixin(Player.class)
public abstract class PlayerAttackTypeMixin implements ICurrentAttackType {

    @Unique
    private DamageType damagecore$attackType = DamageType.SLASHING;

    @Override
    public void damagecore$setCurrentAttackType(DamageType type) {
        this.damagecore$attackType = type;
    }

    @Override
    public DamageType damagecore$getCurrentAttackType() {
        return this.damagecore$attackType;
    }
}