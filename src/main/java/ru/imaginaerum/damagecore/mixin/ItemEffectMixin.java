package ru.imaginaerum.damagecore.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;
import ru.imaginaerum.damagecore.library_damage.WeaponDamageData;

import java.util.Random;

@Mixin(Item.class)
public abstract class ItemEffectMixin {

    private static final Random RANDOM = new Random();

    @Inject(
            method = "inventoryTick",
            at = @At("HEAD")
    )
    private void damagecore$checkAttackEffects(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected, CallbackInfo ci) {
        if (!isSelected || !(entity instanceof Player player) || level.isClientSide) {
            return;
        }

        if (stack.getItem() instanceof IDamageCoreWeapon weapon) {
            Item item = stack.getItem();
            WeaponDamageData data = DamageCore.WEAPON_DAMAGE_MANAGER.getDamageData(item);

            if (data != null) {
                // Проверяем огненный урон и шанс поджога
                if (data.getDamageMap().containsKey(DamageType.FIRE) && data.hasEffectChance(DamageType.FIRE)) {
                    double fireChance = data.getEffectChance(DamageType.FIRE);

                    // Сохраняем данные в персистентные данные игрока для использования при атаке
                    player.getPersistentData().putBoolean("DamageCoreHasFireWeapon", true);
                    player.getPersistentData().putDouble("DamageCoreFireChance", fireChance);
                } else {
                    player.getPersistentData().remove("DamageCoreHasFireWeapon");
                    player.getPersistentData().remove("DamageCoreFireChance");
                }
            }
        }
    }
}