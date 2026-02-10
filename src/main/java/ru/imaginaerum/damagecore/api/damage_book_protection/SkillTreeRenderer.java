package ru.imaginaerum.damagecore.api.damage_book_protection;

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
    private static final int PANEL_DRAW_OFFSET_X_IN_PANEL = AREA_TEX_X0 - PANEL_TEXTURE_U; // 8
    private static final int PANEL_DRAW_OFFSET_Y_IN_PANEL = AREA_TEX_Y0 - PANEL_TEXTURE_V; // 8

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

    public static void load(String resourcePath) {
        nodes.clear();
        List<SkillTreeNode> list = SkillTreeLoader.loadFromResource(resourcePath);
        for (SkillTreeNode n : list) nodes.put(n.id, n);
        rebuildChildrenMap();

        // Сбрасываем смещение при загрузке
        offsetX = 0;
        offsetY = 0;
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
        int centerX = areaX + AREA_WIDTH / 2 - SkillTreeNode.FRAME_SIZE / 2;
        int centerY = areaY + AREA_HEIGHT / 2 - SkillTreeNode.FRAME_SIZE / 2;

        SkillTreeNode start = nodes.values().stream()
                .filter(n -> n.parentId == null || "start".equalsIgnoreCase(n.parentId))
                .findFirst().orElse(null);
        if (start == null && !nodes.isEmpty()) start = nodes.values().iterator().next();

        if (start != null) {
            // Стартовый узел в центре с учетом смещения
            start.x = centerX + offsetX;
            start.y = centerY + offsetY;

            Queue<SkillTreeNode> queue = new ArrayDeque<>();
            queue.add(start);
            Set<String> placed = new HashSet<>();
            placed.add(start.id);

            while (!queue.isEmpty()) {
                SkillTreeNode parent = queue.poll();

                for (SkillTreeNode child : childrenMap.getOrDefault(parent.id, Collections.emptyList())) {
                    if (!placed.contains(child.id)) {
                        // Располагаем детей относительно родителя
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
            }

            // Оставшиеся узлы
            for (SkillTreeNode n : nodes.values()) {
                if (!placed.contains(n.id)) {
                    n.x = centerX + offsetX + SPACING;
                    n.y = centerY + offsetY;
                }
            }

            // Ограничиваем все узлы областью
            for (SkillTreeNode n : nodes.values()) {
                clampNodeToArea(n, areaX, areaY);
            }
        }
    }

    private static void clampNodeToArea(SkillTreeNode n, int areaX, int areaY) {
        int maxX = areaX + AREA_WIDTH - SkillTreeNode.FRAME_SIZE;
        int maxY = areaY + AREA_HEIGHT - SkillTreeNode.FRAME_SIZE;

        if (n.x < areaX) n.x = areaX;
        if (n.y < areaY) n.y = areaY;
        if (n.x > maxX) n.x = maxX;
        if (n.y > maxY) n.y = maxY;
    }

    public static void render(GuiGraphics gui, InventoryScreen screen, int panelScreenX, int panelScreenY, int mouseX, int mouseY) {
        // Если дерево пустое, ничего не рисуем
        if (nodes.isEmpty()) return;

        // Если идёт перетаскивание — обновляем смещение дерева в реальном времени
        if (isDragging) {
            // Рассчитываем дельту относительно начала перетаскивания (используем координаты мыши, переданные в render)
            int deltaX = mouseX - dragStartX;
            int deltaY = mouseY - dragStartY;

            offsetX = dragStartOffsetX + deltaX;
            offsetY = dragStartOffsetY + deltaY;

            // Ограничиваем смещение, чтобы дерево не выходило за границы панели
            limitOffset(panelScreenX, panelScreenY);
        }

        // Обновляем позиции узлов с учётом текущего offset
        calculateAndUpdatePositions(panelScreenX, panelScreenY);

        // Сначала рисуем линии (связи между узлами)
        int lineColor = 0xFF000000;
        for (SkillTreeNode child : nodes.values()) {
            if (child.parentId == null || "start".equalsIgnoreCase(child.parentId)) continue;
            SkillTreeNode parent = nodes.get(child.parentId);
            if (parent == null) continue;
            drawThickLine(gui, parent.centerX(), parent.centerY(), child.centerX(), child.centerY(), 2, lineColor);
        }

        // Затем рисуем ноды
        for (SkillTreeNode n : nodes.values()) {
            // Рамка узла
            gui.fill(n.x, n.y, n.x + SkillTreeNode.FRAME_SIZE, n.y + SkillTreeNode.FRAME_SIZE, 0xFF333333);
            gui.fill(n.x + SkillTreeNode.FRAME_PADDING, n.y + SkillTreeNode.FRAME_PADDING,
                    n.x + SkillTreeNode.FRAME_SIZE - SkillTreeNode.FRAME_PADDING,
                    n.y + SkillTreeNode.FRAME_SIZE - SkillTreeNode.FRAME_PADDING, 0xFF777777);

            // Предмет внутри узла
            int itemX = n.x + (SkillTreeNode.FRAME_SIZE - 16) / 2;
            int itemY = n.y + (SkillTreeNode.FRAME_SIZE - 16) / 2;
            gui.renderItem(n.itemStack, itemX, itemY);
            gui.renderItemDecorations(Minecraft.getInstance().font, n.itemStack, itemX, itemY);
        }

        // Подсказка при наведении
        for (SkillTreeNode n : nodes.values()) {
            if (n.containsPoint(mouseX, mouseY)) {
                gui.drawString(Minecraft.getInstance().font, n.id + (n.locked ? " (locked)" : ""),
                        mouseX + 10, mouseY + 6, 0xFFFFFFFF, false);
                break;
            }
        }
    }


    private static void drawThickLine(GuiGraphics gui, int x1, int y1, int x2, int y2, int thickness, int color) {
        if (y1 == y2) {
            gui.fill(Math.min(x1,x2), y1-thickness/2, Math.max(x1,x2), y1+thickness/2, color);
            return;
        }
        if (x1 == x2) {
            gui.fill(x1-thickness/2, Math.min(y1,y2), x1+thickness/2, Math.max(y1,y2), color);
            return;
        }
        int dx = x2 - x1, dy = y2 - y1, steps = Math.max(Math.abs(dx), Math.abs(dy));
        for (int i=0;i<=steps;i++) {
            float t = i/(float)Math.max(1,steps);
            int px = Math.round(x1+t*dx);
            int py = Math.round(y1+t*dy);
            gui.fill(px-thickness/2, py-thickness/2, px+thickness/2+1, py+thickness/2+1, color);
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

        // Обновляем смещение дерева
        offsetX = dragStartOffsetX + deltaX;
        offsetY = dragStartOffsetY + deltaY;

        // Ограничиваем смещение, чтобы дерево не выходило за границы
        limitOffset(panelScreenX, panelScreenY);

        return true;
    }

    private static void limitOffset(int panelScreenX, int panelScreenY) {
        int areaX = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int areaY = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;

        // Находим границы дерева
        int minNodeX = Integer.MAX_VALUE;
        int maxNodeX = Integer.MIN_VALUE;
        int minNodeY = Integer.MAX_VALUE;
        int maxNodeY = Integer.MIN_VALUE;

        // Временный расчет позиций для определения границ
        SkillTreeNode start = nodes.values().stream()
                .filter(n -> n.parentId == null || "start".equalsIgnoreCase(n.parentId))
                .findFirst().orElse(null);

        if (start != null) {
            // Простой расчет границ (предполагаем, что дерево не больше области)
            // На практике здесь нужно было бы вычислить фактические границы дерева
            // Но для простоты ограничим смещение половиной размера области
            int maxAllowedOffset = AREA_WIDTH / 3;

            if (offsetX > maxAllowedOffset) offsetX = maxAllowedOffset;
            if (offsetX < -maxAllowedOffset) offsetX = -maxAllowedOffset;
            if (offsetY > maxAllowedOffset) offsetY = maxAllowedOffset;
            if (offsetY < -maxAllowedOffset) offsetY = -maxAllowedOffset;
        }
    }

    public static int[] panelPositionFromRenderParams(int renderX, int renderY) {
        return new int[]{ renderX + 2, renderY };
    }

    // Метод для сброса позиции дерева
    public static void resetTreePosition() {
        offsetX = 0;
        offsetY = 0;
    }
    public static boolean isEmpty() {
        return nodes.isEmpty();
    }
}