package ru.imaginaerum.damagecore.mixin.swing_animations_attack;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.animation_attack.ICurrentAttackType;
import ru.imaginaerum.damagecore.animation_attack.IExampleAnimatedPlayer;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;
import ru.imaginaerum.damagecore.library_damage.PacketTypedAttack;

// Новый mixin на LocalPlayer
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void damagecore$attack(Player player, Entity target, CallbackInfo ci) {

        if (!(player.getMainHandItem().getItem() instanceof IDamageCoreWeapon)) return;
        if (!(player instanceof IExampleAnimatedPlayer animated)) return;

        // requestSwordSwing вычислит тип и сохранит в currentAttackType
        animated.requestSwordSwing();

        DamageType type = ((ICurrentAttackType) player).damagecore$getCurrentAttackType();

        // Отправляем ОДИН пакет — тип и цель вместе
        ModNetwork.CHANNEL.sendToServer(new PacketTypedAttack(target.getId(), type));

        // Отменяем стандартный ServerboundInteractPacket —
        // чтобы ванильный Player.attack() на сервере вообще не вызывался
        ci.cancel();
    }
}
