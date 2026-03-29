package ru.imaginaerum.damagecore.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.hud.elements.*;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DamageCoreHudOverlay {

    public static final ResourceLocation HUD_TEXTURE =
            new ResourceLocation("damagecore", "textures/hud/damage_core_hud.png");

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        GuiGraphics gui = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.gameMode == null) return;

        GameType mode = mc.gameMode.getPlayerMode();
        if (mode != GameType.SURVIVAL && mode != GameType.ADVENTURE) return;

        RenderSystem.setShaderTexture(0, HUD_TEXTURE);

        StaminaBarElement.update(mc);

        HudBase.render(gui);
        HealthBarElement.render(gui, mc);
        ManaBarElement.render(gui);
        StaminaBarElement.render(gui);
        EffectIconsElement.render(gui, mc);
    }
}