package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class ArmorStatsBarRenderer {

    static final int TEX_W  = 200;
    static final int TEX_H  = 20;
    static final int EDGE   = 2;

    private ArmorStatsBarRenderer() {}

    public static void drawStretchBar(GuiGraphics gui, ResourceLocation tex,
                                      int x, int y, int width, int height) {
        int mw = width  - EDGE * 2;
        int mh = height - EDGE * 2;

        // Углы
        gui.blit(tex, x, y, 0,0, EDGE, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE + mw, y,        TEX_W-EDGE, 0,          EDGE, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x,          y + EDGE + mh, 0,        TEX_H-EDGE, EDGE, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE + mw, y + EDGE + mh, TEX_W-EDGE, TEX_H-EDGE, EDGE, EDGE, TEX_W, TEX_H);

        // Стороны
        gui.blit(tex, x + EDGE, y,              EDGE, 0,          mw, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE, y + EDGE + mh,  EDGE, TEX_H-EDGE, mw, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x,        y + EDGE,        0,    EDGE,       EDGE, mh, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE + mw, y + EDGE,  TEX_W-EDGE, EDGE, EDGE, mh, TEX_W, TEX_H);

        // Центр
        gui.blit(tex, x + EDGE, y + EDGE, EDGE, EDGE, mw, mh, TEX_W, TEX_H);
    }

    public static void drawWindowWithTile(GuiGraphics gui, ResourceLocation tex,
                                          int x, int y, int width, int height) {
        int mw = width  - EDGE * 2;
        int mh = height - EDGE * 2;

        // Углы
        gui.blit(tex, x,          y,            0,          0,          EDGE, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE + mw, y,          TEX_W-EDGE, 0,          EDGE, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x,          y + EDGE + mh, 0,          TEX_H-EDGE, EDGE, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE + mw, y + EDGE + mh, TEX_W-EDGE, TEX_H-EDGE, EDGE, EDGE, TEX_W, TEX_H);

        // Горизонтальные стороны
        gui.blit(tex, x + EDGE, y,             EDGE, 0,          mw, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE, y + EDGE + mh, EDGE, TEX_H-EDGE, mw, EDGE, TEX_W, TEX_H);

        // Вертикальные стороны с тайлингом
        int vth = TEX_H - EDGE * 2;
        int vRep = (mh + vth - 1) / vth;
        for (int i = 0; i < vRep; i++) {
            int tH = Math.min(vth, mh - i * vth);
            gui.blit(tex, x,          y + EDGE + i * vth, 0,          EDGE, EDGE, tH, TEX_W, TEX_H);
            gui.blit(tex, x + EDGE + mw, y + EDGE + i * vth, TEX_W-EDGE, EDGE, EDGE, tH, TEX_W, TEX_H);
        }

        // Центр с тайлингом
        int ctw = TEX_W - EDGE * 2;
        int cth = TEX_H - EDGE * 2;
        int hRep = (mw + ctw - 1) / ctw;
        for (int i = 0; i < vRep; i++) {
            int tH = Math.min(cth, mh - i * cth);
            for (int j = 0; j < hRep; j++) {
                int tW = Math.min(ctw, mw - j * ctw);
                gui.blit(tex, x + EDGE + j * ctw, y + EDGE + i * cth,
                        EDGE, EDGE, tW, tH, TEX_W, TEX_H);
            }
        }
    }
}