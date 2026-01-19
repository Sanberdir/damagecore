package ru.imaginaerum.damagecore.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.effect.DCEffects;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;

import java.util.Map;
import java.util.Random;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    private static final Random RANDOM = new Random();

    // Флаг для отслеживания приседания при эффекте
    private boolean damagecore$wasCrouchingBeforeStun = false;

    @Inject(
            method = "die",
            at = @At("HEAD")
    )
    private void damagecore$applyWeaponEffectsOnKill(CallbackInfo ci) {
        LivingEntity target = (LivingEntity) (Object) this;
        Level level = target.level();

        if (level.isClientSide) return;

        // Находим источник урона (игрока, который атаковал)
        Entity attacker = target.getLastAttacker();
        if (attacker instanceof Player player) {
            applyWeaponEffects(player, target);
        }
    }

    @Inject(
            method = "hurt",
            at = @At("HEAD")
    )
    private void damagecore$applyWeaponEffectsOnHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        Level level = target.level();

        if (level.isClientSide) return;

        // Находим источник урона
        Entity attacker = target.getLastAttacker();
        if (attacker instanceof Player player) {
            applyWeaponEffects(player, target);
        }
    }

    // Эффект оглушения
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void stunTravel(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;

        if (entity.hasEffect(DCEffects.STUNNING.get())) {
            Vec3 motion = entity.getDeltaMovement();

            // Применяем гравитацию вручную
            if (!entity.onGround()) {
                motion = motion.add(0, -0.08, 0); // ванильная гравитация
            }

            entity.setDeltaMovement(0, motion.y, 0);
            entity.move(MoverType.SELF, entity.getDeltaMovement());

            // Принудительное приседание для игроков
            if (entity instanceof Player) {
                Player player = (Player) entity;
                if (!player.isCrouching()) {
                    player.setShiftKeyDown(true);
                    player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
                }
            }

            ci.cancel();
        }
    }
    @ModifyVariable(
            method = "hurt",
            at = @At("HEAD"),
            argsOnly = true
    )
    private float damagecore$applySharpnessBonus(
            float amount,
            DamageSource source
    ) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack stack = attacker.getMainHandItem();

            if (stack.getItem() instanceof IDamageCoreWeapon) {
                int sharpness = EnchantmentHelper.getItemEnchantmentLevel(
                        Enchantments.SHARPNESS, stack
                );

                if (sharpness > 0) {
                    double bonus = 1.25 * sharpness;

                    // ❗ НЕ трогаем damageMap
                    return (float) (amount + bonus);
                }
            }
        }
        return amount;
    }

    // Обработка приседания при оглушении
    @Inject(method = "tick", at = @At("HEAD"))
    private void stunTick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;

        if (entity.hasEffect(DCEffects.STUNNING.get())) {
            // Принудительное приседание для игроков
            if (entity instanceof Player) {
                Player player = (Player) entity;
                if (!player.isCrouching()) {
                    player.setShiftKeyDown(true);
                    player.setPose(net.minecraft.world.entity.Pose.CROUCHING);
                }
            }
        }
    }

    private void applyWeaponEffects(Player player, LivingEntity target) {
        // Проверяем огненное оружие
        if (player.getPersistentData().getBoolean("DamageCoreHasFireWeapon")) {
            double fireChance = player.getPersistentData().getDouble("DamageCoreFireChance");

            if (RANDOM.nextDouble() * 100 < fireChance) {
                // Поджигаем цель на 3 секунды (60 тиков)
                target.setSecondsOnFire(3);
                System.out.println("Fire effect applied to " + target.getName().getString() + " with chance " + fireChance + "%");
            }
        }
    }
}