package ru.imaginaerum.damagecore.mixin.inventoty_screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.api.damage_book_protection.*;
import ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer.Render;
import ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer.SideTabsRenderer;
import ru.imaginaerum.damagecore.library_stats.IPlayerStats;
import ru.imaginaerum.damagecore.library_stats.PlayerStatsCapability;
import ru.imaginaerum.damagecore.library_stats.StatChangePacket;
import ru.imaginaerum.damagecore.library_stats.StatsType;
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;
import ru.imaginaerum.damagecore.mixin.ScreenInvoker;

import java.util.Arrays;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin implements ISkillTreeAccessor {

    private static final ResourceLocation SKILL_TREE_BUTTON =
            new ResourceLocation("damagecore", "textures/gui/skill_tree_button.png");

    @Unique
    private static final ResourceLocation DAMAGE_CORE_INTERFACE =
            new ResourceLocation("damagecore",
                    "textures/gui/container/creative_inventory/damage_core_interface.png");

    @Unique private static final int INV_BG_U   = 0;
    @Unique private static final int INV_BG_V   = 0;
    @Unique private static final int INV_BG_W   = 176;
    @Unique private static final int INV_BG_H   = 166;
    @Unique private static final int ATLAS_SIZE = 512;

    @Unique private ImageButton damagecore$recipeButton;
    @Unique private ImageButton damagecore$skillTreeButton;
    @Unique private static final int BUTTON_WIDTH  = 20;
    @Unique private static final int BUTTON_HEIGHT = 18;
    @Unique private boolean damageBookVisible = false;
    @Unique private boolean skillTreeVisible  = false;
    @Unique private static final int TAB_WIDTH             = DamageBookRenderer.TAB_WIDTH;
    @Unique private static final int RIGHT_INTERFACE_WIDTH = 289;
    @Unique private int recipeButtonOffsetX = 0;
    @Unique private int recipeButtonOffsetY = 0;
    @Unique private int selectedSmall       = 0;

    @Unique private static final int ROW_H    = 8;
    @Unique private static final int ROW_STEP = ROW_H + 5;

    @Unique private static final int STRIP_U = 176;
    @Unique private static final int STRIP_V = 225;
    @Unique private static final int STRIP_W = 5;
    @Unique private static final int STRIP_H = 15;
    @Unique private static final int STRIP_X = 165;
    @Unique private static final int STRIP_Y = 8;

    @Unique private static final int ROWS_TOTAL    = StatsType.values().length;
    @Unique private static final int ROWS_VISIBLE  = Math.min(4, ROWS_TOTAL);
    @Unique private static final int SCROLL_MAX_PX = (ROWS_TOTAL - ROWS_VISIBLE) * ROW_STEP;

    @Unique private static final int STRIP_Y_MIN_OFF  = STRIP_Y;
    @Unique private static final int STRIP_Y_MAX_OFF  = 60 - STRIP_H;
    @Unique private static final int STRIP_DRAG_RANGE = STRIP_Y_MAX_OFF - STRIP_Y_MIN_OFF;

    @Unique private static final int MINUS_U      = 182;
    @Unique private static final int MINUS_V      = 225;
    @Unique private static final int MINUS_HOVER_U = 182;
    @Unique private static final int MINUS_HOVER_V = 233;
    @Unique private static final int MINUS_W      = 11;
    @Unique private static final int MINUS_H      = 7;
    @Unique private static final int MINUS_X      = 99;
    @Unique private static final int MINUS_Y      = 13;
    @Unique private static final int MINUS_STEP   = ROW_STEP;

    @Unique private static final int PLUS_U       = 194;
    @Unique private static final int PLUS_V       = 225;
    @Unique private static final int PLUS_HOVER_U = 194;
    @Unique private static final int PLUS_HOVER_V = 233;
    @Unique private static final int PLUS_W       = 11;
    @Unique private static final int PLUS_H       = 7;
    @Unique private static final int PLUS_X       = 111;
    @Unique private static final int PLUS_Y       = 13;
    @Unique private static final int PLUS_STEP    = ROW_STEP;

    @Unique private int     damagecore$stripOffsetY  = 0;
    @Unique private boolean damagecore$draggingStrip = false;
    @Unique private double  damagecore$dragMouseY0   = 0;
    @Unique private int     damagecore$dragStripY0   = 0;

    @Unique private int damagecore$activeSideTab = SideTabsRenderer.TAB_NONE;

    // -------------------------------------------------------------------------
    @Unique
    private static int damagecore$getClientXp(net.minecraft.world.entity.player.Player player) {
        int level    = player.experienceLevel;
        float prog   = player.experienceProgress;

        int xpToNext;
        if (level >= 30)      xpToNext = 112 + (level - 30) * 9;
        else if (level >= 15) xpToNext = 37  + (level - 15) * 5;
        else                  xpToNext = 7   + level * 2;

        int totalForLevel;
        if (level >= 32)      totalForLevel = (int)(4.5 * level * level - 162.5 * level + 2220);
        else if (level >= 17) totalForLevel = (int)(2.5 * level * level - 40.5  * level + 360);
        else                  totalForLevel = level * level + 6 * level;

        return totalForLevel + (int)(prog * xpToNext);
    }

    @Unique
    @Override
    public void damagecore$scrollList(double delta) {
        damagecore$stripOffsetY = Math.max(0,
                Math.min(STRIP_DRAG_RANGE, damagecore$stripOffsetY - (int)(delta * ROW_STEP)));
    }

    @Unique
    private static ItemStack damagecore$getHoveredStack(InventoryScreen screen,
                                                        double mouseX, double mouseY) {
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop  = ((AbstractContainerScreenAccessor) screen).getTopPos();
        for (var slot : screen.getMenu().slots) {
            int sx = guiLeft + slot.x;
            int sy = guiTop  + slot.y;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16)
                return slot.getItem();
        }
        return ItemStack.EMPTY;
    }

    // -------------------------------------------------------------------------
    // renderBg
    // -------------------------------------------------------------------------
    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void damagecore$replaceVanillaInventoryBg(GuiGraphics gui, float partialTick,
                                                      int mouseX, int mouseY, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        int leftPos = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int topPos  = ((AbstractContainerScreenAccessor) screen).getTopPos();

        if (this.damagecore$recipeButton != null && this.damagecore$skillTreeButton != null) {
            int newRecipeX = leftPos + this.recipeButtonOffsetX;
            int newRecipeY = topPos  + this.recipeButtonOffsetY;
            this.damagecore$recipeButton.setPosition(newRecipeX, newRecipeY);
            int skillX = newRecipeX + this.damagecore$recipeButton.getWidth() + 2;
            this.damagecore$skillTreeButton.setPosition(skillX, newRecipeY);
        }

        if (!this.skillTreeVisible) return;

        // Кастомный фон
        gui.blit(DAMAGE_CORE_INTERFACE, leftPos, topPos,
                INV_BG_U, INV_BG_V, INV_BG_W, INV_BG_H, ATLAS_SIZE, ATLAS_SIZE);

        // Полоска скролла
        gui.blit(DAMAGE_CORE_INTERFACE,
                leftPos + STRIP_X, topPos + STRIP_Y_MIN_OFF + damagecore$stripOffsetY,
                STRIP_U, STRIP_V, STRIP_W, STRIP_H, ATLAS_SIZE, ATLAS_SIZE);

        // Вкладка броня
        int armorU = (damagecore$activeSideTab == SideTabsRenderer.TAB_ARMOR) ? 208 : 211;
        int armorV = (damagecore$activeSideTab == SideTabsRenderer.TAB_ARMOR) ? 207 : 179;
        int armorW = (damagecore$activeSideTab == SideTabsRenderer.TAB_ARMOR) ? 32  : 25;
        int armorX = leftPos + 466 - (damagecore$activeSideTab == SideTabsRenderer.TAB_ARMOR ? 3 : 0);
        gui.blit(DAMAGE_CORE_INTERFACE,
                armorX, topPos + 4,
                armorU, armorV, armorW, 28, ATLAS_SIZE, ATLAS_SIZE);

// Вкладка эффекты
        int potionU = (damagecore$activeSideTab == SideTabsRenderer.TAB_POTION) ? 240 : 211;
        int potionV = (damagecore$activeSideTab == SideTabsRenderer.TAB_POTION) ? 207 : 179;
        int potionW = (damagecore$activeSideTab == SideTabsRenderer.TAB_POTION) ? 32  : 25;
        int potionX = leftPos + 466 - (damagecore$activeSideTab == SideTabsRenderer.TAB_POTION ? 3 : 0);
        gui.blit(DAMAGE_CORE_INTERFACE,
                potionX, topPos + 33,
                potionU, potionV, potionW, 28, ATLAS_SIZE, ATLAS_SIZE);
// Иконка брони (незеритовый шлем)
        gui.renderItem(new ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET),
                armorX + (armorW - 16) / 2, topPos + 4 + 6);

