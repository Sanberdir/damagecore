package ru.imaginaerum.damagecore.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.library_damage.DamageCoreUtil;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;
import ru.imaginaerum.damagecore.library_damage.WeaponDamageData;

import java.util.HashMap;
import java.util.Map;

@Mixin(SwordItem.class)
public abstract class SwordItemMixin implements IDamageCoreWeapon {

    @Unique
    private Map<DamageType, Double> damagecore$damageMap =
            new HashMap<>();

    @Unique
    private Map<DamageType, Double> damagecore$customDamage =
            null;

    @Unique
    private boolean damagecore$hasCustom = false;

    @Inject(
            method = "getDefaultAttributeModifiers",
            at = @At("RETURN"),
            cancellable = true
    )
    private void damagecore$replaceDamageTypes(
            EquipmentSlot slot,
            CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir
    ) {

        Multimap<Attribute, AttributeModifier> modifiers =
                HashMultimap.create(cir.getReturnValue());

        if (slot == EquipmentSlot.MAINHAND) {

            SwordItem sword = (SwordItem) (Object) this;
            Item item = (Item) sword;

            WeaponDamageData customData =
                    DamageCore.WEAPON_DAMAGE_MANAGER
                            .getDamageData(item);

            // ─────────────────────────────────────────────
            // Custom damage map
            // ─────────────────────────────────────────────

            if (customData != null && !customData.isEmpty()) {

                damagecore$customDamage =
                        new HashMap<>(customData.getDamageMap());

                damagecore$hasCustom = true;

            } else {

                damagecore$hasCustom = false;

                double baseDamage = sword.getDamage();

                double piercing = baseDamage * 0.3;
                double slashing = baseDamage * 0.7;

                damagecore$damageMap.clear();

                damagecore$damageMap.put(
                        DamageType.PIERCING,
                        piercing
                );

                damagecore$damageMap.put(
                        DamageType.SLASHING,
                        slashing
                );
            }

            // ─────────────────────────────────────────────
            // Полностью отключаем vanilla damage
            // ─────────────────────────────────────────────

            modifiers.removeAll(Attributes.ATTACK_DAMAGE);

            modifiers.put(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            DamageCoreUtil.BASE_ATTACK_DAMAGE_UUID,
                            "DamageCore empty damage",
                            -1.0D,
                            AttributeModifier.Operation.ADDITION
                    )
            );

            // ─────────────────────────────────────────────
            // Кастомная скорость атаки
            // ─────────────────────────────────────────────

            if (customData != null && customData.hasAttackSpeed()) {

                modifiers.removeAll(Attributes.ATTACK_SPEED);

                modifiers.put(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                DamageCoreUtil.BASE_ATTACK_SPEED_UUID,
                                "DamageCore attack speed",
                                customData.getAttackSpeed() - 4.0,
                                AttributeModifier.Operation.ADDITION
                        )
                );
            }
        }

        cir.setReturnValue(
                ImmutableMultimap.copyOf(modifiers)
        );
    }

    @Override
    public Map<DamageType, Double> damagecore$getDamageMap() {

        return damagecore$hasCustom
                ? damagecore$customDamage
                : damagecore$damageMap;
    }

    @Override
    public void damagecore$setCustomDamage(
            Map<DamageType, Double> customDamage
    ) {

        this.damagecore$customDamage = customDamage;
        this.damagecore$hasCustom = true;
    }

    @Override
    public boolean damagecore$hasCustomDamage() {
        return damagecore$hasCustom;
    }

    @Unique
    public double damagecore$getTotalDamage() {

        Map<DamageType, Double> map =
                damagecore$getDamageMap();

        return map.values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }
}