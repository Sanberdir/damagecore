package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class SkillTreeNode {
    public enum Side { START, LEFT, RIGHT, TOP, BOTTOM }
    public boolean optionsVisible = false;
    public final String id;
    public String displayId;
    public ItemStack itemStack;
    public boolean locked;
    public boolean learned = false;

    // Изменяем с одного родителя на список
    public final List<String> parentIds; // теперь список
    public final Side side;

    public long xpFailFlashUntil = 0L;

    public int x;
    public int y;

    public boolean hasGridPos = false;
    public int gridX = 0;
    public int gridY = 0;

    public static final int FRAME_SIZE = 24;
    public static final int FRAME_PADDING = 2;

    public final List<ItemStack> options = new ArrayList<>();
    public int selectedOption = -1;

    // Конструктор для обратной совместимости (один родитель)
    public SkillTreeNode(String id, ItemStack itemStack, boolean locked, String parentId, Side side) {
        this.id = id;
        this.displayId = id;
        this.itemStack = itemStack;
        this.locked = locked;
        this.parentIds = parentId != null
                ? new ArrayList<>(Collections.singletonList(parentId))
                : new ArrayList<>();
        this.side = side;
        this.x = 0;
        this.y = 0;
        this.hasGridPos = false;
        this.gridX = 0;
        this.gridY = 0;
        this.learned = false;
    }

    // Новый конструктор для нескольких родителей
    public SkillTreeNode(String id, ItemStack itemStack, boolean locked, List<String> parentIds, Side side) {
        this.id = id;
        this.displayId = id;
        this.itemStack = itemStack;
        this.locked = locked;
        this.parentIds = parentIds != null ? new ArrayList<>(parentIds) : new ArrayList<>();
        this.side = side;
        this.x = 0;
        this.y = 0;
        this.hasGridPos = false;
        this.gridX = 0;
        this.gridY = 0;
        this.learned = false;
    }

    public boolean isRoot() {
        return parentIds.isEmpty() || (parentIds.size() == 1 && "start".equalsIgnoreCase(parentIds.get(0)));
    }
    public boolean canLearn(Set<String> learnedNodes) {
        if (learned || locked) return false;
        if (isRoot()) return true;

        for (String parentId : parentIds) {
            if (parentId == null || "start".equalsIgnoreCase(parentId)) continue;
            if (!learnedNodes.contains(parentId)) {
                return false; // Хотя бы один родитель не изучен
            }
        }
        return true; // Все родители изучены
    }
    public void setGridPos(int gx, int gy) {
        this.hasGridPos = true;
        this.gridX = gx;
        this.gridY = gy;
    }

    public static class Variant {
        public final String displayId;
        public final ItemStack stack;

        public Variant(String displayId, ItemStack stack) {
            this.displayId = displayId;
            this.stack = stack;
        }

        public ItemStack getItemStack() {
            return stack;
        }

        public String getDisplayId() {
            return displayId;
        }
    }

    public final List<Variant> variants = new ArrayList<>();

    public void applyVariant(int index) {
        if (variants == null || index < 0 || index >= variants.size()) return;
        Variant v = variants.get(index);
        if (v == null) return;

        this.itemStack = v.stack;
        this.displayId = v.displayId;
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