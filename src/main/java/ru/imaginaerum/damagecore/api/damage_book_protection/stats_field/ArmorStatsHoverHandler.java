package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionEffect;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;

import java.util.*;

public final class ArmorStatsHoverHandler {

    private static DamageType selectedDamageType = null;
    private static boolean showDetails = false;
    private static long hoverStartTime = 0;
    private static final long HOVER_DELAY_MS = 100;

    private static ResourceLocation hoveredFood = null;
    private static long foodHoverStartTime = 0;
    private static boolean showFoodDetails = false;

    public static boolean isShowFoodDetails()       { return showFoodDetails; }
    public static ResourceLocation getHoveredFood() { return hoveredFood; }


    private static ResourceLocation hoveredPotion = null;
    private static long potionHoverStartTime = 0;
    private static boolean showPotionDetails = false;

    public static boolean isShowPotionDetails()       { return showPotionDetails; }
    public static ResourceLocation getHoveredPotion() { return hoveredPotion; }
    private ArmorStatsHoverHandler() {}
    private static ResourceLocation hoveredEntity = null;
    private static long entityHoverStartTime = 0;
    private static boolean showEntityDetails = false;

    public static boolean isShowEntityDetails()          { return showEntityDetails; }
    public static ResourceLocation getHoveredEntity()    { return hoveredEntity; }
    public static boolean isShowDetails()           { return showDetails; }
    public static DamageType getSelectedDamageType() { return selectedDamageType; }

    public static void handle(InventoryScreen screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen == null) return;

        double mouseX = mc.mouseHandler.xpos()
                * (double) mc.getWindow().getGuiScaledWidth()
                / (double) mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos()
                * (double) mc.getWindow().getGuiScaledHeight()
                / (double) mc.getWindow().getScreenHeight();
        handleFoodHover(screen, mouseX, mouseY);
        handlePotionHover(screen, mouseX, mouseY);
        DamageType hoveredType = getDamageTypeAtPosition(screen, mouseX, mouseY);

