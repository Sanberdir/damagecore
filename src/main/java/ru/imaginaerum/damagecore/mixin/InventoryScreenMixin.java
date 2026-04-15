package ru.imaginaerum.damagecore.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.ModNetwork;
import ru.imaginaerum.damagecore.api.damage_book_protection.*;
import ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer.Render;
import ru.imaginaerum.damagecore.api.damage_book_protection.stats_field.ArmorStatsFieldRenderer;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin implements ISkillTreeAccessor {
    private static final ResourceLocation SKILL_TREE_BUTTON = new ResourceLocation("damagecore", "textures/gui/skill_tree_button.png");
    private static final ResourceLocation ARMOR_STATS = new ResourceLocation("damagecore", "textures/gui/armor_statistics.png");

    @Unique
    private static final ResourceLocation ARMOR_STATS_FIELD =
            new ResourceLocation("damagecore", "textures/gui/container/creative_inventory/armor_statistic_field.png");

    @Unique
    private boolean armorStatsVisible = false;
    @Unique
    private ImageButton damagecore$recipeButton;
    @Unique
    private ImageButton damagecore$skillTreeButton;
    @Unique
    private static final int BUTTON_WIDTH = 20;
    @Unique
    private static final int BUTTON_HEIGHT = 18;
    @Unique
    private boolean damageBookVisible = false;
    @Unique
    private boolean skillTreeVisible = false;
    @Unique
    private static final int TAB_WIDTH = DamageBookRenderer.TAB_WIDTH;
    @Unique
    private static final int RIGHT_INTERFACE_WIDTH = 289;
    @Unique
    private int recipeButtonOffsetX = 0;
    @Unique
    private int recipeButtonOffsetY = 0;
    @Unique
    private int selectedSmall = 0;

    @Unique
    private static ItemStack damagecore$getHoveredStack(InventoryScreen screen, double mouseX, double mouseY) {
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop = ((AbstractContainerScreenAccessor) screen).getTopPos();

        for (var slot : screen.getMenu().slots) {
            int slotX = guiLeft + slot.x;
            int slotY = guiTop + slot.y;

            if (mouseX >= slotX && mouseX < slotX + 16 &&
                    mouseY >= slotY && mouseY < slotY + 16) {
                return slot.getItem();
            }
        }
        return ItemStack.EMPTY;
    }

    // -------------------------------------------------------------------------
    // ЕДИНСТВЕННЫЙ render TAIL — всё рисуем здесь, ArmorStats последним
    // -------------------------------------------------------------------------
    @Inject(method = "render", at = @At("TAIL"))
    private void damagecore$renderAll(GuiGraphics gui, int mouseX, int mouseY,
                                      float partialTicks, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen)(Object)this;

        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        // 1) Skill tree панель
        if (this.skillTreeVisible) {
            int tabX = guiLeft + imageWidth;
            int tabY = guiTop;
            DamageBookRenderer.renderRightInterface(gui, screen, tabX, tabY, mouseX, mouseY);
        }

        // 2) Skill tree hold (перетаскивание/наведение)
        if (this.skillTreeVisible) {
            int panelScreenX = guiLeft + imageWidth + 2;
            int panelScreenY = guiTop;
            SkillTreeRenderer.mouseDragged(mouseX, mouseY, 0, panelScreenX, panelScreenY);
            Render.currentHoveredNode = Render.getHoveredNodeUnderMouse(mouseX, mouseY);
        }

        // 3) Иконка кнопки armor stats
        {
            int x = guiLeft + 62;
            int y = guiTop + 10;
            int iconW = 10;
            int iconH = 9;
            boolean hover = mouseX >= x && mouseX < x + iconW && mouseY >= y && mouseY < y + iconH;
            if (hover) {
                gui.blit(ARMOR_STATS, x, y, 0, 10, iconW, iconH, 10, 19);
            } else {
                gui.blit(ARMOR_STATS, x, y, 0, 0, iconW, iconH, 10, 19);
            }
        }

        // 4) ArmorStats интерфейс — САМЫМ ПОСЛЕДНИМ, поверх всего
        if (this.armorStatsVisible) {
            ItemStack hovered = damagecore$getHoveredStack(screen, mouseX, mouseY);
            ArmorStatsFieldRenderer.setHoveredArmor(hovered);
            ArmorStatsFieldRenderer.render(gui, screen);
        }
    }

    // -------------------------------------------------------------------------
    // HEAD инджект для detectHoveredArmor остаётся отдельно — нужен в начале
    // -------------------------------------------------------------------------
    @Inject(method = "render", at = @At("HEAD"))
    private void damagecore$detectHoveredArmor(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                               float partialTick, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        Slot hoveredSlot = ((AbstractContainerScreenAccessor)screen).getHoveredSlot();

        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            ArmorStatsFieldRenderer.setHoveredArmor(hoveredSlot.getItem());
        } else {
            ArmorStatsFieldRenderer.setHoveredArmor(ItemStack.EMPTY);
        }
    }

    // -------------------------------------------------------------------------
    // mouseClicked
    // -------------------------------------------------------------------------
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void damagecore$toggleArmorStats(double mouseX, double mouseY, int button,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;
        InventoryScreen screen = (InventoryScreen)(Object)this;

        if (ArmorStatsFieldRenderer.isButtonHovered(mouseX, mouseY, screen)) {
            this.armorStatsVisible = !this.armorStatsVisible;
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
            );
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void damagecore$bottomTabsClick(double mouseX, double mouseY, int button,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int panelLeft = guiLeft + imageWidth + 2;
        int panelTop  = guiTop;

        int PANEL_W = 289;
        int TAB_W   = 28;
        int bottomY = panelTop + 163;
        int topY    = panelTop - 25;

        if (DamageBookRenderer.handleArrowClick(mouseX, mouseY, panelLeft, panelTop, PANEL_W)) {
            cir.setReturnValue(true);
            return;
        }
        if (handleRowClick(mouseX, mouseY, panelLeft, PANEL_W, TAB_W, bottomY, true)) {
            cir.setReturnValue(true);
            return;
        }
        if (handleRowClick(mouseX, mouseY, panelLeft, PANEL_W, TAB_W, topY, false)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void damagecore$closeDamageBookWhenRecipeOpen(double mouseX, double mouseY, int button,
                                                          CallbackInfoReturnable<Boolean> cir) {
        InventoryScreen screen = (InventoryScreen)(Object)this;

        if (screen.getRecipeBookComponent().isVisible()) {
            boolean changed = false;
            if (this.damageBookVisible)  { this.damageBookVisible  = false; changed = true; }
            if (this.skillTreeVisible)   { this.skillTreeVisible   = false; changed = true; }
            if (changed) damagecore$updateInventoryPosition(screen);
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void damagecore$handleSmallClick(double mouseX, double mouseY, int button,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!this.damageBookVisible) return;
        InventoryScreen screen = (InventoryScreen)(Object)this;
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop  = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int tabX = guiLeft - TAB_WIDTH;
        int tabY = guiTop;
        this.selectedSmall = DamageBookInputHandler.handleSmallTabsClick(
                mouseX, mouseY, screen, this.selectedSmall, tabX, tabY);
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"), cancellable = true)
    private void damagecore$skillTree_mousePressed(double mouseX, double mouseY, int button,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible) return;

        InventoryScreen screen = (InventoryScreen)(Object)this;
        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int panelScreenX = guiLeft + imageWidth + 2;
        int panelScreenY = guiTop;

        boolean consumed = SkillTreeRenderer.mousePressed(
                (int)mouseX, (int)mouseY, button, panelScreenX, panelScreenY);

        if (button == 0) {
            SkillTreeNode hovered = Render.getHoveredNodeUnderMouse((int)mouseX, (int)mouseY);
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
    // mouseReleased
    // -------------------------------------------------------------------------
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void damagecore$skillTree_mouseReleased(double mouseX, double mouseY, int button,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible) return;

        boolean consumed = SkillTreeRenderer.mouseReleased((int)mouseX, (int)mouseY, button);

        if (button == 0) {
            Render.currentHoveredNode = null;
            Render.mousePressTime = 0L;
        }

        if (consumed) cir.setReturnValue(true);
    }

    // -------------------------------------------------------------------------
    // renderBg — обновление позиций кнопок
    // -------------------------------------------------------------------------
    @Inject(method = "renderBg", at = @At("TAIL"))
    private void damagecore$updateButtonPosition(GuiGraphics gui, float partialTicks,
                                                 int mouseX, int mouseY, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen)(Object)this;
        if (this.damagecore$recipeButton == null || this.damagecore$skillTreeButton == null) return;

        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop  = ((AbstractContainerScreenAccessor) screen).getTopPos();

        int newRecipeX = guiLeft + this.recipeButtonOffsetX;
        int newRecipeY = guiTop  + this.recipeButtonOffsetY;
        this.damagecore$recipeButton.setPosition(newRecipeX, newRecipeY);

        int skillX = newRecipeX + this.damagecore$recipeButton.getWidth() + 2;
        this.damagecore$skillTreeButton.setPosition(skillX, newRecipeY);
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
        InventoryScreen screen = (InventoryScreen)(Object)this;

        SkillTreeRenderer.loadAllTrees("skill_tree");

        for (var child : screen.children()) {
            if (child instanceof ImageButton btn
                    && btn.getWidth() == BUTTON_WIDTH
                    && btn.getHeight() == BUTTON_HEIGHT) {
                this.damagecore$recipeButton = btn;
                this.recipeButtonOffsetX = btn.getX() - ((AbstractContainerScreenAccessor) screen).getLeftPos();
                this.recipeButtonOffsetY = btn.getY() - ((AbstractContainerScreenAccessor) screen).getTopPos();
                break;
            }
        }

        this.damagecore$skillTreeButton = new ImageButton(
                0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, 0, 0, 19, SKILL_TREE_BUTTON, btn -> {
            InventoryScreen s = (InventoryScreen)(Object)this;
            this.skillTreeVisible = !this.skillTreeVisible;

            if (this.skillTreeVisible) {
                if (s.getRecipeBookComponent().isVisible()) {
                    s.getRecipeBookComponent().toggleVisibility();
                }
                this.damageBookVisible = false;
                int currentTab = DamageBookRenderer.selectedBottomTab;
                if (currentTab < SkillTreeRenderer.getTotalTrees()) {
                    SkillTreeRenderer.setActiveTree(currentTab);
                }
            } else {
                SkillTreeRenderer.resetTreePosition();
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
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
            );
        }
        return true;
    }

    @Unique
    private boolean handleRowClick(double mx, double my, int panelLeft, int panelW,
                                   int tabW, int y, boolean bottom) {
        int rowBase = bottom ? 0 : DamageBookRenderer.TABS_PER_ROW;

        int slotLeft  = rowBase;
        int globalLeft = DamageBookRenderer.globalIdForSlot(slotLeft);
        if (SkillTreeRenderer.hasTreeForTab(globalLeft)) {
            if (clickTab(mx, my, panelLeft, y, tabW, globalLeft)) return true;
        }

        for (int i = 0; i < DamageBookRenderer.MIDDLE_TABS; i++) {
            int slotId   = rowBase + 1 + i;
            int globalId = DamageBookRenderer.globalIdForSlot(slotId);
            if (!SkillTreeRenderer.hasTreeForTab(globalId)) continue;
            int x = DamageBookRenderer.calcMiddleX(i, panelLeft, panelW, tabW);
            if (clickTab(mx, my, x, y, tabW, globalId)) return true;
        }

        int slotRight  = rowBase + DamageBookRenderer.TABS_PER_ROW - 1;
        int globalRight = DamageBookRenderer.globalIdForSlot(slotRight);
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