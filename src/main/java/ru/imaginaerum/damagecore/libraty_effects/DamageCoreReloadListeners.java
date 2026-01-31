package ru.imaginaerum.damagecore.libraty_effects;



import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;

@Mod.EventBusSubscriber(modid = DamageCore.MODID)
public class DamageCoreReloadListeners {

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new FoodProtectionReloadListener());
    }
}
