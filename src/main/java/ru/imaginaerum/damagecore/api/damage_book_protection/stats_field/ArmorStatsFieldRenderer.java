package ru.imaginaerum.damagecore.api.damage_book_protection.stats_field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import ru.imaginaerum.damagecore.library_damage.DamageType;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionCapability;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionEffect;
import ru.imaginaerum.damagecore.libraty_effects.FoodProtectionManager;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ArmorStatsFieldRenderer {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation("damagecore", "textures/gui/container/creative_inventory/armor_statistic_field.png");

    private static final int TEX_W = 200;
    private static final int TEX_H = 20;
    private static final int EDGE = 2;

    // Состояние детального окна
    private static DamageType selectedDamageType = null;
    private static boolean showDetails = false;
    private static long hoverStartTime = 0;
    private static final long HOVER_DELAY_MS = 100; // задержка перед открытием окна

    private ArmorStatsFieldRenderer() {
    }

    public static boolean isButtonHovered(double mouseX, double mouseY, InventoryScreen screen) {
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop = ((AbstractContainerScreenAccessor) screen).getTopPos();

        int x = guiLeft + 62;
        int y = guiTop + 10;

        return mouseX >= x && mouseX < x + 10 && mouseY >= y && mouseY < y + 9;
    }

    public static void render(GuiGraphics gui, InventoryScreen screen) {
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int drawW = imageWidth;
        int drawH = 20;

        int topFieldY = guiTop - drawH + 1;
        int bottomFieldY = guiTop + 165;

        // Рисуем обе полоски статистики
        drawStretchBar(gui, TEXTURE, guiLeft, topFieldY, drawW, drawH);
        drawStretchBar(gui, TEXTURE, guiLeft, bottomFieldY, drawW, drawH);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Map<DamageType, Float> totalProtections =
                ArmorStatsCalculator.getPlayerTotalProtectionPercent(mc.player);

        Map<DamageType, Float> armorOnlyProtections =
                ArmorStatsCalculator.getArmorOnlyProtectionPercent(mc.player);

        int iconSize = 8;
        float scale = 2f / 3f;
        int startX = guiLeft + 6;
        int yTop = topFieldY + (drawH - iconSize) / 2;

        int x = startX;

        // Рисуем иконки и проценты
        for (Map.Entry<DamageType, Float> entry : totalProtections.entrySet()) {
            DamageType type = entry.getKey();
            float totalPercent = entry.getValue();
            float armorPercent = armorOnlyProtections.getOrDefault(type, 0f);

            if (totalPercent <= 0) continue;

            ResourceLocation icon = new ResourceLocation(
                    "damagecore","textures/gui/damage_types/" + type.getDamageName() + "_damage.png"
            );

            gui.blit(icon, x, yTop, 0, 0, iconSize, iconSize, iconSize, iconSize);

            int color = 0xEEEEEE;
            if (totalPercent > armorPercent + 0.001f) {
                color = 0xFFD700;
            }

            gui.pose().pushPose();
            int percentY = yTop + (iconSize / 2) - (int)((mc.font.lineHeight * scale) / 2f) + 1;
            gui.pose().translate(x + iconSize + 1, percentY, 0);
            gui.pose().scale(scale, scale, 1.0f);
            gui.drawString(mc.font, (int)(totalPercent * 100) + "%", 0, 0, color, false);
            gui.pose().popPose();

            int textWidth = (int)(mc.font.width((int)(totalPercent * 100) + "%") * scale);
            x += iconSize + 1 + textWidth + 4;
        }

        // Источники защиты
        renderProtectionSources(gui, screen, bottomFieldY, drawH);

        // Обрабатываем ховер для детального окна
        handleHoverLogic(screen);

        // ДЕТАЛЬНОЕ ОКНО
        if (showDetails && selectedDamageType != null) {
            renderDetailsWindow(gui, screen, topFieldY, drawW, drawH);
        }
    }

    private static void handleHoverLogic(InventoryScreen screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen == null) return;

        double mouseX = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();

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
                // Уже показываем этот тип, сбрасываем таймер
                hoverStartTime = 0;
            }
        } else {
            // Мышь не над иконкой урона
            if (showDetails) {
                // Проверяем, не над ли окном мышь
                if (!isMouseOverDetailsWindow(screen, mouseX, mouseY)) {
                    showDetails = false;
                    selectedDamageType = null;
                }
            }
            hoverStartTime = 0;
        }
    }

    public static boolean isDetailsVisible() {
        return showDetails && selectedDamageType != null;
    }

    public static boolean isMouseOverDetailsWindow(InventoryScreen screen, double mouseX, double mouseY) {
        if (!showDetails || selectedDamageType == null) return false;

        Minecraft mc = Minecraft.getInstance();
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int topFieldY = guiTop - 20 + 1;
        int drawH = 20;

        int windowX = guiLeft;
        int windowY = topFieldY + drawH;
        int windowWidth = imageWidth;

        int lines = 5;
        int lineHeight = mc.font.lineHeight + 2;
        int titleHeight = mc.font.lineHeight + 4;
        int paddingTop = 6;
        int paddingBottom = 6;
        int windowHeight = paddingTop + titleHeight + (lines * lineHeight) + paddingBottom;

        return mouseX >= windowX && mouseX <= windowX + windowWidth &&
                mouseY >= windowY && mouseY <= windowY + windowHeight;
    }

    private static void drawStretchBar(GuiGraphics gui, ResourceLocation tex,
                                       int x, int y, int width, int height) {
        int middleWidth = width - EDGE * 2;
        int middleHeight = height - EDGE * 2;

        // Углы (фиксированные)
        gui.blit(tex, x, y, 0, 0, EDGE, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE + middleWidth, y, TEX_W - EDGE, 0, EDGE, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x, y + EDGE + middleHeight, 0, TEX_H - EDGE, EDGE, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE + middleWidth, y + EDGE + middleHeight, TEX_W - EDGE, TEX_H - EDGE, EDGE, EDGE, TEX_W, TEX_H);

        // Стороны (растягиваем)
        gui.blit(tex, x + EDGE, y, EDGE, 0, middleWidth, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE, y + EDGE + middleHeight, EDGE, TEX_H - EDGE, middleWidth, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x, y + EDGE, 0, EDGE, EDGE, middleHeight, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE + middleWidth, y + EDGE, TEX_W - EDGE, EDGE, EDGE, middleHeight, TEX_W, TEX_H);

        // Центр
        gui.blit(tex, x + EDGE, y + EDGE, EDGE, EDGE, middleWidth, middleHeight, TEX_W, TEX_H);
    }

    private static void renderProtectionSources(GuiGraphics gui, InventoryScreen screen, int barY, int barHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int iconSize = 16;
        int effectIconSize = 14;
        int padding = 2;
        int x = guiLeft + 6;
        int y = barY + (barHeight - iconSize) / 2;

        Set<ResourceLocation> renderedKeys = new HashSet<>();

        // Зачарованная броня
        for (ItemStack stack : mc.player.getArmorSlots()) {
            if (stack.isEmpty()) continue;

            boolean hasProtectionEnchant = false;
            net.minecraft.nbt.ListTag enchantments = stack.getEnchantmentTags();
            if (enchantments != null && !enchantments.isEmpty()) {
                for (int i = 0; i < enchantments.size(); i++) {
                    net.minecraft.nbt.CompoundTag enchantTag = enchantments.getCompound(i);
                    String enchantId = enchantTag.getString("id");
                    if (enchantId.equals("minecraft:protection") ||
                            enchantId.equals("minecraft:fire_protection") ||
                            enchantId.equals("minecraft:projectile_protection") ||
                            enchantId.equals("minecraft:blast_protection") ||
                            enchantId.equals("minecraft:feather_falling")) {
                        hasProtectionEnchant = true;
                        break;
                    }
                }
            }

            if (!hasProtectionEnchant) continue;

            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (renderedKeys.add(key)) {
                gui.renderItem(stack, x, y);
                x += iconSize + padding;
            }
        }

        // Еда
        FoodProtectionManager foodManager = FoodProtectionCapability.get(mc.player);
        if (foodManager != null) {
            for (FoodProtectionEffect effect : foodManager.getAllEffects()) {
                if (effect.getProtectionPercent() <= 0) continue;

                ResourceLocation key = BuiltInRegistries.ITEM.getKey(effect.getItem());
                if (renderedKeys.add(key)) {
                    gui.renderItem(new ItemStack(effect.getItem()), x, y);
                    x += iconSize + padding;
                }
            }
        }

        // Эффекты
        for (MobEffectInstance effectInstance : mc.player.getActiveEffects()) {
            ResourceLocation key = BuiltInRegistries.MOB_EFFECT.getKey(effectInstance.getEffect());

            if (renderedKeys.add(key)) {
                int effectY = y + (iconSize - effectIconSize) / 2;
                net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = mc.getMobEffectTextures().get(effectInstance.getEffect());
                gui.blit(x, effectY, 0, effectIconSize, effectIconSize, sprite);

                int amplifier = effectInstance.getAmplifier() + 1;
                if (amplifier > 1) {
                    String levelText = String.valueOf(amplifier);
                    gui.pose().pushPose();
                    gui.pose().scale(0.7f, 0.7f, 1.0f);
                    float scaledX = (x + effectIconSize / 2f) / 0.7f;
                    float scaledY = (effectY + effectIconSize / 2f - 3) / 0.7f;
                    int textWidth = mc.font.width(levelText);
                    gui.drawString(mc.font, levelText,
                            (int)(scaledX - (textWidth * 0.7f) / 2f),
                            (int)scaledY,
                            0xFFFFFF, true);
                    gui.pose().popPose();
                }

                x += effectIconSize + padding;
            }
        }
    }

    private static void renderDetailsWindow(GuiGraphics gui, InventoryScreen screen, int topFieldY, int fieldWidth, int fieldHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || selectedDamageType == null) return;

        // Получаем проценты
        float totalPercent = ArmorStatsCalculator.getPlayerTotalProtectionPercent(mc.player).getOrDefault(selectedDamageType, 0f) * 100;
        float armorPercent = ArmorStatsCalculator.getArmorOnlyProtectionPercent(mc.player).getOrDefault(selectedDamageType, 0f) * 100;
        float foodPercent = 0f;
        FoodProtectionManager foodManager = FoodProtectionCapability.get(mc.player);
        if (foodManager != null) {
            foodPercent = foodManager.getTotalProtectionPercent(selectedDamageType) * 100;
        }
        float enchantPercent = Math.max(0, totalPercent - armorPercent - foodPercent);
        float effectPercent = 0f;

        // Собираем только ненулевые строки
        java.util.List<Component> lines = new java.util.ArrayList<>();
        Component armorText = Component.translatable("armor_stat.armor");
        Component enchantText = Component.translatable("armor_stat.enchant");
        Component foodText = Component.translatable("armor_stat.food");
        Component effectText = Component.translatable("armor_stat.effect");
        Component totalText = Component.translatable("armor_stat.total");

        if (armorPercent > 0.01f) {
            lines.add(Component.literal(armorText.getString() + ": " + String.format("%.1f", armorPercent) + "%"));
        }
        if (enchantPercent > 0.01f) {
            lines.add(Component.literal(enchantText.getString() + ": " + String.format("%.1f", enchantPercent) + "%"));
        }
        if (foodPercent > 0.01f) {
            lines.add(Component.literal(foodText.getString() + ": " + String.format("%.1f", foodPercent) + "%"));
        }
        if (effectPercent > 0.01f) {
            lines.add(Component.literal(effectText.getString() + ": " + String.format("%.1f", effectPercent) + "%"));
        }
        // Итог показываем всегда, даже если 0%
        lines.add(Component.literal(totalText.getString() + ": " + String.format("%.1f", totalPercent) + "%"));

        // Рассчитываем высоту окна с учётом только отображаемых строк
        int linesCount = lines.size();
        int lineHeight = mc.font.lineHeight + 2;
        int titleHeight = mc.font.lineHeight + 4;
        int paddingTop = 6;
        int paddingBottom = 6;
        int windowHeight = paddingTop + titleHeight + (linesCount * lineHeight) + paddingBottom;

        int windowX = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int windowY = topFieldY + fieldHeight;

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 300);

        // Полупрозрачный черный фон
        gui.fill(windowX, windowY, windowX + fieldWidth, windowY + windowHeight, 0xCC000000);

        // Рисуем рамку
        drawWindowWithTile(gui, TEXTURE, windowX, windowY, fieldWidth, windowHeight);

        // Координаты текста
        int textX = windowX + EDGE + 4;
        int textY = windowY + paddingTop;

        // Заголовок по центру
        Component damageTitle = Component.translatable("damage_type." + selectedDamageType.getDamageName());
        int titleWidth = mc.font.width(damageTitle);
        gui.drawString(mc.font, damageTitle, windowX + (fieldWidth - titleWidth) / 2, textY, 0xFFFFFF, false);
        textY += titleHeight;

        // Рисуем только ненулевые строки
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int color = (i == lines.size() - 1) ? 0xFFD700 : 0xEEEEEE; // последняя строка (итог) золотая
            gui.drawString(mc.font, line.getString(), textX, textY, color, false);
            textY += lineHeight;
        }

        gui.pose().popPose();
    }

    private static void drawWindowWithTile(GuiGraphics gui, ResourceLocation tex,
                                           int x, int y, int width, int height) {
        int middleWidth = width - EDGE * 2;
        int middleHeight = height - EDGE * 2;

        // Углы
        gui.blit(tex, x, y, 0, 0, EDGE, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE + middleWidth, y, TEX_W - EDGE, 0, EDGE, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x, y + EDGE + middleHeight, 0, TEX_H - EDGE, EDGE, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE + middleWidth, y + EDGE + middleHeight, TEX_W - EDGE, TEX_H - EDGE, EDGE, EDGE, TEX_W, TEX_H);

        // Горизонтальные стороны
        gui.blit(tex, x + EDGE, y, EDGE, 0, middleWidth, EDGE, TEX_W, TEX_H);
        gui.blit(tex, x + EDGE, y + EDGE + middleHeight, EDGE, TEX_H - EDGE, middleWidth, EDGE, TEX_W, TEX_H);

        // Вертикальные стороны с тайлингом
        int vertTileHeight = TEX_H - EDGE * 2;
        int vertRepeats = (middleHeight + vertTileHeight - 1) / vertTileHeight;

        for (int i = 0; i < vertRepeats; i++) {
            int tileH = Math.min(vertTileHeight, middleHeight - i * vertTileHeight);
            gui.blit(tex, x, y + EDGE + i * vertTileHeight, 0, EDGE, EDGE, tileH, TEX_W, TEX_H);
            gui.blit(tex, x + EDGE + middleWidth, y + EDGE + i * vertTileHeight, TEX_W - EDGE, EDGE, EDGE, tileH, TEX_W, TEX_H);
        }

        // Центр с тайлингом
        int centerTileW = TEX_W - EDGE * 2;
        int centerTileH = TEX_H - EDGE * 2;
        int horRepeats = (middleWidth + centerTileW - 1) / centerTileW;

        for (int i = 0; i < vertRepeats; i++) {
            int tileH = Math.min(centerTileH, middleHeight - i * centerTileH);
            for (int j = 0; j < horRepeats; j++) {
                int tileW = Math.min(centerTileW, middleWidth - j * centerTileW);
                gui.blit(tex, x + EDGE + j * centerTileW, y + EDGE + i * centerTileH,
                        EDGE, EDGE, tileW, tileH, TEX_W, TEX_H);
            }
        }
    }

    private static DamageType getDamageTypeAtPosition(InventoryScreen screen, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;

        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int drawW = imageWidth;
        int drawH = 20;
        int topFieldY = guiTop - drawH + 1;
        int yTop = topFieldY + (drawH - 8) / 2;
        int startX = guiLeft + 6;
        int x = startX;

        Map<DamageType, Float> totalProtections = ArmorStatsCalculator.getPlayerTotalProtectionPercent(mc.player);
        float scale = 2f / 3f;

        for (Map.Entry<DamageType, Float> entry : totalProtections.entrySet()) {
            DamageType type = entry.getKey();
            float totalPercent = entry.getValue();
            if (totalPercent <= 0) continue;

            int iconSize = 8;
            int textWidth = (int)(mc.font.width((int)(totalPercent * 100) + "%") * scale);
            int hitboxStartX = x;
            int hitboxEndX = x + iconSize + 1 + textWidth + 4;
            int hitboxStartY = yTop;
            int hitboxEndY = yTop + iconSize;

            if (mouseX >= hitboxStartX && mouseX <= hitboxEndX && mouseY >= hitboxStartY && mouseY <= hitboxEndY) {
                return type;
            }

            x = hitboxEndX;
        }
        return null;
    }
}