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
    // client-side cache: treeId -> (nodeId -> selectedOption)
    private static final Map<Integer, Map<String, Integer>> variantCache = new ConcurrentHashMap<>();

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

    /** Обновляет кэш изученных нод и сразу применяет к текущим деревьям */
    public static void applyLearnedNodes(int treeId, List<String> learnedIds) {
        if (learnedIds == null || learnedIds.isEmpty()) return;
        learnedCache.put(treeId, ConcurrentHashMap.newKeySet());
        learnedCache.get(treeId).addAll(learnedIds);
        applyCacheToTree(treeId);
    }

    /** Обновляет кэш выбранных вариантов и сразу применяет к текущим деревьям */
    public static void applyVariants(int treeId, Map<String, Integer> variants) {
        System.out.println("applyVariants called for tree " + treeId + " with variants: " + variants);
        if (variants == null || variants.isEmpty()) {
            System.out.println("Variants is null or empty, returning");
            return;
        }
        variantCache.put(treeId, new HashMap<>(variants));
        System.out.println("Variant cache now: " + variantCache);

        // Проверяем, загружено ли дерево
        Object treeObj = getTreesMap().get(treeId);
        if (treeObj != null) {
            System.out.println("Tree already loaded, applying variants now");
            applyVariantsToTree(treeId);
        } else {
            System.out.println("Tree not loaded yet, variants will be applied when tree loads");
            // Кэш сохранится, применится при загрузке дерева
        }
    }
    /** Применяет кэш ко всем деревьям после loadAllTrees */
    public static void reapplyCachedForAllTrees() {
        System.out.println("reapplyCachedForAllTrees() called");
        Map<?, ?> trees = getTreesMap();
        System.out.println("Trees map size: " + trees.size());

        for (Object key : trees.keySet()) {
            if (!(key instanceof Integer treeId)) continue;
            System.out.println("Applying cache for tree " + treeId);

            if (learnedCache.containsKey(treeId)) {
                System.out.println("Has learned cache for tree " + treeId);
                applyCacheToTree(treeId);
            }

            if (variantCache.containsKey(treeId)) {
                System.out.println("Has variant cache for tree " + treeId + ": " + variantCache.get(treeId));
                applyVariantsToTree(treeId);
            }
        }
    }
    private static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) return null;
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(obj);
        } catch (NoSuchFieldException nsf) {
            // пробуем у супер-класса
            Class<?> c = obj.getClass();
            while ((c = c.getSuperclass()) != null) {
                try {
                    Field f = c.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(obj);
                } catch (NoSuchFieldException ignored) {}
                catch (Throwable ex) { ex.printStackTrace(); return null; }
            }
            return null;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private static void setFieldValue(Object obj, String fieldName, Object value) {
        if (obj == null) return;
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (NoSuchFieldException nsf) {
            Class<?> c = obj.getClass();
            while ((c = c.getSuperclass()) != null) {
                try {
                    Field f = c.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(obj, value);
                    return;
                } catch (NoSuchFieldException ignored) {}
                catch (Throwable ex) { ex.printStackTrace(); return; }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    /** Применяет кэш конкретного дерева: изученные ноды */
    /** Применяет кэш конкретного дерева: изученные ноды */
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

            // ИСПРАВЛЕНИЕ: получаем активное меню опций
            String activeOptionsNodeId = (String) getFieldValue(treeObj, "activeOptionsNodeId");

            for (String id : set) {
                Object n = nodesMap.get(id);
                if (n instanceof SkillTreeNode node && !node.learned) {
                    node.learned = true;
                    node.locked = false;
                    changed = true;

                    // ИСПРАВЛЕНИЕ: если это та нода, у которой открыто меню опций - закрываем его
                    if (id.equals(activeOptionsNodeId)) {
                        setFieldValue(treeObj, "activeOptionsNodeId", null);
                    }

                    if (Render.currentHoveredNode == node) {
                        Render.mousePressTime = 0L;
                        Render.currentHoveredNode = null;
                    }
                }
            }

            // Пересчёт locked нод
            for (Object entryObj : nodesMap.values()) {
                if (!(entryObj instanceof SkillTreeNode)) continue;
                SkillTreeNode node = (SkillTreeNode) entryObj;

                boolean shouldBeLocked = true;
                if (node.learned) shouldBeLocked = false;
                else if (node.parentId == null || "start".equalsIgnoreCase(node.parentId)) shouldBeLocked = false;
                else if (set.contains(node.parentId)) shouldBeLocked = false;

                if (node.locked != shouldBeLocked) {
                    node.locked = shouldBeLocked;
                    changed = true;
                }
            }

            if (changed) {
                Render.invokePrivateCalculateAndUpdatePositions(treeObj,
                        Render.currentPanelScreenX,
                        Render.currentPanelScreenY);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** Применяет кэш выбранных вариантов конкретного дерева */
    /** Применяет кэш выбранных вариантов конкретного дерева */
    private static void applyVariantsToTree(int treeId) {
        Map<String, Integer> variants = variantCache.get(treeId);
        if (variants == null || variants.isEmpty()) return;

        Object treeObj = getTreesMap().get(treeId);
        if (treeObj == null) return;

        try {
            Field nodesField = treeObj.getClass().getDeclaredField("nodes");
            nodesField.setAccessible(true);
            Object nodesObj = nodesField.get(treeObj);
            if (!(nodesObj instanceof Map<?, ?> nodesMap)) return;

            boolean changed = false;
            for (Map.Entry<String, Integer> entry : variants.entrySet()) {
                Object n = nodesMap.get(entry.getKey());
                if (n instanceof SkillTreeNode node) {
                    // ИСПРАВЛЕНИЕ: применяем вариант, чтобы обновить itemStack и displayId
                    if (node.selectedOption != entry.getValue()) {
                        node.applyVariant(entry.getValue());
                        changed = true;
                    }
                }
            }

            if (changed) {
                Render.invokePrivateCalculateAndUpdatePositions(treeObj,
                        Render.currentPanelScreenX,
                        Render.currentPanelScreenY);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}