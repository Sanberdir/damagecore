package ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
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

    private static int currentPanelScreenX = 0;
    private static int currentPanelScreenY = 0;

    // --- вспомогательные упрощения, не трогающие логику ---
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void render(GuiGraphics gui, InventoryScreen screen,
                              int panelScreenX, int panelScreenY,
                              int mouseX, int mouseY) {
        try {
            Object treeObj = invokePrivateGetCurrentTree();
            if (treeObj == null) return;

            Map<String, SkillTreeNode> nodes =
                    (Map<String, SkillTreeNode>) getFieldValue(treeObj, "nodes");
            if (nodes == null || nodes.isEmpty()) return;

            currentPanelScreenX = panelScreenX;
            currentPanelScreenY = panelScreenY;

            float scale = ((Number) getFieldValue(treeObj, "scale")).floatValue();
            boolean isDragging = Boolean.TRUE.equals(getFieldValue(treeObj, "isDragging"));

            // --- drag ---
            if (isDragging) {
                int dragStartOffsetX = ((Number) getFieldValue(treeObj, "dragStartOffsetX")).intValue();
                int dragStartOffsetY = ((Number) getFieldValue(treeObj, "dragStartOffsetY")).intValue();
                int dragStartX = ((Number) getFieldValue(treeObj, "dragStartX")).intValue();
                int dragStartY = ((Number) getFieldValue(treeObj, "dragStartY")).intValue();

                int newOffsetX = dragStartOffsetX + (int) ((mouseX - dragStartX) / scale);
                int newOffsetY = dragStartOffsetY + (int) ((mouseY - dragStartY) / scale);

                setFieldValue(treeObj, "offsetX", newOffsetX);
                setFieldValue(treeObj, "offsetY", newOffsetY);
            }

            invokePrivateCalculateAndUpdatePositions(treeObj, panelScreenX, panelScreenY);

            // --- clip area ---
            int clipX1 = panelScreenX + PANEL_DRAW_OFFSET_X_IN_PANEL;
            int clipY1 = panelScreenY + PANEL_DRAW_OFFSET_Y_IN_PANEL;
            int clipX2 = clipX1 + AREA_WIDTH;
            int clipY2 = clipY1 + AREA_HEIGHT;

            if (clipX2 > clipX1 && clipY2 > clipY1)
                gui.enableScissor(clipX1, clipY1, clipX2, clipY2);

            PoseStack pose = gui.pose();
            int pivotX = clipX1 + AREA_WIDTH / 2;
            int pivotY = clipY1 + AREA_HEIGHT / 2;

            // Получаем ID активного узла с опциями ДО отрисовки
            String activeOptionsNodeId = (String) getFieldValue(treeObj, "activeOptionsNodeId");

            // =============================
            // 1) ЛИНИИ (низ)
            // =============================
            pose.pushPose();
            pose.translate(pivotX, pivotY, 0);
            pose.scale(scale, scale, 1f);
            pose.translate(-pivotX, -pivotY, 0);

            for (SkillTreeNode child : nodes.values()) {
                if (child.parentId == null || "start".equalsIgnoreCase(child.parentId)) continue;
                SkillTreeNode parent = nodes.get(child.parentId);
                if (parent == null) continue;

                RenderDrawUtils.drawThickLine(gui,
                        parent.centerX(), parent.centerY(),
                        child.centerX(), child.centerY(),
                        2, 0xFF000000);
            }
            pose.popPose();

            int unscaledMouseX = (int) ((mouseX - pivotX) / scale + pivotX);
            int unscaledMouseY = (int) ((mouseY - pivotY) / scale + pivotY);

            // --- найти hovered ---
            SkillTreeNode hoveredNode = null;
            RenderDrawUtils.OptionHoverInfo hoveredOption = null;
            for (SkillTreeNode n : nodes.values()) {
                if (n.containsPoint(unscaledMouseX, unscaledMouseY)) {
                    hoveredNode = n;
                    break;
                }
            }

            // =============================
            // 2) ВСЕ НОДЫ (кроме hovered)
            // =============================
            pose.pushPose();
            pose.translate(pivotX, pivotY, 200);
            pose.scale(scale, scale, 1f);
            pose.translate(-pivotX, -pivotY, 0);

            for (SkillTreeNode n : nodes.values()) {
                if (n == hoveredNode) continue;

                // Рисуем базовую ноду
                RenderDrawUtils.drawNode(gui, n, unscaledMouseX, unscaledMouseY);

                // Рисуем опции, если это активный узел
                if (activeOptionsNodeId != null && activeOptionsNodeId.equals(n.id)) {
                    RenderDrawUtils.OptionHoverInfo hi = RenderDrawUtils.drawOptions(gui, n, treeObj, unscaledMouseX, unscaledMouseY);
                    if (hi != null) hoveredOption = hi;
                }
            }

            pose.popPose();


            // =============================
            // 4) КОПИЯ HOVERED НОДЫ (САМЫЙ ВЕРХ, Z=1000)
            // =============================
            if (hoveredNode != null) {
                pose.pushPose();
                pose.translate(pivotX, pivotY, 1000);
                pose.scale(scale, scale, 1f);
                pose.translate(-pivotX, -pivotY, 0);

                // Рисуем hovered ноду
                RenderDrawUtils.drawNode(gui, hoveredNode, unscaledMouseX, unscaledMouseY);

                // Если hovered нода - это активный узел с опциями, рисуем их поверх
                if (activeOptionsNodeId != null && activeOptionsNodeId.equals(hoveredNode.id)) {
                    RenderDrawUtils.OptionHoverInfo hi = RenderDrawUtils.drawOptions(gui, hoveredNode, treeObj, unscaledMouseX, unscaledMouseY);
                    if (hi != null) hoveredOption = hi;
                }

                pose.popPose();
            }

            // =============================
            // 5) ЗАТЕМНЕНИЕ, ЕСЛИ ОТКРЫТЫ ОПЦИИ (опционально)
            // =============================
            if (activeOptionsNodeId != null) {
                gui.fill(0, 0, screen.width, screen.height, 0x88000000); // полупрозрачный черный оверлей
            }
            if (clipX2 > clipX1 && clipY2 > clipY1)
                gui.disableScissor();
            // =============================
// 3A) ТУЛТИП ДЛЯ OPTION
// =============================
            if (hoveredOption != null) {

                Font font = Minecraft.getInstance().font;

                String baseKey =
                        "damagecore.skilltree.variant."
                                + hoveredOption.variant.id;

                String title = Component.translatable(baseKey).getString();
                String desc  = Component.translatable(baseKey + ".desc").getString();

                if (desc.equals(baseKey + ".desc")) {
                    desc = "";
                }

                RenderDrawUtils.drawScaledTooltipAt(
                        gui,
                        font,
                        hoveredOption.centerX,
                        hoveredOption.centerY,
                        title,
                        desc,
                        pivotX,
                        pivotY,
                        scale
                );
            }
            // =============================
            // 3) ТУЛТИП (Z=500)
            // =============================
            if (hoveredNode != null) {

                Font font = Minecraft.getInstance().font;

                String baseKey = "damagecore.skilltree.node." + hoveredNode.id;

                String title = Component.translatable(baseKey).getString();
                String desc  = Component.translatable(baseKey + ".desc").getString();

                // если описание не существует — не показываем его
                if (desc.equals(baseKey + ".desc")) {
                    desc = "";
                }

                RenderDrawUtils.drawScaledTooltip(
                        gui,
                        font,
                        hoveredNode,
                        title,
                        desc,
                        pivotX,
                        pivotY,
                        scale
                );
            }

        } catch (Throwable t) {
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
}