package ru.imaginaerum.damagecore.mixin.arrow_data;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.arrow_data.ArrowDamageManager;

import java.util.HashMap;
import java.util.Map;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @Shadow protected boolean inGround;
    @Shadow protected abstract ItemStack getPickupItem();
    @Unique private boolean dc_flatDamage = false;
    @Unique private float dc_distanceTraveled     = 0f;
    @Unique private boolean dc_rangeCrossed       = false;
    @Unique private Map<DamageType, Double> dc_finalDamageMap = null;
    @Unique private float dc_maxRange             = -1f;
    @Unique private float dc_postRangeDamageScale = 0f;
    @Unique private float dc_postRangeSpeedScale  = 0f;
    @Unique private float dc_gravityScale         = 1.0f;
    @Unique private static final java.util.Random DC_RANDOM = new java.util.Random();
    @Unique private boolean dc_initialized = false;


    @Inject(method = "shoot", at = @At("TAIL"))
    private void dc_onShoot(double x, double y, double z, float velocity, float inaccuracy, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow)(Object)this;
        ItemStack stack = getPickupItem();

        if (stack == null || ArrowDamageManager.INSTANCE == null
                || !ArrowDamageManager.INSTANCE.hasData(stack.getItem())) return;

        var data = ArrowDamageManager.INSTANCE.getData(stack.getItem());

        // Скорость применяем сразу — она нужна именно в момент выстрела
        float speedMultiplier = data.getBaseSpeed() / 3.0f;
        if (Math.abs(speedMultiplier - 1.0f) > 0.01f) {
            self.setDeltaMovement(self.getDeltaMovement().scale(speedMultiplier));
        }

        // Сбрасываем флаг — tick переинициализирует с актуальными данными
        dc_initialized = false;
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void dc_applyCustomGravity(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow)(Object)this;
        if (inGround) return;

        ItemStack stack = getPickupItem();
        if (stack == null || ArrowDamageManager.INSTANCE == null ||
                !ArrowDamageManager.INSTANCE.hasData(stack.getItem())) return;

        // Инициализируем при первом тике где есть данные
        if (!dc_initialized) {

            var data = ArrowDamageManager.INSTANCE.getData(stack.getItem());
            dc_gravityScale = data.getGravityScale();
            dc_flatDamage = data.isFlatDamage();
            dc_setArrowStats(
                    data.getDamageMap(),
                    data.getBaseRange(),
                    data.getPostRangeDamageScale(),
                    data.getPostRangeSpeedScale()
            );
            dc_initialized = true;
        }

        if (dc_gravityScale == 1.0f) return;

        Vec3 vel = self.getDeltaMovement();
        double correction = 0.05 * (1.0 - dc_gravityScale);
        self.setDeltaMovement(vel.x, vel.y + correction, vel.z);
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void dc_onTick(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow)(Object)this;
        if (inGround) return;

        ItemStack stack = getPickupItem();
        boolean hasData = stack != null && ArrowDamageManager.INSTANCE != null
                && ArrowDamageManager.INSTANCE.hasData(stack.getItem());

        if (!hasData) return;


        // Дальность
        Vec3 vel = self.getDeltaMovement();
        dc_distanceTraveled += (float) vel.length();

        if (dc_maxRange > 0 && !dc_rangeCrossed && dc_distanceTraveled >= dc_maxRange) {
            dc_rangeCrossed = true;

            if (dc_postRangeSpeedScale != 0f) {
                self.setDeltaMovement(self.getDeltaMovement().scale(1.0 + dc_postRangeSpeedScale));
            }
        }
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void dc_onHitEntity(EntityHitResult result, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow)(Object)this;
        if (inGround) return;

        Map<DamageType, Double> damageMap = dc_finalDamageMap;
        if (damageMap == null) {
            ItemStack stack = getPickupItem();
            if (stack != null && ArrowDamageManager.INSTANCE != null
                    && ArrowDamageManager.INSTANCE.hasData(stack.getItem())) {
                damageMap = ArrowDamageManager.INSTANCE.getData(stack.getItem()).getDamageMap();
            }
        }
        float damageMult = 1.0f;
        if (dc_maxRange > 0 && dc_distanceTraveled > dc_maxRange && dc_postRangeDamageScale != 0f) {
            float over = (dc_distanceTraveled - dc_maxRange) / dc_maxRange; // насколько перелетел относительно base_range
            damageMult = 1.0f + over * dc_postRangeDamageScale;
            damageMult = Math.max(damageMult, 0.1f); // минимум 0.1
        }
        if (damageMap == null || damageMap.isEmpty()) return;

        Entity target = result.getEntity();
        Entity owner  = self.getOwner();
        if (target == null) return;

        boolean isCrit = self.isCritArrow();
        float speed = (float) self.getDeltaMovement().length();

        for (Map.Entry<DamageType, Double> entry : damageMap.entrySet()) {
            DamageType type  = entry.getKey();
            double rawDamage = entry.getValue();

            if (isCrit) {
                rawDamage += DC_RANDOM.nextInt((int)(rawDamage / 2) + 2);
                rawDamage  = Math.min(rawDamage, Integer.MAX_VALUE);
            }

            int damage = Mth.ceil(rawDamage * (dc_flatDamage ? 1.0f : speed) * damageMult);
            applyPreEffects(self, target, type);
            target.hurt(self.damageSources().arrow(self, owner != null ? owner : self), damage);
        }

        handlePostHit(self, target);
        ci.cancel();
    }

    @Unique
    private void applyPreEffects(AbstractArrow arrow, Entity target, DamageType type) {
        switch (type) {
            case FIRE -> target.setSecondsOnFire(5);
            case POISON -> {
                if (target instanceof LivingEntity le)
                    le.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.POISON, 80, 0));
            }
            case COLD -> {
                if (target instanceof LivingEntity le)
                    le.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            case LIGHTNING -> {
                if (!arrow.level().isClientSide)
                    arrow.level().addFreshEntity(
                            new net.minecraft.world.entity.LightningBolt(
                                    net.minecraft.world.entity.EntityType.LIGHTNING_BOLT,
                                    arrow.level()));
            }
            default -> {}
        }
    }

    @Unique
    private void handlePostHit(AbstractArrow self, Entity target) {
        if (self.getKnockback() > 0 && target instanceof LivingEntity le) {
            double resistance = Math.max(0.0,
                    1.0 - le.getAttributeValue(
                            net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
            Vec3 kb = self.getDeltaMovement()
                    .multiply(1, 0, 1).normalize()
                    .scale(self.getKnockback() * 0.6 * resistance);
            if (kb.lengthSqr() > 0) le.push(kb.x, 0.1, kb.z);
        }

        self.playSound(net.minecraft.sounds.SoundEvents.ARROW_HIT,
                1.0f, 1.2f / (DC_RANDOM.nextFloat() * 0.2f + 0.9f));

        if (self.getPierceLevel() <= 0) self.discard();
    }

    @Unique
    public void dc_setArrowStats(Map<DamageType, Double> damageMap,
                                 float maxRange,
                                 float postRangeDmgScale,
                                 float postRangeSpeedScale) {
        this.dc_finalDamageMap       = new HashMap<>(damageMap);
        this.dc_maxRange             = maxRange;
        this.dc_postRangeDamageScale = postRangeDmgScale;
        this.dc_postRangeSpeedScale  = postRangeSpeedScale;
        this.dc_distanceTraveled     = 0f;
        this.dc_rangeCrossed         = false;
    }
}