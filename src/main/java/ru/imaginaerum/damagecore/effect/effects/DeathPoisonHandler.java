package ru.imaginaerum.damagecore.effect.effects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.effect.DCEffects;

import java.util.Random;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DeathPoisonHandler {
    private static final Random random = new Random();
    private static float shakeOffsetX = 0;
    private static float shakeOffsetY = 0;

    // Добавляем переменные для анимации урона
    private static int damageAnimationTicks = 0;
    private static final int DAMAGE_ANIMATION_DURATION = 2; // 2 тика
    private static float lastHealth = 0;

    @SubscribeEvent
    public static void onRenderHealthOverlay(RenderGuiOverlayEvent.Pre event) {
        // Проверяем, что это именно оверлей здоровья
        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            // Если у игрока есть эффект, отменяем стандартную отрисовку
            if (player != null && player.hasEffect(DCEffects.DEATH_POISON.get())) {
                event.setCanceled(true); // <-- Вот ключевой момент!
                // И сразу рисуем наши сердца
                renderPoisonHeartsOverlay(event.getGuiGraphics(), mc, player);
            }
        }
    }

    private static void renderPoisonHeartsOverlay(GuiGraphics guiGraphics, Minecraft mc, Player player) {
        ResourceLocation GUI_ICONS = new ResourceLocation("textures/gui/icons.png");
        // Текстуры для сердец иссушения (Wither)
        int fullWitherHeartU = 88;
        int halfWitherHeartU = 97;
        int emptyWitherHeartU = 16; // Пустое сердце иссушения
        int witherHeartV = 0;
        int heartWidth = 9;
        int heartHeight = 9;
        // Текстуры для сердец поглощения
        int fullAbsorptionHeartU = 16;
        int halfAbsorptionHeartU = 25;
        int emptyAbsorptionHeartU = 34;
        // Текстуры для анимации урона
        int damageHeartU = 106;  // Текстура урона для полных/половинчатых сердец
        int emptyDamageHeartU = 43; // Текстура урона для пустых сердец

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        int absorption = (int) player.getAbsorptionAmount();

        int left = mc.getWindow().getGuiScaledWidth() / 2 - 91;
        int top = mc.getWindow().getGuiScaledHeight() - 39;

        int totalHealthWithAbsorption = (int)(maxHealth + absorption);
        int healthRows = (int)Math.ceil(totalHealthWithAbsorption / 20.0F);
        int rowHeight = Math.max(10 - (healthRows - 2), 3);

        int currentHealth = (int)health;
        int heartCount = (totalHealthWithAbsorption + 1) / 2;

        // Определяем, нужно ли анимировать сердца (здоровье <= 5)
        boolean shouldAnimate = (currentHealth <= 5 && currentHealth > 0);
        // Проверяем, активна ли анимация урона
        boolean showDamageAnimation = damageAnimationTicks > 0;

        for (int i = 0; i < heartCount; i++) {
            int row = i / 10;
            int col = i % 10;
            int x = left + col * 8;
            int y = top - row * rowHeight;

            int heartIndex = i * 2;

            // 1. Сначала определяем, какой тип сердца должен быть на этой позиции
            boolean isAbsorption = false;
            boolean renderFull = false;
            boolean renderHalf = false;
            boolean isEmpty = false;

            // Проверяем поглощение (рисуется поверх здоровья)
            if (heartIndex < absorption) {
                isAbsorption = true;
                if (heartIndex + 1 <= absorption) {
                    renderFull = true;
                } else if (heartIndex == absorption) {
                    renderHalf = true;
                }
            }

            // Если это не поглощение, проверяем здоровье
            if (!isAbsorption) {
                int healthOffset = heartIndex;
                if (healthOffset + 1 <= currentHealth) {
                    renderFull = true;
                } else if (healthOffset == currentHealth) {
                    renderHalf = true;
                } else {
                    isEmpty = true; // Сердце полностью пустое
                }
            }

            // 2. Применяем анимацию дрожания, если здоровье низкое
            float animatedX = x;
            float animatedY = y;
            if (shouldAnimate && !isAbsorption) {
                // Применяем только резкое случайное смещение
                animatedX += shakeOffsetX;
                animatedY += shakeOffsetY;
            }

            // 3. Рисуем сердце
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.98F);

            // Определяем, какую текстуру использовать
            int textureU;

            if (showDamageAnimation) {
                // Во время анимации урона используем специальные текстуры
                if (renderFull || renderHalf) {
                    textureU = damageHeartU; // 106 для непустых сердец
                } else {
                    textureU = emptyDamageHeartU; // 33 для пустых сердец
                }

                // Рисуем текстуру урона
                if (isAbsorption) {
                    guiGraphics.blit(GUI_ICONS, (int)animatedX, (int)animatedY, emptyAbsorptionHeartU, witherHeartV, heartWidth, heartHeight, 256, 256);
                    // Поверх рисуем текстуру урона
                    guiGraphics.blit(GUI_ICONS, (int)animatedX, (int)animatedY, textureU, witherHeartV, heartWidth, heartHeight, 256, 256);
                } else {
                    guiGraphics.blit(GUI_ICONS, (int)animatedX, (int)animatedY, emptyWitherHeartU, witherHeartV, heartWidth, heartHeight, 256, 256);
                    // Поверх рисуем текстуру урона
                    guiGraphics.blit(GUI_ICONS, (int)animatedX, (int)animatedY, textureU, witherHeartV, heartWidth, heartHeight, 256, 256);
                }
            } else {
                // Обычная отрисовка без анимации урона
                if (isAbsorption) {
                    textureU = emptyAbsorptionHeartU;
                    guiGraphics.blit(GUI_ICONS, (int)animatedX, (int)animatedY, textureU, witherHeartV, heartWidth, heartHeight, 256, 256);
                } else {
                    textureU = emptyWitherHeartU;
                    guiGraphics.blit(GUI_ICONS, (int)animatedX, (int)animatedY, textureU, witherHeartV, heartWidth, heartHeight, 256, 256);
                }

                // 4. Рисуем поверх полное или половинку сердца, если нужно
                if (renderFull || renderHalf) {
                    if (isAbsorption) {
                        textureU = renderFull ? fullAbsorptionHeartU : halfAbsorptionHeartU;
                    } else {
                        textureU = renderFull ? fullWitherHeartU : halfWitherHeartU;
                    }
                    guiGraphics.blit(GUI_ICONS, (int)animatedX, (int)animatedY, textureU, witherHeartV, heartWidth, heartHeight, 256, 256);
                }
            }

            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F); // Сбрасываем цвет
        }
    }

    // Объединенный метод для всех тиковых событий на клиенте
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Обновляем анимацию урона
        if (damageAnimationTicks > 0) {
            damageAnimationTicks--;
        }

        // Проверяем изменения здоровья для запуска анимации
        float currentHealth = player.getHealth();
        if (currentHealth < lastHealth) {
            // Игрок получил урон - запускаем анимацию
            damageAnimationTicks = DAMAGE_ANIMATION_DURATION;
        }
        lastHealth = currentHealth;

        // Генерируем новое резкое смещение для дрожания каждый тик
        // Увеличим диапазон для более заметной дрожи
        shakeOffsetX = (random.nextFloat() - 0.5f) * 1.5f;
        shakeOffsetY = (random.nextFloat() - 0.5f) * 1.5f;
    }

    // Дополнительный обработчик для гарантированного отслеживания урона
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof Player player && player == Minecraft.getInstance().player) {
            // Если игрок получил урон и у него есть эффект смерти-яда
            if (player.hasEffect(DCEffects.DEATH_POISON.get())) {
                // Запускаем анимацию урона
                damageAnimationTicks = DAMAGE_ANIMATION_DURATION;
            }
        }
    }
}