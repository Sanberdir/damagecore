package ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Вспомогательные методы рисования, вынесённые из Render.
 * Реализация методов скопирована точно.
 */
public class RenderDrawUtils {
    private static final int OPTION_BASE_RADIUS = 36; // базовый радиус от центра ноды до центра ячейки опции
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

    public static final float Z_NODE_TOP = 450f;  // копия ноды при вариантах — ниже тултипов, но выше опций

    private static final float Z_LINES        = 100f; // линии связей
    private static final float Z_OPTIONS_BG   = 330f; // круглая подложка вариантов
    private static final float Z_NODE         = 50f;  // основная нода (под опциями)
    private static final float Z_OPTIONS      = 400f; // ячейки вариантов (под копией ноды)
    // Тултипы — ВСЕ части рисуются выше Z_NODE_TOP
    private static final float Z_TOOLTIP_DESC_BG   = 460f;  // фон описания (нижний слой тултипа)
    private static final float Z_TOOLTIP_DESC_TEXT = 470f;  // текст описания
    private static final float Z_TOOLTIP_TITLE_BG  = 480f;  // фон заголовка тултипа
    private static final float Z_TOOLTIP_TITLE_TEXT= 490f;  // текст заголовка тултипа
    // --- вспомогательные упрощения, не трогающие логику ---
    public static void blitTex(GuiGraphics gui, ResourceLocation tex, int x, int y, int u, int v, int w, int h) {
        gui.blit(tex, x, y, u, v, w, h, 512, 512);
    }
    public static void drawNodeDimmed(GuiGraphics gui, SkillTreeNode n) {
        int frameSize = SkillTreeNode.FRAME_SIZE;
        int padding   = SkillTreeNode.FRAME_PADDING;
        int ITEM_SIZE = 16;

        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(0, 0, Z_NODE);

        // рамка
        gui.fill(n.x, n.y, n.x + frameSize, n.y + frameSize, 0xFF222222);

        // внутренняя часть затемнена
        gui.fill(n.x + padding, n.y + padding, n.x + frameSize - padding, n.y + frameSize - padding, 0xAA444444);

        int itemX = n.x + (frameSize - ITEM_SIZE) / 2;
        int itemY = n.y + (frameSize - ITEM_SIZE) / 2;
        gui.renderItem(n.itemStack, itemX, itemY);
        gui.renderItemDecorations(Minecraft.getInstance().font, n.itemStack, itemX, itemY);

        pose.popPose();
    }
    public static void drawScaledTooltipAt(GuiGraphics gui,
                                           Font font,
                                           int centerX,
                                           int centerY,
                                           String title,
                                           String desc,
                                           int pivotX,
                                           int pivotY,
                                           float scale) {

        final int TEXT_MAX_PIXELS = 220;

        List<String> titleLines = splitStringToPixelWidth(font, title, TEXT_MAX_PIXELS);
        List<String> descLines  = splitStringToPixelWidth(font, desc, TEXT_MAX_PIXELS);

        if (titleLines.isEmpty() && descLines.isEmpty()) return;

        int frame = SkillTreeNode.FRAME_SIZE;

        int maxWidth = 0;
        for (String s : titleLines) maxWidth = Math.max(maxWidth, font.width(s));
        for (String s : descLines)  maxWidth = Math.max(maxWidth, font.width(s));

        int cellLeft   = centerX - frame / 2;
        int cellRight  = centerX + frame / 2;
        int cellCenterY = centerY;

        int stripLeft = cellLeft - TOOLTIP_LEFT_OVERHANG;
        int textStartX = cellRight + 4;

        int stripWidth = (textStartX - stripLeft) + maxWidth + TOOLTIP_RIGHT_PAD;

        int titleHeight = Math.max(TOOLTIP_SRC_H, titleLines.size() * font.lineHeight + 6);
        int descHeight = descLines.isEmpty() ? 0 : Math.max(TOOLTIP_SRC_H, descLines.size() * font.lineHeight + 4);

        int titleTop = cellCenterY - titleHeight / 2;
        int descTop = titleTop + titleHeight - 4;

        PoseStack pose = gui.pose();

        if (descHeight > 0) {
            pose.pushPose();
            pose.translate(pivotX, pivotY, Z_TOOLTIP_DESC_BG);
            pose.scale(scale, scale, 1f);
            pose.translate(-pivotX, -pivotY, 0);
            drawNineSliceTiled(gui, TOOLTIP_TEXTURE, stripLeft, descTop, stripWidth, descHeight + 6,
                    TOOLTIP_DESC_SRC_U, TOOLTIP_DESC_SRC_V, TOOLTIP_SRC_W, TOOLTIP_SRC_H, TOOLTIP_CAP);
            pose.popPose();
        }

        pose.pushPose();
        pose.translate(pivotX, pivotY, Z_TOOLTIP_TITLE_BG);
        pose.scale(scale, scale, 1f);
        pose.translate(-pivotX, -pivotY, 0);
        drawNineSliceTiled(gui, TOOLTIP_TEXTURE, stripLeft, titleTop, stripWidth, titleHeight,
                TOOLTIP_TITLE_SRC_U, TOOLTIP_TITLE_SRC_V, TOOLTIP_SRC_W, TOOLTIP_SRC_H, TOOLTIP_CAP);
        pose.popPose();

        if (!descLines.isEmpty()) {
            pose.pushPose();
            pose.translate(pivotX, pivotY, Z_TOOLTIP_DESC_TEXT);
            pose.scale(scale, scale, 1f);
            pose.translate(-pivotX, -pivotY, 0);
            int descTextY = descTop + DESC_PADDING + 4;
            for (String s : descLines) gui.drawString(font, s, stripLeft + 4, descTextY, 0xFFE0E0E0, false);
            pose.popPose();
        }

        pose.pushPose();
        pose.translate(pivotX, pivotY, Z_TOOLTIP_TITLE_TEXT);
        pose.scale(scale, scale, 1f);
        pose.translate(-pivotX, -pivotY, 0);
        int titleTextY = titleTop + (titleHeight - titleLines.size() * font.lineHeight) / 2;
        for (String s : titleLines) gui.drawString(font, s, textStartX, titleTextY, 0xFFFFFFFF, true);
        pose.popPose();
    }

