package ru.imaginaerum.damagecore.api.damage_book_protection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.Font;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    // spacing used for old relative layout (kept for compatibility)
    private static final int SPACING = SkillTreeNode.FRAME_SIZE + GAP;

    // grid step: FRAME_SIZE + 1 pixel gap => nodes не слипнутся и не пересекутся
    private static final int GRID_STEP = SkillTreeNode.FRAME_SIZE + 1;

    // Карта деревьев: ключ - ID вкладки, значение - дерево
    private static final Map<Integer, SkillTreeData> trees = new ConcurrentHashMap<>();

    // Карта для соответствия имени файла и ID вкладки
    private static final Map<String, Integer> fileNameToTabId = new HashMap<>();

    // Список имен файлов в алфавитном порядке
    private static List<String> sortedFileNames = new ArrayList<>();

    // Текущее активное дерево (по ID вкладки)
    private static int activeTreeId = 0;

    // Внутренний класс для хранения состояния дерева
    private static class SkillTreeData {
        final Map<String, SkillTreeNode> nodes = new LinkedHashMap<>();
        final Map<String, List<SkillTreeNode>> childrenMap = new HashMap<>();

        // Состояние перетаскивания для каждого дерева
        boolean isDragging = false;
        int dragStartX = 0;
        int dragStartY = 0;
        int dragStartOffsetX = 0;
        int dragStartOffsetY = 0;

        // Текущее смещение дерева (пан/приближение)
        int offsetX = 0;
        int offsetY = 0;

        float scale = 1.0f;

        // Имя дерева (имя файла)
        String fileName;
        // Отображаемое имя (без расширения)
        String displayName;

        SkillTreeData(String fileName, String displayName) {
            this.fileName = fileName;
            this.displayName = displayName;
        }
    }

    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2.0f;
    private static final float ZOOM_STEP = 0.1f;

    // Текущие координаты панели для пересчета позиций
    private static int currentPanelScreenX = 0;
    private static int currentPanelScreenY = 0;

    // Получить текущее активное дерево
    private static SkillTreeData getCurrentTree() {
        SkillTreeData tree = trees.get(activeTreeId);
        if (tree == null) {
            // Если дерево не найдено, создаем пустое
            tree = new SkillTreeData("empty", "Empty Tree " + (activeTreeId + 1));
            trees.put(activeTreeId, tree);
        }
        return tree;
    }

    // Установить активное дерево по ID вкладки
    public static void setActiveTree(int tabId) {
        if (tabId >= 0 && tabId < trees.size()) {
            activeTreeId = tabId;
        }
    }

    // Загрузить все деревья из папки assets/damagecore/skill_tree/
    public static void loadAllTrees(String folderPath) {
        trees.clear();
        fileNameToTabId.clear();
        sortedFileNames.clear();

        try {
            String path = folderPath;
            if (path.startsWith("/")) path = path.substring(1);
            if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            final String finalPath = path;

            var resourceManager = Minecraft.getInstance().getResourceManager();

            var resources = resourceManager.listResources(finalPath,
                    location -> {
                        String fullPath = location.getPath();
                        return fullPath.startsWith(finalPath + "/") && fullPath.endsWith(".json");
                    });

            sortedFileNames = new ArrayList<>();
            for (var location : resources.keySet()) {
                String fullPath = location.getPath();
                String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
                sortedFileNames.add(fileName);
                System.out.println("Found skill tree file: " + fileName);
            }

            Collections.sort(sortedFileNames, String.CASE_INSENSITIVE_ORDER);

            System.out.println("Total files found: " + sortedFileNames.size());
            if (!sortedFileNames.isEmpty()) {
                System.out.println("Files in alphabetical order: " + String.join(", ", sortedFileNames));
            }

            int tabId = 0;
            for (String fileName : sortedFileNames) {
                String resourcePath = finalPath + "/" + fileName;
                String displayName = fileName.replace(".json", "");

                List<SkillTreeNode> list = SkillTreeLoader.loadFromResource(resourcePath);
                if (!list.isEmpty()) {
                    SkillTreeData tree = new SkillTreeData(fileName, displayName);

                    for (SkillTreeNode n : list) {
                        tree.nodes.put(n.id, n);
                    }
                    rebuildChildrenMap(tree);

                    // Сбрасываем состояние
                    tree.offsetX = 0;
                    tree.offsetY = 0;
                    tree.scale = 1.0f;
                    tree.isDragging = false;

                    trees.put(tabId, tree);
                    fileNameToTabId.put(fileName, tabId);
                    System.out.println("Loaded skill tree: " + fileName + " as tab ID " + tabId);
                    tabId++;
                } else {
                    System.out.println("Failed to load skill tree from: " + resourcePath + " (empty or invalid)");
                }
            }

            System.out.println("Successfully loaded " + trees.size() + " skill trees");

        } catch (Exception e) {
            System.err.println("Error loading skill trees:");
            e.printStackTrace();
        }
    }

    private static void loadTreesFallback(String folderPath) {
        try {
            for (int i = 0; i <= 23; i++) {
                String resourcePath = folderPath + "/skill_tree_" + i + ".json";
                List<SkillTreeNode> list = SkillTreeLoader.loadFromResource(resourcePath);

                if (!list.isEmpty()) {
                    String fileName = "skill_tree_" + i + ".json";
                    String displayName = "Tree " + i;

                    SkillTreeData tree = new SkillTreeData(fileName, displayName);

                    for (SkillTreeNode n : list) {
                        tree.nodes.put(n.id, n);
                    }
                    rebuildChildrenMap(tree);

                    tree.offsetX = 0;
                    tree.offsetY = 0;
                    tree.scale = 1.0f;
                    tree.isDragging = false;

                    int tabId = trees.size();
                    trees.put(tabId, tree);
                    fileNameToTabId.put(fileName, tabId);
                    sortedFileNames.add(fileName);
                    System.out.println("Fallback: Loaded " + fileName + " as tab ID " + tabId);
                }
            }

            Collections.sort(sortedFileNames, String.CASE_INSENSITIVE_ORDER);
            System.out.println("Fallback loaded " + trees.size() + " trees");

        } catch (Exception e) {
            System.err.println("Fallback loading also failed:");
            e.printStackTrace();
        }
    }

    public static Integer getTabIdByFileName(String fileName) {
        return fileNameToTabId.get(fileName);
    }

    public static String getFileNameByTabId(int tabId) {
        SkillTreeData tree = trees.get(tabId);
        return tree != null ? tree.fileName : null;
    }

    public static String getDisplayNameByTabId(int tabId) {
        SkillTreeData tree = trees.get(tabId);
        return tree != null ? tree.displayName : "Tab " + tabId;
    }

    private static void rebuildChildrenMap(SkillTreeData tree) {
        tree.childrenMap.clear();
        for (SkillTreeNode n : tree.nodes.values()) {
            if (n.parentId != null && !"start".equalsIgnoreCase(n.parentId)) {
                tree.childrenMap.computeIfAbsent(n.parentId, k -> new ArrayList<>()).add(n);
            }
        }
    }

    // Новая логика: grid-based placement
    // Если у узла указана grid-позиция -> используем её (жёстко).
    // Иначе: BFS от root, выставляем integer grid координаты (gx, gy) с шагом GRID_STEP.
    private static void calculateAndUpdatePositions(SkillTreeData tree,
                                                    int panelScreenX,
                                                    int panelScreenY) {
        if (tree.nodes.isEmpty()) return;

        int areaX = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int areaY = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;

        int centerX = areaX + AREA_WIDTH / 2 + tree.offsetX;
        int centerY = areaY + AREA_HEIGHT / 2 + tree.offsetY;

        // --- поиск стартового узла ---
        SkillTreeNode start = tree.nodes.values().stream()
                .filter(n -> n.parentId == null || "start".equalsIgnoreCase(n.parentId))
                .findFirst().orElse(null);

        if (start == null && !tree.nodes.isEmpty())
            start = tree.nodes.values().iterator().next();

        Set<Long> occupied = new HashSet<>();
        final var keyOf = (java.util.function.BiFunction<Integer,Integer,Long>)
                (gx, gy) -> (((long)gx) << 32) | (gy & 0xffffffffL);

        if (!start.hasGridPos) {
            start.setGridPos(0, 0);
        }
        occupied.add(keyOf.apply(start.gridX, start.gridY));

        Queue<SkillTreeNode> q = new ArrayDeque<>();
        q.add(start);

        // резервируем заранее заданные grid-позиции
        for (SkillTreeNode n : tree.nodes.values()) {
            if (n.hasGridPos) {
                occupied.add(keyOf.apply(n.gridX, n.gridY));
            }
        }

        // --- BFS размещение ---
        while (!q.isEmpty()) {
            SkillTreeNode parent = q.poll();
            List<SkillTreeNode> children =
                    tree.childrenMap.getOrDefault(parent.id, Collections.emptyList());

            for (SkillTreeNode child : children) {
                if (child == parent) continue;

                if (child.hasGridPos) {
                    q.add(child);
                    continue;
                }

                int gx = parent.gridX;
                int gy = parent.gridY;

                switch (child.side) {
                    case RIGHT -> gx = parent.gridX + 1;
                    case LEFT  -> gx = parent.gridX - 1;
                    case TOP   -> gy = parent.gridY - 1;  // -Y вверх
                    case BOTTOM-> gy = parent.gridY + 1;  // +Y вниз
                    default    -> gx = parent.gridX + 1;
                }

                while (occupied.contains(keyOf.apply(gx, gy))) {
                    gx++;
                }

                child.gridX = gx;
                child.gridY = gy;
                child.hasGridPos = true;

                occupied.add(keyOf.apply(gx, gy));
                q.add(child);
            }
        }

        // --- перевод grid → экран ---
        for (SkillTreeNode n : tree.nodes.values()) {
            int nodeScreenX =
                    centerX + n.gridX * GRID_STEP - SkillTreeNode.FRAME_SIZE / 2;

            int nodeScreenY =
                    centerY + n.gridY * GRID_STEP - SkillTreeNode.FRAME_SIZE / 2;

            n.x = nodeScreenX;
            n.y = nodeScreenY;
        }
    }


    private static void limitOffset(SkillTreeData tree, int panelScreenX, int panelScreenY) {
        if (tree.nodes.isEmpty()) return;

        int areaX = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int areaY = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (SkillTreeNode n : tree.nodes.values()) {
            int nodeScreenX = n.x;
            int nodeScreenY = n.y;
            int frameSize = (int)(SkillTreeNode.FRAME_SIZE * tree.scale);

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
            tree.offsetX = Math.max(minOffsetX, Math.min(maxOffsetX, tree.offsetX));
        } else {
            tree.offsetX = (minOffsetX + maxOffsetX) / 2;
        }

        if (minOffsetY < maxOffsetY) {
            tree.offsetY = Math.max(minOffsetY, Math.min(maxOffsetY, tree.offsetY));
        } else {
            tree.offsetY = (minOffsetY + maxOffsetY) / 2;
        }
    }

    public static void render(GuiGraphics gui, InventoryScreen screen,
                              int panelScreenX, int panelScreenY,
                              int mouseX, int mouseY) {

        SkillTreeData currentTree = getCurrentTree();
        if (currentTree.nodes.isEmpty()) return;

        currentPanelScreenX = panelScreenX;
        currentPanelScreenY = panelScreenY;

        if (currentTree.isDragging) {
            currentTree.offsetX = currentTree.dragStartOffsetX + (mouseX - currentTree.dragStartX);
            currentTree.offsetY = currentTree.dragStartOffsetY + (mouseY - currentTree.dragStartY);
        }

        calculateAndUpdatePositions(currentTree, panelScreenX, panelScreenY);

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
        pose.scale(currentTree.scale, currentTree.scale, 1f);
        pose.translate(-pivotX, -pivotY, 0);

        // -------- ЛИНИИ --------
        int lineColor = 0xFF000000;

        for (SkillTreeNode child : currentTree.nodes.values()) {
            if (child.parentId == null || "start".equalsIgnoreCase(child.parentId)) continue;

            SkillTreeNode parent = currentTree.nodes.get(child.parentId);
            if (parent == null) continue;

            // рисуем линию только если у child и у parent выставлены позиции (они есть после calculate...)
            drawThickLine(gui,
                    parent.centerX(),
                    parent.centerY(),
                    child.centerX(),
                    child.centerY(),
                    2,
                    lineColor);
        }

        // -------- НОДЫ --------
        for (SkillTreeNode n : currentTree.nodes.values()) {
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

        // -------- ИНФОРМАЦИЯ О ДЕРЕВЕ --------
        Font font = Minecraft.getInstance().font;
        String scaleText = String.format("%.0f%%", currentTree.scale * 100);
        String treeName = currentTree.displayName + " (Tab " + (activeTreeId + 1) + "/" + trees.size() + ")";

        int textX = clipX1 + 5;
        int textY = clipY1 + 5;

        int w1 = font.width(scaleText);
        int w2 = font.width(treeName);
        int maxW = Math.max(w1, w2);

        gui.fill(textX - 2, textY - 2, textX + maxW + 2, textY + font.lineHeight * 2 + 4, 0xAA000000);

        gui.drawString(font, treeName, textX, textY, 0xFFFFFFAA, true);
        gui.drawString(font, scaleText, textX, textY + font.lineHeight + 2, 0xFFFFFFFF, true);

        // -------- TOOLTIP --------
        for (SkillTreeNode n : currentTree.nodes.values()) {
            if (n.containsPoint(mouseX, mouseY, currentTree.scale)) {
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

    // ------ INPUT: обработка для активного дерева (без изменений) ------
    public static boolean mousePressed(int mouseX, int mouseY, int button, int panelScreenX, int panelScreenY) {
        SkillTreeData currentTree = getCurrentTree();

        int areaX = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int areaY = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;

        if (mouseX >= areaX && mouseX <= areaX + AREA_WIDTH &&
                mouseY >= areaY && mouseY <= areaY + AREA_HEIGHT) {
            currentTree.isDragging = true;
            currentTree.dragStartX = mouseX;
            currentTree.dragStartY = mouseY;
            currentTree.dragStartOffsetX = currentTree.offsetX;
            currentTree.dragStartOffsetY = currentTree.offsetY;
            return true;
        }
        return false;
    }

    public static boolean mouseReleased(int mouseX, int mouseY, int button) {
        SkillTreeData currentTree = getCurrentTree();
        if (currentTree.isDragging) {
            currentTree.isDragging = false;
            return true;
        }
        return false;
    }

    public static boolean mouseDragged(int mouseX, int mouseY, int button, int panelScreenX, int panelScreenY) {
        SkillTreeData currentTree = getCurrentTree();
        if (!currentTree.isDragging || button != 0) return false;

        int deltaX = mouseX - currentTree.dragStartX;
        int deltaY = mouseY - currentTree.dragStartY;

        if (deltaX == 0 && deltaY == 0) return false;

        currentTree.offsetX = currentTree.dragStartOffsetX + deltaX;
        currentTree.offsetY = currentTree.dragStartOffsetY + deltaY;

        calculateAndUpdatePositions(currentTree, panelScreenX, panelScreenY);
        limitOffset(currentTree, panelScreenX, panelScreenY);

        return true;
    }

    public static boolean mouseScrolled(int mouseX, int mouseY, double delta, int panelScreenX, int panelScreenY) {
        SkillTreeData currentTree = getCurrentTree();

        float oldScale = currentTree.scale;
        currentTree.scale = currentTree.scale + (delta > 0 ? ZOOM_STEP : -ZOOM_STEP);
        currentTree.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, currentTree.scale));

        if (!currentTree.nodes.isEmpty()) {
            calculateAndUpdatePositions(currentTree, panelScreenX, panelScreenY);
        }

        return true;
    }

    public static java.util.List<Integer> getAvailableTabIds() {
        return new java.util.ArrayList<>(trees.keySet());
    }

    public static int getMaxTabId() {
        return trees.keySet().stream().max(Integer::compareTo).orElse(-1);
    }

    public static int getMinTabId() {
        return trees.keySet().stream().min(Integer::compareTo).orElse(-1);
    }

    public static boolean hasTreeForTab(int tabId) {
        return trees.containsKey(tabId);
    }

    public static void resetTreePosition() {
        SkillTreeData currentTree = getCurrentTree();
        currentTree.offsetX = 0;
        currentTree.offsetY = 0;
        currentTree.scale = 1.0f;
    }
    public static ItemStack getRootIcon(int treeId) {
        SkillTreeData data = trees.get(treeId);
        if (data == null || data.nodes == null) {
            return ItemStack.EMPTY;
        }

        for (SkillTreeNode n : data.nodes.values()) {
            if ("start".equals(n.parentId)) {
                return n.itemStack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static boolean isEmpty() {
        SkillTreeData currentTree = trees.get(activeTreeId);
        return currentTree == null || currentTree.nodes.isEmpty();
    }

    public static float getScale() {
        return getCurrentTree().scale;
    }

    public static int getOffsetX() {
        return getCurrentTree().offsetX;
    }

    public static int getOffsetY() {
        return getCurrentTree().offsetY;
    }

    public static int getTotalTrees() {
        return trees.size();
    }

    public static java.util.List<String> getSortedFileNames() {
        return new java.util.ArrayList<>(sortedFileNames);
    }
}
