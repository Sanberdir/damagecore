package ru.imaginaerum.damagecore.effect.replace_vanila;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.library_damage.DamageType;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = "damagecore")
public class VanillaResistanceOverrideHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        // Если у сущности есть ванильный эффект Resistance — применим нашу формулу.
        // (миксин уже предотвратил стандартное -20%/ур внутри hurt)
        if (!entity.hasEffect(MobEffects.DAMAGE_RESISTANCE)) return;
        MobEffectInstance inst = entity.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (inst == null) return;

        int level = inst.getAmplifier() + 1; // 0 -> level 1
        float perLevel = 0.10f; // 10% per level
        float totalReduction = perLevel * level;

        DamageSource source = event.getSource();
        DamageType dt = mapDamageSourceToDamageType(source);

        if (dt == null) return;

        if (dt == DamageType.PIERCING || dt == DamageType.SLASHING || dt == DamageType.FIRE || dt == DamageType.BLUDGEONING) {
            float old = event.getAmount();
            float updated = old * (1.0f - totalReduction);
            if (updated < 0f) updated = 0f;
            event.setAmount(updated);
        }
    }

    // mapDamageSourceToDamageType(...) - можно взять ту же реализацию,
    // которую я уже присылал ранее (reflection + строковые сопоставления)
    private static DamageType mapDamageSourceToDamageType(DamageSource source) {
        if (source == null) return null;

        try {
            ResourceLocation id = source.typeHolder().unwrapKey().orElseThrow().location();
            String path = id.getPath(); // например "arrow", "player", "in_fire", и т.п.
            switch (path) {
                case "arrow":
                case "trident":
                case "piercing":
                    return DamageType.PIERCING;
                case "player":
                case "mob":
                case "bludgeoning":
                case "fall":
                case "fly_into_wall":
                    return DamageType.BLUDGEONING;
                case "slashing":
                    return DamageType.SLASHING;
            }

        } catch (Exception ignored) {}

        return null;
    }
}
