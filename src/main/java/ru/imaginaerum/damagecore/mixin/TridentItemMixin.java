package ru.imaginaerum.damagecore.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(TridentItem.class)
public abstract class TridentItemMixin implements IDamageCoreWeapon {

    private static final UUID BASE_ATTACK_DAMAGE_UUID =
            UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    @Unique
    private final Map<DamageType, Double> damagecore$damageMap = new HashMap<>();

    @Inject(
            method = "getDefaultAttributeModifiers",
            at = @At("HEAD"),
            cancellable = true
    )
    private void damagecore$overrideAttributes(
            EquipmentSlot slot,
            CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir
    ) {
        if (slot != EquipmentSlot.MAINHAND) return;

        // Базовый vanilla-урон трезубца
        double baseDamage = TridentItem.BASE_DAMAGE;

        // Весь урон — колющий
        damagecore$damageMap.clear();
        damagecore$damageMap.put(DamageType.PIERCING, baseDamage);

        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        modifiers.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        BASE_ATTACK_DAMAGE_UUID,
                        "DamageCore trident piercing",
                        damagecore$getTotalDamage(),
                        AttributeModifier.Operation.ADDITION
                )
        );

        cir.setReturnValue(ImmutableMultimap.copyOf(modifiers));
    }

    @Override
    public Map<DamageType, Double> damagecore$getDamageMap() {
        return damagecore$damageMap;
    }

    @Unique
    public double damagecore$getTotalDamage() {
        return damagecore$damageMap.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }
}
