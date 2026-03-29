package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionEffect;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;

import java.util.HashSet;
import java.util.Set;

public final class ArmorStatsBottomBar {

    private ArmorStatsBottomBar() {}

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

        // Эффекты
        for (MobEffectInstance effectInstance : mc.player.getActiveEffects()) {
            ResourceLocation key = BuiltInRegistries.MOB_EFFECT.getKey(effectInstance.getEffect());
            if (!renderedKeys.add(key)) continue;

            int effectY = y + (iconSize - effectIconSize) / 2;
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite =
                    mc.getMobEffectTextures().get(effectInstance.getEffect());
            gui.blit(x, effectY, 0, effectIconSize, effectIconSize, sprite);

            int amplifier = effectInstance.getAmplifier() + 1;
            if (amplifier > 1) {
                String levelText = String.valueOf(amplifier);
                gui.pose().pushPose();
                gui.pose().scale(0.7f, 0.7f, 1.0f);
                float scaledX = (x + effectIconSize / 2f) / 0.7f;
                float scaledY = (effectY + effectIconSize / 2f - 3) / 0.7f;
                int textWidth = mc.font.width(levelText);
                gui.drawString(mc.font, levelText,
                        (int)(scaledX - (textWidth * 0.7f) / 2f),
                        (int)scaledY,
                        0xFFFFFF, true);
                gui.pose().popPose();
            }

            x += effectIconSize + padding;
        }
    }
}