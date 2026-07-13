package ru.imaginaerum.damagecore.library_weapon_types;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.DamageCore;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = DamageCore.MODID, value = Dist.CLIENT)
public class WeaponTypeTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getKey(stack.getItem());
        if (itemId == null) return;

        WeaponType type = WeaponTypeManager.INSTANCE.getType(itemId);
        if (type == null) return;

        // Вставляем тип первой строкой (индекс 1 — сразу после названия предмета)
        event.getToolTip().add(1, type.getDisplayName());
    }
}