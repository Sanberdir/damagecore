package ru.imaginaerum.damagecore.lybrary_extra_slots;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import ru.imaginaerum.damagecore.api.ModNetwork;

@Mod.EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT)
public class ExtraSlotKeyHandler {

    // Shift+E: не открываем инвентарь, а меняем местами 0 и 1 слоты
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof InventoryScreen && Screen.hasShiftDown()) {
            event.setCanceled(true);
            ModNetwork.CHANNEL.sendToServer(new SwapExtraSlotsPacket(SwapExtraSlotsPacket.Action.SWAP_0_1));
        }
    }

    // Shift+Q: гасим ванильный дроп предмета и вместо этого меняем слот щита (2) с оффхендом
    @SubscribeEvent
    public static void onRawKey(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        if (event.getKey() != GLFW.GLFW_KEY_Q) return;
        if (!Screen.hasShiftDown()) return;

        // сбрасываем состояние ванильного keyDrop, чтобы отмена дропа сработала до тика Minecraft
        KeyMapping dropKey = mc.options.keyDrop;
        InputConstants.Key inputKey = InputConstants.getKey(event.getKey(), event.getScanCode());
        KeyMapping.set(inputKey, false);
        dropKey.consumeClick();

        ModNetwork.CHANNEL.sendToServer(new SwapExtraSlotsPacket(SwapExtraSlotsPacket.Action.SWAP_2_OFFHAND));
    }
}