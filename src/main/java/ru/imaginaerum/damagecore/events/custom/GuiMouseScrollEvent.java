package ru.imaginaerum.damagecore.events.custom;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable // опционально — чтобы подписчики могли отменить дальнейшую обработку
public class GuiMouseScrollEvent extends Event {
    private final Screen screen;
    private final double scrollDelta;
    private final double mouseX;
    private final double mouseY;

    public GuiMouseScrollEvent(Screen screen, double scrollDelta, double mouseX, double mouseY) {
        this.screen = screen;
        this.scrollDelta = scrollDelta;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public Screen getScreen() { return screen; }
    public double getScrollDelta() { return scrollDelta; }
    public double getMouseX() { return mouseX; }
    public double getMouseY() { return mouseY; }
}
