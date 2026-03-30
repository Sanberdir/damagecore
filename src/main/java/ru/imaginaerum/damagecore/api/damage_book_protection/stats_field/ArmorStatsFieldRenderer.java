package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;

public final class ArmorStatsFieldRenderer {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation("damagecore", "textures/gui/container/creative_inventory/armor_statistic_field.png");

    private static ItemStack hoveredArmor = ItemStack.EMPTY;

    private ArmorStatsFieldRenderer() {}

    public static void setHoveredArmor(ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem) {
            hoveredArmor = stack;
        } else {
            hoveredArmor = ItemStack.EMPTY;
        }
    }

    public static ItemStack getHoveredArmor() {
        return hoveredArmor;
    }

    public static boolean isButtonHovered(double mouseX, double mouseY, InventoryScreen screen) {
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop  = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int x = guiLeft + 62;
        int y = guiTop  + 10;
        return mouseX >= x && mouseX < x + 10 && mouseY >= y && mouseY < y + 9;
    }

    public static void render(GuiGraphics gui, InventoryScreen screen) {
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int drawW      = imageWidth;
        int drawH      = 20;
        int topFieldY  = guiTop - drawH + 1;
        int bottomFieldY = guiTop + 165;

        // Рамки полосок
        ArmorStatsBarRenderer.drawStretchBar(gui, TEXTURE, guiLeft, topFieldY,    drawW, drawH);
        ArmorStatsBarRenderer.drawStretchBar(gui, TEXTURE, guiLeft, bottomFieldY, drawW, drawH);

        // Верхняя полоска — иконки типов урона
        ArmorStatsTopBar.render(gui, screen, guiLeft, topFieldY, drawH);

        // Нижняя полоска — источники защиты
        ArmorStatsBottomBar.render(gui, screen, bottomFieldY, drawH);

        // Ховер-логика
        ArmorStatsHoverHandler.handle(screen);
        if (ArmorStatsHoverHandler.isShowFoodDetails()) {
            gui.pose().pushPose();
            gui.pose().translate(0, 0, 400);  // выше чем иконки (300) и стандартные tooltips
            ArmorStatsDetailsWindow.renderFoodDetails(gui, screen, bottomFieldY, drawW);
            gui.pose().popPose();
        }

        if (ArmorStatsHoverHandler.isShowDetails()) {
            gui.pose().pushPose();
            gui.pose().translate(0, 0, 400);
            ArmorStatsDetailsWindow.render(gui, screen, topFieldY, drawW, drawH);
            gui.pose().popPose();
        }

        if (ArmorStatsHoverHandler.isShowEntityDetails()) {
            gui.pose().pushPose();
            gui.pose().translate(0, 0, 400);
            ArmorStatsDetailsWindow.renderEntityDetails(gui, screen, bottomFieldY, drawW);
            gui.pose().popPose();
        }
    }
}