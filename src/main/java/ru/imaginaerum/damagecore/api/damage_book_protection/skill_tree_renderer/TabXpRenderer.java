package ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Утиль для отрисовки полоски опыта внутри GUI-вкладки дерева.
 *
 * Как использовать:
 *  - из DamageBookRenderer.drawSideTab / drawMiddleRow вызови:
 *      TabXpRenderer.renderXpBar(gui, tabX, tabY, tabW, tabH, isTopTab, progress);
 *
 *  - progress: 0.0f .. 1.0f (0%..100%)
 *
 * Принятые конвенции:
 *  - отступ от левого края вкладки = 3px
 *  - отступ от верхнего или нижнего края = 3px
 */
public final class TabXpRenderer {
    private TabXpRenderer() {}

    // текстура (совпадает с той, что ты используешь в DamageBookRenderer)
    private static final ResourceLocation DAMAGE_CORE_INTERFACE =
            new ResourceLocation("damagecore", "textures/gui/container/creative_inventory/damage_core_interface.png");

    // region в текстуре для полоски опыта
    private static final int XP_BAR_U = 176;
    private static final int XP_BAR_V = 214;
    private static final int XP_BAR_FULL_WIDTH = 31;
    private static final int XP_BAR_HEIGHT = 5;

    // отступы внутри вкладки
    private static final int LEFT_PADDING = 3;
    private static final int VERTICAL_PADDING = 3;

    /**
     * Нарисовать заполненную часть полоски опыта.
     *
     * @param gui       GuiGraphics для рисования
     * @param tabX      X координата левого верхнего угла вкладки (в screen координатах)
     * @param tabY      Y координата левого верхнего угла вкладки (в screen координатах)
     * @param tabWidth  ширина вкладки
     * @param tabHeight высота вкладки
     * @param topTab    true — вкладка верхняя, false — нижняя
     * @param progress  заполнение 0.0..1.0
     */
    public static void renderXpBar(GuiGraphics gui, int tabX, int tabY, int tabWidth, int tabHeight, boolean topTab, float progress) {
        if (progress <= 0f) return; // нечего рисовать

        // clamp progress
        if (progress < 0f) progress = 0f;
        if (progress > 1f) progress = 1f;

        int xpX = tabX + LEFT_PADDING;
        int xpY = topTab
                ? tabY + VERTICAL_PADDING
                : tabY + tabHeight - XP_BAR_HEIGHT - VERTICAL_PADDING;

        // вычисляем ширину заполненной части в текстурных пикселях
        int filledWidth = Math.max(1, Math.round(XP_BAR_FULL_WIDTH * progress));

        // Отрисовка: используем тот же ResourceLocation, что и интерфейс
        // (u, v, width, height) — рисуем только заполненную часть
        gui.blit(DAMAGE_CORE_INTERFACE, xpX, xpY, XP_BAR_U, XP_BAR_V, filledWidth, XP_BAR_HEIGHT);
    }

    /**
     * Вспомогательный метод: нарисовать пустую рамку/фон полоски и затем заполнение.
     * - полезно если хочешь всегда видеть контур полоски.
     *
     * Для этого нужен ещё один регион в текстуре (фон). Если у тебя в текстуре нет
     * отдельного фонового региона — этот метод всё равно нарисует только заполнение.
     *
     * @param gui       графика
     * @param tabX      левый верх вкладки
     * @param tabY      верх вкладки
     * @param tabW      ширина вкладки
     * @param tabH      высота вкладки
     * @param topTab    верхняя ли вкладка
     * @param progress  0..1
     */
    public static void renderXpBarWithBackground(GuiGraphics gui, int tabX, int tabY, int tabW, int tabH, boolean topTab, float progress) {
        // Если в твоей текстуре есть фон полоски в другом месте —
        // добавь константы и отрисуй фон тут перед заполнением.
        // Для простоты сейчас лишь вызываем renderXpBar (только заполнение).
        renderXpBar(gui, tabX, tabY, tabW, tabH, topTab, progress);
    }

    // ==== Пример "заглушки" как получить progress для конкретного дерева ====
    // Ниже — демонстрационная функция, она НЕ использует внутренних данных SkillTreeRenderer,
    // потому что их публичный API у тебя не предоставляет информацию о "learned" прямо.
    // Поэтому здесь просто пример: если в твоём коде есть способ получить learned/total — реализуй здесь.
    //
    // public static float getProgressForTree(int globalTreeId) {
    //     // Пример: caller может хранить прогресс в отдельной структуре и возвращать его здесь.
    //     return 0.34f; // 34% — временный заглушка
    // }
}
