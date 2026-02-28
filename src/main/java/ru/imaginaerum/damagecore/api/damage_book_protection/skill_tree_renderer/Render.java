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

    private static final int OPTION_SIZE = 18; // размер ячейки опции (внутри масштабирования)
    private static final int OPTION_BASE_RADIUS = 28; // базовый радиус от центра ноды до центра ячейки опции
    private static final int OPTION_RADIUS_STEP = 8;  // дополнительный шаг радиуса при росте числа опций

    private static int currentPanelScreenX = 0;
    private static int currentPanelScreenY = 0;

    private static final ResourceLocation TOOLTIP_TEXTURE = new ResourceLocation("damagecore", "textures/gui/container/creative_inventory/damage_core_interface.png");
    // координаты в текстуре (по условию)
    private static final int TOOLTIP_TITLE_SRC_U = 0;
    private static final int TOOLTIP_TITLE_SRC_V = 252;
    private static final int TOOLTIP_DESC_SRC_U  = 0;
    private static final int TOOLTIP_DESC_SRC_V  = 294;
    private static final int TOOLTIP_SRC_W = 200;
    private static final int TOOLTIP_SRC_H = 20;
    private static final int TOOLTIP_CAP = 4; // непроходимые края (px), которые не растягиваются
    private static final int TOOLTIP_LEFT_OVERHANG = 4; // полоска выступает влево от ячейки на 4px
    private static final int TOOLTIP_RIGHT_PAD = 4;    // справа полоска заканчивается на 4px правее текста
    private static final int DESC_PADDING = 3; // отступ текста внутри полоски для описания

    private static List<String> splitStringToPixelWidth(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null) return lines;
        text = text.trim();
        if (text.isEmpty()) return lines;

        String[] words = text.split("\\s+");
        StringBuilder cur = new StringBuilder();

        for (String w : words) {
            if (cur.length() == 0) {
                // если само слово длиннее чем maxWidth, разрежем его по символам
                if (font.width(w) > maxWidth) {
                    StringBuilder part = new StringBuilder();
                    for (char c : w.toCharArray()) {
                        if (font.width(part.toString() + c) <= maxWidth) {
                            part.append(c);
                        } else {
                            if (part.length() > 0) lines.add(part.toString());
                            part = new StringBuilder();
                            part.append(c);
                        }
                    }
                    if (part.length() > 0) {
                        // продолжим как обычное слово (cur останется пустым)
                        lines.add(part.toString());
                    }
                } else {
                    cur.append(w);
                }
            } else {
                String candidate = cur.toString() + ' ' + w;
                if (font.width(candidate) <= maxWidth) {
                    cur.append(' ').append(w);
                } else {
                    lines.add(cur.toString());
                    // если слово само по себе длинное — разрежем его
                    if (font.width(w) > maxWidth) {
                        StringBuilder part = new StringBuilder();
                        for (char c : w.toCharArray()) {
                            if (font.width(part.toString() + c) <= maxWidth) {
                                part.append(c);
                            } else {
                                if (part.length() > 0) lines.add(part.toString());
                                part = new StringBuilder();
                                part.append(c);
                            }
                        }
                        if (part.length() > 0) lines.add(part.toString());
                        cur = new StringBuilder();
                    } else {
                        cur = new StringBuilder(w);
                    }
                }
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    /**
     * Рисует горизонтально растягиваемую "полосу" из текстуры: left cap, middle (растягивается), right cap.
     * srcW/ srcH — размеры исходной полосы в текстуре; capPx — ширина нерастягиваемых краёв (левый/правый).
     */
    private static void drawNineSliceTiled(GuiGraphics gui, ResourceLocation tex,
                                           int destX, int destY, int destW, int destH,
                                           int srcU, int srcV, int srcW, int srcH,
                                           int cap) {

        if (destW <= 0 || destH <= 0) return;

        // если слишком маленький прямоугольник — просто рисуем как есть
        if (destW <= cap * 2 || destH <= cap * 2) {
            gui.blit(tex, destX, destY, srcU, srcV, destW, destH, 512, 512);
            return;
        }

        int innerSrcW = srcW - cap * 2;
        int innerSrcH = srcH - cap * 2;

        // --- УГЛЫ ---
        // TL
        gui.blit(tex, destX, destY, srcU, srcV, cap, cap, 512, 512);
        // TR
        gui.blit(tex, destX + destW - cap, destY,
                srcU + srcW - cap, srcV,
                cap, cap, 512, 512);
        // BL
        gui.blit(tex, destX, destY + destH - cap,
                srcU, srcV + srcH - cap,
                cap, cap, 512, 512);
        // BR
        gui.blit(tex, destX + destW - cap, destY + destH - cap,
                srcU + srcW - cap, srcV + srcH - cap,
                cap, cap, 512, 512);

        // --- ВЕРХ И НИЗ (тайл по X) ---
        int x = destX + cap;
        while (x < destX + destW - cap) {
            int tileW = Math.min(innerSrcW, destX + destW - cap - x);

            // верх
            gui.blit(tex, x, destY,
                    srcU + cap, srcV,
                    tileW, cap,
                    512, 512);

            // низ
            gui.blit(tex, x, destY + destH - cap,
                    srcU + cap, srcV + srcH - cap,
                    tileW, cap,
                    512, 512);

            x += tileW;
        }

        // --- ЛЕВО И ПРАВО (тайл по Y) ---
        int y = destY + cap;
        while (y < destY + destH - cap) {
            int tileH = Math.min(innerSrcH, destY + destH - cap - y);

            // лево
            gui.blit(tex, destX, y,
                    srcU, srcV + cap,
                    cap, tileH,
                    512, 512);

            // право
            gui.blit(tex, destX + destW - cap, y,
                    srcU + srcW - cap, srcV + cap,
                    cap, tileH,
                    512, 512);

            y += tileH;
        }

        // --- ЦЕНТР (тайл по X и Y) ---
        y = destY + cap;
        while (y < destY + destH - cap) {

            int tileH = Math.min(innerSrcH, destY + destH - cap - y);
            x = destX + cap;

            while (x < destX + destW - cap) {
                int tileW = Math.min(innerSrcW, destX + destW - cap - x);

                gui.blit(tex, x, y,
                        srcU + cap, srcV + cap,
                        tileW, tileH,
                        512, 512);

                x += tileW;
            }

            y += tileH;
        }
    }

    private static void drawScaledTooltip(GuiGraphics gui, Font font, SkillTreeNode n,
                                          String title, String desc, int pivotX, int pivotY, float scale) {
        final int TEXT_MAX_PIXELS = 220;

        List<String> titleLines = splitStringToPixelWidth(font, title, TEXT_MAX_PIXELS);
        List<String> descLines  = splitStringToPixelWidth(font, desc, TEXT_MAX_PIXELS);

        if (titleLines.isEmpty() && descLines.isEmpty()) return;

        int frame = SkillTreeNode.FRAME_SIZE;

        int maxWidth = 0;
        for (String s : titleLines) maxWidth = Math.max(maxWidth, font.width(s));
        for (String s : descLines)  maxWidth = Math.max(maxWidth, font.width(s));

        int cellLeft   = n.x;
        int cellRight  = n.x + frame;
        int cellCenterY = n.y + frame / 2;

        int stripLeft = cellLeft - TOOLTIP_LEFT_OVERHANG;
        int textStartX = cellRight + 4;

        int stripWidth = (textStartX - stripLeft) + maxWidth + TOOLTIP_RIGHT_PAD;

        int titleHeight = Math.max(TOOLTIP_SRC_H,
                titleLines.size() * font.lineHeight + 6);

        // описание растянуто на 4px
        int descHeight = descLines.isEmpty() ? 0 :
                Math.max(TOOLTIP_SRC_H,
                        descLines.size() * font.lineHeight + 4);

        int titleTop = cellCenterY - titleHeight / 2;

        // подтянуть описание под заголовок
        int descTop = titleTop + titleHeight - 4;

        PoseStack pose = gui.pose();

        // --- Рисуем фон описания (НИЖЕ по Z) ---
        if (descHeight > 0) {
            pose.pushPose();
            pose.translate(pivotX, pivotY, 440);
            pose.scale(scale, scale, 1f);
            pose.translate(-pivotX, -pivotY, 0);

            drawNineSliceTiled(gui, TOOLTIP_TEXTURE,
                    stripLeft, descTop,
                    stripWidth, descHeight + 6, // растянули фон на 4 пикселя
                    TOOLTIP_DESC_SRC_U, TOOLTIP_DESC_SRC_V,
                    TOOLTIP_SRC_W, TOOLTIP_SRC_H,
                    TOOLTIP_CAP);

            pose.popPose();
        }

        // --- Рисуем фон заголовка (ВЫШЕ по Z) ---
        pose.pushPose();
        pose.translate(pivotX, pivotY, 460);
        pose.scale(scale, scale, 1f);
        pose.translate(-pivotX, -pivotY, 0);

        drawNineSliceTiled(gui, TOOLTIP_TEXTURE,
                stripLeft, titleTop,
                stripWidth, titleHeight,
                TOOLTIP_TITLE_SRC_U, TOOLTIP_TITLE_SRC_V,
                TOOLTIP_SRC_W, TOOLTIP_SRC_H,
                TOOLTIP_CAP);

        pose.popPose();

        // --- Текст описания (тот же Z, что и фон описания) ---
        if (!descLines.isEmpty()) {
            pose.pushPose();
            pose.translate(pivotX, pivotY, 445);
            pose.scale(scale, scale, 1f);
            pose.translate(-pivotX, -pivotY, 0);

            int descTextY = descTop + DESC_PADDING + 4; // смещаем текст на 2 пикселя
            for (String s : descLines) {
                gui.drawString(font, s, stripLeft + 4, descTextY, 0xFFE0E0E0, false);
                descTextY += font.lineHeight;
            }

            pose.popPose();
        }

        // --- Текст заголовка (тот же Z, что и фон заголовка) ---
        pose.pushPose();
        pose.translate(pivotX, pivotY, 470);
        pose.scale(scale, scale, 1f);
        pose.translate(-pivotX, -pivotY, 0);

        int titleTextY = titleTop + (titleHeight - titleLines.size() * font.lineHeight) / 2;
        for (String s : titleLines) {
            gui.drawString(font, s, textStartX, titleTextY, 0xFFFFFFFF, false);
            titleTextY += font.lineHeight;
        }

        pose.popPose();
    }

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

                drawThickLine(gui,
                        parent.centerX(), parent.centerY(),
                        child.centerX(), child.centerY(),
                        2, 0xFF000000);
            }
            pose.popPose();

            int unscaledMouseX = (int) ((mouseX - pivotX) / scale + pivotX);
            int unscaledMouseY = (int) ((mouseY - pivotY) / scale + pivotY);

            // --- найти hovered ---
            SkillTreeNode hoveredNode = null;
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

            final int ITEM_SIZE = 16;

            for (SkillTreeNode n : nodes.values()) {
                if (n == hoveredNode) continue;

                // Рисуем базовую ноду
                drawNode(gui, n, unscaledMouseX, unscaledMouseY);

                // Рисуем опции, если это активный узел
                if (activeOptionsNodeId != null && activeOptionsNodeId.equals(n.id)) {
                    drawOptions(gui, n, treeObj, unscaledMouseX, unscaledMouseY);
                }
            }

            pose.popPose();

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

                drawScaledTooltip(
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
            // =============================
            // 4) КОПИЯ HOVERED НОДЫ (САМЫЙ ВЕРХ, Z=1000)
            // =============================
            if (hoveredNode != null) {
                pose.pushPose();
                pose.translate(pivotX, pivotY, 1000);
                pose.scale(scale, scale, 1f);
                pose.translate(-pivotX, -pivotY, 0);

                // Рисуем hovered ноду
                drawNode(gui, hoveredNode, unscaledMouseX, unscaledMouseY);

                // Если hovered нода - это активный узел с опциями, рисуем их поверх
                if (activeOptionsNodeId != null && activeOptionsNodeId.equals(hoveredNode.id)) {
                    drawOptions(gui, hoveredNode, treeObj, unscaledMouseX, unscaledMouseY);
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

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Рисует опции узла по кругу
     */
    private static void drawOptions(GuiGraphics gui,
                                    SkillTreeNode node,
                                    Object treeObj,
                                    int mouseX,
                                    int mouseY) {
        try {
            List<?> opts = (List<?>) getFieldValue(node, "options");
            if (opts == null || opts.isEmpty()) return;

            final int ITEM_SIZE = 16;

            PoseStack pose = gui.pose();
            pose.pushPose();
            pose.translate(0, 0, 250); // Z выше, чем предметы ноды

            for (int i = 0; i < opts.size(); i++) {
                int[] pos = optionCenterForIndex(node, i);
                int ox = pos[0], oy = pos[1];

                // Проверяем hover для опции
                boolean optionHovered = (mouseX >= ox - OPTION_SIZE / 2 &&
                        mouseX < ox + OPTION_SIZE / 2 &&
                        mouseY >= oy - OPTION_SIZE / 2 &&
                        mouseY < oy + OPTION_SIZE / 2);

                int selectedOption = -1;
                try {
                    Object so = getFieldValue(node, "selectedOption");
                    if (so instanceof Number) selectedOption = ((Number) so).intValue();
                } catch (Throwable ignored) {}

                int bgColor;
                if (selectedOption >= 0 && selectedOption == i) {
                    bgColor = 0xFFFFFFFF;
                } else if (optionHovered) {
                    bgColor = 0xAAFFFFFF;
                } else {
                    bgColor = 0xDD555555;
                }

                int left = Math.round(ox - OPTION_SIZE / 2.0f);
                int top = Math.round(oy - OPTION_SIZE / 2.0f);
                gui.fill(left, top, left + OPTION_SIZE, top + OPTION_SIZE, bgColor);

                Object opt = opts.get(i);
                if (opt instanceof net.minecraft.world.item.ItemStack item) {
                    int itemOX = Math.round(ox - ITEM_SIZE / 2.0f);
                    int itemOY = Math.round(oy - ITEM_SIZE / 2.0f);
                    gui.renderItem(item, itemOX, itemOY);
                    gui.renderItemDecorations(Minecraft.getInstance().font, item, itemOX, itemOY);
                }
            }

            pose.popPose();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Рисует отдельный узел
     */
    private static void drawNode(GuiGraphics gui,
                                 SkillTreeNode n,
                                 int mouseX,
                                 int mouseY) {

        int frameSize = SkillTreeNode.FRAME_SIZE;
        int padding = SkillTreeNode.FRAME_PADDING;
        int ITEM_SIZE = 16;

        // Рамка узла
        gui.fill(n.x, n.y, n.x + frameSize, n.y + frameSize, 0xFF333333);

        boolean hovered = n.containsPoint(mouseX, mouseY);
        int innerColor = hovered
                ? (n.locked ? 0xFF444444 : 0xAAFFFFFF)
                : 0xFF777777;

        // Внутренняя заливка
        gui.fill(n.x + padding,
                n.y + padding,
                n.x + frameSize - padding,
                n.y + frameSize - padding,
                innerColor);

        // Иконка предмета
        int itemX = Math.round(n.x + (frameSize - ITEM_SIZE) / 2f);
        int itemY = Math.round(n.y + (frameSize - ITEM_SIZE) / 2f);

        gui.renderItem(n.itemStack, itemX, itemY);
        gui.renderItemDecorations(Minecraft.getInstance().font,
                n.itemStack, itemX, itemY);
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