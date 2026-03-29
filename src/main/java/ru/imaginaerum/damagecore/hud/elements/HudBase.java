package ru.imaginaerum.damagecore.hud.elements;

import net.minecraft.client.gui.GuiGraphics;
import ru.imaginaerum.damagecore.hud.DamageCoreHudOverlay;

public class HudBase {

    public static void render(GuiGraphics gui) {
        gui.blit(DamageCoreHudOverlay.HUD_TEXTURE, 0, 0, 0, 0, 48, 48, 160, 208);
    }
}