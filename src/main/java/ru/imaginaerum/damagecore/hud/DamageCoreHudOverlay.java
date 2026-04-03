package ru.imaginaerum.damagecore.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
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
        ThirstBarElement.tick(mc);   // в onRenderGui перед рендером
        ThirstBarElement.render(gui, mc);
        HungerBarElement.render(gui, mc);
        HudBase.render(gui);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            float health = player.getHealth();
            float maxHealth = player.getMaxHealth(); // обычно 20
            float percent = health / maxHealth;
            HealthBarElement.render(gui, percent);
        }
        ManaBarElement.render(gui);
        StaminaBarElement.render(gui);
        EffectIconsElement.render(gui, mc);
    }
}