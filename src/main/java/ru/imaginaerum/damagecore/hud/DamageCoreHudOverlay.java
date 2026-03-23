package ru.imaginaerum.damagecore.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
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

    // ===== MANA =====
    private static float mana = 40f;
    private static final float MAX_MANA = 40f;
    private static final int MANA_TEXTURE_X = 45;
    private static final int MANA_TEXTURE_Y = 89;
    private static final int MANA_TEXTURE_EMPTY_Y = 185;

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

        if (sprinting && moving) {
            // Бег - тратим стамину
            stamina -= 0.1f;
        } else {
            // Не бежим - восстанавливаем стамину
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

    // ===== Рисование полоски стамины и маны без скосов =====
    private static void renderSimpleBar(GuiGraphics gui, int barX, int barY,
                                        int textureX, int textureYFull, int textureYEmpty,
                                        float value, float maxValue) {
        int barW = TEXTURE_BAR_WIDTH;
        int barH = 6;

        // Сначала пустая
        gui.blit(HUD_TEXTURE, barX, barY, textureX, textureYEmpty, barW, barH, 160, 208);
        // Потом заполнение
        int filled = (int) (barW * (value / maxValue));
        if (filled > 0) {
            gui.blit(HUD_TEXTURE, barX, barY, textureX, textureYFull, filled, barH, 160, 208);
        }
    }
    // Добавьте этот метод в конец класса DamageCoreHudOverlay
    public static float getStamina() {
        return stamina;
    }

    // Если нужен также сеттер для изменения стамины извне
    public static void setStamina(float value) {
        stamina = Math.max(0, Math.min(MAX_STAMINA, value));
    }

    // Метод для расходования стамины при беге
    public static void consumeStaminaForSprint(float amount) {
        stamina -= amount;
        if (stamina < 0) stamina = 0;
    }
    private static void renderStaminaBar(GuiGraphics gui) {
        renderSimpleBar(gui, 38, 33, STAMINA_TEXTURE_X, STAMINA_TEXTURE_Y, STAMINA_TEXTURE_EMPTY_Y, stamina, MAX_STAMINA);
    }

    private static void renderManaBar(GuiGraphics gui) {
        renderSimpleBar(gui, 45, 25, MANA_TEXTURE_X, MANA_TEXTURE_Y, MANA_TEXTURE_EMPTY_Y, mana, MAX_MANA);
    }

    // ===== HP-полоска с растягиванием под health_boost =====
    private static void renderHealthAndAbsorptionBars(GuiGraphics gui, Minecraft mc) {
        float health = mc.player.getHealth();
        float maxHealth = mc.player.getMaxHealth();
        float absorption = mc.player.getAbsorptionAmount();

        int baseBarW = 52;
        float baseMaxHealth = 20f; // стандартное здоровье
        int barW = (int)(baseBarW * (maxHealth / baseMaxHealth)); // растягиваем под health_boost
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

            // 3. Правый скос — заполняется постепенно (последние 6px)
            // 3. Правый край
            if (width > 0) {
                int right = Math.min(width, EDGE_WIDTH);

                if (hasAbsorption) {
                    // Если есть absorption — используем середину вместо правого скоса
                    gui.blit(HUD_TEXTURE, fillX, barY,
                            textureX + EDGE_WIDTH,
                            textureYHealth,
                            right, barH, 160, 208);
                } else {
                    // Если absorption нет — обычный правый скос
                    gui.blit(HUD_TEXTURE, fillX, barY,
                            textureX + TEXTURE_BAR_WIDTH - EDGE_WIDTH,
                            textureYHealth,
                            right, barH, 160, 208);
                }
            }
        }

        // ===== Жёлтая полоска (Absorption) =====
        if (hasAbsorption) {
            boolean hasHealth = healthWidth > 0;
            int goldX = hasHealth ? barX + healthWidth : barX;

            remaining = absorptionWidth;
            drawX = goldX;

            if (!hasHealth && remaining > 0) {
                leftSkew = Math.min(EDGE_WIDTH, remaining);
                gui.blit(HUD_TEXTURE, drawX, barY, textureX, textureYAbsorption, leftSkew, barH, 160, 208);
                drawX += leftSkew;
                remaining -= leftSkew;
            }

            if (remaining > EDGE_WIDTH) {
                int mid = remaining - EDGE_WIDTH;
                gui.blit(HUD_TEXTURE, drawX, barY, textureX + EDGE_WIDTH, textureYAbsorption, mid, barH, 160, 208);
                drawX += mid;
                remaining -= mid;
            }

            if (remaining > 0) {
                gui.blit(HUD_TEXTURE, drawX, barY, textureX + TEXTURE_BAR_WIDTH - EDGE_WIDTH, textureYAbsorption, remaining, barH, 160, 208);
            }
        }
    }
}