package ru.imaginaerum.damagecore.mixin.tree_mixins.shooting;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.implementation_skills.shooting.ClientHundredArmedData;

@OnlyIn(Dist.CLIENT)
@Mixin(ItemProperties.class)
public class ItemPropertiesMixin {

    private static final ResourceLocation PULL = new ResourceLocation("pull");

    @Inject(
            method = "getProperty",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void interceptPull(Item item, ResourceLocation id,
                                      CallbackInfoReturnable<ItemPropertyFunction> cir) {
        if (!PULL.equals(id)) return;
        if (!(item instanceof BowItem)) return;


        cir.setReturnValue((s, level, entity, seed) -> {
            if (entity == null) return 0.0F;
            if (entity.getUseItem() != s) return 0.0F;

            float divisor = ClientHundredArmedData.hasSkill ? 10.0F : 20.0F;
            float pull = (float)(entity.getTicksUsingItem()) / divisor;
            return Math.min(pull, 1.0F);
        });
    }
}