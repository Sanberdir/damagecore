package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.api.damage_book_protection.node_variant.SelectVariantPacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class SkillTreeRenderer {
    private SkillTreeRenderer() {}
    private static boolean treesLoaded = false; // НОВОЕ
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
    private static final int GRID_STEP = SkillTreeNode.FRAME_SIZE + 1;
    private static final int OPTION_SIZE = 18; // размер ячейки опции (внутри масштабирования)
    private static final int OPTION_BASE_RADIUS = 28; // базовый радиус от центра ноды до центра ячейки опции
    private static final int OPTION_RADIUS_STEP = 8;  // дополнительный шаг радиуса при росте числа опций

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

        boolean isDragging = false;
        int dragStartX = 0;
        int dragStartY = 0;
        int dragStartOffsetX = 0;
        int dragStartOffsetY = 0;

        int offsetX = 0;
        int offsetY = 0;

        float scale = 1.0f;

        String fileName;
        String displayName;

        ItemStack tabIcon = ItemStack.EMPTY;

        // NEW: активное меню опций: id ноды или null
        String activeOptionsNodeId = null;

        SkillTreeData(String fileName, String displayName) {
            this.fileName = fileName;
            this.displayName = displayName;
        }
    }
    // В SkillTreeRenderer
    public static SkillTreeData getActiveTree() {
        return getCurrentTree();
    }
    // Открыть меню опций для ноды (возвращает true если открылось)
    private static boolean openOptionsForNode(SkillTreeData tree, SkillTreeNode node) {
        // ИСПРАВЛЕНИЕ: не открываем опции для изученных узлов
        if (node == null || node.options == null || node.options.isEmpty() || node.learned) return false;
        tree.activeOptionsNodeId = node.id;
        return true;
    }

    private static void closeOptions(SkillTreeData tree) {
        tree.activeOptionsNodeId = null;
    }

    // Проверяет и возвращает индекс опции под мышью или -1, если нет
    private static int optionIndexAtPoint(SkillTreeNode node, int px, int py, int optionSize) {
        int n = node.options.size();
        if (n == 0) return -1;

        int radius = OPTION_BASE_RADIUS + Math.max(0, n - 1) * OPTION_RADIUS_STEP;

        for (int i = 0; i < n; i++) {
            double angle = 2.0 * Math.PI * i / n;
            int cx = node.centerX() + (int)Math.round(radius * Math.cos(angle));
            int cy = node.centerY() + (int)Math.round(radius * Math.sin(angle));

            int left = cx - optionSize / 2;
            int top  = cy - optionSize / 2;

            if (px >= left && px < left + optionSize && py >= top && py < top + optionSize) {
                return i;
            }
        }
        return -1;
    }

    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2.0f;
    private static final float ZOOM_STEP = 0.1f;

    private static int currentPanelScreenX = 0;
    private static int currentPanelScreenY = 0;

    private static SkillTreeData getCurrentTree() {
        SkillTreeData tree = trees.get(activeTreeId);
        if (tree == null) {
            tree = new SkillTreeData("empty", "Empty Tree " + (activeTreeId + 1));
            trees.put(activeTreeId, tree);
        }
        return tree;
    }

    public static void setActiveTree(int tabId) {
        if (tabId >= 0 && tabId < trees.size()) {
            activeTreeId = tabId;
        }
    }
    // Загрузить все деревья из папки assets/damagecore/skill_tree/
    public static void loadAllTrees(String folderPath) {
        if (treesLoaded) {
            System.out.println("Trees already loaded, skipping...");
            return;
        }

        System.out.println("Loading trees from " + folderPath);
        treesLoaded = true;

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
            }

            Collections.sort(sortedFileNames, String.CASE_INSENSITIVE_ORDER);

            int tabId = 0;
            for (String fileName : sortedFileNames) {
                String resourcePath = finalPath + "/" + fileName;
                String displayName = fileName.replace(".json", "");

                // Теперь загрузчик возвращает Object[]{ItemStack, List<SkillTreeNode>}
                Object[] loaderResult = SkillTreeLoader.loadFromResource(resourcePath);
                ItemStack tabIcon = ItemStack.EMPTY;
                List<SkillTreeNode> list = Collections.emptyList();
                if (loaderResult != null) {
                    if (loaderResult[0] instanceof ItemStack) tabIcon = (ItemStack) loaderResult[0];
                    if (loaderResult[1] instanceof List) list = (List<SkillTreeNode>) loaderResult[1];
                }

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

                    // Устанавливаем иконку вкладки, если есть
                    if (tabIcon != null && !tabIcon.isEmpty()) {
                        tree.tabIcon = tabIcon;
                    }

                    trees.put(tabId, tree);
                    fileNameToTabId.put(fileName, tabId);
                    tabId++;
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading skill trees:");
            e.printStackTrace();
        }
        // после загрузки всех деревьев
        System.out.println("Trees loaded, reapplying cache...");
        SkillTreeClientSync.reapplyCachedForAllTrees();
    }

    private static void rebuildChildrenMap(SkillTreeData tree) {
        tree.childrenMap.clear();
        for (SkillTreeNode n : tree.nodes.values()) {
            // Для каждого родителя добавляем этого ребенка
            for (String parentId : n.parentIds) {
                if (parentId != null && !"start".equalsIgnoreCase(parentId)) {
                    tree.childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(n);
                }
            }
        }
    }

    private static void calculateAndUpdatePositions(SkillTreeData tree,
                                                    int panelScreenX,
                                                    int panelScreenY) {
        if (tree.nodes.isEmpty()) return;

        int areaX = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int areaY = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;

        int centerX = areaX + AREA_WIDTH / 2 + tree.offsetX;
        int centerY = areaY + AREA_HEIGHT / 2 + tree.offsetY;

        // Находим все корневые узлы (те, у кого нет родителей или parent="start")
        List<SkillTreeNode> roots = tree.nodes.values().stream()
                .filter(n -> n.isRoot())
                .collect(Collectors.toList());

        if (roots.isEmpty() && !tree.nodes.isEmpty())
            roots.add(tree.nodes.values().iterator().next());

        Set<Long> occupied = new HashSet<>();
        final var keyOf = (java.util.function.BiFunction<Integer,Integer,Long>)
                (gx, gy) -> (((long)gx) << 32) | (gy & 0xffffffffL);

        // Инициализация корней
        Queue<SkillTreeNode> q = new ArrayDeque<>();
        int rootX = 0;
        for (SkillTreeNode root : roots) {
            if (!root.hasGridPos) {
                root.setGridPos(rootX, 0);
                rootX += 2; // Разносим корни по горизонтали
            }
            occupied.add(keyOf.apply(root.gridX, root.gridY));
            q.add(root);
        }

        // Загружаем уже заданные позиции
        for (SkillTreeNode n : tree.nodes.values()) {
            if (n.hasGridPos) {
                occupied.add(keyOf.apply(n.gridX, n.gridY));
            }
        }

        // BFS для размещения детей
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

                // Определяем позицию на основе всех родителей
                if (child.parentIds.size() > 1) {
                    // Узел с несколькими родителями - размещаем между ними
                    positionMultiParentNode(child, tree, occupied);
                } else {
                    // Обычный узел с одним родителем
                    positionSingleParentNode(child, parent, occupied);
                }

                q.add(child);
            }
        }

        // Конвертируем grid-координаты в экранные
        for (SkillTreeNode n : tree.nodes.values()) {
            int nodeScreenX = centerX + n.gridX * GRID_STEP - SkillTreeNode.FRAME_SIZE / 2;
            int nodeScreenY = centerY - n.gridY * GRID_STEP - SkillTreeNode.FRAME_SIZE / 2;
            n.x = nodeScreenX;
            n.y = nodeScreenY;
        }
    }
    private static void positionSingleParentNode(SkillTreeNode child,
                                                 SkillTreeNode parent,
                                                 Set<Long> occupied) {
        int gx = parent.gridX;
        int gy = parent.gridY;

        switch (child.side) {
            case RIGHT -> gx = parent.gridX + 1;
            case LEFT  -> gx = parent.gridX - 1;
            case TOP   -> gy = parent.gridY - 1;
            case BOTTOM-> gy = parent.gridY + 1;
            default    -> gx = parent.gridX + 1;
        }

        final var keyOf = (java.util.function.BiFunction<Integer,Integer,Long>)
                (x, y) -> (((long)x) << 32) | (y & 0xffffffffL);

        while (occupied.contains(keyOf.apply(gx, gy))) {
            gx++;
        }

        child.gridX = gx;
        child.gridY = gy;
        child.hasGridPos = true;
        occupied.add(keyOf.apply(gx, gy));
    }

    private static void positionMultiParentNode(SkillTreeNode child,
                                                SkillTreeData tree,
                                                Set<Long> occupied) {
        // Находим всех родителей
        List<SkillTreeNode> parents = new ArrayList<>();
        for (String parentId : child.parentIds) {
            SkillTreeNode parent = tree.nodes.get(parentId);
            if (parent != null && parent.hasGridPos) {
                parents.add(parent);
            }
        }

        if (parents.isEmpty()) {
            // Нет родителей с позициями - размещаем как обычный узел
            positionSingleParentNode(child, tree.nodes.values().iterator().next(), occupied);
            return;
        }

        // Вычисляем среднюю позицию между всеми родителями
        int sumX = 0, sumY = 0;
        for (SkillTreeNode p : parents) {
            sumX += p.gridX;
            sumY += p.gridY;
        }
        int avgX = sumX / parents.size();
        int avgY = sumY / parents.size();

        // Применяем смещение на основе side
        switch (child.side) {
            case RIGHT -> avgX += 1;
            case LEFT  -> avgX -= 1;
            case TOP   -> avgY -= 1;
            case BOTTOM-> avgY += 1;
        }

        final var keyOf = (java.util.function.BiFunction<Integer,Integer,Long>)
                (x, y) -> (((long)x) << 32) | (y & 0xffffffffL);

        // Ищем свободную позицию рядом с расчетной
        int radius = 0;
        while (true) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.abs(dx) == radius || Math.abs(dy) == radius) {
                        int testX = avgX + dx;
                        int testY = avgY + dy;
                        if (!occupied.contains(keyOf.apply(testX, testY))) {
                            child.gridX = testX;
                            child.gridY = testY;
                            child.hasGridPos = true;
                            occupied.add(keyOf.apply(testX, testY));
                            return;
                        }
                    }
                }
            }
            radius++;
        }
    }

    // ------ INPUT: обработка для активного дерева (без изменений) ------
    public static boolean mousePressed(int mouseX, int mouseY, int button, int panelScreenX, int panelScreenY) {
        SkillTreeData currentTree = getCurrentTree();

        int areaX = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
        int areaY = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;

        // клик вне области дерева — закрываем меню опций
        if (!(mouseX >= areaX && mouseX <= areaX + AREA_WIDTH &&
                mouseY >= areaY && mouseY <= areaY + AREA_HEIGHT)) {
            if (currentTree.activeOptionsNodeId != null) {
                closeOptions(currentTree);
                return true;
            }
            return false;
        }

        // вычисляем unscaled coords для проверки попадания по нодам/опциям
        int pivotX = areaX + AREA_WIDTH / 2;
        int pivotY = areaY + AREA_HEIGHT / 2;
        float scale = currentTree.scale;
        int unscaledMouseX = (int)((mouseX - pivotX) / scale + pivotX);
        int unscaledMouseY = (int)((mouseY - pivotY) / scale + pivotY);

        // если меню опций открыто
        if (currentTree.activeOptionsNodeId != null) {
            SkillTreeNode node = currentTree.nodes.get(currentTree.activeOptionsNodeId);
            if (node != null && !node.options.isEmpty()) {
                int idx = optionIndexAtPoint(node, unscaledMouseX, unscaledMouseY, OPTION_SIZE);
                if (idx >= 0) {
                    // Применяем вариант к ноде (изменяем отображаемый стек и displayId)
                    node.applyVariant(idx);

                    // Отправляем на сервер выбор варианта для постоянного сохранения
                    try {
                        if (ModNetwork.CHANNEL != null) {
                            int treeId = activeTreeId;
                            ModNetwork.CHANNEL.sendToServer(new SelectVariantPacket(treeId, node.id, idx));
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }

                    closeOptions(currentTree);
                    return true;
                } else {
                    // Клик внутри области опций, но не по конкретной опции — просто закрываем
                    closeOptions(currentTree);
                    return true;
                }
            } else {
                closeOptions(currentTree);
                return true;
            }
        }

        // проверка клика по нодам
        for (SkillTreeNode node : currentTree.nodes.values()) {
            if (node.containsPoint(unscaledMouseX, unscaledMouseY)) {
                // Если нода заблокирована - игнорируем
                if (node.locked) {
                    return false;
                }

                // ИСПРАВЛЕНИЕ: если нода уже изучена - не открываем опции
                if (node.learned) {
                    return false; // Просто игнорируем клик по изученной ноде
                }

                if (node.options != null && !node.options.isEmpty()) {
                    openOptionsForNode(currentTree, node);
                    return true;
                } else if (button == 0) {
                    currentTree.isDragging = true;
                    currentTree.dragStartX = mouseX;
                    currentTree.dragStartY = mouseY;
                    currentTree.dragStartOffsetX = currentTree.offsetX;
                    currentTree.dragStartOffsetY = currentTree.offsetY;
                    return true;
                }
                return false;
            }
        }

        // клик по пустому месту — начинаем drag
        if (button == 0) {
            currentTree.isDragging = true;
            currentTree.dragStartX = mouseX;
            currentTree.dragStartY = mouseY;
            currentTree.dragStartOffsetX = currentTree.offsetX;
            currentTree.dragStartOffsetY = currentTree.offsetY;
            return true;
        }

        return false;
    }

    public static boolean mouseDragged(int mouseX, int mouseY, int button, int panelScreenX, int panelScreenY) {
        SkillTreeData currentTree = getCurrentTree();
        if (!currentTree.isDragging || button != 0) return false;

        float scale = currentTree.scale;

        // дельта считается в screen-координатах и делится на масштаб
        int deltaX = (int)((mouseX - currentTree.dragStartX) / scale);
        int deltaY = (int)((mouseY - currentTree.dragStartY) / scale);

        currentTree.offsetX = currentTree.dragStartOffsetX + deltaX;
        currentTree.offsetY = currentTree.dragStartOffsetY + deltaY;

        calculateAndUpdatePositions(currentTree, panelScreenX, panelScreenY);
        return true;
    }
    public static boolean mouseReleased(int mouseX, int mouseY, int button) {
        SkillTreeData currentTree = getCurrentTree();
        if (currentTree.isDragging) {
            currentTree.isDragging = false;
            return true;
        }
        return false;
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

    public static boolean hasTreeForTab(int tabId) {
        return trees.containsKey(tabId);
    }

    public static void resetTreePosition() {
        SkillTreeData currentTree = getCurrentTree();
        currentTree.offsetX = 0;
        currentTree.offsetY = 0;
        currentTree.scale = 1.0f;
    }

    // Возвращает иконку для вкладки: сначала tabIcon, затем fallback на root-node
    public static ItemStack getRootIcon(int treeId) {
        SkillTreeData data = trees.get(treeId);
        if (data == null) {
            return ItemStack.EMPTY;
        }

        if (data.tabIcon != null && !data.tabIcon.isEmpty()) {
            return data.tabIcon;
        }

        if (data.nodes == null) {
            return ItemStack.EMPTY;
        }

        for (SkillTreeNode n : data.nodes.values()) {
            // Проверяем, является ли узел корневым (нет родителей или только "start")
            if (n.isRoot()) {
                return n.itemStack;
            }
            // Для обратной совместимости также проверяем id="root"
            if ("root".equalsIgnoreCase(n.id)) {
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