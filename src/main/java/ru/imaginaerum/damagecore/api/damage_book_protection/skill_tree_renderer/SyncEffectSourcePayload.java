package ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import ru.imaginaerum.damagecore.DamageCore;

import java.util.function.Supplier;

public record SyncEffectSourcePayload(MobEffect effect, EntityType<?> sourceType) {

    public static final ResourceLocation ID =
            new ResourceLocation(DamageCore.MODID, "sync_effect_source");

    public static void encode(SyncEffectSourcePayload msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(BuiltInRegistries.MOB_EFFECT.getKey(msg.effect));
        buf.writeResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(msg.sourceType));
    }

    public static SyncEffectSourcePayload decode(FriendlyByteBuf buf) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT
                .getOptional(buf.readResourceLocation()).orElse(null);
        EntityType<?> sourceType = BuiltInRegistries.ENTITY_TYPE
                .getOptional(buf.readResourceLocation()).orElse(null);
        return new SyncEffectSourcePayload(effect, sourceType);
    }

    public static void handle(SyncEffectSourcePayload msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (msg.effect() == null || msg.sourceType() == null) return;
            // ИСПРАВЛЕНО: вместо прямого вызова PotionTrackingClient используем прокси,
            // чтобы JVM не загружала клиентский класс на выделенном сервере.
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    SyncEffectSourceClientProxy.apply(msg.effect(), msg.sourceType())
            );
        });
        ctx.setPacketHandled(true);
    }
}