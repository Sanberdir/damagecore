package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ArmorStatsTopBar {

    static final List<DamageType> DISPLAY_ORDER = List.of(
            DamageType.SLASHING,
            DamageType.PIERCING,
            DamageType.BLUDGEONING,
            DamageType.FIRE
    );

    private ArmorStatsTopBar() {}

    public static void render(GuiGraphics gui, InventoryScreen screen,
                              int guiLeft, int topFieldY, int drawH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack hoveredArmor = ArmorStatsFieldRenderer.getHoveredArmor();
        boolean compareMode = !hoveredArmor.isEmpty();

        Map<DamageType, Float> baseProtections =
                ArmorStatsCalculator.getPlayerTotalProtectionPercent(mc.player, ItemStack.EMPTY);
        Map<DamageType, Float> totalProtections = compareMode
                ? ArmorStatsCalculator.getPlayerTotalProtectionPercent(mc.player, hoveredArmor)
                : baseProtections;
        Map<DamageType, Float> armorOnlyProtections = compareMode
                ? ArmorStatsCalculator.getArmorOnlyProtectionPercent(mc.player, hoveredArmor)
                : ArmorStatsCalculator.getArmorOnlyProtectionPercent(mc.player, ItemStack.EMPTY);

        List<DamageType> orderedTypes = new ArrayList<>(DISPLAY_ORDER);
        for (DamageType type : totalProtections.keySet()) {
            if (!orderedTypes.contains(type)) orderedTypes.add(type);
        }
        for (DamageType type : baseProtections.keySet()) {
            if (!orderedTypes.contains(type)) orderedTypes.add(type);
        }

        int iconSize = 8;
        float scale  = 2f / 3f;
        float EPS    = 0.001f;
        int yTop     = topFieldY + (drawH - iconSize) / 2;
        int x        = guiLeft + 6;

        for (DamageType type : orderedTypes) {
            float totalPercent = totalProtections.getOrDefault(type, 0f);
            float basePercent  = baseProtections.getOrDefault(type, 0f);
            float diff         = totalPercent - basePercent;
            float armorOnly    = armorOnlyProtections.getOrDefault(type, 0f);

            boolean hasValue = totalPercent > EPS || (compareMode && basePercent > EPS);
            if (!hasValue) continue;

            ResourceLocation icon = new ResourceLocation(
                    "damagecore", "textures/gui/damage_types/" + type.getDamageName() + "_damage.png");
            gui.blit(icon, x, yTop, 0, 0, iconSize, iconSize, iconSize, iconSize);

            int color;
            if (compareMode) {
                if (diff > EPS) {
                    color = 0x7FD6FF;
                } else if (diff < -EPS && totalPercent > EPS) {
                    color = 0xFF8A8A;
                } else if (totalPercent <= EPS && basePercent > EPS) {
                    color = 0xFF8A8A;
                } else {
                    color = 0xEEEEEE;
                }
            } else {
                color = (totalPercent - armorOnly) > EPS ? 0xFFD700 : 0xEEEEEE;
            }

            String text = (int)(totalPercent * 100) + "%";

            gui.pose().pushPose();
            int percentY = yTop + (iconSize / 2) - (int)((mc.font.lineHeight * scale) / 2f) + 1;
            gui.pose().translate(x + iconSize + 1, percentY, 0);
            gui.pose().scale(scale, scale, 1.0f);
            gui.drawString(mc.font, text, 0, 0, color, false);
            gui.pose().popPose();

            int textWidth = (int)(mc.font.width(text) * scale);
            x += iconSize + 1 + textWidth + 4;
        }
    }
}