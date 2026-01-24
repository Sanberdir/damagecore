package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;

public final class DamageBookPositionHelper {
    private DamageBookPositionHelper() {}

    public static void updateInventoryPosition(InventoryScreen screen, boolean damageBookVisible, int tabWidth) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;

        if (screen.getRecipeBookComponent().isVisible()) {
            return;
        }

        int totalWidth = accessor.damagecore$getImageWidth();
        if (damageBookVisible) totalWidth += tabWidth;

        int newLeftPos = (screen.width - totalWidth) / 2 + 2;

        if (damageBookVisible) {
            accessor.setLeftPos(newLeftPos + tabWidth);
        } else {
            accessor.setLeftPos(newLeftPos);
        }
    }
}
