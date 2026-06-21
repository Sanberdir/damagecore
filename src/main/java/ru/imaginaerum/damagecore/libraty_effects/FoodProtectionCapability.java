package ru.imaginaerum.damagecore.libraty_effects;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import org.jetbrains.annotations.Nullable;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.api.ModNetwork;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        if (event.getObject() instanceof Player player) {
            event.addCapability(FOOD_PROTECTION_ID, new FoodProtectionProvider(player));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Тикаем на обеих сторонах — клиент сам отсчитывает время
        event.player.getCapability(FOOD_PROTECTION).ifPresent(manager -> {
            manager.tick();

            // Синхронизируем только с сервера раз в секунду
            if (!event.player.level().isClientSide && event.player.tickCount % 20 == 0) {
                syncToClient((ServerPlayer) event.player);
            }
        });
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return; // обрабатываем только на сервере

        Item item = event.getItem().getItem();
        List<FoodProtectionReloadListener.Effect> defs = FoodProtectionReloadListener.EFFECTS.get(item);
        if (defs == null || defs.isEmpty()) return;

        List<MobEffectInstance> vanillaMobEffects = new ArrayList<>();
        if (item.getFoodProperties(event.getItem(), player) != null) {
            for (var pair : item.getFoodProperties(event.getItem(), player).getEffects()) {
                MobEffectInstance inst = player.getEffect(pair.getFirst().getEffect());
                if (inst != null) {
                    vanillaMobEffects.add(new MobEffectInstance(inst));
                }
            }
        }

        player.getCapability(FOOD_PROTECTION).ifPresent(manager -> {
            // Сначала собираем все removeEffects со всех defs
            Set<ResourceLocation> allRemoved = new HashSet<>();
            for (var def : defs) {
                if (def.overrideVanilla()) {
                    allRemoved.addAll(def.removeEffects());
                }
            }

            // Фильтруем снимок — убираем те эффекты, которые будут удалены
            List<MobEffectInstance> filteredMobEffects = vanillaMobEffects.stream()
                    .filter(inst -> {
                        ResourceLocation loc = BuiltInRegistries.MOB_EFFECT
                                .getKey(inst.getEffect());
                        return loc == null || !allRemoved.contains(loc);
                    })
                    .toList();

            for (var def : defs) {
                manager.addEffect(new FoodProtectionEffect(
                        item,
                        def.damageType(),
                        def.protection(),
                        def.duration(),
                        filteredMobEffects  // ← уже без удалённых
                ));

                if (def.overrideVanilla()) {
                    for (ResourceLocation effLoc : def.removeEffects()) {
                        try {
                            MobEffect mob = BuiltInRegistries.MOB_EFFECT.get(effLoc);
                            if (mob != null) player.removeEffect(mob);
                        } catch (Exception ignored) {}
                    }
                }
            }

            syncToClient((ServerPlayer) player);
        });

        // Сообщение показываем через пакет — клиент получит данные и сам обновит GUI
        // Либо можно отправить отдельным пакетом, здесь убрали isClientSide-ветку
    }

    public static void syncToClient(ServerPlayer player) {
        player.getCapability(FOOD_PROTECTION).ifPresent(manager ->
                ModNetwork.CHANNEL.sendTo(
                        new FoodProtectionSyncPacket(manager.save()),
                        player.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT
                )
        );
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