// Иконка эффектов (золотая морковь)
        gui.renderItem(new ItemStack(net.minecraft.world.item.Items.GOLDEN_CARROT),
                potionX + (potionW - 16) / 2, topPos + 33 + 6);
        // Строки текста + кнопки
        Component[] rowLabels = Arrays.stream(StatsType.values())
                .map(s -> Component.translatable(s.getTranslationKey()))
                .toArray(Component[]::new);

        int scrollPx = STRIP_DRAG_RANGE > 0
                ? (damagecore$stripOffsetY * SCROLL_MAX_PX) / STRIP_DRAG_RANGE : 0;

        gui.enableScissor(leftPos + 97, topPos + 8, leftPos + 163, topPos + 60);
        for (int i = 0; i < ROWS_TOTAL; i++) {
            int rowScreenY = topPos + 8 + i * ROW_STEP - scrollPx;

            StatsType statType  = StatsType.values()[i];
            int statValue  = PlayerStatsCapability.get(Minecraft.getInstance().player)
                    .map(s -> s.getStat(statType)).orElse(0);
            int pressCount = PlayerStatsCapability.get(Minecraft.getInstance().player)
                    .map(s -> s.getPressCount(statType)).orElse(0);
            int nextCost   = PlayerStatsCapability.get(Minecraft.getInstance().player)
                    .map(s -> s.getNextCost(statType)).orElse(IPlayerStats.BASE_COST);
            int playerXp   = Minecraft.getInstance().player != null
                    ? damagecore$getClientXp(Minecraft.getInstance().player) : 0;

            boolean isZero      = statValue == 0 && pressCount == 0;
            boolean plusBlocked = pressCount >= IPlayerStats.MAX_LEVEL || playerXp < nextCost;

            // Кнопка минус
            int minusScreenX = leftPos + MINUS_X;
            int minusScreenY = topPos  + MINUS_Y + i * MINUS_STEP - scrollPx;
            boolean minusHovered = !isZero
                    && mouseX >= minusScreenX && mouseX < minusScreenX + MINUS_W / 1.2f
                    && mouseY >= minusScreenY && mouseY < minusScreenY + MINUS_H / 1.2f;
            int minusU = isZero ? 182 : (minusHovered ? MINUS_HOVER_U : MINUS_U);
            int minusV = isZero ? 241 : (minusHovered ? MINUS_HOVER_V : MINUS_V);
            gui.pose().pushPose();
            gui.pose().translate(minusScreenX, minusScreenY, 0);
            gui.pose().scale(1f / 1.2f, 1f / 1.2f, 1f);
            gui.blit(DAMAGE_CORE_INTERFACE, 0, 0, minusU, minusV, MINUS_W, MINUS_H, ATLAS_SIZE, ATLAS_SIZE);
            gui.pose().popPose();

            // Кнопка плюс
            int plusScreenX = leftPos + PLUS_X;
            int plusScreenY = topPos  + PLUS_Y + i * PLUS_STEP - scrollPx;
            boolean plusHovered = !plusBlocked
                    && mouseX >= plusScreenX && mouseX < plusScreenX + PLUS_W / 1.2f
                    && mouseY >= plusScreenY && mouseY < plusScreenY + PLUS_H / 1.2f;
            int plusU = plusBlocked ? 194 : (plusHovered ? PLUS_HOVER_U : PLUS_U);
            int plusV = plusBlocked ? 241 : (plusHovered ? PLUS_HOVER_V : PLUS_V);
            gui.pose().pushPose();
            gui.pose().translate(plusScreenX, plusScreenY, 0);
            gui.pose().scale(1f / 1.2f, 1f / 1.2f, 1f);
            gui.blit(DAMAGE_CORE_INTERFACE, 0, 0, plusU, plusV, PLUS_W, PLUS_H, ATLAS_SIZE, ATLAS_SIZE);
            gui.pose().popPose();

            // Число
            int numX = leftPos + PLUS_X + PLUS_W + 3;
            gui.pose().pushPose();
            gui.pose().translate(numX, plusScreenY, 0);
            gui.pose().scale(1f / 1.4f, 1f / 1.4f, 1f);
            gui.drawString(Minecraft.getInstance().font,
                    Component.literal(String.valueOf(statValue)), 0, 0, 0xFFFFFF, true);
            gui.pose().popPose();

            // Текст строки
            gui.pose().pushPose();
            gui.pose().translate(leftPos + 99, rowScreenY, 0);
            gui.pose().scale(0.5f, 0.5f, 1f);
            gui.drawString(Minecraft.getInstance().font, rowLabels[i], 0, 0, 0xFFFFFF, true);
            gui.pose().popPose();
        }
        gui.disableScissor();

        if (Minecraft.getInstance().player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    gui, leftPos + 51, topPos + 75, 30,
                    (float)(leftPos + 51) - mouseX,
                    (float)(topPos + 75 - 50) - mouseY,
                    Minecraft.getInstance().player);
        }

        ci.cancel();
    }

    // -------------------------------------------------------------------------
    // renderLabels
    // -------------------------------------------------------------------------
    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void damagecore$hideLabels(GuiGraphics gui, int mouseX, int mouseY, CallbackInfo ci) {
        if (!this.skillTreeVisible) return;
        ci.cancel();
    }

    // -------------------------------------------------------------------------
    // render TAIL
    // -------------------------------------------------------------------------
    @Inject(method = "render", at = @At("TAIL"))
    private void damagecore$renderAll(GuiGraphics gui, int mouseX, int mouseY,
                                      float partialTicks, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        if (damagecore$draggingStrip) {
            int delta = (int)(mouseY - damagecore$dragMouseY0);
            damagecore$stripOffsetY = Math.max(0,
                    Math.min(STRIP_DRAG_RANGE, damagecore$dragStripY0 + delta));
        }

        if (this.skillTreeVisible) {
            int panelScreenX = guiLeft + imageWidth + 2;
            int panelScreenY = guiTop;
            int tabX         = guiLeft + imageWidth;
            boolean sideTabActive = damagecore$activeSideTab != SideTabsRenderer.TAB_NONE;

            DamageBookRenderer.renderRightInterface(gui, screen, tabX, guiTop, mouseX, mouseY, sideTabActive);

            if (sideTabActive) {
                SideTabsRenderer.render(gui, panelScreenX, panelScreenY,
                        damagecore$activeSideTab, mouseX, mouseY);
            } else {
                SkillTreeRenderer.mouseDragged(mouseX, mouseY, 0, panelScreenX, panelScreenY);
                Render.currentHoveredNode = Render.getHoveredNodeUnderMouse(mouseX, mouseY);
            }

            // Боковые вкладки поверх правой панели
            int armorU = (damagecore$activeSideTab == SideTabsRenderer.TAB_ARMOR) ? 208 : 211;
            int armorV = (damagecore$activeSideTab == SideTabsRenderer.TAB_ARMOR) ? 207 : 179;
            int armorW = (damagecore$activeSideTab == SideTabsRenderer.TAB_ARMOR) ? 32  : 25;
            int armorX = guiLeft + 466 - (damagecore$activeSideTab == SideTabsRenderer.TAB_ARMOR ? 3 : 0);
            gui.blit(DAMAGE_CORE_INTERFACE,
                    armorX, guiTop + 4,
                    armorU, armorV, armorW, 28, ATLAS_SIZE, ATLAS_SIZE);
            gui.renderItem(new ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET),
                    armorX + (armorW - 16) / 2, guiTop + 4 + 6);

            int potionU = (damagecore$activeSideTab == SideTabsRenderer.TAB_POTION) ? 240 : 211;
            int potionV = (damagecore$activeSideTab == SideTabsRenderer.TAB_POTION) ? 207 : 179;
            int potionW = (damagecore$activeSideTab == SideTabsRenderer.TAB_POTION) ? 32  : 25;
            int potionX = guiLeft + 466 - (damagecore$activeSideTab == SideTabsRenderer.TAB_POTION ? 3 : 0);
            gui.blit(DAMAGE_CORE_INTERFACE,
                    potionX, guiTop + 33,
                    potionU, potionV, potionW, 28, ATLAS_SIZE, ATLAS_SIZE);
            gui.renderItem(new ItemStack(net.minecraft.world.item.Items.GOLDEN_CARROT),
                    potionX + (potionW - 16) / 2, guiTop + 33 + 6);
        }
    }

    // -------------------------------------------------------------------------
    // mouseClicked — боковые вкладки
    // -------------------------------------------------------------------------
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void damagecore$sideTabsClick(double mouseX, double mouseY, int button,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible || button != 0) return;

        InventoryScreen screen = (InventoryScreen)(Object)this;
        int leftPos = ((AbstractContainerScreenAccessor)screen).getLeftPos();
        int topPos  = ((AbstractContainerScreenAccessor)screen).getTopPos();

        // Вкладка броня
        if (mouseX >= leftPos + 466 && mouseX < leftPos + 491
                && mouseY >= topPos + 4 && mouseY < topPos + 32) {
            if (damagecore$activeSideTab == SideTabsRenderer.TAB_ARMOR) {
                damagecore$activeSideTab = SideTabsRenderer.TAB_NONE;
                // Восстанавливаем первое дерево
                DamageBookRenderer.setBottomTab(0);
                SkillTreeRenderer.setActiveTree(0);
            } else {
                damagecore$activeSideTab = SideTabsRenderer.TAB_ARMOR;
                // Снимаем выделение со всех вкладок дерева не удаляя их визуально
                DamageBookRenderer.setBottomTab(Integer.MAX_VALUE);
                SkillTreeRenderer.resetTreePosition();
            }
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            cir.setReturnValue(true);
            return;
        }

