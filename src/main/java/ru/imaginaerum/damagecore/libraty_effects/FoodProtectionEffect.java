package ru.imaginaerum.damagecore.libraty_effects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import ru.imaginaerum.damagecore.library_damage.DamageType;

public class FoodProtectionEffect {

    private final Item item;                  // ← источник (еда)
    private final DamageType damageType;
    private final float protectionPercent;
    private int remainingTicks;

    public FoodProtectionEffect(Item item, DamageType damageType, float protectionPercent, int durationTicks) {
        this.item = item;
        this.damageType = damageType;
        this.protectionPercent = protectionPercent;
        this.remainingTicks = durationTicks;
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

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("item", item.builtInRegistryHolder().key().location().toString());
        tag.putString("damageType", damageType.name());
        tag.putFloat("protection", protectionPercent);
        tag.putInt("ticks", remainingTicks);
        return tag;
    }

    public static FoodProtectionEffect load(CompoundTag tag) {
        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                new net.minecraft.resources.ResourceLocation(tag.getString("item"))
        );
        DamageType type = DamageType.valueOf(tag.getString("damageType"));
        float protection = tag.getFloat("protection");
        int ticks = tag.getInt("ticks");

        return new FoodProtectionEffect(item, type, protection, ticks);
    }
}
