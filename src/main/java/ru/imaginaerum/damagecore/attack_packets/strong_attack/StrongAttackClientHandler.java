package ru.imaginaerum.damagecore.attack_packets.strong_attack;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.animation_attack.IExampleAnimatedPlayer;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.attack_packets.KeyBindings;
import ru.imaginaerum.damagecore.hud.elements.StaminaManager;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StrongAttackClientHandler {

    private static boolean wasPressed = false;
    private static long lastSwingTime = 0L;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        boolean isPressed = KeyBindings.STRONG_ATTACK_KEY.isDown();

        if (isPressed && !wasPressed) {
            long now = System.currentTimeMillis();
            if (now - lastSwingTime >= StrongAttackPacket.COOLDOWN_MS) {

                // Блокируем если истощён или стамины меньше 6
                if (StaminaManager.isExhausted() || StaminaManager.getStamina() < 6.0f) return;

                lastSwingTime = now;
                if (mc.player instanceof IExampleAnimatedPlayer animated) {
                    animated.requestStrongAttack();
                }

                mc.player.swing(InteractionHand.MAIN_HAND);
                ModNetwork.CHANNEL.sendToServer(new StrongAttackPacket());
            }
        }

        wasPressed = isPressed;
    }
}