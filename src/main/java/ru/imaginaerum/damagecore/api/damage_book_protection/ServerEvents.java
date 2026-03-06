package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;

@Mod.EventBusSubscriber(modid = "damagecore", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            System.out.println("Player logged in, sending full sync"); // ДОБАВИТЬ
            SkillTreeServerHandler.sendFullSyncToPlayer(sp);
        }
    }
}
