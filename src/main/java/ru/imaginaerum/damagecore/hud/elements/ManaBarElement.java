package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.hud.DamageCoreHudOverlay;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatsType;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ManaBarElement {

    private static final int TEXTURE_BAR_WIDTH = 32;
    private static final int EDGE_WIDTH = 6;

    private static final int BAR_X = 47;
    private static final int BAR_Y = 25;
    private static final int BAR_W = 26;
    private static final int BAR_H = 6;

    private static final int TEXTURE_X       = 0;
    private static final int TEXTURE_Y_EMPTY = 72;

    private static final int FILLED_TEX_X = 2;
    private static final int FILLED_TEX_Y = 55;
    private static final int FILLED_TEX_W = 28;
    private static final int FILLED_TEX_H = 4;

    private static final int HUD_TEXTURE_WIDTH  = 160;
    private static final int HUD_TEXTURE_HEIGHT = 208;

    public static final float BASE_MANA  = 10f;
    private static final float REGEN_PER_TICK = 0.02f; // в секунду ~0.4 маны (20 тиков)

    private static float currentMana = BASE_MANA;

    public static float getMaxMana() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return BASE_MANA;
        return PlayerStatsCapability.get(mc.player)
                .map(stats -> BASE_MANA + stats.getStat(StatsType.MIND) * 0.5f)
                .orElse(BASE_MANA);
    }

    public static float getMana()            { return currentMana; }
    public static void  setMana(float value) { currentMana = Math.max(0, Math.min(value, getMaxMana())); }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) return;

        float maxMana = getMaxMana();

        if (mc.player.isCreative() || mc.player.isSpectator()) {
            currentMana = maxMana;
            return;
        }

        // Обрезаем если максимум уменьшился (например убрали уровень MIND)
        if (currentMana > maxMana) {
            currentMana = maxMana;
        }

        // Регенерация
        if (currentMana < maxMana) {
            currentMana = Math.min(currentMana + REGEN_PER_TICK, maxMana);
        }
    }

    public static void render(GuiGraphics gui) {
        float maxMana    = getMaxMana();
        float widthScale = maxMana / BASE_MANA;
        int scaledBarW   = Math.round(BAR_W * widthScale);

        renderEmptyBar(gui, BAR_X, BAR_Y, scaledBarW, BAR_H, TEXTURE_X, TEXTURE_Y_EMPTY);

        float fraction = maxMana > 0 ? currentMana / maxMana : 0f;
        int maxFilledW = scaledBarW - 4;
        int filledW    = Math.round(maxFilledW * fraction);

        if (filledW > 0) {
            renderFilledBar(gui,
                    BAR_X + 2,
                    BAR_Y + 1,
                    filledW,
                    BAR_H - 2,
                    FILLED_TEX_X,
                    FILLED_TEX_Y,
                    maxFilledW);
        }
    }

    static void renderEmptyBar(GuiGraphics gui, int barX, int barY, int barW, int barH,
                               int texX, int texY) {
        int drawX = barX;

        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY,
                EDGE_WIDTH, barH,
                texX, texY,
                EDGE_WIDTH, barH,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        drawX += EDGE_WIDTH;

        int midWidth = barW - EDGE_WIDTH * 2;
        int midTexX  = texX + EDGE_WIDTH;
        int midTexW  = TEXTURE_BAR_WIDTH - EDGE_WIDTH * 2;
        if (midWidth > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    midWidth, barH,
                    midTexX, texY,
                    midTexW, barH,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
            drawX += midWidth;
        }

        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY,
                EDGE_WIDTH, barH,
                texX + TEXTURE_BAR_WIDTH - EDGE_WIDTH, texY,
                EDGE_WIDTH, barH,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
    }

    static void renderFilledBar(GuiGraphics gui, int barX, int barY, int barW, int barH,
                                int texX, int texY, int maxFilledW) {
        int drawX = barX;
        int filledEdgeWidth = Math.min(EDGE_WIDTH, FILLED_TEX_W / 2);
        int midTexW = FILLED_TEX_W - filledEdgeWidth * 2;

        // Левый край
        int leftW = Math.min(filledEdgeWidth, barW);
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                drawX, barY,
                leftW, barH,
                texX, texY,
                leftW, FILLED_TEX_H,
                HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        drawX += leftW;
        barW  -= leftW;
        if (barW <= 0) return;

        int distanceFromEnd  = maxFilledW - (leftW + barW);
        int rightEdgeVisible = Math.max(0,
                Math.min(filledEdgeWidth, filledEdgeWidth - distanceFromEnd));

        // Середина
        int midScreenW = Math.max(0, barW - rightEdgeVisible);
        if (midScreenW > 0 && midTexW > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    midScreenW, barH,
                    texX + filledEdgeWidth, texY,
                    midTexW, FILLED_TEX_H,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
            drawX += midScreenW;
            barW  -= midScreenW;
        }

        // Правый скос
        if (rightEdgeVisible > 0 && barW > 0) {
            gui.blit(DamageCoreHudOverlay.HUD_TEXTURE,
                    drawX, barY,
                    rightEdgeVisible, barH,
                    texX + FILLED_TEX_W - filledEdgeWidth, texY,
                    rightEdgeVisible, FILLED_TEX_H,
                    HUD_TEXTURE_WIDTH, HUD_TEXTURE_HEIGHT);
        }
    }
}