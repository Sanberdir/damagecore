package ru.imaginaerum.damagecore.lybrary_extra_slots;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = "damagecore")
public class ExtraSlotEvents {

    public static final ResourceLocation ID =
            new ResourceLocation("damagecore", "extra_slot");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (!(event.getObject() instanceof Player)) return;

        event.addCapability(ID, new ICapabilitySerializable<CompoundTag>() {
            private final ExtraSlotImpl impl = new ExtraSlotImpl();
            private final LazyOptional<IExtraSlot> opt = LazyOptional.of(() -> impl);

            @Override
            public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ExtraSlotCapability.INSTANCE ? opt.cast() : LazyOptional.empty();
            }

            @Override
            public CompoundTag serializeNBT() {
                return impl.serializeNBT();
            }

            @Override
            public void deserializeNBT(CompoundTag nbt) {
                impl.deserializeNBT(nbt);
            }
        });
    }
}