package ru.imaginaerum.damagecore;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import ru.imaginaerum.damagecore.entity.goals.StrayMeleeAttackGoal;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(DamageCore.MODID)
public class DamageCore {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "damagecore";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "damagecore" namespace



    public DamageCore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);


        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab


        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
    @SubscribeEvent
    public void onSkeletonJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Skeleton skeleton)) return;

        if (skeleton.getRandom().nextFloat() > 0.45f) return;

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, sword);
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.05F);

        skeleton.goalSelector.addGoal(2, new MeleeAttackGoal(skeleton, 1.2D, true));
    }


    @SubscribeEvent
    public void onStrayJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Stray stray)) return;

        if (stray.getRandom().nextFloat() > 0.25f) return;

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        stray.setItemSlot(EquipmentSlot.MAINHAND, sword);
        stray.setDropChance(EquipmentSlot.MAINHAND, 0.05F);

        // вот тут заменяем цель
        stray.goalSelector.addGoal(2, new StrayMeleeAttackGoal(stray, 1.2D, true));
    }
    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    // Add the example block item to the building blocks tab

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}
