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
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.library_damage.DamageCoreUtil;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.library_damage.IDamageCoreWeapon;
import ru.imaginaerum.damagecore.library_damage.WeaponDamageData;

import java.util.HashMap;
import java.util.Map;

@Mixin(DiggerItem.class)
public abstract class DiggerItemMixin implements IDamageCoreWeapon {

    @Unique
    private final Map<DamageType, Double> damagecore$damageMap = new HashMap<>();

    @Unique
    private Map<DamageType, Double> damagecore$customDamage = null;

    @Unique
    private boolean damagecore$hasCustom = false;

    @Unique
    private boolean damagecore$initialized = false;

    @Unique
    private Double damagecore$cachedAttackSpeed = null;

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
        Item item = (Item) tool;

        // Инициализируем данные только один раз
        if (!damagecore$initialized) {
            damagecore$initializeData(tool, item);
            damagecore$initialized = true;
        }

        // создаём новые модификаторы с нуля
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        double totalDamage = damagecore$getTotalDamage();

        // Добавляем кастомный урон
        modifiers.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        DamageCoreUtil.BASE_ATTACK_DAMAGE_UUID,
                        "DamageCore tool damage",
                        totalDamage,
                        AttributeModifier.Operation.ADDITION
                )
        );

        // Добавляем скорость атаки
        double attackSpeed = damagecore$getAttackSpeed(tool);
        modifiers.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        DamageCoreUtil.BASE_ATTACK_SPEED_UUID,
                        "DamageCore tool attack speed",
                        attackSpeed - 4.0, // Относительно базовой скорости 4.0
                        AttributeModifier.Operation.ADDITION
                )
        );

        cir.setReturnValue(ImmutableMultimap.copyOf(modifiers));
    }

    @Unique
    private void damagecore$initializeData(DiggerItem tool, Item item) {

        // Проверяем кастомные данные
        WeaponDamageData customData = DamageCore.WEAPON_DAMAGE_MANAGER.getDamageData(item);

        if (customData != null && !customData.isEmpty()) {
            // Используем кастомные данные из JSON
            damagecore$customDamage = new HashMap<>(customData.getDamageMap());
            damagecore$hasCustom = true;

            if (customData.hasAttackSpeed()) {
                damagecore$cachedAttackSpeed = customData.getAttackSpeed();
            }
        } else {
            // Стандартное распределение
            damagecore$hasCustom = false;
            double baseDamage = tool.getAttackDamage();

            // распределение по умолчанию
            double bludgeoning = baseDamage * 0.6;
            double slashing = baseDamage * 0.2;
            double piercing = baseDamage * 0.2;

            // специальные правила для конкретных инструментов
            if (tool instanceof AxeItem) {
                slashing = baseDamage * 0.75;
                bludgeoning = baseDamage * 0.25;
                piercing = 0;
            } else if (tool instanceof PickaxeItem) {
                bludgeoning = baseDamage * 0.8;
                piercing = baseDamage * 0.2;
                slashing = 0;
            } else if (tool instanceof ShovelItem) {
                bludgeoning = baseDamage * 0.5;
                piercing = baseDamage * 0.3;
                slashing = baseDamage * 0.2;
            } else if (tool instanceof HoeItem) {
                slashing = baseDamage;
                piercing = baseDamage + 1;
                bludgeoning = baseDamage;
            }

            // сохраняем карту
            damagecore$damageMap.clear();
            if (bludgeoning > 0) damagecore$damageMap.put(DamageType.BLUDGEONING, bludgeoning);
            if (slashing > 0) damagecore$damageMap.put(DamageType.SLASHING, slashing);
            if (piercing > 0) damagecore$damageMap.put(DamageType.PIERCING, piercing);

            damagecore$cachedAttackSpeed = getDefaultAttackSpeed(tool);
        }
    }

    @Unique
    private double damagecore$getAttackSpeed(DiggerItem tool) {
        if (damagecore$cachedAttackSpeed != null) {
            return damagecore$cachedAttackSpeed;
        }
        return getDefaultAttackSpeed(tool);
    }

    @Unique
    private double getDefaultAttackSpeed(DiggerItem tool) {
        // Базовая скорость для разных типов инструментов
        if (tool instanceof AxeItem) return 1.0;
        if (tool instanceof PickaxeItem) return 1.2;
        if (tool instanceof ShovelItem) return 1.5;
        if (tool instanceof HoeItem) return 2.0;
        return 1.0; // по умолчанию
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