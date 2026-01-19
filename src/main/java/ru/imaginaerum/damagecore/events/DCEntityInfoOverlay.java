package ru.imaginaerum.damagecore.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.entities.DCEntityType;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DCEntityInfoOverlay {
        // При релизе Удалить! Показывает типы сущностей при наведении
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof EntityHitResult ehr)) return;

        Entity entity = ehr.getEntity();
        if (!(entity instanceof LivingEntity)) return;

        DCEntityType type = DCEntityType.resolve(entity);

        GuiGraphics gg = event.getGuiGraphics();
        int x = mc.getWindow().getGuiScaledWidth() / 2 + 8;
        int y = mc.getWindow().getGuiScaledHeight() / 2 + 8;

        gg.drawString(
                mc.font,
                Component.literal("Тип: " + type.title),
                x,
                y,
                type.color,
                true
        );
    }
}
