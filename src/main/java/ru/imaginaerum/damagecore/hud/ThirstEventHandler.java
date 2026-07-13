package ru.imaginaerum.damagecore.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.hud.elements.ThirstBarElement;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ThirstEventHandler {

    // ПКМ по блоку воды
    private static int drinkTicks = 0;
    private static final int DRINK_DURATION = 25; // 2 секунды

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean rmb = mc.options.keyUse.isDown(), emptyHands = mc.player.getMainHandItem().isEmpty(), lookingAtWater = isLookingAtWater(mc);

        if (rmb && emptyHands && lookingAtWater) {
            drinkTicks++;
            if (drinkTicks % 4 == 0) {
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            if (drinkTicks >= DRINK_DURATION) {
                drinkTicks = 0;
                ThirstBarElement.drink(4f);
                mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1f, 1f);
            }
        } else {
            drinkTicks = 0;
        }
    }

    private static boolean isLookingAtWater(Minecraft mc) {
        if (mc.player == null || mc.level == null) return false;

        double reach = mc.player.getBlockReach();
        var start = mc.player.getEyePosition();
        var look = mc.player.getViewVector(1.0f);
        var end = start.add(look.x * reach, look.y * reach, look.z * reach);

        var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(
                start, end,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.ANY,
                mc.player
        ));

        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return false;

        return mc.level.getFluidState(hit.getBlockPos()).is(net.minecraft.tags.FluidTags.WATER);
    }

    // Окончание использования предмета (бутылка воды, зелья)
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!event.getEntity().level().isClientSide()) return;

        ItemStack stack = event.getItem();

        // Water Bottle
        if (stack.is(Items.POTION)) {
            if (PotionUtils.getPotion(stack) == Potions.WATER) {
                ThirstBarElement.drink(6f);
                return;
            }
            ThirstBarElement.drink(3f); // любое зелье считается питьём (меньше воды)
            return;
        }

        // Молоко тоже жидкость
        if (stack.is(Items.MILK_BUCKET)) {
            ThirstBarElement.drink(4f);
        }
    }
}