package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;

import java.util.ArrayList;
import java.util.List;

public final class ArmorStatsDetailsWindow {

    private ArmorStatsDetailsWindow() {}

    public static void render(GuiGraphics gui, InventoryScreen screen,
                              int topFieldY, int fieldWidth, int fieldHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        DamageType selectedType = ArmorStatsHoverHandler.getSelectedDamageType();
        if (selectedType == null) return;

        float totalPercent  = ArmorStatsCalculator.getPlayerTotalProtectionPercent(mc.player)
                .getOrDefault(selectedType, 0f) * 100;
        float armorPercent  = ArmorStatsCalculator.getArmorOnlyProtectionPercent(mc.player)
                .getOrDefault(selectedType, 0f) * 100;
        float foodPercent   = 0f;

        FoodProtectionManager foodManager = FoodProtectionCapability.get(mc.player);
        if (foodManager != null) {
            foodPercent = foodManager.getTotalProtectionPercent(selectedType) * 100;
        }

        float enchantPercent = Math.max(0, totalPercent - armorPercent - foodPercent);

        List<Component> lines = new ArrayList<>();
        int nonZeroCount = 0;

        if (armorPercent   > 0.01f) { lines.add(line("armor_stat.armor",   armorPercent));   nonZeroCount++; }
        if (enchantPercent > 0.01f) { lines.add(line("armor_stat.enchant", enchantPercent)); nonZeroCount++; }
        if (foodPercent    > 0.01f) { lines.add(line("armor_stat.food",    foodPercent));    nonZeroCount++; }

        if (nonZeroCount >= 2) {
            lines.add(line("armor_stat.total", totalPercent));
        }

        int lineHeight   = mc.font.lineHeight + 2;
        int titleHeight  = mc.font.lineHeight + 4;
        int paddingTop   = 6;
        int paddingBottom = 6;
        int windowHeight = paddingTop + titleHeight + (lines.size() * lineHeight) + paddingBottom;

        int windowX = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int windowY = topFieldY + fieldHeight;

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 300);

        gui.fill(windowX, windowY, windowX + fieldWidth, windowY + windowHeight, 0xCC000000);
        ArmorStatsBarRenderer.drawWindowWithTile(gui, ArmorStatsFieldRenderer.TEXTURE,
                windowX, windowY, fieldWidth, windowHeight);

        int textX = windowX + ArmorStatsBarRenderer.EDGE + 4;
        int textY = windowY + paddingTop;

        Component title = Component.translatable("damage_type." + selectedType.getDamageName());
        gui.drawString(mc.font, title,
                windowX + (fieldWidth - mc.font.width(title)) / 2, textY, 0xFFFFFF, false);
        textY += titleHeight;

        for (int i = 0; i < lines.size(); i++) {
            int color = (nonZeroCount >= 2 && i == lines.size() - 1) ? 0xFFD700 : 0xEEEEEE;
            gui.drawString(mc.font, lines.get(i).getString(), textX, textY, color, false);
            textY += lineHeight;
        }

        gui.pose().popPose();
    }
    public static void renderEntityDetails(GuiGraphics gui, InventoryScreen screen,
                                           int bottomFieldY, int fieldWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ResourceLocation entityKey = ArmorStatsHoverHandler.getHoveredEntity();
        if (entityKey == null) return;

        // Собираем эффекты от этой сущности
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            LivingEntity source = EffectSourceManager.getSource(mc.player, inst.getEffect());
            if (source != null) {
                ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(source.getType());
                if (entityKey.equals(key)) effects.add(inst);
            }
        }

        int lineHeight   = mc.font.lineHeight + 2;
        int titleHeight  = mc.font.lineHeight + 4;
        int paddingTop   = 6;
        int paddingBottom = 6;
        int windowHeight = paddingTop + titleHeight + effects.size() * lineHeight + paddingBottom;

        int windowX = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int windowY = bottomFieldY - windowHeight; // окно рисуется ВЫШЕ нижней полоски

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 300);

        gui.fill(windowX, windowY, windowX + fieldWidth, windowY + windowHeight, 0xCC000000);
        ArmorStatsBarRenderer.drawWindowWithTile(gui, ArmorStatsFieldRenderer.TEXTURE,
                windowX, windowY, fieldWidth, windowHeight);

        int textX = windowX + ArmorStatsBarRenderer.EDGE + 4;
        int textY = windowY + paddingTop;

        // Название сущности по центру
        String entityName = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .get(entityKey).getDescription().getString();
        gui.drawString(mc.font, entityName,
                windowX + (fieldWidth - mc.font.width(entityName)) / 2, textY, 0xFFFFFF, false);
        textY += titleHeight;

        // Список эффектов
        for (MobEffectInstance inst : effects) {
            String effectName = inst.getEffect().getDisplayName().getString();

            // Форматируем время
            int totalTicks = inst.getDuration();
            String timeStr;
            if (totalTicks == Integer.MAX_VALUE) {
                timeStr = "\u221E"; // символ бесконечности
            } else {
                int seconds = totalTicks / 20;
                int minutes = seconds / 60;
                seconds = seconds % 60;
                timeStr = minutes > 0 ? minutes + ":" + String.format("%02d", seconds) : seconds + "s";
            }

            // Иконка эффекта (16x16 из sprite-листа) — рисуется перед текстом
            int iconX = textX;
            int iconY = textY - 1;
            TextureAtlasSprite sprite =
                    mc.getMobEffectTextures().get(inst.getEffect());

            if (sprite != null) {
                gui.blit(iconX, iconY, 0, 9, 9, sprite);
            }
            int textOffsetX = textX + 18;
            gui.drawString(mc.font, effectName + "  " + timeStr, textOffsetX, textY, 0xEEEEEE, false);
            textY += lineHeight;
        }

        gui.pose().popPose();
    }
    private static Component line(String key, float value) {
        return Component.literal(
                Component.translatable(key).getString() + ": " + String.format("%.1f", value) + "%");
    }
}