package ru.imaginaerum.damagecore.libraty_effects;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.damagecore.DamageCore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = DamageCore.MODID)
public class FoodProtectionCapability {
    public static final Capability<FoodProtectionManager> FOOD_PROTECTION =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation FOOD_PROTECTION_ID =
            new ResourceLocation(DamageCore.MODID, "food_protection");

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(FoodProtectionManager.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(FOOD_PROTECTION_ID, new FoodProtectionProvider((Player) event.getObject()));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            event.player.getCapability(FOOD_PROTECTION).ifPresent(FoodProtectionManager::tick);
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Item item = event.getItem().getItem();
        List<FoodProtectionReloadListener.Effect> defs = FoodProtectionReloadListener.EFFECTS.get(item);

        if (defs == null || defs.isEmpty()) return;

        // Снимок mob effects ДО добавления наших эффектов
        // (после поедания ванильные эффекты уже применены к игроку)
        List<MobEffectInstance> vanillaMobEffects = new ArrayList<>();
        if (item.getFoodProperties(event.getItem(), player) != null) {
            for (var pair : item.getFoodProperties(event.getItem(), player).getEffects()) {
                MobEffectInstance inst = player.getEffect(pair.getFirst().getEffect());
                if (inst != null) {
                    vanillaMobEffects.add(new MobEffectInstance(inst)); // копия
                }
            }
        }

        player.getCapability(FoodProtectionCapability.FOOD_PROTECTION).ifPresent(manager -> {
            for (var def : defs) {
                manager.addEffect(new FoodProtectionEffect(
                        item,
                        def.damageType(),
                        def.protection(),
                        def.duration(),
                        vanillaMobEffects  // ← передаём mob effects
                ));

                if (def.overrideVanilla()) {
                    for (ResourceLocation effLoc : def.removeEffects()) {
                        try {
                            MobEffect mob = BuiltInRegistries.MOB_EFFECT.get(effLoc);
                            if (mob != null) player.removeEffect(mob);
                        } catch (Exception ex) { }
                    }
                }
            }

            if (player.level().isClientSide) {
                player.displayClientMessage(
                        Component.literal("§eПолучена защита от еды (см. Damage Book)"),
                        true
                );
            }
        });
    }


    public static FoodProtectionManager get(Player player) {
        return player.getCapability(FOOD_PROTECTION).orElse(null);
    }
}

class FoodProtectionProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final FoodProtectionManager manager;
    private final LazyOptional<FoodProtectionManager> optional;

    public FoodProtectionProvider(Player player) {
        this.manager = new FoodProtectionManager(player);
        this.optional = LazyOptional.of(() -> manager);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return FoodProtectionCapability.FOOD_PROTECTION.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return manager.save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        manager.load(nbt);
    }
}