    // --- добавляем вспомогательный класс, положи его в начало RenderDrawUtils после объявления класса ---
    public static class OptionHoverInfo {
        public final SkillTreeNode node;
        public final SkillTreeNode.Variant variant; // <--- ссылка на вариант (может быть null)
        public final int optionIndex;
        public final int centerX;
        public final int centerY;

        public OptionHoverInfo(SkillTreeNode node, SkillTreeNode.Variant variant, int optionIndex, int centerX, int centerY) {
            this.node = node;
            this.variant = variant;
            this.optionIndex = optionIndex;
            this.centerX = centerX;
            this.centerY = centerY;
        }
    }

    /**
     * Рисует опции узла по кругу.
     * Возвращает OptionHoverInfo если над одной из опций сейчас hover (иначе возвращает null).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static OptionHoverInfo drawOptions(GuiGraphics gui,
                                              SkillTreeNode node,
                                              Object treeObj,
                                              int mouseX,
                                              int mouseY) {
        try {
            if (node == null || node.locked) return null;

            List<?> opts = (List<?>) getFieldValue(node, "options");
            if (opts == null || opts.isEmpty()) return null;

            final int OPTION_SIZE = 18;
            final int ITEM_SIZE = 16;

            OptionHoverInfo hoveredInfo = null;

            int selectedOption = -1;
            try {
                Object so = getFieldValue(node, "selectedOption");
                if (so instanceof Number n) selectedOption = n.intValue();
            } catch (Throwable ignored) {}

            PoseStack pose = gui.pose();

            // подложка под варианты
            final int BG_U = 224;
            final int BG_V = 176;
            final int BG_SIZE = 80;
            int bgLeft = node.centerX() - BG_SIZE / 2;
            int bgTop  = node.centerY() - BG_SIZE / 2;

            pose.pushPose();
            pose.translate(0, 0, Z_OPTIONS_BG);
            blitTex(gui, TOOLTIP_TEXTURE, bgLeft, bgTop, BG_U, BG_V, BG_SIZE, BG_SIZE);
            pose.popPose();

            // варианты
            for (int i = 0; i < opts.size(); i++) {

                int[] pos = optionCenterForIndex(node, i);
                int ox = pos[0], oy = pos[1];

                boolean hovered =
                        mouseX >= ox - OPTION_SIZE / 2 &&
                                mouseX <  ox + OPTION_SIZE / 2 &&
                                mouseY >= oy - OPTION_SIZE / 2 &&
                                mouseY <  oy + OPTION_SIZE / 2;

                int left = ox - OPTION_SIZE / 2;
                int top  = oy - OPTION_SIZE / 2;

                pose.pushPose();
                pose.translate(0, 0, Z_OPTIONS);

                gui.fill(left, top, left + OPTION_SIZE, top + OPTION_SIZE, 0xFF333333);

                int innerColor;
                if (selectedOption == i) innerColor = 0xFFFFFFFF;
                else if (hovered) innerColor = 0xAAFFFFFF;
                else innerColor = 0xFF777777;

                gui.fill(left + 1, top + 1,
                        left + OPTION_SIZE - 1,
                        top + OPTION_SIZE - 1,
                        innerColor);

                int border = 0x88FFFFFF;
                gui.fill(left, top, left + OPTION_SIZE, top + 1, border);
                gui.fill(left, top + OPTION_SIZE - 1, left + OPTION_SIZE, top + OPTION_SIZE, border);
                gui.fill(left, top, left + 1, top + OPTION_SIZE, border);
                gui.fill(left + OPTION_SIZE - 1, top, left + OPTION_SIZE, top + OPTION_SIZE, border);

                Object opt = opts.get(i);
                if (opt instanceof net.minecraft.world.item.ItemStack stack) {
                    int itemX = left + (OPTION_SIZE - ITEM_SIZE) / 2;
                    int itemY = top  + (OPTION_SIZE - ITEM_SIZE) / 2;
                    gui.renderItem(stack, itemX, itemY);
                    gui.renderItemDecorations(Minecraft.getInstance().font, stack, itemX, itemY);
                }

                if (hovered) {
                    gui.fill(left + 1, top + 1, left + OPTION_SIZE - 1, top + OPTION_SIZE - 1, 0x33FFFFFF);
                }

                pose.popPose();

                // перерисовка ноды поверх
                pose.pushPose();
                pose.translate(0, 0, Z_NODE_TOP);
                drawNode(gui, node, mouseX, mouseY);
                pose.popPose();

                // если hover — применяем вариант
                if (hovered && hoveredInfo == null) {
                    node.applyVariant(i);
                    node.optionsVisible = false;

                    SkillTreeNode.Variant variant = null;
                    if (node.variants != null && i < node.variants.size()) {
                        variant = node.variants.get(i);
                    }
                    hoveredInfo = new OptionHoverInfo(node, variant, i, ox, oy);
                }
            }

            return hoveredInfo;

        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
    public static List<String> splitLongWord(Font font, String w, int maxWidth) {
        List<String> parts = new ArrayList<>();
        if (w == null || w.isEmpty()) return parts;
        StringBuilder part = new StringBuilder();
        for (char c : w.toCharArray()) {
            if (font.width(part.toString() + c) <= maxWidth) {
                part.append(c);
            } else {
                if (part.length() > 0) parts.add(part.toString());
                part = new StringBuilder();
                part.append(c);
            }
        }
        if (part.length() > 0) parts.add(part.toString());
        return parts;
    }

    public static List<String> splitStringToPixelWidth(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null) return lines;
        text = text.trim();
        if (text.isEmpty()) return lines;

        String[] words = text.split("\\s+");
        StringBuilder cur = new StringBuilder();

        for (String w : words) {
            if (cur.length() == 0) {
                if (font.width(w) > maxWidth) {
                    lines.addAll(splitLongWord(font, w, maxWidth));
                } else {
                    cur.append(w);
                }
            } else {
                String candidate = cur.toString() + ' ' + w;
                if (font.width(candidate) <= maxWidth) {
                    cur.append(' ').append(w);
                } else {
                    lines.add(cur.toString());
                    if (font.width(w) > maxWidth) {
                        lines.addAll(splitLongWord(font, w, maxWidth));
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
    public static void drawNineSliceTiled(GuiGraphics gui, ResourceLocation tex,
                                          int destX, int destY, int destW, int destH,
                                          int srcU, int srcV, int srcW, int srcH,
                                          int cap) {

        if (destW <= 0 || destH <= 0) return;

        // если слишком маленький прямоугольник — просто рисуем как есть
        if (destW <= cap * 2 || destH <= cap * 2) {
            blitTex(gui, tex, destX, destY, srcU, srcV, destW, destH);
            return;
        }

        int innerSrcW = srcW - cap * 2;
        int innerSrcH = srcH - cap * 2;

        // --- УГЛЫ ---
        blitTex(gui, tex, destX, destY, srcU, srcV, cap, cap); // TL
        blitTex(gui, tex, destX + destW - cap, destY, srcU + srcW - cap, srcV, cap, cap); // TR
        blitTex(gui, tex, destX, destY + destH - cap, srcU, srcV + srcH - cap, cap, cap); // BL
        blitTex(gui, tex, destX + destW - cap, destY + destH - cap, srcU + srcW - cap, srcV + srcH - cap, cap, cap); // BR

        // --- ВЕРХ И НИЗ (тайл по X) ---
        int x = destX + cap;
        while (x < destX + destW - cap) {
            int tileW = Math.min(innerSrcW, destX + destW - cap - x);

            blitTex(gui, tex, x, destY, srcU + cap, srcV, tileW, cap); // верх
            blitTex(gui, tex, x, destY + destH - cap, srcU + cap, srcV + srcH - cap, tileW, cap); // низ

            x += tileW;
        }

        // --- ЛЕВО И ПРАВО (тайл по Y) ---
        int y = destY + cap;
        while (y < destY + destH - cap) {
            int tileH = Math.min(innerSrcH, destY + destH - cap - y);

            blitTex(gui, tex, destX, y, srcU, srcV + cap, cap, tileH); // лево
            blitTex(gui, tex, destX + destW - cap, y, srcU + srcW - cap, srcV + cap, cap, tileH); // право

            y += tileH;
        }

        // --- ЦЕНТР (тайл по X и Y) ---
        y = destY + cap;
        while (y < destY + destH - cap) {

            int tileH = Math.min(innerSrcH, destY + destH - cap - y);
            x = destX + cap;

            while (x < destX + destW - cap) {
                int tileW = Math.min(innerSrcW, destX + destW - cap - x);

                blitTex(gui, tex, x, y, srcU + cap, srcV + cap, tileW, tileH);

                x += tileW;
            }

            y += tileH;
        }
    }

    public static void drawScaledTooltip(GuiGraphics gui, Font font, SkillTreeNode n,
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
            pose.translate(pivotX, pivotY, Z_TOOLTIP_DESC_BG);
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
        pose.translate(pivotX, pivotY, Z_TOOLTIP_TITLE_BG);
        pose.scale(scale, scale, 1f);
        pose.translate(-pivotX, -pivotY, 0);

        drawNineSliceTiled(gui, TOOLTIP_TEXTURE,
                stripLeft, titleTop,
                stripWidth, titleHeight,
                TOOLTIP_TITLE_SRC_U, TOOLTIP_TITLE_SRC_V,
                TOOLTIP_SRC_W, TOOLTIP_SRC_H,
                TOOLTIP_CAP);

        pose.popPose();

        // --- Текст описания (тот же З, что и фон описания) ---
        if (!descLines.isEmpty()) {
            pose.pushPose();
            pose.translate(pivotX, pivotY, Z_TOOLTIP_DESC_TEXT);
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
        pose.translate(pivotX, pivotY, Z_TOOLTIP_TITLE_TEXT);
        pose.scale(scale, scale, 1f);
        pose.translate(-pivotX, -pivotY, 0);

        int titleTextY = titleTop + (titleHeight - titleLines.size() * font.lineHeight) / 2;
        for (String s : titleLines) {
            gui.drawString(font, s, textStartX, titleTextY, 0xFFFFFFFF, true);
            titleTextY += font.lineHeight;
        }

        pose.popPose();
    }

    /**
     * Рисует опции узла по кругу
     */


