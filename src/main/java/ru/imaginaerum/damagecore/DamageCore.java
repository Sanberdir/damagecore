package ru.imaginaerum.damagecore;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.armor.DamageArmorModifier;
import ru.imaginaerum.damagecore.datagen.DamageTypeProvider;
import ru.imaginaerum.damagecore.effect.DCEffects;
import ru.imaginaerum.damagecore.item.DCItems;
import ru.imaginaerum.damagecore.library_damage.WeaponDamageManager;
import ru.imaginaerum.damagecore.library_damage.arrow_data.ArrowDamageManager;
import ru.imaginaerum.damagecore.library_weapon_types.WeaponTypeManager;
import ru.imaginaerum.damagecore.particle.DCParticles;
import ru.imaginaerum.damagecore.sounds.CustomSoundEvents;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;


// The value here should match an entry in the META-INF/mods.toml file
@Mod(DamageCore.MODID)
public class DamageCore {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "damagecore";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "damagecore" namespace

    public static final WeaponDamageManager WEAPON_DAMAGE_MANAGER = new WeaponDamageManager();
    @SubscribeEvent
    public void gatherData(final GatherDataEvent event) {
        var generator = event.getGenerator();
        var packOutput = generator.getPackOutput();
        var lookupProvider = event.getLookupProvider();
        var existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(),
                new DamageTypeProvider(packOutput, lookupProvider, existingFileHelper));
    }

    public DamageCore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        DCItems.ITEMS.register(modEventBus);
        DCEffects.MOB_EFFECTS.register(modEventBus);
        DCParticles.PARTICLE_TYPES.register(modEventBus);

        // Register the commonSetup method for modloading
        CustomSoundEvents.SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        forgeEventBus.addListener(this::onAddReloadListeners);
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        // Register the item to a creative tab


        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
    public static final DamageArmorModifier ARMOR_MODIFIER = new DamageArmorModifier(); // только здесь, final
    public static final ArrowDamageManager ARROW_DAMAGE_MANAGER = new ArrowDamageManager(); // ← добавить

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(WEAPON_DAMAGE_MANAGER);
        event.addListener(ARROW_DAMAGE_MANAGER);
        event.addListener(ARMOR_MODIFIER); // добавь это

        event.addListener(WeaponTypeManager.INSTANCE); // ← добавить
    }
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.init();

            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> DamageCore::clientOnlySetup
            );
        });
    }

    private static void clientOnlySetup() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.keySwapOffhand.setKey(InputConstants.UNKNOWN);
        mc.options.save();
    }

    // Add the example block item to the building blocks tab
// В вашем главном классе мода

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int messageID = 0;
    public static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
        PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer);
        messageID++;
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // ✅ Регистрируем фабрику слоёв — ОБЯЗАТЕЛЬНО здесь
        }

        @SubscribeEvent
        public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(DamageCore.ARMOR_MODIFIER);
        }
    }
}
