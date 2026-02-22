package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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

    // Координаты на grid (целые)
    public boolean hasGridPos = false;
    public int gridX = 0;
    public int gridY = 0;

    public static final int FRAME_SIZE = 24; // размер квадрата рамки (в пикселях)
    public static final int FRAME_PADDING = 2; // внутренняя рамка/отступ при рисовании

    // Новое: варианты для ноды (может быть пусто)
    public final List<ItemStack> options = new ArrayList<>();

    // Индекс выбранного варианта (-1 = ничего не выбрано)
    public int selectedOption = -1;

    public SkillTreeNode(String id, ItemStack itemStack, boolean locked, String parentId, Side side) {
        this.id = id;
        this.itemStack = itemStack;
        this.locked = locked;
        this.parentId = parentId;
        this.side = side;
        this.x = 0;
        this.y = 0;
        this.hasGridPos = false;
        this.gridX = 0;
        this.gridY = 0;
    }

    // Удобный сеттер для grid-координат (используется Loader если в JSON указаны gridX/gridY)
    public void setGridPos(int gx, int gy) {
        this.hasGridPos = true;
        this.gridX = gx;
        this.gridY = gy;
    }
    public static class Variant {
        public final String id;
        public final ItemStack stack;

        public Variant(String id, ItemStack stack) {
            this.id = id;
            this.stack = stack;
        }
    }

    public final List<Variant> variants = new ArrayList<>();
    public int centerX() {
        return x + FRAME_SIZE / 2;
    }

    public int centerY() {
        return y + FRAME_SIZE / 2;
    }
    public boolean containsPoint(int px, int py) {
        return px >= x && px < x + FRAME_SIZE
                && py >= y && py < y + FRAME_SIZE;
    }
}