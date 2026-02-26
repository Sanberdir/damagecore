package ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Standalone renderer for skill tree UI.
 *
 * This class uses reflection to access the private SkillTreeData instance inside
 * SkillTreeRenderer so we don't have to change SkillTreeRenderer visibility.
 *
 * If you'd rather, you can instead make SkillTreeRenderer.getCurrentTree() and
 * calculateAndUpdatePositions(...) public and remove reflection code.
 */
public class Render {
    // copy of necessary constants (must match SkillTreeRenderer)
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

    private static final int OPTION_SIZE = 18; // размер ячейки опции (внутри масштабирования)
    private static final int OPTION_BASE_RADIUS = 28; // базовый радиус от центра ноды до центра ячейки опции
    private static final int OPTION_RADIUS_STEP = 8;  // дополнительный шаг радиуса при росте числа опций

    private static int currentPanelScreenX = 0;
    private static int currentPanelScreenY = 0;

    /**
     * Главный метод render — адаптирован для работы через рефлексию.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void render(GuiGraphics gui, InventoryScreen screen,
                              int panelScreenX, int panelScreenY,
                              int mouseX, int mouseY) {
        try {
            // Получаем приватный SkillTreeData объект через приватный метод SkillTreeRenderer.getCurrentTree()
            Object treeObj = invokePrivateGetCurrentTree();
            if (treeObj == null) return;

            // Получим nodes map из treeObj
            Map<String, SkillTreeNode> nodes = (Map<String, SkillTreeNode>) getFieldValue(treeObj, "nodes");
            if (nodes == null || nodes.isEmpty()) return;

            currentPanelScreenX = panelScreenX;
            currentPanelScreenY = panelScreenY;

            // Получаем флаги/поля из treeObj
            boolean isDragging = Boolean.TRUE.equals(getFieldValue(treeObj, "isDragging"));
            if (isDragging) {
                // dragStartOffsetX, dragStartOffsetY, dragStartX, dragStartY, scale
                int dragStartOffsetX = ((Number) getFieldValue(treeObj, "dragStartOffsetX")).intValue();
                int dragStartOffsetY = ((Number) getFieldValue(treeObj, "dragStartOffsetY")).intValue();
                int dragStartX = ((Number) getFieldValue(treeObj, "dragStartX")).intValue();
                int dragStartY = ((Number) getFieldValue(treeObj, "dragStartY")).intValue();
                float scale = ((Number) getFieldValue(treeObj, "scale")).floatValue();

                int newOffsetX = dragStartOffsetX + (int) ((mouseX - dragStartX) / scale);
                int newOffsetY = dragStartOffsetY + (int) ((mouseY - dragStartY) / scale);

                // Записать обратно offsetX/offsetY в treeObj
                setFieldValue(treeObj, "offsetX", newOffsetX);
                setFieldValue(treeObj, "offsetY", newOffsetY);
            }

            // Вызвать приватный calculateAndUpdatePositions(treeObj, panelScreenX, panelScreenY)
            invokePrivateCalculateAndUpdatePositions(treeObj, panelScreenX, panelScreenY);

            int clipX1 = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
            int clipY1 = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;
            int clipX2 = clipX1 + AREA_WIDTH;
            int clipY2 = clipY1 + AREA_HEIGHT;

            if (clipX2 > clipX1 && clipY2 > clipY1) {
                gui.enableScissor(clipX1, clipY1, clipX2, clipY2);
            }

            PoseStack pose = gui.pose();

            int pivotX = clipX1 + AREA_WIDTH / 2;
            int pivotY = clipY1 + AREA_HEIGHT / 2;

            // --- 1. Линии + все ноды ---
            pose.pushPose();
            pose.translate(pivotX, pivotY, 0);
            float scale = ((Number) getFieldValue(treeObj, "scale")).floatValue();
            pose.scale(scale, scale, 1f);
            pose.translate(-pivotX, -pivotY, 0);

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

            // Координаты мыши под scale
            int unscaledMouseX = (int) ((mouseX - pivotX) / scale + pivotX);
            int unscaledMouseY = (int) ((mouseY - pivotY) / scale + pivotY);

            for (SkillTreeNode n : nodes.values()) {
                int frameSize = SkillTreeNode.FRAME_SIZE;
                int padding = SkillTreeNode.FRAME_PADDING;

                // рамка
                gui.fill(n.x, n.y, n.x + frameSize, n.y + frameSize, 0xFF333333);

                // фон ноды
                int innerColor = 0xFF777777;
                if (n.containsPoint(unscaledMouseX, unscaledMouseY)) {
                    innerColor = n.locked ? 0xFF444444 : 0xAAFFFFFF;
                }
                gui.fill(n.x + padding, n.y + padding,
                        n.x + frameSize - padding,
                        n.y + frameSize - padding,
                        innerColor);

                // предмет
                int itemX = n.x + (frameSize - 16) / 2;
                int itemY = n.y + (frameSize - 16) / 2;
                gui.renderItem(n.itemStack, itemX, itemY);
                gui.renderItemDecorations(Minecraft.getInstance().font, n.itemStack, itemX, itemY);
            }
            pose.popPose();

            // --- 2. Затемнение всего дерева (без scale) ---
            String activeOptionsNodeId = (String) getFieldValue(treeObj, "activeOptionsNodeId");
            if (activeOptionsNodeId != null) {
                gui.fill(clipX1, clipY1, clipX2, clipY2, 0xAA000000);
            }

            // --- 3. Активная нода + её опции (с scale) ---
            if (activeOptionsNodeId != null) {
                SkillTreeNode clicked = nodes.get(activeOptionsNodeId);
                if (clicked != null) {
                    pose.pushPose();
                    pose.translate(pivotX, pivotY, 0);
                    pose.scale(scale, scale, 1f);
                    pose.translate(-pivotX, -pivotY, 0);

                    int frameSize = SkillTreeNode.FRAME_SIZE;
                    int padding = SkillTreeNode.FRAME_PADDING;

                    // рамка
                    gui.fill(clicked.x, clicked.y, clicked.x + frameSize, clicked.y + frameSize, 0xFF333333);

                    // яркий фон
                    int brightInner = clicked.locked ? 0xFF444444 : 0xFFFFFFFF;
                    gui.fill(clicked.x + padding, clicked.y + padding,
                            clicked.x + frameSize - padding,
                            clicked.y + frameSize - padding,
                            brightInner);

                    // предмет
                    int itemX = clicked.x + (frameSize - 16) / 2;
                    int itemY = clicked.y + (frameSize - 16) / 2;
                    gui.renderItem(clicked.itemStack, itemX, itemY);
                    gui.renderItemDecorations(Minecraft.getInstance().font, clicked.itemStack, itemX, itemY);

                    // опции (поднятый Z)
                    List<?> options = (List<?>) getFieldValue(clicked, "options");
                    if (options != null && !options.isEmpty()) {
                        int n = options.size();

                        pose.pushPose();
                        pose.translate(0.0f, 0.0f, 200.0f); // поднять Z

                        for (int i = 0; i < n; i++) {
                            int[] center = optionCenterForIndex(clicked, i);
                            int cx = center[0];
                            int cy = center[1];

                            int left = cx - OPTION_SIZE / 2;
                            int top = cy - OPTION_SIZE / 2;

                            boolean hover = (unscaledMouseX >= left && unscaledMouseX < left + OPTION_SIZE
                                    && unscaledMouseY >= top && unscaledMouseY < top + OPTION_SIZE);

                            int selectedOption = (int) ((Number) getFieldValue(clicked, "selectedOption")).intValue();
                            int bg;
                            if (selectedOption >= 0 && selectedOption == i) {
                                bg = 0xFFFFFFFF;
                            } else if (hover) {
                                bg = 0xAAFFFFFF;
                            } else {
                                bg = 0xDD555555;
                            }

                            gui.fill(left, top, left + OPTION_SIZE, top + OPTION_SIZE, bg);

                            Object optItem = options.get(i);
                            // renderItem expects an ItemStack; we assume option list contains ItemStack
                            if (optItem instanceof net.minecraft.world.item.ItemStack) {
                                net.minecraft.world.item.ItemStack stack = (net.minecraft.world.item.ItemStack) optItem;
                                gui.renderItem(stack, left + (OPTION_SIZE - 16) / 2,
                                        top + (OPTION_SIZE - 16) / 2);
                                gui.renderItemDecorations(Minecraft.getInstance().font, stack,
                                        left + (OPTION_SIZE - 16) / 2, top + (OPTION_SIZE - 16) / 2);
                            }
                        }

                        pose.popPose();
                    }

                    pose.popPose();
                }
            }

            if (clipX2 > clipX1 && clipY2 > clipY1) {
                gui.disableScissor();
            }

            // --- текст и тултипы ---
            Font font = Minecraft.getInstance().font;
            String scaleText = String.format("%.0f%%", scale * 100);

            // Для подсчёта total trees и activeTreeId используем рефлексию на SkillTreeRenderer
            int totalTrees = getTotalTreesReflect();
            int activeTreeId = getActiveTreeIdReflect();

            String displayName = (String) getFieldValue(treeObj, "displayName");
            if (displayName == null) displayName = "Tree";

            String treeName = displayName + " (Tab " + (activeTreeId + 1) + "/" + Math.max(1, totalTrees) + ")";

            int textX = clipX1 + 5;
            int textY = clipY1 + 5;

            int w1 = font.width(scaleText);
            int w2 = font.width(treeName);
            int maxW = Math.max(w1, w2);

            gui.fill(textX - 2, textY - 2, textX + maxW + 2, textY + font.lineHeight * 2 + 4, 0xAA000000);
            gui.drawString(font, treeName, textX, textY, 0xFFFFFFAA, true);
            gui.drawString(font, scaleText, textX, textY + font.lineHeight + 2, 0xFFFFFFFF, true);

            PoseStack tooltipPose = gui.pose();
            tooltipPose.pushPose();
            tooltipPose.translate(0, 0, 200);

            for (SkillTreeNode n : nodes.values()) {
                if (n.containsPoint(unscaledMouseX, unscaledMouseY)) {
                    String key = "damagecore.skilltree.node." + n.id;
                    String text = net.minecraft.network.chat.Component.translatable(key).getString();
                    if (n.locked) {
                        text += " (" + net.minecraft.network.chat.Component.translatable("damagecore.skilltree.locked").getString() + ")";
                    }
                    gui.drawString(font, text, mouseX + 10, mouseY + 6, 0xFFFFFFFF, false);
                    break;
                }
            }
            tooltipPose.popPose();

        } catch (Throwable t) {
            // Если что-то идёт не так (рефлексия, изменения в SkillTreeRenderer и т.п.) —
            // печатаем стэк и не ломаем GUI.
            t.printStackTrace();
        }
    }

    // ----------------- Рефлексия / вспомогательные методы -----------------

    private static Object invokePrivateGetCurrentTree() {
        try {
            Class<?> cls = Class.forName("ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer");
            Method m = cls.getDeclaredMethod("getCurrentTree");
            m.setAccessible(true);
            return m.invoke(null);
        } catch (NoSuchMethodException e) {
            // fallback: попробуем получить trees + activeTreeId
            try {
                Class<?> cls = Class.forName("ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer");
                Field treesField = cls.getDeclaredField("trees");
                Field activeField = cls.getDeclaredField("activeTreeId");
                treesField.setAccessible(true);
                activeField.setAccessible(true);
                Object treesObj = treesField.get(null);
                int activeId = activeField.getInt(null);
                if (treesObj instanceof java.util.Map) {
                    java.util.Map<?, ?> trees = (java.util.Map<?, ?>) treesObj;
                    return trees.get(activeId);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static void invokePrivateCalculateAndUpdatePositions(Object treeObj, int panelScreenX, int panelScreenY) {
        try {
            Class<?> renderClass = Class.forName("ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer");
            Method m = renderClass.getDeclaredMethod("calculateAndUpdatePositions", treeObj.getClass(), int.class, int.class);
            m.setAccessible(true);
            m.invoke(null, treeObj, panelScreenX, panelScreenY);
        } catch (NoSuchMethodException e) {
            // Если приватный метод отсутствует (или подпись изменилась), просто пропускаем.
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) return null;
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(obj);
        } catch (NoSuchFieldException nsf) {
            // пробуем у супер-класса (на случай внутренних реализаций)
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

    private static int getTotalTreesReflect() {
        try {
            Class<?> cls = Class.forName("ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer");
            Field f = cls.getDeclaredField("trees");
            f.setAccessible(true);
            Object treesObj = f.get(null);
            if (treesObj instanceof java.util.Map) {
                return ((java.util.Map<?, ?>) treesObj).size();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return 0;
    }

    private static int getActiveTreeIdReflect() {
        try {
            Class<?> cls = Class.forName("ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer");
            Field f = cls.getDeclaredField("activeTreeId");
            f.setAccessible(true);
            return f.getInt(null);
        } catch (Throwable t) {
            // ignore
        }
        return 0;
    }

    private static int[] optionCenterForIndex(SkillTreeNode node, int index) {
        try {
            List<?> opts = (List<?>) getFieldValue(node, "options");
            int n = (opts == null) ? 0 : opts.size();
            if (n == 0) return new int[]{node.centerX(), node.centerY()};

            int radius = OPTION_BASE_RADIUS + Math.max(0, n - 1) * OPTION_RADIUS_STEP;
            double angle = 2.0 * Math.PI * index / n;
            int cx = node.centerX() + (int) Math.round(radius * Math.cos(angle));
            int cy = node.centerY() + (int) Math.round(radius * Math.sin(angle));
            return new int[]{cx, cy};
        } catch (Throwable t) {
            t.printStackTrace();
            return new int[]{node.centerX(), node.centerY()};
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
            gui.fill(Math.min(x1, x2), y1 - thickness / 2,
                    Math.max(x1, x2), y1 + thickness / 2 + 1, color);
            return;
        }

        if (x1 == x2) {
            gui.fill(x1 - thickness / 2, Math.min(y1, y2),
                    x1 + thickness / 2 + 1, Math.max(y1, y2), color);
            return;
        }

        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));

        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int px = Math.round(x1 + t * dx);
            int py = Math.round(y1 + t * dy);

            gui.fill(px - thickness / 2, py - thickness / 2,
                    px + thickness / 2 + 1, py + thickness / 2 + 1, color);
        }
    }
}