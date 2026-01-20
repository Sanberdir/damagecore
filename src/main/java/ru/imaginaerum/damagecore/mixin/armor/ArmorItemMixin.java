package ru.imaginaerum.damagecore.mixin.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorItem.class)
public abstract class ArmorItemMixin extends Item {

    @Shadow protected ArmorMaterial material;

    public ArmorItemMixin(Properties properties) {
        super(properties);
    }

    // Самый простой и безопасный миксин - только удаляем стандартную защиту
    @Inject(method = "getDefaultAttributeModifiers", at = @At("RETURN"), cancellable = true)
    private void damagecore$onGetDefaultAttributeModifiers(EquipmentSlot slot,
                                                           CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {

        ArmorItem self = (ArmorItem)(Object)this;

        // Только для нужного слота
        if (slot == self.getType().getSlot()) {
            Multimap<Attribute, AttributeModifier> original = cir.getReturnValue();
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            // Копируем все существующие модификаторы, кроме стандартных атрибутов защиты
            for (var entry : original.entries()) {
                Attribute attribute = entry.getKey();
                // Удаляем только стандартную защиту
                if (attribute != Attributes.ARMOR && attribute != Attributes.ARMOR_TOUGHNESS) {
                    builder.put(entry);
                }
            }

            cir.setReturnValue(builder.build());
        }
    }
}