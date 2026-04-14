package ru.imaginaerum.damagecore;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue SHOW_STAMINA_HUD = BUILDER
            .comment("Whether to show the stamina HUD overlay")
            .define("showStaminaHud", true);
    private static final ForgeConfigSpec.DoubleValue THIRST_DRAIN_MULTIPLIER = BUILDER
            .comment("Multiplier for thirst drain rate")
            .defineInRange("thirstDrainMultiplier", 1.0, 0.0, 10.0);
    private static final ForgeConfigSpec.BooleanValue ENABLE_THIRST = BUILDER
            .comment("Enable/disable thirst system")
            .define("enableThirst", true);
    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableThirst;
    public static boolean showStaminaHud;
    public static double thirstDrainMultiplier;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        showStaminaHud = SHOW_STAMINA_HUD.get();
        enableThirst = ENABLE_THIRST.get();
        thirstDrainMultiplier = THIRST_DRAIN_MULTIPLIER.get();
    }
}