        if (hoveredType != null) {
            if (!showDetails || selectedDamageType != hoveredType) {
                if (hoverStartTime == 0) {
                    hoverStartTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - hoverStartTime >= HOVER_DELAY_MS) {
                    selectedDamageType = hoveredType;
                    showDetails = true;
                }
            } else {
                hoverStartTime = 0;
            }
        } else {
            if (showDetails && !isMouseOverDetailsWindow(screen, mouseX, mouseY)) {
                showDetails = false;
                selectedDamageType = null;
            }
            hoverStartTime = 0;
        }
        handleEntityHover(screen, mouseX, mouseY);

    }
    private static void handlePotionHover(InventoryScreen screen, double mouseX, double mouseY) {
        ResourceLocation found = null;
        for (Map.Entry<ResourceLocation, int[]> entry : ArmorStatsBottomBar.getPotionIconPositions().entrySet()) {
            int[] pos = entry.getValue();
            if (mouseX >= pos[0] && mouseX <= pos[0] + pos[2]
                    && mouseY >= pos[1] && mouseY <= pos[1] + pos[3]) {
                found = entry.getKey();
                break;
            }
        }

        if (found != null) {
            if (!showPotionDetails || !found.equals(hoveredPotion)) {
                if (potionHoverStartTime == 0) {
                    potionHoverStartTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - potionHoverStartTime >= HOVER_DELAY_MS) {
                    hoveredPotion = found;
                    showPotionDetails = true;
                }
            } else {
                potionHoverStartTime = 0;
            }
        } else {
            if (showPotionDetails && !isMouseOverPotionDetailsWindow(screen, mouseX, mouseY)) {
                showPotionDetails = false;
                hoveredPotion = null;
            }
            potionHoverStartTime = 0;
        }
    }

    public static boolean isMouseOverPotionDetailsWindow(InventoryScreen screen,
                                                         double mouseX, double mouseY) {
        if (!showPotionDetails || hoveredPotion == null) return false;
        Minecraft mc = Minecraft.getInstance();
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int bottomFieldY = guiTop + 165;
        int windowHeight = estimatePotionWindowHeight(mc);
        return mouseX >= guiLeft && mouseX <= guiLeft + imageWidth
                && mouseY >= (bottomFieldY - windowHeight)
                && mouseY <= bottomFieldY;
    }

    private static int estimatePotionWindowHeight(Minecraft mc) {
        if (hoveredPotion == null) return 0;
        ItemStack stack = PotionTracker.getActivePotions().get(hoveredPotion);
        int count = stack != null ? PotionUtils.getMobEffects(stack).size() : 1;
        int lineH  = mc.font.lineHeight + 2;
        int titleH = mc.font.lineHeight + 4;
        return 6 + titleH + Math.max(1, count) * lineH + 6;
    }


    private static void handleFoodHover(InventoryScreen screen, double mouseX, double mouseY) {
        ResourceLocation found = null;
        for (Map.Entry<ResourceLocation, int[]> entry : ArmorStatsBottomBar.getFoodIconPositions().entrySet()) {
            int[] pos = entry.getValue();
            if (mouseX >= pos[0] && mouseX <= pos[0] + pos[2]
                    && mouseY >= pos[1] && mouseY <= pos[1] + pos[3]) {
                found = entry.getKey();
                break;
            }
        }

        if (found != null) {
            if (!showFoodDetails || !found.equals(hoveredFood)) {
                if (foodHoverStartTime == 0) {
                    foodHoverStartTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - foodHoverStartTime >= HOVER_DELAY_MS) {
                    hoveredFood = found;
                    showFoodDetails = true;
                }
            } else {
                foodHoverStartTime = 0;
            }
        } else {
            if (showFoodDetails && !isMouseOverFoodDetailsWindow(screen, mouseX, mouseY)) {
                showFoodDetails = false;
                hoveredFood = null;
            }
            foodHoverStartTime = 0;
        }
    }

    public static boolean isMouseOverFoodDetailsWindow(InventoryScreen screen, double mouseX, double mouseY) {
        if (!showFoodDetails || hoveredFood == null) return false;
        Minecraft mc = Minecraft.getInstance();
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int bottomFieldY = guiTop + 165;
        int windowHeight = estimateFoodWindowHeight(mc);
        int windowY = bottomFieldY - windowHeight;
        return mouseX >= guiLeft && mouseX <= guiLeft + imageWidth
                && mouseY >= windowY && mouseY <= windowY + windowHeight;
    }

    private static int estimateFoodWindowHeight(Minecraft mc) {
        if (hoveredFood == null) return 0;
        int lineH  = mc.font.lineHeight + 2;
        int titleH = mc.font.lineHeight + 4;
        int count  = getFoodEffectCount(hoveredFood);
        return 6 + titleH + Math.max(1, count) * lineH + 6;

    }
    public static boolean isHoveringAnyCustomWindow(InventoryScreen screen, double mouseX, double mouseY) {
        return isMouseOverDetailsWindow(screen, mouseX, mouseY)
                || isMouseOverFoodDetailsWindow(screen, mouseX, mouseY)
                || isMouseOverEntityDetailsWindow(screen, mouseX, mouseY)
                || isMouseOverPotionDetailsWindow(screen, mouseX, mouseY);
    }
    private static int getFoodEffectCount(ResourceLocation foodKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        FoodProtectionManager foodManager = FoodProtectionCapability.get(mc.player);
        if (foodManager == null) return 0;

        Set<DamageType> types = new HashSet<>();
        for (FoodProtectionEffect effect : foodManager.getAllEffects()) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(effect.getItem());
            if (foodKey.equals(key)) {
                types.add(effect.getDamageType());
            }
        }
        return Math.max(1, types.size());
    }

    private static void handleEntityHover(InventoryScreen screen, double mouseX, double mouseY) {
        ResourceLocation found = null;
        for (Map.Entry<ResourceLocation, int[]> entry : ArmorStatsBottomBar.getEntityIconPositions().entrySet()) {
            int[] pos = entry.getValue(); // [x, barY, iconW, barH]
            if (mouseX >= pos[0] && mouseX <= pos[0] + pos[2]
                    && mouseY >= pos[1] && mouseY <= pos[1] + pos[3]) {
                found = entry.getKey();
                break;
            }
        }

        if (found != null) {
            if (!showEntityDetails || !found.equals(hoveredEntity)) {
                if (entityHoverStartTime == 0) {
                    entityHoverStartTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - entityHoverStartTime >= HOVER_DELAY_MS) {
                    hoveredEntity = found;
                    showEntityDetails = true;
                }
            } else {
                entityHoverStartTime = 0;
            }
        } else {
            if (showEntityDetails && !isMouseOverEntityDetailsWindow(screen, mouseX, mouseY)) {
                showEntityDetails = false;
                hoveredEntity = null;
            }
            entityHoverStartTime = 0;
        }
    }

    public static boolean isMouseOverEntityDetailsWindow(InventoryScreen screen, double mouseX, double mouseY) {
        if (!showEntityDetails || hoveredEntity == null) return false;
        Minecraft mc = Minecraft.getInstance();
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int bottomFieldY = guiTop + 165;

        int windowX = guiLeft;
        int windowY = bottomFieldY - estimateEntityWindowHeight(mc);
        return mouseX >= windowX && mouseX <= windowX + imageWidth
                && mouseY >= windowY && mouseY <= windowY + estimateEntityWindowHeight(mc);
    }

    private static int estimateEntityWindowHeight(Minecraft mc) {
        if (hoveredEntity == null) return 0;
        // Примерная высота: заголовок + до 5 эффектов
        int lineH = mc.font.lineHeight + 2;
        int titleH = mc.font.lineHeight + 4;
        int effectCount = getEffectCountForEntity(hoveredEntity);
        return 6 + titleH + effectCount * lineH + 6;
    }

    private static int getEffectCountForEntity(ResourceLocation entityKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        int count = 0;
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            LivingEntity source = EffectSourceManager.getSource(mc.player, inst.getEffect());
            if (source != null) {
                ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(source.getType());
                if (entityKey.equals(key)) count++;
            }
        }
        return Math.max(1, count);
    }
    public static boolean isMouseOverDetailsWindow(InventoryScreen screen, double mouseX, double mouseY) {
        if (!showDetails || selectedDamageType == null) return false;

        Minecraft mc = Minecraft.getInstance();
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int topFieldY    = guiTop - 20 + 1;
        int windowX      = guiLeft;
        int windowY      = topFieldY + 20;
        int windowWidth  = imageWidth;

        int lineHeight   = mc.font.lineHeight + 2;
        int titleHeight  = mc.font.lineHeight + 4;
        int windowHeight = 6 + titleHeight + (5 * lineHeight) + 6;

        return mouseX >= windowX && mouseX <= windowX + windowWidth
                && mouseY >= windowY && mouseY <= windowY + windowHeight;
    }

    private static DamageType getDamageTypeAtPosition(InventoryScreen screen, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;

        int guiLeft  = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop   = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int drawH    = 20;
        int topFieldY = guiTop - drawH + 1;
        int yTop     = topFieldY + (drawH - 8) / 2;
        int x        = guiLeft + 6;

        float scale = 2f / 3f;

        Map<DamageType, Float> totalProtections =
                ArmorStatsCalculator.getPlayerTotalProtectionPercent(mc.player);

        List<DamageType> orderedTypes = new ArrayList<>(ArmorStatsTopBar.DISPLAY_ORDER);
        for (DamageType type : totalProtections.keySet()) {
            if (!orderedTypes.contains(type)) orderedTypes.add(type);
        }

        for (DamageType type : orderedTypes) {
            float totalPercent = totalProtections.getOrDefault(type, 0f);
            if (totalPercent <= 0) continue;

            int iconSize  = 8;
            int textWidth = (int)(mc.font.width((int)(totalPercent * 100) + "%") * scale);
            int endX      = x + iconSize + 1 + textWidth + 4;

            if (mouseX >= x && mouseX <= endX && mouseY >= yTop && mouseY <= yTop + iconSize) {
                return type;
            }
            x = endX;
        }
        return null;
    }
}