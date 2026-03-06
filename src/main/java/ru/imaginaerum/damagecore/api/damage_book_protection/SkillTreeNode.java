package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class SkillTreeNode {
    public enum Side { START, LEFT, RIGHT, TOP, BOTTOM }
    public boolean optionsVisible = false;
    public final String id;
    public String displayId; // отображаемый id (используется для тултипов). По умолчанию равен id.
    public ItemStack itemStack; // отображаемый предмет (может быть Items.AIR) — теперь изменяемый
    public boolean locked;
    public boolean learned = false; // новое поле: изучена или нет
    public final String parentId; // "start" или id родителя
    public final Side side;
    public long xpFailFlashUntil = 0L;
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
        this.displayId = id;
        this.itemStack = itemStack;
        this.locked = locked;
        this.parentId = parentId;
        this.side = side;
        this.x = 0;
        this.y = 0;
        this.hasGridPos = false;
        this.gridX = 0;
        this.gridY = 0;
        this.learned = false; // явно
    }

    // Удобный сеттер для grid-координат (используется Loader если в JSON указаны gridX/gridY)
    public void setGridPos(int gx, int gy) {
        this.hasGridPos = true;
        this.gridX = gx;
        this.gridY = gy;
    }

    public static class Variant {
        public final String displayId;  // текст/название варианта
        public final ItemStack stack;   // отображаемый предмет

        public Variant(String displayId, ItemStack stack) {
            this.displayId = displayId;
            this.stack = stack;
        }

        // геттеры для удобства
        public ItemStack getItemStack() {
            return stack;
        }

        public String getDisplayId() {
            return displayId;
        }
    }

    public final List<Variant> variants = new ArrayList<>();

    /**
     * Применить вариант (при выборе опции).
     * Меняет selectedOption, обновляет отображаемый itemStack и displayId (для тултипов).
     * NOTE: не меняет "id" — он остаётся ключом ноды в дереве.
     */
    public void applyVariant(int index) {
        if (variants == null || index < 0 || index >= variants.size()) return;
        Variant v = variants.get(index);
        if (v == null) return;

        this.itemStack = v.stack;   // отображаемый предмет
        this.displayId = v.displayId;      // текст/ID для тултипа
        this.selectedOption = index;
    }

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