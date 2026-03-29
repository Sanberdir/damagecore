package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
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

    private static Component line(String key, float value) {
        return Component.literal(
                Component.translatable(key).getString() + ": " + String.format("%.1f", value) + "%");
    }
}