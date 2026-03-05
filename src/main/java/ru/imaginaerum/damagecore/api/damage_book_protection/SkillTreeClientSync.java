package ru.imaginaerum.damagecore.api.damage_book_protection;

import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeNode;
import ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer.Render;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillTreeClientSync {
    private SkillTreeClientSync() {}

    // client-side cache: treeId -> set of learned node ids
    private static final Map<Integer, Set<String>> learnedCache = new ConcurrentHashMap<>();

    private static Field treesField = null;

    private static Map<?, ?> getTreesMap() {
        try {
            if (treesField == null) {
                Class<?> rendererCls = Class.forName("ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer");
                treesField = rendererCls.getDeclaredField("trees");
                treesField.setAccessible(true);
            }
            Object obj = treesField.get(null);
            if (obj instanceof Map<?, ?> map) return map;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return Collections.emptyMap();
    }

    /** Обновляет кэш и сразу применяет к текущим деревьям */
    public static void applyLearnedNodes(int treeId, List<String> learnedIds) {
        if (learnedIds == null || learnedIds.isEmpty()) return;
        learnedCache.put(treeId, ConcurrentHashMap.newKeySet());
        learnedCache.get(treeId).addAll(learnedIds);
        applyCacheToTree(treeId);
    }

    /** Применяет кэш ко всем деревьям после loadAllTrees */
    public static void reapplyCachedForAllTrees() {
        Map<?, ?> trees = getTreesMap();
        for (Object key : trees.keySet()) {
            if (!(key instanceof Integer treeId)) continue;
            applyCacheToTree(treeId);
        }
    }

    /** Применяет кэш конкретного дерева */
    /** Применяет кэш конкретного дерева */
    private static void applyCacheToTree(int treeId) {
        Set<String> set = learnedCache.get(treeId);
        if (set == null) return;

        Object treeObj = getTreesMap().get(treeId);
        if (treeObj == null) return;

        try {
            Field nodesField = treeObj.getClass().getDeclaredField("nodes");
            nodesField.setAccessible(true);
            Object nodesObj = nodesField.get(treeObj);
            if (!(nodesObj instanceof Map<?, ?> nodesMap)) return;

            boolean changed = false;

            // 1) Отмечаем изученные ноды
            for (String id : set) {
                Object n = nodesMap.get(id);
                if (n instanceof SkillTreeNode node && !node.learned) {
                    node.learned = true;
                    // если нода изучена — явно разблокируем её
                    node.locked = false;
                    changed = true;

                    // сбрасываем визуальный hover/hold прогресс
                    if (Render.currentHoveredNode == node) {
                        Render.mousePressTime = 0L;
                        Render.currentHoveredNode = null;
                    }
                }
            }

            // 2) Пересчитываем locked для всех нод: нода разблокирована если:
            //    - она уже learned, или
            //    - её parentId == null || "start", или
            //    - её parentId присутствует в множестве изученных
            for (Object entryObj : nodesMap.values()) {
                if (!(entryObj instanceof SkillTreeNode)) continue;
                SkillTreeNode node = (SkillTreeNode) entryObj;

                boolean shouldBeLocked = true;

                if (node.learned) {
                    shouldBeLocked = false;
                } else if (node.parentId == null) {
                    shouldBeLocked = false; // корневая?
                } else if ("start".equalsIgnoreCase(node.parentId)) {
                    shouldBeLocked = false; // стартовая позиция
                } else if (set.contains(node.parentId)) {
                    // родитель изучен -> разблокировать
                    shouldBeLocked = false;
                } else {
                    // если родитель не в множестве — остаётся заблокированной
                    shouldBeLocked = true;
                }

                if (node.locked != shouldBeLocked) {
                    node.locked = shouldBeLocked;
                    changed = true;
                }
            }

            if (changed) {
                // обновляем позиции и состояния для UI
                Render.invokePrivateCalculateAndUpdatePositions(treeObj,
                        Render.currentPanelScreenX,
                        Render.currentPanelScreenY);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}