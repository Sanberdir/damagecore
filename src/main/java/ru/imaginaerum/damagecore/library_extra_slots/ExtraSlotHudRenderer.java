package ru.imaginaerum.damagecore.library_extra_slots;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT)
public class ExtraSlotHudRenderer {

    private static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/widgets.png");

    // ==== РЕГУЛИРОВКА РАМОК ПО X ====
    private static final int SHIELD_FRAME_OFFSET_X = 22;
    private static final int SLOT0_FRAME_OFFSET_X = 4;
    private static final int FRAME_OFFSET_Y = -1;

    // ==== РЕГУЛИРОВКА ПОЛОЖЕНИЯ САМИХ ЯЧЕЕК ПО X ====
    private static final int SHIELD_POSITION_OFFSET_X = -4; // щит на 2 пикселя левее
    private static final int SLOT1_POSITION_OFFSET_X = 4;   // слот 1 на 2 пикселя правее

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        IExtraSlot data = player.getCapability(ExtraSlotCapability.INSTANCE).orElse(null);
        if (data == null) return; // combat mode больше не проверяем — HUD всегда виден

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int i = screenWidth / 2;

        int bgY = screenHeight - 23;
        int itemY = screenHeight - 16 - 3;

        boolean combatMode = data.isCombatMode();

        // ---- ЩИТ (слот 2) ----
        ItemStack shieldStack = data.getHandler().getStackInSlot(2);
        HumanoidArm offhandSide = player.getMainArm().getOpposite();

        int shieldBgX, shieldItemX;
        if (offhandSide == HumanoidArm.LEFT) {
            shieldBgX = i - 91 - 29 - 21 + SHIELD_POSITION_OFFSET_X;
            shieldItemX = i - 91 - 26 - 21 + SHIELD_POSITION_OFFSET_X;
            guiGraphics.blit(WIDGETS_LOCATION, shieldBgX, bgY, 24, 22, 29, 24);
        } else {
            shieldBgX = i + 91 + 29 + SHIELD_POSITION_OFFSET_X;
            shieldItemX = i + 91 + 10 + 29 + SHIELD_POSITION_OFFSET_X;
            guiGraphics.blit(WIDGETS_LOCATION, shieldBgX, bgY, 53, 22, 29, 24);
        }

        if (combatMode) {
            drawSelectionFrame(guiGraphics, shieldBgX, bgY, SHIELD_FRAME_OFFSET_X);
        }

        if (!shieldStack.isEmpty()) {
            guiGraphics.renderItem(shieldStack, shieldItemX, itemY);
            guiGraphics.renderItemDecorations(mc.font, shieldStack, shieldItemX, itemY);
        }

        // ---- СЛОТ 0 и СЛОТ 1 ----
        int slot0BgX = i + 91;
        int slot0ItemX = slot0BgX + 10;

        int slot1BgX = slot0BgX + 21 + SLOT1_POSITION_OFFSET_X;
        int slot1ItemX = slot1BgX + 10;

        renderExtraCell(guiGraphics, mc, data.getHandler().getStackInSlot(0), slot0BgX, slot0ItemX, bgY, itemY, combatMode, SLOT0_FRAME_OFFSET_X);
        renderExtraCell(guiGraphics, mc, data.getHandler().getStackInSlot(1), slot1BgX, slot1ItemX, bgY, itemY, false, 0);
    }

    private static void renderExtraCell(GuiGraphics guiGraphics, Minecraft mc, ItemStack stack,
                                        int bgX, int itemX, int bgY, int itemY,
                                        boolean selected, int frameOffsetX) {
        guiGraphics.blit(WIDGETS_LOCATION, bgX, bgY, 53, 22, 29, 24);

        if (selected) {
            drawSelectionFrame(guiGraphics, bgX, bgY, frameOffsetX);
        }

        if (!stack.isEmpty()) {
            guiGraphics.renderItem(stack, itemX, itemY);
            guiGraphics.renderItemDecorations(mc.font, stack, itemX, itemY);
        }
    }

    private static void drawSelectionFrame(GuiGraphics guiGraphics, int bgX, int bgY, int offsetX) {
        int frameX = bgX + (29 - 24) / 2 + offsetX;
        int frameY = bgY + (24 - 22) / 2 + FRAME_OFFSET_Y;
        guiGraphics.blit(WIDGETS_LOCATION, frameX, frameY, 0, 22, 24, 22);
    }
}