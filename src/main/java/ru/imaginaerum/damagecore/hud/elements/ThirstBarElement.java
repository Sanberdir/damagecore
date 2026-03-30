package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.api.damage_book_protection.ModNetwork;
import ru.imaginaerum.damagecore.hud.net.ThirstDamagePacket;

public class ThirstBarElement {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("damagecore", "textures/hud/requirement_line.png");

    private static final int TEXTURE_W = 80;
    private static final int TEXTURE_H = 48;

    // Позиция элемента
    private static final int ELEMENT_X = 38;
    private static final int ELEMENT_Y = 40;

    // Значок жажды: X32 Y12 → X49 Y32 (17×20px)
    private static final int ICON_SRC_X = 32;
    private static final int ICON_SRC_Y = 12;
    private static final int ICON_W     = 17;
    private static final int ICON_H     = 20;

    // Заполненная часть: X33 Y12 → X49 Y22 (16×10px)
    private static final int FILL_SRC_X = 33;
    private static final int FILL_SRC_Y = 12;
    private static final int FILL_W     = 16;
    private static final int FILL_H     = 10;

    // Пустая часть (та же что у голода): X0 Y38 → X16 Y48
    private static final int EMPTY_SRC_X = 0;
    private static final int EMPTY_SRC_Y = 38;
    private static final int EMPTY_W     = 16;
    private static final int EMPTY_H     = 10;

    private static final int BAR_OFFSET_X = 1;
    private static final int BAR_OFFSET_Y = 0;

    // --- Система жажды ---
    public static float thirst    = 20f;
    public static final float MAX_THIRST = 20f;

    // Накопленный шанс для случайного убывания


    private static int tickTimer = 0;
    private static float drainAccum = 0f;

    public static void tick(Minecraft mc) {
        if (mc.player == null) return;

        // Мирная сложность — восстанавливаем до максимума и не тратим
        if (mc.player.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            thirst = Math.min(MAX_THIRST, thirst + 1f);
            return;
        }

        tickTimer++;
        if (tickTimer < 20) return;
        tickTimer = 0;

        boolean sprinting = mc.player.isSprinting();
        boolean moving    = mc.player.zza != 0 || mc.player.xxa != 0;

        float drain = 0f;

        if (sprinting) {
            if (Math.random() < 0.5) drain = 0.05f;
        } else if (moving) {
            if (Math.random() < 0.5) drain = 0.025f;
        } else {
            drain = 1f / 450f;
        }

        drainAccum += drain;
        if (drainAccum >= 1f) {
            drainAccum -= 1f;
            thirst = Math.max(0f, thirst - 1f);
            if (thirst <= 0f) {
                ModNetwork.CHANNEL.sendToServer(new ThirstDamagePacket());
            }
        }
    }

    /**
     * Вызывается когда игрок пьёт:
     * - ПКМ по блоку воды
     * - Выпил Water Bottle
     * - Выпил зелье
     */
    public static void drink(float amount) {
        thirst = Math.min(MAX_THIRST, thirst + amount);
    }

    public static void render(GuiGraphics gui, Minecraft mc) {
        if (mc.player == null) return;

        int screenX = ELEMENT_X;
        int screenY = ELEMENT_Y;

        // 1. Иконка
        gui.blit(TEXTURE,
                screenX, screenY,
                ICON_SRC_X, ICON_SRC_Y,
                ICON_W, ICON_H,
                TEXTURE_W, TEXTURE_H);

        int barX = screenX + BAR_OFFSET_X;
        int barY = screenY + BAR_OFFSET_Y;

        // 2. Пустая полоска
        gui.blit(TEXTURE,
                barX, barY,
                EMPTY_SRC_X, EMPTY_SRC_Y,
                EMPTY_W, EMPTY_H,
                TEXTURE_W, TEXTURE_H);

        // 3. Заполненная (убывает сверху вниз)
        int fillH = Math.round(FILL_H * (thirst / MAX_THIRST));
        if (fillH > 0) {
            int cut = FILL_H - fillH;
            gui.blit(TEXTURE,
                    barX,       barY + cut,
                    FILL_SRC_X, FILL_SRC_Y + cut,
                    FILL_W,     fillH,
                    TEXTURE_W,  TEXTURE_H);
        }
    }
}