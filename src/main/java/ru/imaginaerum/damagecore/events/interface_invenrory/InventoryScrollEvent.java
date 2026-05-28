package ru.imaginaerum.damagecore.events.interface_invenrory;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.ISkillTreeAccessor;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;

@Mod.EventBusSubscriber(modid = "damagecore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class InventoryScrollEvent {

    @SubscribeEvent
    public static void onMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        if (!(screen instanceof ISkillTreeAccessor accessor) || !accessor.damagecore$isSkillTreeVisible()) return;

        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        double delta  = event.getScrollDelta();

        // ---- Список плюсиков: X97..X170, Y6..Y60 ----
        boolean overList = mouseX >= guiLeft + 97  && mouseX < guiLeft + 170
                && mouseY >= guiTop  + 6   && mouseY < guiTop  + 60;
        if (overList) {
            accessor.damagecore$scrollList(delta);
            event.setCanceled(true);
            return;
        }

        // ---- Дерево скиллов ----
        boolean consumed = SkillTreeRenderer.mouseScrolled(
                (int) mouseX, (int) mouseY, delta,
                guiLeft + imageWidth + 2, guiTop);
        if (consumed) event.setCanceled(true);
    }
}