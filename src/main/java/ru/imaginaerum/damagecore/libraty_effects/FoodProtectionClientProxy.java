package ru.imaginaerum.damagecore.libraty_effects;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FoodProtectionClientProxy {
    public static void apply(CompoundTag data) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        player.getCapability(FoodProtectionCapability.FOOD_PROTECTION)
                .ifPresent(manager -> manager.load(data));
    }
}