// Вкладка эффекты
        if (mouseX >= leftPos + 466 && mouseX < leftPos + 491
                && mouseY >= topPos + 33 && mouseY < topPos + 61) {
            if (damagecore$activeSideTab == SideTabsRenderer.TAB_POTION) {
                damagecore$activeSideTab = SideTabsRenderer.TAB_NONE;
                DamageBookRenderer.setBottomTab(0);
                SkillTreeRenderer.setActiveTree(0);
            } else {
                damagecore$activeSideTab = SideTabsRenderer.TAB_POTION;
                DamageBookRenderer.setBottomTab(Integer.MAX_VALUE);
                SkillTreeRenderer.resetTreePosition();
            }
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            cir.setReturnValue(true);
        }
    }

    // -------------------------------------------------------------------------
    // mouseClicked — кнопки статов
    // -------------------------------------------------------------------------
    @Inject(method = "mouseClicked", at = @At("TAIL"), cancellable = true)
    private void damagecore$statsButtonsClick(double mouseX, double mouseY, int button,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible || button != 0) return;
        if (Minecraft.getInstance().player == null) return;

        InventoryScreen screen = (InventoryScreen)(Object)this;
        int leftPos = ((AbstractContainerScreenAccessor)screen).getLeftPos();
        int topPos  = ((AbstractContainerScreenAccessor)screen).getTopPos();

        int scrollPx = STRIP_DRAG_RANGE > 0
                ? (damagecore$stripOffsetY * SCROLL_MAX_PX) / STRIP_DRAG_RANGE : 0;

        for (int i = 0; i < ROWS_TOTAL; i++) {
            StatsType statType   = StatsType.values()[i];
            int minusScreenX     = leftPos + MINUS_X;
            int minusScreenY     = topPos  + MINUS_Y + i * MINUS_STEP - scrollPx;
            int plusScreenX      = leftPos + PLUS_X;
            int plusScreenY      = topPos  + PLUS_Y  + i * PLUS_STEP  - scrollPx;

            if (mouseX >= plusScreenX && mouseX < plusScreenX + PLUS_W / 1.2f
                    && mouseY >= plusScreenY && mouseY < plusScreenY + PLUS_H / 1.2f) {
                ModNetwork.CHANNEL.sendToServer(new StatChangePacket(statType, true));
                cir.setReturnValue(true);
                return;
            }
            if (mouseX >= minusScreenX && mouseX < minusScreenX + MINUS_W / 1.2f
                    && mouseY >= minusScreenY && mouseY < minusScreenY + MINUS_H / 1.2f) {
                ModNetwork.CHANNEL.sendToServer(new StatChangePacket(statType, false));
                cir.setReturnValue(true);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // mouseClicked — полоска скролла
    // -------------------------------------------------------------------------
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void damagecore$stripMouseClicked(double mouseX, double mouseY, int button,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible || button != 0) return;
        InventoryScreen screen = (InventoryScreen)(Object)this;
        int leftPos = ((AbstractContainerScreenAccessor)screen).getLeftPos();
        int topPos  = ((AbstractContainerScreenAccessor)screen).getTopPos();

        int sx = leftPos + STRIP_X;
        int sy = topPos  + STRIP_Y_MIN_OFF + damagecore$stripOffsetY;

        if (mouseX >= sx && mouseX < sx + STRIP_W &&
                mouseY >= sy && mouseY < sy + STRIP_H) {
            damagecore$draggingStrip = true;
            damagecore$dragMouseY0   = mouseY;
            damagecore$dragStripY0   = damagecore$stripOffsetY;
            cir.setReturnValue(true);
        }
    }

    // -------------------------------------------------------------------------
    // mouseReleased
    // -------------------------------------------------------------------------
    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void damagecore$stripMouseReleased(double mouseX, double mouseY, int button,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (button == 0 && damagecore$draggingStrip)
            damagecore$draggingStrip = false;
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void damagecore$skillTree_mouseReleased(double mouseX, double mouseY, int button,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible) return;
        boolean consumed = SkillTreeRenderer.mouseReleased((int) mouseX, (int) mouseY, button);
        if (button == 0) {
            Render.currentHoveredNode = null;
            Render.mousePressTime = 0L;
        }
        if (consumed) cir.setReturnValue(true);
    }

    // -------------------------------------------------------------------------
    // mouseClicked — bottom tabs, skill tree press
    // -------------------------------------------------------------------------
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void damagecore$bottomTabsClick(double mouseX, double mouseY, int button,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int panelLeft = guiLeft + imageWidth + 2;
        int panelTop  = guiTop;
        int PANEL_W   = 289;
        int TAB_W     = 28;
        int bottomY   = panelTop + 163;
        int topY      = panelTop - 25;

        if (DamageBookRenderer.handleArrowClick(mouseX, mouseY, panelLeft, panelTop, PANEL_W)) {
            cir.setReturnValue(true); return;
        }
        if (handleRowClick(mouseX, mouseY, panelLeft, PANEL_W, TAB_W, bottomY, true)) {
            damagecore$activeSideTab = SideTabsRenderer.TAB_NONE;
            cir.setReturnValue(true); return;
        }
        if (handleRowClick(mouseX, mouseY, panelLeft, PANEL_W, TAB_W, topY, false)) {
            damagecore$activeSideTab = SideTabsRenderer.TAB_NONE;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void damagecore$closeDamageBookWhenRecipeOpen(double mouseX, double mouseY, int button,
                                                          CallbackInfoReturnable<Boolean> cir) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        if (screen.getRecipeBookComponent().isVisible()) {
            boolean changed = false;
            if (this.damageBookVisible) { this.damageBookVisible = false; changed = true; }
            if (this.skillTreeVisible)  { this.skillTreeVisible  = false; changed = true; }
            if (changed) damagecore$updateInventoryPosition(screen);
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void damagecore$handleSmallClick(double mouseX, double mouseY, int button,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!this.damageBookVisible) return;
        InventoryScreen screen = (InventoryScreen) (Object) this;
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop  = ((AbstractContainerScreenAccessor) screen).getTopPos();
        this.selectedSmall = DamageBookInputHandler.handleSmallTabsClick(
                mouseX, mouseY, screen, this.selectedSmall,
                guiLeft - TAB_WIDTH, guiTop);
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"), cancellable = true)
    private void damagecore$skillTree_mousePressed(double mouseX, double mouseY, int button,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible) return;
        if (damagecore$activeSideTab != SideTabsRenderer.TAB_NONE) return;

        InventoryScreen screen = (InventoryScreen) (Object) this;
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int panelScreenX = guiLeft + imageWidth + 2;
        int panelScreenY = guiTop;

        boolean consumed = SkillTreeRenderer.mousePressed(
                (int) mouseX, (int) mouseY, button, panelScreenX, panelScreenY);

        if (button == 0) {
            SkillTreeNode hovered = Render.getHoveredNodeUnderMouse((int) mouseX, (int) mouseY);
            if (hovered != null && !hovered.isMaxLevel() && !hovered.locked) {
                Render.currentHoveredNode = hovered;
                Render.mousePressTime = System.currentTimeMillis();
            } else {
                Render.currentHoveredNode = null;
                Render.mousePressTime = 0L;
            }
        }
        if (consumed) cir.setReturnValue(true);
    }

    // -------------------------------------------------------------------------
    // init
    // -------------------------------------------------------------------------
    @Inject(method = "init", at = @At("HEAD"))
    private void damagecore$resetSyncFlagOnInit(CallbackInfo ci) {
        ClientSyncState.syncRequested = false;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void damagecore$init(CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        SkillTreeRenderer.loadAllTrees("skill_tree");

        for (var child : screen.children()) {
            if (child instanceof ImageButton btn
                    && btn.getWidth()  == BUTTON_WIDTH
                    && btn.getHeight() == BUTTON_HEIGHT) {
                this.damagecore$recipeButton = btn;
                this.recipeButtonOffsetX = btn.getX() - ((AbstractContainerScreenAccessor) screen).getLeftPos();
                this.recipeButtonOffsetY = btn.getY() - ((AbstractContainerScreenAccessor) screen).getTopPos();
                break;
            }
        }

        this.damagecore$skillTreeButton = new ImageButton(
                0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, 0, 0, 19, SKILL_TREE_BUTTON, btn -> {
            InventoryScreen s = (InventoryScreen) (Object) this;
            this.skillTreeVisible = !this.skillTreeVisible;
            if (this.skillTreeVisible) {
                if (s.getRecipeBookComponent().isVisible())
                    s.getRecipeBookComponent().toggleVisibility();
                this.damageBookVisible = false;
                int currentTab = DamageBookRenderer.selectedBottomTab;
                if (currentTab < SkillTreeRenderer.getTotalTrees())
                    SkillTreeRenderer.setActiveTree(currentTab);
            } else {
                SkillTreeRenderer.resetTreePosition();
                damagecore$activeSideTab = SideTabsRenderer.TAB_NONE;
            }
            damagecore$updateInventoryPosition(s);
        });

        ((ScreenInvoker) screen).damagecore$addRenderableWidget(this.damagecore$skillTreeButton);

        if (Minecraft.getInstance().player != null && !ClientSyncState.syncRequested) {
            ModNetwork.CHANNEL.sendToServer(new RequestFullSyncPacket());
            ClientSyncState.syncRequested = true;
        }
    }

    // -------------------------------------------------------------------------
    // Вспомогательные методы
    // -------------------------------------------------------------------------
    @Override
    public boolean damagecore$isSkillTreeVisible() {
        return this.skillTreeVisible;
    }

    @Unique
    private boolean clickTab(double mx, double my, int x, int y, int w, int globalId) {
        if (!inside(mx, my, x, y - 3, w, 32)) return false;
        if (DamageBookRenderer.selectedBottomTab != globalId) {
            DamageBookRenderer.setBottomTab(globalId);
            SkillTreeRenderer.setActiveTree(globalId);
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        return true;
    }

    @Unique
    private boolean handleRowClick(double mx, double my, int panelLeft, int panelW,
                                   int tabW, int y, boolean bottom) {
        int rowBase = bottom ? 0 : DamageBookRenderer.TABS_PER_ROW;

        int globalLeft = DamageBookRenderer.globalIdForSlot(rowBase);
        if (SkillTreeRenderer.hasTreeForTab(globalLeft))
            if (clickTab(mx, my, panelLeft, y, tabW, globalLeft)) return true;

        for (int i = 0; i < DamageBookRenderer.MIDDLE_TABS; i++) {
            int globalId = DamageBookRenderer.globalIdForSlot(rowBase + 1 + i);
            if (!SkillTreeRenderer.hasTreeForTab(globalId)) continue;
            int x = DamageBookRenderer.calcMiddleX(i, panelLeft, panelW, tabW);
            if (clickTab(mx, my, x, y, tabW, globalId)) return true;
        }

        int globalRight = DamageBookRenderer.globalIdForSlot(rowBase + DamageBookRenderer.TABS_PER_ROW - 1);
        return SkillTreeRenderer.hasTreeForTab(globalRight)
                && clickTab(mx, my, panelLeft + panelW - tabW, y, tabW, globalRight);
    }

    @Unique
    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Unique
    private void damagecore$updateInventoryPosition(InventoryScreen screen) {
        DamageBookPositionHelper.updateInventoryPosition(
                screen, this.damageBookVisible, this.skillTreeVisible,
                TAB_WIDTH, RIGHT_INTERFACE_WIDTH);
    }
}