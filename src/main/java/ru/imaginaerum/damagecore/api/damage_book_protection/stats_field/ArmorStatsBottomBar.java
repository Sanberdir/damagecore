package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionEffect;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ArmorStatsBottomBar {

    private ArmorStatsBottomBar() {}
    // Добавить в класс статичное поле:
    private static final Map<ResourceLocation, int[]> ENTITY_ICON_POSITIONS = new LinkedHashMap<>();

    public static Map<ResourceLocation, int[]> getEntityIconPositions() {
        return ENTITY_ICON_POSITIONS;
    }
    public static void render(GuiGraphics gui, InventoryScreen screen, int barY, int barHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int guiLeft       = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int iconSize      = 16;
        int effectIconSize = 14;
        int padding       = 2;
        int x = guiLeft + 6;
        int y = barY + (barHeight - iconSize) / 2;

        Set<ResourceLocation> renderedKeys = new HashSet<>();

        // Зачарованная броня
        for (ItemStack stack : mc.player.getArmorSlots()) {
            if (stack.isEmpty()) continue;

            boolean hasProtectionEnchant = false;
            ListTag enchantments = stack.getEnchantmentTags();
            if (enchantments != null && !enchantments.isEmpty()) {
                for (int i = 0; i < enchantments.size(); i++) {
                    CompoundTag tag = enchantments.getCompound(i);
                    String id = tag.getString("id");
                    if (id.equals("minecraft:protection") ||
                            id.equals("minecraft:fire_protection") ||
                            id.equals("minecraft:projectile_protection") ||
                            id.equals("minecraft:blast_protection") ||
                            id.equals("minecraft:feather_falling")) {
                        hasProtectionEnchant = true;
                        break;
                    }
                }
            }
            if (!hasProtectionEnchant) continue;

            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (renderedKeys.add(key)) {
                gui.renderItem(stack, x, y);
                x += iconSize + padding;
            }
        }

        // Еда
        FoodProtectionManager foodManager = FoodProtectionCapability.get(mc.player);
        if (foodManager != null) {
            for (FoodProtectionEffect effect : foodManager.getAllEffects()) {
                if (effect.getProtectionPercent() <= 0) continue;
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(effect.getItem());
                if (renderedKeys.add(key)) {
                    gui.renderItem(new ItemStack(effect.getItem()), x, y);
                    x += iconSize + padding;
                }
            }
        }
        // Сущности — включаем scissor на всю полоску сразу
//        gui.enableScissor(guiLeft, barY + 1, guiLeft + ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth(), barY + barHeight - 1);
        ENTITY_ICON_POSITIONS.clear();
        for (MobEffectInstance effectInstance : mc.player.getActiveEffects()) {
            LivingEntity source = EffectSourceManager.getSource(mc.player, effectInstance.getEffect());
            if (source == null) continue;

            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(source.getType());
            if (key == null || !renderedKeys.add(key)) continue;

            LivingEntity staticCopy = createStaticCopy(source);
            staticCopy.yBodyRot = 0f;
            staticCopy.yHeadRot = 0f;
            staticCopy.setYRot(0f);
            staticCopy.setXRot(0f);

            Quaternionf bodyRot = new Quaternionf()
                    .rotateY((float) Math.toRadians(35))
                    .rotateZ((float) Math.toRadians(20))
                    .rotateX((float) Math.toRadians(200));

            Quaternionf headRot = new Quaternionf()
                    .rotateY((float) Math.toRadians(35))
                    .rotateZ((float) Math.toRadians(20))
                    .rotateX((float) Math.toRadians(200));

            float entityHeight = staticCopy.getBbHeight();
            int size = (int)(12f / entityHeight);
            size = Math.max(4, Math.min(size, 12));

            int targetIconSize = 12;
            int entityX = x + targetIconSize / 2;
            int entityY = barY + barHeight / 2 + 3 + (entityHeight > 1.5f ? 3 : 0);
            ENTITY_ICON_POSITIONS.put(key, new int[]{x, barY, targetIconSize, barHeight});

            gui.pose().pushPose();
            gui.pose().translate(0, 0, 200);

            InventoryScreen.renderEntityInInventory(
                    gui,
                    entityX,
                    entityY,
                    size,
                    bodyRot,
                    headRot,
                    staticCopy
            );

            gui.pose().popPose();

            x += targetIconSize + padding;
        }

//        gui.disableScissor();
    }
    private static LivingEntity createStaticCopy(LivingEntity original) {
        // Создаём временную сущность того же типа
        LivingEntity copy = (LivingEntity) original.getType().create(original.level());
        if (copy == null) return original;

        // Копируем визуально важные данные
        copy.copyPosition(original);
        copy.setCustomName(original.getCustomName());
        copy.setCustomNameVisible(original.isCustomNameVisible());

        // Копируем броню через массив предметов
        ItemStack[] armorItems = new ItemStack[4];
        int i = 0;
        for (ItemStack stack : original.getArmorSlots()) {
            if (i < armorItems.length) {
                armorItems[i] = stack.copy();
            }
            i++;
        }

        // Устанавливаем броню на копию
        for (int slot = 0; slot < armorItems.length; slot++) {
            if (armorItems[slot] != null) {
                copy.getArmorSlots().forEach(item -> {}); // это не работает, используем другой метод
            }
        }

        // Альтернативный способ: устанавливаем предметы напрямую через слоты
        copy.setItemSlot(EquipmentSlot.FEET, original.getItemBySlot(EquipmentSlot.FEET).copy());
        copy.setItemSlot(EquipmentSlot.LEGS, original.getItemBySlot(EquipmentSlot.LEGS).copy());
        copy.setItemSlot(EquipmentSlot.CHEST, original.getItemBySlot(EquipmentSlot.CHEST).copy());
        copy.setItemSlot(EquipmentSlot.HEAD, original.getItemBySlot(EquipmentSlot.HEAD).copy());

        // Копируем предметы в руках
        copy.setItemSlot(EquipmentSlot.MAINHAND, original.getMainHandItem().copy());
        copy.setItemSlot(EquipmentSlot.OFFHAND, original.getOffhandItem().copy());

        // Фиксируем все вращения
        copy.yBodyRot = 0f;
        copy.yHeadRot = 0f;
        copy.setYRot(0f);
        copy.setXRot(0f);
        copy.yBodyRotO = 0f;
        copy.yHeadRotO = 0f;
        copy.yRotO = 0f;
        copy.xRotO = 0f;

        // Отключаем анимацию ходьбы
        copy.walkDistO = 0f;
        copy.walkDist = 0f;

        return copy;
    }
}