package ru.imaginaerum.damagecore.animation_attack.types;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.library_weapon_types.WeaponTypeManager;

@Mod.EventBusSubscriber(modid = DamageCore.MODID)
public class ReloadListeners {
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(WeaponTypeManager.INSTANCE);
        event.addListener(WeaponAnimationManager.INSTANCE);
    }
}