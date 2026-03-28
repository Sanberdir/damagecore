package ru.imaginaerum.damagecore.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DamageCoreHudOverlay {

    private static final ResourceLocation HUD_TEXTURE =
            new ResourceLocation("damagecore", "textures/hud/damage_core_hud.png");

    private static final int TEXTURE_BAR_WIDTH = 104;
    private static final int EDGE_WIDTH = 6;

    // ===== STAMINA =====
    private static float stamina = 40f;
    private static final float MAX_STAMINA = 40f;
    private static final int STAMINA_TEXTURE_X = 38;
    private static final int STAMINA_TEXTURE_Y = 97;
    private static final int STAMINA_TEXTURE_EMPTY_Y = 193;
    private static final int STAMINA_BASE_BAR_WIDTH = 52; // в 2 раза меньше 104
    // ===== MANA =====
    private static final int MANA_TEXTURE_X = 45;
    private static final int MANA_TEXTURE_Y = 89;
    private static final int MANA_TEXTURE_EMPTY_Y = 185;
    private static float mana = 10f;
    private static final float MAX_MANA = 10f;

    private static final int MANA_BASE_BAR_WIDTH = 26;
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        GuiGraphics gui = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.gameMode == null) return;

        GameType mode = mc.gameMode.getPlayerMode();
        if (mode != GameType.SURVIVAL && mode != GameType.ADVENTURE) return;

        RenderSystem.setShaderTexture(0, HUD_TEXTURE);

        updateStamina(mc);

        renderHudBase(gui);

        // HP-полоска (растягивается под health_boost)
        renderHealthAndAbsorptionBars(gui, mc);

        // Мана поверх стамины
        renderManaBar(gui);
        renderStaminaBar(gui);
    }

    private static void renderHudBase(GuiGraphics gui) {
        gui.blit(HUD_TEXTURE, 0, 0, 0, 0, 48, 48, 160, 208);
    }

    private static void updateStamina(Minecraft mc) {
        if (mc.player == null) return;

        boolean sprinting = mc.player.isSprinting();
        boolean moving = mc.player.zza != 0 || mc.player.xxa != 0;

        // Проверка, находится ли игрок в лодке И движется ли лодка
        boolean isInMovingBoat = false;
        if (mc.player.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat boat) {
            // Проверяем, движется ли лодка (есть горизонтальная скорость)
            isInMovingBoat = boat.getDeltaMovement().horizontalDistanceSqr() > 0.001;
        }

        if ((sprinting && moving) || isInMovingBoat) {
            // Бег или плавание на движущейся лодке - тратим стамину
            float staminaCost = isInMovingBoat ? 0.03f : 0.1f; // Лодка тратит медленнее
            stamina -= staminaCost;
        } else {
            // Не тратим стамину - восстанавливаем
            if (moving) {
                // Идём - медленное восстановление
                stamina += 0.04f;
            } else {
                // Стоим на месте - быстрое восстановление
                stamina += 0.16f;
            }
        }

        // Ограничиваем значения
        stamina = Math.max(0, Math.min(MAX_STAMINA, stamina));
    }


    // Добавьте этот метод в конец класса DamageCoreHudOverlay
    public static float getStamina() {
        return stamina;
    }


    private static void renderStaminaBar(GuiGraphics gui) {
        int barW = STAMINA_BASE_BAR_WIDTH;
        int barH = 6;

        int barX = 38;
        int barY = 33;

        int textureX = STAMINA_TEXTURE_X;
        int textureYFull = STAMINA_TEXTURE_Y;
        int textureYEmpty = STAMINA_TEXTURE_EMPTY_Y;

        int filledWidth = Math.max(0, Math.min(barW, (int)(barW * stamina / MAX_STAMINA)));

        int remaining = barW;
        int drawX = barX;

        // ===== ПУСТАЯ ПОЛОСКА (со скосами) =====
        int leftSkew = Math.min(EDGE_WIDTH, remaining);
        gui.blit(HUD_TEXTURE, drawX, barY, textureX, textureYEmpty, leftSkew, barH, 160, 208);
        drawX += leftSkew;
        remaining -= leftSkew;

        if (remaining > EDGE_WIDTH) {
            int mid = remaining - EDGE_WIDTH;
            gui.blit(HUD_TEXTURE, drawX, barY, textureX + EDGE_WIDTH, textureYEmpty, mid, barH, 160, 208);
            drawX += mid;
            remaining -= mid;
        }

        if (remaining > 0) {
            gui.blit(HUD_TEXTURE, drawX, barY,
                    textureX + TEXTURE_BAR_WIDTH - EDGE_WIDTH,
                    textureYEmpty,
                    remaining, barH, 160, 208);
        }

        // ===== ЗАПОЛНЕНИЕ =====
        if (filledWidth > 0) {
            int fillX = barX;
            int width = filledWidth;

            // Левый скос
            int left = Math.min(EDGE_WIDTH, width);
            gui.blit(HUD_TEXTURE, fillX, barY,
                    textureX, textureYFull,
                    left, barH, 160, 208);

            fillX += left;
            width -= left;

            // Середина
            int middleMax = barW - EDGE_WIDTH * 2;
            if (width > 0) {
                int mid = Math.min(width, middleMax);
                gui.blit(HUD_TEXTURE, fillX, barY,
                        textureX + EDGE_WIDTH, textureYFull,
                        mid, barH, 160, 208);

                fillX += mid;
                width -= mid;
            }

            // Правый скос
            if (width > 0) {
                int right = Math.min(width, EDGE_WIDTH);
                gui.blit(HUD_TEXTURE, fillX, barY,
                        textureX + TEXTURE_BAR_WIDTH - EDGE_WIDTH,
                        textureYFull,
                        right, barH, 160, 208);
            }
        }
    }

    private static void renderManaBar(GuiGraphics gui) {
        int barW = MANA_BASE_BAR_WIDTH;
        int barH = 6;

        int barX = 45;
        int barY = 25;

        int textureX = MANA_TEXTURE_X;
        int textureYFull = MANA_TEXTURE_Y;
        int textureYEmpty = MANA_TEXTURE_EMPTY_Y;

        int filledWidth = Math.max(0, Math.min(barW, (int)(barW * mana / MAX_MANA)));

        int remaining = barW;
        int drawX = barX;

        // ===== Пустая полоска =====
        int leftSkew = Math.min(EDGE_WIDTH, remaining);
        gui.blit(HUD_TEXTURE, drawX, barY, textureX, textureYEmpty, leftSkew, barH, 160, 208);
        drawX += leftSkew;
        remaining -= leftSkew;

        if (remaining > EDGE_WIDTH) {
            int mid = remaining - EDGE_WIDTH;
            gui.blit(HUD_TEXTURE, drawX, barY, textureX + EDGE_WIDTH, textureYEmpty, mid, barH, 160, 208);
            drawX += mid;
            remaining -= mid;
        }

        if (remaining > 0) {
            gui.blit(HUD_TEXTURE, drawX, barY,
                    textureX + TEXTURE_BAR_WIDTH - EDGE_WIDTH,
                    textureYEmpty,
                    remaining, barH, 160, 208);
        }

        // ===== Заполнение =====
        if (filledWidth > 0) {
            int fillX = barX;
            int width = filledWidth;

            // Левый скос
            int left = Math.min(EDGE_WIDTH, width);
            gui.blit(HUD_TEXTURE, fillX, barY,
                    textureX, textureYFull,
                    left, barH, 160, 208);

            fillX += left;
            width -= left;

            // Середина
            int middleMax = barW - EDGE_WIDTH * 2;
            if (width > 0) {
                int mid = Math.min(width, middleMax);
                gui.blit(HUD_TEXTURE, fillX, barY,
                        textureX + EDGE_WIDTH, textureYFull,
                        mid, barH, 160, 208);

                fillX += mid;
                width -= mid;
            }

            // Правый скос
            if (width > 0) {
                int right = Math.min(width, EDGE_WIDTH);
                gui.blit(HUD_TEXTURE, fillX, barY,
                        textureX + TEXTURE_BAR_WIDTH - EDGE_WIDTH,
                        textureYFull,
                        right, barH, 160, 208);
            }
        }
    }

    // ===== HP-полоска с растягиванием под health_boost =====
    private static void renderHealthAndAbsorptionBars(GuiGraphics gui, Minecraft mc) {
        float health = mc.player.getHealth();
        float maxHealth = mc.player.getMaxHealth();
        float absorption = mc.player.getAbsorptionAmount();

        int baseBarW = 52;
        float baseMaxHealth = 20f;
        int barW = (int)(baseBarW * (maxHealth / baseMaxHealth));
        int barH = 6;

        int barX = 45;
        int barY = 17;

        int textureX = 45;
        int textureYEmpty = 177;
        int textureYHealth = 81;
        int textureYAbsorption = 113;

        int healthWidth = Math.max(0, Math.min(barW, (int)(barW * health / maxHealth)));
        int absorptionWidth = Math.max(0, Math.min(barW, (int)(barW * absorption / maxHealth)));

        boolean hasAbsorption = absorptionWidth > 0;

        int remaining = barW;
        int drawX = barX;

        int leftSkew = Math.min(EDGE_WIDTH, remaining);
        gui.blit(HUD_TEXTURE, drawX, barY, textureX, textureYEmpty, leftSkew, barH, 160, 208);
        drawX += leftSkew;
        remaining -= leftSkew;

        if (remaining > EDGE_WIDTH) {
            int mid = remaining - EDGE_WIDTH;
            gui.blit(HUD_TEXTURE, drawX, barY, textureX + EDGE_WIDTH, textureYEmpty, mid, barH, 160, 208);
            drawX += mid;
            remaining -= mid;
        }

        if (remaining > 0) {
            gui.blit(HUD_TEXTURE, drawX, barY, textureX + TEXTURE_BAR_WIDTH - EDGE_WIDTH, textureYEmpty, remaining, barH, 160, 208);
        }

        // ===== Красная HP =====
        if (healthWidth > 0) {
            int fillX = barX;
            int width = healthWidth;

            // 1. Левый скос
            int left = Math.min(EDGE_WIDTH, width);
            gui.blit(HUD_TEXTURE, fillX, barY,
                    textureX, textureYHealth,
                    left, barH, 160, 208);

            fillX += left;
            width -= left;

            // 2. Середина
            int middleMax = barW - EDGE_WIDTH * 2;
            if (width > 0) {
                int mid = Math.min(width, middleMax);
                gui.blit(HUD_TEXTURE, fillX, barY,
                        textureX + EDGE_WIDTH, textureYHealth,
                        mid, barH, 160, 208);

                fillX += mid;
                width -= mid;
            }

            // 3. Правый скос - рисуем только если нет жёлтой полоски
            if (width > 0 && !hasAbsorption) {
                int right = Math.min(width, EDGE_WIDTH);
                gui.blit(HUD_TEXTURE, fillX, barY,
                        textureX + TEXTURE_BAR_WIDTH - EDGE_WIDTH,
                        textureYHealth,
                        right, barH, 160, 208);
            } else if (width > 0 && hasAbsorption) {
                // Если есть жёлтая, то используем середину для последней части красной
                gui.blit(HUD_TEXTURE, fillX, barY,
                        textureX + EDGE_WIDTH,
                        textureYHealth,
                        width, barH, 160, 208);
            }
        }

        // ===== Жёлтая полоска (Absorption) =====
        if (hasAbsorption) {
            int goldStartX = barX + healthWidth;
            int barEnd = barX + barW;
            int rightSkewStart = barEnd - EDGE_WIDTH;

            int absorptionEnd = goldStartX + absorptionWidth;

            int leftLen = 0;
            int rightLen = 0;

            // Левый скос только если начинается с самого начала бара
            if (goldStartX == barX) {
                leftLen = Math.min(EDGE_WIDTH, absorptionWidth);
            }

            // Правый скос - плавно уменьшается по мере убывания absorption
            if (absorptionEnd > rightSkewStart) {
                // absorptionEnd заходит в зону правого скоса
                rightLen = absorptionEnd - rightSkewStart; // сколько пикселей скоса видно
                rightLen = Math.min(rightLen, EDGE_WIDTH);
                rightLen = Math.min(rightLen, absorptionWidth - leftLen); // не больше чем есть
            }

            int midLen = absorptionWidth - leftLen - rightLen;
            if (midLen < 0) midLen = 0;

            drawX = goldStartX;

            // Левый
            if (leftLen > 0) {
                gui.blit(HUD_TEXTURE, drawX, barY,
                        textureX, textureYAbsorption,
                        leftLen, barH, 160, 208);
                drawX += leftLen;
            }

            // Середина
            if (midLen > 0) {
                gui.blit(HUD_TEXTURE, drawX, barY,
                        textureX + EDGE_WIDTH, textureYAbsorption,
                        midLen, barH, 160, 208);
                drawX += midLen;
            }

            // Правый скос - рисуем только видимую часть, смещая UV вправо
            if (rightLen > 0) {
                int uvOffset = EDGE_WIDTH - rightLen; // смещение в текстуре
                gui.blit(HUD_TEXTURE, drawX, barY,
                        textureX + TEXTURE_BAR_WIDTH - EDGE_WIDTH + uvOffset,
                        textureYAbsorption,
                        rightLen, barH, 160, 208);
            }
        }
    }
}