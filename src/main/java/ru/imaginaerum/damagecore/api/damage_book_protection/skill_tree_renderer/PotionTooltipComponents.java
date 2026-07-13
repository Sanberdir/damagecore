package ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PotionTooltipComponents {

    private PotionTooltipComponents() {}

    @SubscribeEvent
    public static void register(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(DurationBarTooltip.class, DurationBarClientTooltip::new);
    }
}