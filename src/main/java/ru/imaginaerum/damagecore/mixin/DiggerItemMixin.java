package ru.imaginaerum.damagecore.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
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

@Mixin(DiggerItem.class)
public abstract class DiggerItemMixin implements IDamageCoreWeapon {

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

        DiggerItem tool = (DiggerItem) (Object) this;
        double baseDamage = tool.getAttackDamage();

        // распределение по умолчанию
        double bludgeoding = baseDamage * 0.6;
        double slashing = baseDamage * 0.2;
        double piercing = baseDamage * 0.2;

        // специальные правила для конкретных инструментов
        if (tool instanceof AxeItem) {
            slashing = baseDamage * 0.75;
            bludgeoding = baseDamage * 0.25;
            piercing = 0;
        } else if (tool instanceof PickaxeItem) {
            bludgeoding = baseDamage * 0.8;
            piercing = baseDamage * 0.2;
            slashing = 0;
        } else if (tool instanceof ShovelItem) {
            bludgeoding = baseDamage * 0.5;
            piercing = baseDamage * 0.3;
            slashing = baseDamage * 0.2;
        } else if (tool instanceof HoeItem) {
            slashing = baseDamage;
            piercing = baseDamage + 1;
            bludgeoding = baseDamage;
        }

        // сохраняем карту
        damagecore$damageMap.clear();
        if (bludgeoding > 0) damagecore$damageMap.put(DamageType.BLUDGEONING, bludgeoding);
        if (slashing > 0) damagecore$damageMap.put(DamageType.SLASHING, slashing);
        if (piercing > 0) damagecore$damageMap.put(DamageType.PIERCING, piercing);

        // создаём новые модификаторы
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        modifiers.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        BASE_ATTACK_DAMAGE_UUID,
                        "DamageCore tool damage",
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
