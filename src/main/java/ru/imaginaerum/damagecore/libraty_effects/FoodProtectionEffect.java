package ru.imaginaerum.damagecore.libraty_effects;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import ru.imaginaerum.damagecore.library_damage.DamageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FoodProtectionEffect {

    private final Item item;
    private final DamageType damageType;
    private final float protectionPercent;
    private int remainingTicks;

    // Новое поле — mob effects, которые дала эта еда
    private final List<MobEffectInstance> mobEffects;

    public FoodProtectionEffect(Item item, DamageType damageType,
                                float protectionPercent, int durationTicks,
                                List<MobEffectInstance> mobEffects) {
        this.item = item;
        this.damageType = damageType;
        this.protectionPercent = protectionPercent;
        this.remainingTicks = durationTicks;
        this.mobEffects = mobEffects != null ? mobEffects : Collections.emptyList();
    }

    // Обратная совместимость — старый конструктор без mob effects
    public FoodProtectionEffect(Item item, DamageType damageType,
                                float protectionPercent, int durationTicks) {
        this(item, damageType, protectionPercent, durationTicks, Collections.emptyList());
    }

    public List<MobEffectInstance> getMobEffects() {
        return mobEffects;
    }

    public Item getItem() {
        return item;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public float getProtectionPercent() {
        return protectionPercent;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public void tick() {
        remainingTicks--;
    }

    public boolean isExpired() {
        return remainingTicks <= 0;
    }

    // ===== NBT =====

    // В FoodProtectionEffect.save():
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("item", item.builtInRegistryHolder().key().location().toString());
        tag.putString("damageType", damageType.name());
        tag.putFloat("protection", protectionPercent);
        tag.putInt("ticks", remainingTicks);

// Сохраняем mob effects
        ListTag mobList = new ListTag();
        for (MobEffectInstance inst : mobEffects) {
            CompoundTag effectTag = new CompoundTag();
            inst.save(effectTag);  // ← передаём tag как аргумент
            mobList.add(effectTag);
        }
        tag.put("mobEffects", mobList);

        return tag;
    }

    // В FoodProtectionEffect.load():
    public static FoodProtectionEffect load(CompoundTag tag) {
        Item item = BuiltInRegistries.ITEM.get(
                new ResourceLocation(tag.getString("item")));
        DamageType type = DamageType.valueOf(tag.getString("damageType"));
        float protection = tag.getFloat("protection");
        int ticks = tag.getInt("ticks");

        List<MobEffectInstance> mobEffects = new ArrayList<>();
        if (tag.contains("mobEffects")) {
            ListTag mobList = tag.getList("mobEffects", 10);
            for (int i = 0; i < mobList.size(); i++) {
                MobEffectInstance inst = MobEffectInstance.load(mobList.getCompound(i));
                if (inst != null) mobEffects.add(inst);
            }
        }

        return new FoodProtectionEffect(item, type, protection, ticks, mobEffects);
    }

}
