package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer;
import ru.imaginaerum.damagecore.mixin.InventoryScreenMixin;

@Mod.EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {
    @SubscribeEvent
    public static void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        System.out.println("[DamageCore] Client logged in — FULL RESET before requesting sync");

        // Полная очистка состояния (деревья + кэш)
        SkillTreeRenderer.clearAllCaches();      // очистит деревья и вызовет SkillTreeClientSync.clearCache()
        // (дополнительно) если есть отдельный метод сброса нод — вызови его
        // SkillTreeRenderer.resetAllNodes();

        ClientSyncState.syncRequested = false;

        // Теперь безопасно запросить новый синк
        if (Minecraft.getInstance().player != null) {
            ModNetwork.CHANNEL.sendToServer(new RequestFullSyncPacket());
            ClientSyncState.syncRequested = true;
            System.out.println("[DamageCore] RequestFullSyncPacket sent");
        }
    }

    @SubscribeEvent
    public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        System.out.println("[DamageCore] Client logged out — clearing everything");
        SkillTreeRenderer.clearAllCaches();
        ClientSyncState.syncRequested = false;
    }
}