    /**
     * Рисует отдельный узел
     */
    public static void drawNode(GuiGraphics gui,
                                SkillTreeNode n,
                                int mouseX,
                                int mouseY) {

        int frameSize = SkillTreeNode.FRAME_SIZE;
        int padding   = SkillTreeNode.FRAME_PADDING;
        int ITEM_SIZE = 16;

        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(0, 0, Z_NODE);

        // рамка
        gui.fill(n.x, n.y,
                n.x + frameSize,
                n.y + frameSize,
                0xFF333333);

        boolean hovered = n.containsPoint(mouseX, mouseY);
        int innerColor = hovered
                ? (n.locked ? 0xFF444444 : 0xAAFFFFFF)
                : 0xFF777777;

        gui.fill(n.x + padding,
                n.y + padding,
                n.x + frameSize - padding,
                n.y + frameSize - padding,
                innerColor);

        // Используем itemStack ноды (уже обновлённый вариант)
        int itemX = n.x + (frameSize - ITEM_SIZE) / 2;
        int itemY = n.y + (frameSize - ITEM_SIZE) / 2;
        gui.renderItem(n.itemStack, itemX, itemY);
        gui.renderItemDecorations(Minecraft.getInstance().font,
                n.itemStack, itemX, itemY);

        pose.popPose();
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

    private static int[] optionCenterForIndex(SkillTreeNode node, int index) {
        try {
            List<?> opts = (List<?>) getFieldValue(node, "options");
            int n = (opts == null) ? 0 : opts.size();
            if (n == 0) return new int[]{node.centerX(), node.centerY()};

            int radius = OPTION_BASE_RADIUS;
            double angle = 2.0 * Math.PI * index / n;
            int cx = node.centerX() + (int) Math.round(radius * Math.cos(angle));
            int cy = node.centerY() + (int) Math.round(radius * Math.sin(angle));
            return new int[]{cx, cy};
        } catch (Throwable t) {
            t.printStackTrace();
            return new int[]{node.centerX(), node.centerY()};
        }
    }

    public static void drawThickLine(
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