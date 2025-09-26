package ru.imaginaerum.damagecore.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.SwordItem;
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

@Mixin(SwordItem.class)
public abstract class SwordItemMixin implements IDamageCoreWeapon {

    private static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    @Unique
    private Map<DamageType, Double> damagecore$damageMap = new HashMap<>();

    @Inject(method = "getDefaultAttributeModifiers", at = @At("RETURN"), cancellable = true)
    private void damagecore$replaceDamageTypes(EquipmentSlot slot, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create(cir.getReturnValue());

        if (slot == EquipmentSlot.MAINHAND) {
            // Убираем vanilla ATTACK_DAMAGE
            modifiers.removeAll(Attributes.ATTACK_DAMAGE);

            SwordItem sword = (SwordItem) (Object) this;
            double baseDamage = sword.getDamage();

            double piercing = baseDamage * 0.3;
            double slashing = baseDamage * 0.7;

            // Сохраняем распределённый урон в карту
            damagecore$damageMap.clear();
            damagecore$damageMap.put(DamageType.PIERCING, piercing);
            damagecore$damageMap.put(DamageType.SLASHING, slashing);

            // Добавляем суммарный урон обратно для реального урона
            double totalDamage = piercing + slashing;
            modifiers.put(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            BASE_ATTACK_DAMAGE_UUID,
                            "DamageCore sword damage",
                            totalDamage,
                            AttributeModifier.Operation.ADDITION
                    )
            );
        }

        cir.setReturnValue(ImmutableMultimap.copyOf(modifiers));
    }

    @Override
    public Map<DamageType, Double> damagecore$getDamageMap() {
        return damagecore$damageMap;
    }

    @Unique
    public double damagecore$getTotalDamage() {
        return damagecore$damageMap.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
