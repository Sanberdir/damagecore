package ru.imaginaerum.damagecore.library_extra_slots;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "damagecore")
public class ExtraSlotCloneHandler {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // Если хотите ТЕРЯТЬ содержимое слотов при смерти — оставьте return
            // Если хотите СОХРАНЯТЬ через смерть — уберите это условие
        }

        event.getOriginal().getCapability(ExtraSlotCapability.INSTANCE).ifPresent(oldData -> {
            event.getEntity().getCapability(ExtraSlotCapability.INSTANCE).ifPresent(newData -> {
                CompoundTag tag = oldData.serializeNBT();
                newData.deserializeNBT(tag);
            });
        });
    }
}