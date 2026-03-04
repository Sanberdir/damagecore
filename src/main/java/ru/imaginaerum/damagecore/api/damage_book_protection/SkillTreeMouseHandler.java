package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer.Render;

@Mod.EventBusSubscriber(modid = "damagecore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SkillTreeMouseHandler {

    @SubscribeEvent
    public static void onGuiMouseScroll(ScreenEvent.MouseScrolled event) {
        // ДОБАВИТЬ: проверка что это нужный экран
        if (!(event.getScreen() instanceof InventoryScreen)) return;

        Minecraft mc = Minecraft.getInstance();

        // Получаем актуальные координаты панели из DamageBookRenderer
        int panelScreenX = 100; // TODO: получить реальные координаты
        int panelScreenY = 100;

        boolean used = SkillTreeRenderer.mouseScrolled(
                (int) event.getMouseX(),
                (int) event.getMouseY(),
                event.getScrollDelta(),
                panelScreenX,
                panelScreenY
        );

        if (used) {
            event.setCanceled(true);

            // ДОБАВИТЬ: принудительное обновление рендера
            // SkillTreeRenderer.forceRecalculate();
        }
    }
}