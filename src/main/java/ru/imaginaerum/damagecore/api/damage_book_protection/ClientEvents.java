package ru.imaginaerum.damagecore.api.damage_book_protection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeRenderer;
import ru.imaginaerum.damagecore.api.implementation_skills.shooting.ClientHundredArmedData;
import ru.imaginaerum.damagecore.mixin.InventoryScreenMixin;

@Mod.EventBusSubscriber(modid = "damagecore", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {

        // Полная очистка состояния (деревья + кэш)
        SkillTreeRenderer.clearAllCaches();      // очистит деревья и вызовет SkillTreeClientSync.clearCache()
        // (дополнительно) если есть отдельный метод сброса нод — вызови его
        // SkillTreeRenderer.resetAllNodes();

        ClientSyncState.syncRequested = false;

        // Теперь безопасно запросить новый синк
        if (Minecraft.getInstance().player != null) {
            ModNetwork.CHANNEL.sendToServer(new RequestFullSyncPacket());
            ClientSyncState.syncRequested = true;
        }
    }


    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        ItemProperties.register(Items.BOW, new ResourceLocation("pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null) return 0.0F;
                    if (entity.getUseItem() != stack) return 0.0F;


                    float divisor = ClientHundredArmedData.hasSkill ? 10.0F : 20.0F;
                    float pull = (float)(entity.getTicksUsingItem()) / divisor;
                    return Math.min(pull, 1.0F);
                }
        );
    }

    // быстрое натяжение лука
    @SubscribeEvent
    public static void onHundredArmed(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(Items.BOW, new ResourceLocation("pull"),
                    (stack, level, entity, seed) -> {
                        if (entity == null) return 0.0F;
                        if (entity.getUseItem() != stack) return 0.0F;

                        float divisor = ClientHundredArmedData.hasSkill ? 5.0F : 20.0F;
                        float pull = (float)(entity.getTicksUsingItem()) / divisor;
                        return Math.min(pull, 1.0F);
                    }
            );
        });
    }
    @SubscribeEvent
    public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SkillTreeRenderer.clearAllCaches();
        ClientSyncState.syncRequested = false;
    }
}

