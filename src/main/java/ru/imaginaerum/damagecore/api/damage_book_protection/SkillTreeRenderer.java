package ru.imaginaerum.damagecore.api.damage_book_protection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.Font;

import java.util.*;

public final class SkillTreeRenderer {
    private SkillTreeRenderer() {}

    public static final int AREA_TEX_X0 = 187;
    public static final int AREA_TEX_Y0 = 8;
    public static final int AREA_TEX_X1 = 460;
    public static final int AREA_TEX_Y1 = 158;
    public static final int AREA_WIDTH = AREA_TEX_X1 - AREA_TEX_X0;
    public static final int AREA_HEIGHT = AREA_TEX_Y1 - AREA_TEX_Y0;

    private static final int PANEL_TEXTURE_U = 179;
    private static final int PANEL_TEXTURE_V = 0;
    public static final int PANEL_DRAW_OFFSET_X_IN_PANEL = AREA_TEX_X0 - PANEL_TEXTURE_U; // 8
    public static final int PANEL_DRAW_OFFSET_Y_IN_PANEL = AREA_TEX_Y0 - PANEL_TEXTURE_V; // 8

    private static final int GAP = 6;
    private static final int SPACING = SkillTreeNode.FRAME_SIZE + GAP;

    private static final Map<String, SkillTreeNode> nodes = new LinkedHashMap<>();
    private static final Map<String, List<SkillTreeNode>> childrenMap = new HashMap<>();

    // Состояние перетаскивания
    private static boolean isDragging = false;
    private static int dragStartX = 0;
    private static int dragStartY = 0;
    private static int dragStartOffsetX = 0;
    private static int dragStartOffsetY = 0;

    // Текущее смещение дерева
    private static int offsetX = 0;
    private static int offsetY = 0;

    private static float scale = 1.0f;           // текущий масштаб
    private static final float MIN_SCALE = 0.5f; // минимальный масштаб
    private static final float MAX_SCALE = 2.0f; // максимальный масштаб
    private static final float ZOOM_STEP = 0.1f; // шаг масштабирования

    // Текущие координаты панели для пересчета позиций
    private static int currentPanelScreenX = 0;
    private static int currentPanelScreenY = 0;

