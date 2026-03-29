package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ArmorStatsHoverHandler {

    private static DamageType selectedDamageType = null;
    private static boolean showDetails = false;
    private static long hoverStartTime = 0;
    private static final long HOVER_DELAY_MS = 100;

    private ArmorStatsHoverHandler() {}

    public static boolean isShowDetails()           { return showDetails; }
    public static DamageType getSelectedDamageType() { return selectedDamageType; }

    public static void handle(InventoryScreen screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen == null) return;

        double mouseX = mc.mouseHandler.xpos()
                * (double) mc.getWindow().getGuiScaledWidth()
                / (double) mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos()
                * (double) mc.getWindow().getGuiScaledHeight()
                / (double) mc.getWindow().getScreenHeight();

        DamageType hoveredType = getDamageTypeAtPosition(screen, mouseX, mouseY);

        if (hoveredType != null) {
            if (!showDetails || selectedDamageType != hoveredType) {
                if (hoverStartTime == 0) {
                    hoverStartTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - hoverStartTime >= HOVER_DELAY_MS) {
                    selectedDamageType = hoveredType;
                    showDetails = true;
                }
            } else {
                hoverStartTime = 0;
            }
        } else {
            if (showDetails && !isMouseOverDetailsWindow(screen, mouseX, mouseY)) {
                showDetails = false;
                selectedDamageType = null;
            }
            hoverStartTime = 0;
        }
    }

    public static boolean isMouseOverDetailsWindow(InventoryScreen screen, double mouseX, double mouseY) {
        if (!showDetails || selectedDamageType == null) return false;

        Minecraft mc = Minecraft.getInstance();
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int topFieldY    = guiTop - 20 + 1;
        int windowX      = guiLeft;
        int windowY      = topFieldY + 20;
        int windowWidth  = imageWidth;

        int lineHeight   = mc.font.lineHeight + 2;
        int titleHeight  = mc.font.lineHeight + 4;
        int windowHeight = 6 + titleHeight + (5 * lineHeight) + 6;

        return mouseX >= windowX && mouseX <= windowX + windowWidth
                && mouseY >= windowY && mouseY <= windowY + windowHeight;
    }

    private static DamageType getDamageTypeAtPosition(InventoryScreen screen, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;

        int guiLeft  = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop   = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int drawH    = 20;
        int topFieldY = guiTop - drawH + 1;
        int yTop     = topFieldY + (drawH - 8) / 2;
        int x        = guiLeft + 6;

        float scale = 2f / 3f;

        Map<DamageType, Float> totalProtections =
                ArmorStatsCalculator.getPlayerTotalProtectionPercent(mc.player);

        List<DamageType> orderedTypes = new ArrayList<>(ArmorStatsTopBar.DISPLAY_ORDER);
        for (DamageType type : totalProtections.keySet()) {
            if (!orderedTypes.contains(type)) orderedTypes.add(type);
        }

        for (DamageType type : orderedTypes) {
            float totalPercent = totalProtections.getOrDefault(type, 0f);
            if (totalPercent <= 0) continue;

            int iconSize  = 8;
            int textWidth = (int)(mc.font.width((int)(totalPercent * 100) + "%") * scale);
            int endX      = x + iconSize + 1 + textWidth + 4;

            if (mouseX >= x && mouseX <= endX && mouseY >= yTop && mouseY <= yTop + iconSize) {
                return type;
            }
            x = endX;
        }
        return null;
    }
}