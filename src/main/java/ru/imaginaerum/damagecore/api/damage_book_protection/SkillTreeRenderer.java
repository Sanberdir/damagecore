package ru.imaginaerum.damagecore.api.damage_book_protection;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import java.util.*;

public final class SkillTreeRenderer {
    private SkillTreeRenderer() {}

    private static final Map<String, SkillTreeNode> nodes = new LinkedHashMap<>();
    private static final Map<String, List<SkillTreeNode>> childrenMap = new HashMap<>();

    // Размер панели дерева
    public static final int AREA_WIDTH = 273;
    public static final int AREA_HEIGHT = 148;
    private static final int PANEL_OFFSET_X = 8;
    private static final int PANEL_OFFSET_Y = 8;

    // Смещение дерева для перетаскивания
    private static int offsetX = 0;
    private static int offsetY = 0;

    // Состояние перетаскивания
    private static boolean dragging = false;
    private static int dragStartX = 0;
    private static int dragStartY = 0;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;

    // --- ЗАГРУЗКА ---
    public static void load(String resourcePath) {
        nodes.clear();
        List<SkillTreeNode> list = SkillTreeLoader.loadFromResource(resourcePath);
        for (SkillTreeNode n : list) nodes.put(n.id, n);

        rebuildChildrenMap();
        resetTreePosition();
    }

    private static void rebuildChildrenMap() {
        childrenMap.clear();
        for (SkillTreeNode n : nodes.values()) {
            if (n.parentId != null && !"start".equalsIgnoreCase(n.parentId))
                childrenMap.computeIfAbsent(n.parentId, k -> new ArrayList<>()).add(n);
        }
    }

    public static void resetTreePosition() {
        offsetX = 0;
        offsetY = 0;
        dragging = false;
    }

    // --- РЕНДЕР ---
    public static void render(GuiGraphics gui, InventoryScreen screen, int panelX, int panelY, int mouseX, int mouseY) {
        if (nodes.isEmpty()) return;

        int areaX = panelX + PANEL_OFFSET_X;
        int areaY = panelY + PANEL_OFFSET_Y;

        // Обновляем позиции узлов с учётом смещения
        calculateNodePositions(areaX, areaY);

        // SCISSOR для обрезки за пределами панели
        Window win = Minecraft.getInstance().getWindow();
        double scale = win.getGuiScale();
        RenderSystem.enableScissor(
                (int)(areaX*scale),
                (int)(win.getHeight() - (areaY + AREA_HEIGHT)*scale),
                (int)(AREA_WIDTH*scale),
                (int)(AREA_HEIGHT*scale)
        );

        renderEdges(gui, areaX, areaY);
        renderNodes(gui, areaX, areaY, mouseX, mouseY);

        RenderSystem.disableScissor();
    }

    private static void calculateNodePositions(int areaX, int areaY) {
        if (nodes.isEmpty()) return;

        SkillTreeNode start = nodes.values().stream()
                .filter(n -> n.parentId == null || "start".equalsIgnoreCase(n.parentId))
                .findFirst().orElse(null);

        if (start == null) return;

        // стартовый узел в центре панели с учётом смещения
        start.x = AREA_WIDTH/2 - SkillTreeNode.FRAME_SIZE/2 + offsetX;
        start.y = AREA_HEIGHT/2 - SkillTreeNode.FRAME_SIZE/2 + offsetY;

        Queue<SkillTreeNode> queue = new ArrayDeque<>();
        Set<String> placed = new HashSet<>();
        queue.add(start);
        placed.add(start.id);

        int spacing = SkillTreeNode.FRAME_SIZE + 6;

        while (!queue.isEmpty()) {
            SkillTreeNode parent = queue.poll();

            for (SkillTreeNode child : childrenMap.getOrDefault(parent.id, Collections.emptyList())) {
                if (placed.contains(child.id)) continue;

                switch (child.side) {
                    case RIGHT -> { child.x = parent.x + spacing; child.y = parent.y; }
                    case LEFT -> { child.x = parent.x - spacing; child.y = parent.y; }
                    case TOP -> { child.x = parent.x; child.y = parent.y - spacing; }
                    case BOTTOM -> { child.x = parent.x; child.y = parent.y + spacing; }
                    default -> { child.x = parent.x + spacing; child.y = parent.y; }
                }

                placed.add(child.id);
                queue.add(child);
            }
        }
    }

    private static void renderEdges(GuiGraphics gui, int areaX, int areaY) {
        int color = 0xFFAAAAAA;
        for (SkillTreeNode child : nodes.values()) {
            if (child.parentId == null || "start".equalsIgnoreCase(child.parentId)) continue;
            SkillTreeNode parent = nodes.get(child.parentId);
            if (parent == null) continue;
            drawLine(gui, parent.centerX(), parent.centerY(), child.centerX(), child.centerY(), color);
        }
    }

    private static void renderNodes(GuiGraphics gui, int areaX, int areaY, int mouseX, int mouseY) {
        for (SkillTreeNode n : nodes.values()) {
            gui.fill(areaX + n.x, areaY + n.y, areaX + n.x + SkillTreeNode.FRAME_SIZE,
                    areaY + n.y + SkillTreeNode.FRAME_SIZE, 0xFF333333);
            gui.fill(areaX + n.x + SkillTreeNode.FRAME_PADDING, areaY + n.y + SkillTreeNode.FRAME_PADDING,
                    areaX + n.x + SkillTreeNode.FRAME_SIZE - SkillTreeNode.FRAME_PADDING,
                    areaY + n.y + SkillTreeNode.FRAME_SIZE - SkillTreeNode.FRAME_PADDING, 0xFF777777);

            gui.renderItem(n.itemStack, areaX + n.x + (SkillTreeNode.FRAME_SIZE-16)/2,
                    areaY + n.y + (SkillTreeNode.FRAME_SIZE-16)/2);
        }

        // подсказка
        for (SkillTreeNode n : nodes.values()) {
            if (n.containsPoint(mouseX - areaX, mouseY - areaY)) {
                gui.drawString(Minecraft.getInstance().font, n.id + (n.locked ? " (locked)" : ""),
                        mouseX + 10, mouseY + 6, 0xFFFFFFFF, false);
                break;
            }
        }
    }

    private static void drawLine(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        for (int i=0;i<=steps;i++) {
            int px = x1 + dx*i/steps;
            int py = y1 + dy*i/steps;
            gui.fill(px, py, px+1, py+1, color);
        }
    }

    // --- INPUT: drag & drop ---
    public static boolean mousePressed(int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        dragging = true;
        dragStartX = mouseX;
        dragStartY = mouseY;
        dragOffsetX = offsetX;
        dragOffsetY = offsetY;
        return true;
    }

    public static boolean mouseDragged(int mouseX, int mouseY, int button) {
        if (!dragging || button != 0) return false;
        offsetX = dragOffsetX + (mouseX - dragStartX);
        offsetY = dragOffsetY + (mouseY - dragStartY);
        return true;
    }

    public static boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (!dragging) return false;
        dragging = false;
        return true;
    }

    public static boolean isEmpty() { return nodes.isEmpty(); }
}
