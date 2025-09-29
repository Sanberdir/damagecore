package ru.imaginaerum.damagecore.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TridentItem;
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

@Mixin(TridentItem.class)
public abstract class TridentItemMixin implements IDamageCoreWeapon {

    @Unique
    private final Map<DamageType, Double> damagecore$damageMap = new HashMap<>();

    @Unique
    private Map<DamageType, Double> damagecore$customDamage = null;

    @Unique
    private boolean damagecore$hasCustom = false;

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

        TridentItem trident = (TridentItem) (Object) this;
        Item item = (Item) trident;

        System.out.println("=== Processing trident: " + item);

        // Проверяем кастомные данные
        WeaponDamageData customData = DamageCore.WEAPON_DAMAGE_MANAGER.getDamageData(item);

        if (customData != null && !customData.isEmpty()) {
            // Используем кастомные данные из JSON
            System.out.println("Using CUSTOM damage data for trident: " + customData.getDamageMap());
            damagecore$customDamage = new HashMap<>(customData.getDamageMap());
            damagecore$hasCustom = true;
        } else {
            // Стандартное распределение - весь урон колющий
            System.out.println("Using DEFAULT damage distribution for trident");
            damagecore$hasCustom = false;
            double baseDamage = TridentItem.BASE_DAMAGE;

            damagecore$damageMap.clear();
            damagecore$damageMap.put(DamageType.PIERCING, baseDamage);
        }

        // создаём новые модификаторы
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        double totalDamage = damagecore$getTotalDamage();
        System.out.println("Total trident damage: " + totalDamage);

        modifiers.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        DamageCoreUtil.BASE_ATTACK_DAMAGE_UUID,
                        "DamageCore trident damage",
                        totalDamage,
                        AttributeModifier.Operation.ADDITION
                )
        );

        cir.setReturnValue(ImmutableMultimap.copyOf(modifiers));
    }

    @Override
    public Map<DamageType, Double> damagecore$getDamageMap() {
        return damagecore$hasCustom ? damagecore$customDamage : damagecore$damageMap;
    }

    @Override
    public void damagecore$setCustomDamage(Map<DamageType, Double> customDamage) {
        this.damagecore$customDamage = customDamage;
        this.damagecore$hasCustom = true;
    }

    @Override
    public boolean damagecore$hasCustomDamage() {
        return damagecore$hasCustom;
    }

    @Unique
    public double damagecore$getTotalDamage() {
        Map<DamageType, Double> map = damagecore$getDamageMap();
        return map.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }
}