package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.world.item.ItemStack;

public final class SkillTreeNode {
    public enum Side { START, LEFT, RIGHT, TOP, BOTTOM }

    public final String id;
    public final ItemStack itemStack; // отображаемый предмет (может быть Items.AIR)
    public final boolean locked;
    public final String parentId; // "start" или id родителя
    public final Side side;

    // Экранные координаты (верхний левый угол рамки узла)
    public int x;
    public int y;

    public static final int FRAME_SIZE = 24; // размер квадрата рамки (в пикселях)
    public static final int FRAME_PADDING = 2; // внутренняя рамка/отступ при рисовании

    public SkillTreeNode(String id, ItemStack itemStack, boolean locked, String parentId, Side side) {
        this.id = id;
        this.itemStack = itemStack;
        this.locked = locked;
        this.parentId = parentId;
        this.side = side;
        this.x = 0;
        this.y = 0;
    }

    public int centerX() {
        return x + FRAME_SIZE / 2;
    }

    public int centerY() {
        return y + FRAME_SIZE / 2;
    }

    public boolean containsPoint(int px, int py, float scale) {
        int size = (int)(FRAME_SIZE * scale);
        return px >= x && px < x + size && py >= y && py < y + size;
    }

}
