package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientDimensionEvents {

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Сбрасываем всё состояние skill tree
        SkillTreeRenderer.clearAllCaches(); // очищает деревья, кэши и активное дерево
        ClientSyncState.syncRequested = false;

        // Запрашиваем полную синхронизацию у сервера
        if (Minecraft.getInstance().player != null) {
            ModNetwork.CHANNEL.sendToServer(new RequestFullSyncPacket());
            ClientSyncState.syncRequested = true;
        }

        System.out.println("[DamageCore] Dimension changed — skill trees reset and sync requested");
    }
}