    // ------ INPUT: масштабирование колесиком мыши ------
    public static boolean mouseScrolled(int mouseX, int mouseY, double delta, int panelScreenX, int panelScreenY) {
        System.out.println("!!! mouseScrolled CALLED !!! delta=" + delta);

        float oldScale = scale;
        scale = scale + (delta > 0 ? ZOOM_STEP : -ZOOM_STEP);
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));

        System.out.println("Scale changed: " + oldScale + " -> " + scale);

        // Принудительно пересчитываем позиции
        if (!nodes.isEmpty()) {
            calculateAndUpdatePositions(panelScreenX, panelScreenY);
        }

        return true;
    }

    public static void load(String resourcePath) {
        nodes.clear();
        List<SkillTreeNode> list = SkillTreeLoader.loadFromResource(resourcePath);
        for (SkillTreeNode n : list) nodes.put(n.id, n);
        rebuildChildrenMap();

        // Сбрасываем смещение и масштаб при загрузке
        offsetX = 0;
        offsetY = 0;
        scale = 1.0f;
        isDragging = false;
    }

    private static void rebuildChildrenMap() {
        childrenMap.clear();
        for (SkillTreeNode n : nodes.values()) {
            if (n.parentId != null && !"start".equalsIgnoreCase(n.parentId))
                childrenMap.computeIfAbsent(n.parentId, k -> new ArrayList<>()).add(n);
        }
    }

    // Вычисляет базовые позиции узлов (без смещения)
    private static void calculateAndUpdatePositions(int panelScreenX, int panelScreenY) {
        if (nodes.isEmpty()) return;

        int areaX = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int areaY = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;

        int centerX = areaX + AREA_WIDTH / 2 + offsetX;
        int centerY = areaY + AREA_HEIGHT / 2 + offsetY;

        SkillTreeNode start = nodes.values().stream()
                .filter(n -> n.parentId == null || "start".equalsIgnoreCase(n.parentId))
                .findFirst().orElse(null);

        if (start == null && !nodes.isEmpty())
            start = nodes.values().iterator().next();

        if (start == null) return;

        start.x = centerX - SkillTreeNode.FRAME_SIZE / 2;
        start.y = centerY - SkillTreeNode.FRAME_SIZE / 2;

        Queue<SkillTreeNode> queue = new ArrayDeque<>();
        queue.add(start);

        Set<String> placed = new HashSet<>();
        placed.add(start.id);

        while (!queue.isEmpty()) {
            SkillTreeNode parent = queue.poll();

            for (SkillTreeNode child : childrenMap.getOrDefault(parent.id, Collections.emptyList())) {
                if (placed.contains(child.id)) continue;

                switch (child.side) {
                    case RIGHT -> {
                        child.x = parent.x + SPACING;
                        child.y = parent.y;
                    }
                    case LEFT -> {
                        child.x = parent.x - SPACING;
                        child.y = parent.y;
                    }
                    case TOP -> {
                        child.x = parent.x;
                        child.y = parent.y - SPACING;
                    }
                    case BOTTOM -> {
                        child.x = parent.x;
                        child.y = parent.y + SPACING;
                    }
                    default -> {
                        child.x = parent.x + SPACING;
                        child.y = parent.y;
                    }
                }

                placed.add(child.id);
                queue.add(child);
            }
        }

        for (SkillTreeNode n : nodes.values()) {
            if (!placed.contains(n.id)) {
                n.x = centerX + SPACING;
                n.y = centerY + SPACING;
            }
        }
    }

    public static void render(GuiGraphics gui, InventoryScreen screen,
                              int panelScreenX, int panelScreenY,
                              int mouseX, int mouseY) {

        if (nodes.isEmpty()) return;

        currentPanelScreenX = panelScreenX;
        currentPanelScreenY = panelScreenY;

        if (isDragging) {
            offsetX = dragStartOffsetX + (mouseX - dragStartX);
            offsetY = dragStartOffsetY + (mouseY - dragStartY);
        }

        calculateAndUpdatePositions(panelScreenX, panelScreenY);

        int clipX1 = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int clipY1 = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;
        int clipX2 = clipX1 + AREA_WIDTH;
        int clipY2 = clipY1 + AREA_HEIGHT;

        if (clipX2 > clipX1 && clipY2 > clipY1) {
            gui.enableScissor(clipX1, clipY1, clipX2, clipY2);
        }

        PoseStack pose = gui.pose();
        pose.pushPose();

        int pivotX = clipX1 + AREA_WIDTH / 2;
        int pivotY = clipY1 + AREA_HEIGHT / 2;

        pose.translate(pivotX, pivotY, 0);
        pose.scale(scale, scale, 1f);
        pose.translate(-pivotX, -pivotY, 0);

        // -------- ЛИНИИ --------
        int lineColor = 0xFF000000;

        for (SkillTreeNode child : nodes.values()) {
            if (child.parentId == null || "start".equalsIgnoreCase(child.parentId)) continue;

            SkillTreeNode parent = nodes.get(child.parentId);
            if (parent == null) continue;

            drawThickLine(gui,
                    parent.centerX(),
                    parent.centerY(),
                    child.centerX(),
                    child.centerY(),
                    2,
                    lineColor);
        }
        // -------- НОДЫ --------
        for (SkillTreeNode n : nodes.values()) {
            int frameSize = SkillTreeNode.FRAME_SIZE;
            int padding = SkillTreeNode.FRAME_PADDING;

            gui.fill(n.x, n.y, n.x + frameSize, n.y + frameSize, 0xFF333333);
            gui.fill(n.x + padding, n.y + padding,
                    n.x + frameSize - padding,
                    n.y + frameSize - padding,
                    0xFF777777);

            int itemX = n.x + (frameSize - 16) / 2;
            int itemY = n.y + (frameSize - 16) / 2;

            gui.renderItem(n.itemStack, itemX, itemY);
            gui.renderItemDecorations(Minecraft.getInstance().font, n.itemStack, itemX, itemY);
        }

        pose.popPose();

        if (clipX2 > clipX1 && clipY2 > clipY1) {
            gui.disableScissor();
        }
        // -------- SCALE TEXT --------
        Font font = Minecraft.getInstance().font;
        String scaleText = String.format("%.0f%%", scale * 100);

        int textX = clipX1 + 5;
        int textY = clipY1 + 5;
        int w = font.width(scaleText);

        gui.fill(textX - 2, textY - 2, textX + w + 2, textY + font.lineHeight + 2, 0xAA000000);
        gui.drawString(font, scaleText, textX, textY, 0xFFFFFFFF, true);

        // -------- TOOLTIP --------
        for (SkillTreeNode n : nodes.values()) {
            if (n.containsPoint(mouseX, mouseY, scale)) {
                gui.drawString(font,
                        n.id + (n.locked ? " (locked)" : ""),
                        mouseX + 10,
                        mouseY + 6,
                        0xFFFFFFFF,
                        false);
                break;
            }
        }
    }
    private static void drawThickLine(
            GuiGraphics gui,
            int x1, int y1,
            int x2, int y2,
            int thickness,
            int color
    ) {
        if (y1 == y2) {
            gui.fill(Math.min(x1,x2), y1 - thickness/2,
                    Math.max(x1,x2), y1 + thickness/2 + 1, color);
            return;
        }

        if (x1 == x2) {
            gui.fill(x1 - thickness/2, Math.min(y1,y2),
                    x1 + thickness/2 + 1, Math.max(y1,y2), color);
            return;
        }

        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));

        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int px = Math.round(x1 + t * dx);
            int py = Math.round(y1 + t * dy);

            gui.fill(px - thickness/2, py - thickness/2,
                    px + thickness/2 + 1, py + thickness/2 + 1, color);
        }
    }
    // ------ INPUT: drag & drop всего дерева ------
    public static boolean mousePressed(int mouseX, int mouseY, int button, int panelScreenX, int panelScreenY) {
        int areaX = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int areaY = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;

        // Проверяем, кликнули ли внутри области дерева
        if (mouseX >= areaX && mouseX <= areaX + AREA_WIDTH &&
                mouseY >= areaY && mouseY <= areaY + AREA_HEIGHT) {
            isDragging = true;
            dragStartX = mouseX;
            dragStartY = mouseY;
            dragStartOffsetX = offsetX;
            dragStartOffsetY = offsetY;
            return true;
        }
        return false;
    }

    public static boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (isDragging) {
            isDragging = false;
            return true;
        }
        return false;
    }

    public static boolean mouseDragged(int mouseX, int mouseY, int button, int panelScreenX, int panelScreenY) {
        if (!isDragging || button != 0) return false;

        // Вычисляем дельту перемещения мыши
        int deltaX = mouseX - dragStartX;
        int deltaY = mouseY - dragStartY;

        if (deltaX == 0 && deltaY == 0) return false;

        offsetX = dragStartOffsetX + deltaX;
        offsetY = dragStartOffsetY + deltaY;

        // Пересчитываем позиции при перетаскивании
        calculateAndUpdatePositions(panelScreenX, panelScreenY);
        limitOffset(panelScreenX, panelScreenY);

        return true;
    }

    private static void limitOffset(int panelScreenX, int panelScreenY) {
        if (nodes.isEmpty()) return;

        int areaX = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int areaY = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (SkillTreeNode n : nodes.values()) {
            int nodeScreenX = n.x;
            int nodeScreenY = n.y;
            int frameSize = (int)(SkillTreeNode.FRAME_SIZE * scale);

            minX = Math.min(minX, nodeScreenX);
            maxX = Math.max(maxX, nodeScreenX + frameSize);
            minY = Math.min(minY, nodeScreenY);
            maxY = Math.max(maxY, nodeScreenY + frameSize);
        }

        int padding = 20;

        int minOffsetX = -(minX - areaX - padding);
        int maxOffsetX = areaX + AREA_WIDTH - maxX - padding;
        int minOffsetY = -(minY - areaY - padding);
        int maxOffsetY = areaY + AREA_HEIGHT - maxY - padding;

        if (minOffsetX < maxOffsetX) {
            offsetX = Math.max(minOffsetX, Math.min(maxOffsetX, offsetX));
        } else {
            offsetX = (minOffsetX + maxOffsetX) / 2;
        }

        if (minOffsetY < maxOffsetY) {
            offsetY = Math.max(minOffsetY, Math.min(maxOffsetY, offsetY));
        } else {
            offsetY = (minOffsetY + maxOffsetY) / 2;
        }
    }

    // Метод для сброса позиции дерева и масштаба
    public static void resetTreePosition() {
        offsetX = 0;
        offsetY = 0;
        scale = 1.0f;
    }

    public static boolean isEmpty() {
        return nodes.isEmpty();
    }
    // Геттеры для масштаба и смещения
    public static float getScale() {
        return scale;
    }
    public static int getOffsetX() {
        return offsetX;
    }
    public static int getOffsetY() {
        return offsetY;
    }
}