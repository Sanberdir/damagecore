package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import ru.imaginaerum.damagecore.Config;
import ru.imaginaerum.damagecore.api.damage_book_protection.ModNetwork;
import ru.imaginaerum.damagecore.hud.net.ThirstDamagePacket;

public class ThirstBarElement {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("damagecore", "textures/hud/damage_core_hud.png");

    private static final int TEXTURE_W = 160;
    private static final int TEXTURE_H = 208;

    // Значок жажды
    private static final int ICON_SRC_X = 16;
    private static final int ICON_SRC_Y = 109;
    private static final int ICON_W     = 18;
    private static final int ICON_H     = 20;

    // Заполненная часть
    private static final int FILL_SRC_X = 20;
    private static final int FILL_SRC_Y = 109;
    private static final int FILL_W     = 16;
    private static final int FILL_H     = 10;

    // Пустая часть
    private static final int EMPTY_SRC_X = 0;
    private static final int EMPTY_SRC_Y = 156;
    private static final int EMPTY_W     = 16;
    private static final int EMPTY_H     = 10;

    private static final int BAR_OFFSET_X = 2;
    private static final int BAR_OFFSET_Y = 0;

    // --- Система жажды ---
    public static float thirst = 20f;
    public static final float MAX_THIRST = 20f;

    private static int   tickTimer  = 0;
    private static float drainAccum = 0f;

    public static void tick(Minecraft mc) {
        if (mc.player == null) return;
        if (!Config.enableThirst) return;
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

        float diffMult = switch (mc.player.level().getDifficulty()) {
            case EASY   -> 1f / (1.5f * 1.5f);
            case NORMAL -> 1f / 1.5f;
            default     -> 1f;
        };

        drainAccum += drain * diffMult * Config.thirstDrainMultiplier;
        if (drainAccum >= 1f) {
            drainAccum -= 1f;
            thirst = Math.max(0f, thirst - 1f);
            if (thirst <= 0f) {
                ModNetwork.CHANNEL.sendToServer(new ThirstDamagePacket());
            }
        }
    }

    public static void drink(float amount) {
        thirst = Math.min(MAX_THIRST, thirst + amount);
    }

    public static void render(GuiGraphics gui, Minecraft mc) {
        if (mc.player == null) return;
        if (!Config.enableThirst) return;
        int screenH    = mc.getWindow().getGuiScaledHeight();
        int screenW    = mc.getWindow().getGuiScaledWidth();
        int hotbarLeft = screenW / 2 - 50;
        int heartsY    = screenH - 49;

        int screenX = hotbarLeft - 38 + 18 - 3 - 4;
        int screenY = heartsY + 7;

        // 1. Иконка (как у hunger — сдвиг текстуры на 1 вверх)
        gui.blit(TEXTURE,
                screenX, screenY,
                ICON_SRC_X, ICON_SRC_Y - 1,
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

        // 3. Заполненная часть (как у hunger)
        int fillH = Math.round(FILL_H * (thirst / MAX_THIRST));
        if (fillH > 0) {
            int cut = FILL_H - fillH;

            int fillSrcY = FILL_SRC_Y + cut - 1;

            int screenFillX = barX + 2;
            int screenFillY = barY + cut;

            gui.blit(TEXTURE,
                    screenFillX,
                    screenFillY,
                    FILL_SRC_X,
                    fillSrcY,
                    FILL_W,
                    fillH,
                    TEXTURE_W,
                    TEXTURE_H);
        }
    }
}