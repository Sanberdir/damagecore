package ru.imaginaerum.damagecore.api.damage_book_protection.pages_book;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantments;
import ru.imaginaerum.damagecore.api.damage_book_protection.DamageBookStateCollector;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenderActiveEffects {
    public static final int TAB_WIDTH = 150;
    protected static final ResourceLocation INVENTORY_LOCATION =
            new ResourceLocation("textures/gui/container/inventory.png");
    public static void renderActiveEffects(
            GuiGraphics gui,
            int tabX,
            int tabY,
            DamageBookStateCollector.ProtectionData protectionData,
            int mouseX,
            int mouseY
    ) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        List<ItemStack> allItemsToDisplay = new ArrayList<>();

        // ==== Эффекты из FoodProtectionCapability ====
        var foodManager = FoodProtectionCapability.get(player);
        if (foodManager != null) {
            var effects = foodManager.getActiveEffects();
            Map<Item, List<FoodProtectionEffect>> grouped = new HashMap<>();

            for (List<FoodProtectionEffect> list : effects.values()) {
                for (FoodProtectionEffect effect : list) {
                    grouped.computeIfAbsent(effect.getItem(), k -> new ArrayList<>()).add(effect);
                }
            }

            for (Item item : grouped.keySet()) {
                allItemsToDisplay.add(new ItemStack(item));
            }
        }
        // ==== Броня с Protection ====
        for (ItemStack armor : player.getArmorSlots()) {
            if ((armor.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION) > 0)
                    ||(armor.getEnchantmentLevel(Enchantments.FIRE_PROTECTION) > 0)
                    ||(armor.getEnchantmentLevel(Enchantments.BLAST_PROTECTION) > 0)
                    ||(armor.getEnchantmentLevel(Enchantments.FALL_PROTECTION) > 0)
                    ||(armor.getEnchantmentLevel(Enchantments.PROJECTILE_PROTECTION) > 0)
            ) {
                allItemsToDisplay.add(armor.copy());
            }
        }
        // ==== Ванильные эффекты как зелья ====
        for (MobEffectInstance inst : player.getActiveEffects()) {
            if (inst.getEffect() == MobEffects.DAMAGE_RESISTANCE) continue;

            ItemStack potionStack = findPotionForEffect(inst);
            if (potionStack != null) {
                allItemsToDisplay.add(potionStack);
            }
        }

        final int START_X = tabX + 10;
        final int START_Y = tabY + 20;
        final int CELL_SPACING = 2;
        final int MAX_PER_ROW = 7;
        final int MAX_ROWS = 3;

        Font font = Minecraft.getInstance().font;

        int curX = START_X;
        int curY = START_Y;
        int col = 0;
        int row = 0;

        for (ItemStack stack : allItemsToDisplay) {
            if (col >= MAX_PER_ROW) {
                col = 0;
                curX = START_X;
                curY += 18 + CELL_SPACING;
                row++;
                if (row >= MAX_ROWS) break;
            }

            gui.blit(INVENTORY_LOCATION, curX, curY, 7, 83, 18, 18);
            gui.renderItem(stack, curX + 1, curY + 1);
            gui.renderItemDecorations(font, stack, curX + 1, curY + 1);

            curX += 18 + CELL_SPACING;
            col++;
        }

        // ==== Если вообще нет эффектов ====
        boolean hasResistance = player.getActiveEffects()
                .stream().anyMatch(e -> e.getEffect() == MobEffects.DAMAGE_RESISTANCE);

        if (allItemsToDisplay.isEmpty() && !hasResistance) {
            String text = Component.translatable("damagecore.effects.none").getString();
            gui.pose().pushPose();
            gui.pose().scale(0.8f, 0.8f, 1f);
            gui.drawString(
                    font,
                    text,
                    (int) ((tabX + TAB_WIDTH / 2f - font.width(text) * 0.4f) / 0.8f),
                    (int) ((START_Y + 40) / 0.8f),
                    0xAAAAAA,
                    false
            );
            gui.pose().popPose();
        }
    }

    private static ItemStack findPotionForEffect(MobEffectInstance effect) {
        for (Potion potion : BuiltInRegistries.POTION) {
            for (MobEffectInstance inst : potion.getEffects()) {
                if (inst.getEffect() == effect.getEffect()) {
                    ItemStack stack = new ItemStack(Items.POTION);
                    PotionUtils.setPotion(stack, potion);
                    return stack;
                }
            }
        }
        return null;
    }
}
