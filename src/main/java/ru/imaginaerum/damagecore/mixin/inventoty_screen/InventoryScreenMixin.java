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
import ru.imaginaerum.damagecore.mixin.AbstractContainerScreenAccessor;
import ru.imaginaerum.damagecore.mixin.ScreenInvoker;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin implements ISkillTreeAccessor {

    private static final ResourceLocation SKILL_TREE_BUTTON =
            new ResourceLocation("damagecore", "textures/gui/skill_tree_button.png");
    private static final ResourceLocation ARMOR_STATS =
            new ResourceLocation("damagecore", "textures/gui/armor_statistics.png");

    @Unique
    private static final ResourceLocation ARMOR_STATS_FIELD =
            new ResourceLocation("damagecore",
                    "textures/gui/container/creative_inventory/armor_statistic_field.png");

    // Текстурный атлас — регион [0..176 × 0..166] используется как фон инвентаря выживания
    @Unique
    private static final ResourceLocation DAMAGE_CORE_INTERFACE =
            new ResourceLocation("damagecore",
                    "textures/gui/container/creative_inventory/damage_core_interface.png");

    // UV-координаты региона фона внутри атласа 512×512
    @Unique private static final int INV_BG_U    = 0;
    @Unique private static final int INV_BG_V    = 0;
    @Unique private static final int INV_BG_W    = 176;
    @Unique private static final int INV_BG_H    = 166;
    @Unique private static final int ATLAS_SIZE  = 512;

    @Unique private boolean armorStatsVisible = false;
    @Unique private ImageButton damagecore$recipeButton;
    @Unique private ImageButton damagecore$skillTreeButton;
    @Unique private static final int BUTTON_WIDTH  = 20;
    @Unique private static final int BUTTON_HEIGHT = 18;
    @Unique private boolean damageBookVisible = false;
    @Unique private boolean skillTreeVisible  = false;
    @Unique private static final int TAB_WIDTH = DamageBookRenderer.TAB_WIDTH;
    @Unique private static final int RIGHT_INTERFACE_WIDTH = 289;
    @Unique private int recipeButtonOffsetX = 0;
    @Unique private int recipeButtonOffsetY = 0;
    @Unique private int selectedSmall = 0;


    // UV исходной текстуры плюсика (195..203 × 225..233 = 8×8 px)
    @Unique private static final int PLUS_U = 195;
    @Unique private static final int PLUS_V = 225;
    @Unique private static final int PLUS_W = 8;
    @Unique private static final int PLUS_H = 8;

    // Позиция первого плюсика относительно leftPos/topPos
    @Unique private static final int PLUS_X = 154;
    @Unique private static final int PLUS_Y = 8;

    // Шаг между плюсиками = высота иконки + 4px зазор
    @Unique private static final int PLUS_STEP = PLUS_H + 6;

    // UV вертикальной полоски (176..181 × 225..240 = 5×15 px)
    @Unique private static final int STRIP_U = 176;
    @Unique private static final int STRIP_V = 225;
    @Unique private static final int STRIP_W = 5;
    @Unique private static final int STRIP_H = 15;

    // Позиция относительно leftPos/topPos
    @Unique private static final int STRIP_X = 165;
    @Unique private static final int STRIP_Y = 6;


    // ---- Скроллируемый список плюсиков ----
    @Unique private static final int PLUS_ROWS_TOTAL   = 6;
    @Unique private static final int PLUS_ROWS_VISIBLE = 4;
    // Пиксельный диапазон скролла = 2 скрытые строки × шаг
    @Unique private static final int SCROLL_MAX_PX    = (PLUS_ROWS_TOTAL - PLUS_ROWS_VISIBLE) * PLUS_STEP; // 28
    // UV плюсика при наведении (X187..X195 × Y225..Y233 = 8×8 px)
    @Unique private static final int PLUS_HOVER_U = 187;
    @Unique private static final int PLUS_HOVER_V = 225;

    // Диапазон перетаскивания полоски: верх от Y6 до Y(60-STRIP_H)=Y45
    @Unique private static final int STRIP_Y_MIN_OFF  = STRIP_Y;            // 6
    @Unique private static final int STRIP_Y_MAX_OFF  = 60 - STRIP_H;       // 45
    @Unique private static final int STRIP_DRAG_RANGE = STRIP_Y_MAX_OFF - STRIP_Y_MIN_OFF; // 39

    // Состояние перетаскивания
    @Unique private int     damagecore$stripOffsetY  = 0;
    @Unique private boolean damagecore$draggingStrip = false;
    @Unique private double  damagecore$dragMouseY0   = 0;
    @Unique private int     damagecore$dragStripY0   = 0;

    @Unique
    @Override
    public void damagecore$scrollList(double delta) {
        int step = PLUS_STEP;
        damagecore$stripOffsetY = Math.max(0,
                Math.min(STRIP_DRAG_RANGE, damagecore$stripOffsetY - (int)(delta * step)));
    }
    // -------------------------------------------------------------------------
    // Вспомогательный метод — предмет под курсором
    // -------------------------------------------------------------------------
    @Unique
    private static ItemStack damagecore$getHoveredStack(InventoryScreen screen,
                                                        double mouseX, double mouseY) {
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop  = ((AbstractContainerScreenAccessor) screen).getTopPos();

        for (var slot : screen.getMenu().slots) {
            int slotX = guiLeft + slot.x;
            int slotY = guiTop  + slot.y;
            if (mouseX >= slotX && mouseX < slotX + 16 &&
                    mouseY >= slotY && mouseY < slotY + 16) {
                return slot.getItem();
            }
        }
        return ItemStack.EMPTY;
    }

    // -------------------------------------------------------------------------
    // renderBg — ПОЛНАЯ ЗАМЕНА фона выживания + позиции кнопок
    //
    // TAIL-инджект (damagecore$updateButtonPosition) удалён, потому что после
    // ci.cancel() он не вызывается. Логика обновления кнопок перенесена сюда.
    // -------------------------------------------------------------------------
    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void damagecore$replaceVanillaInventoryBg(GuiGraphics gui, float partialTick,
                                                      int mouseX, int mouseY, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;

        int leftPos = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int topPos  = ((AbstractContainerScreenAccessor) screen).getTopPos();

        // Позиции кнопок — всегда
        if (this.damagecore$recipeButton != null && this.damagecore$skillTreeButton != null) {
            int newRecipeX = leftPos + this.recipeButtonOffsetX;
            int newRecipeY = topPos  + this.recipeButtonOffsetY;
            this.damagecore$recipeButton.setPosition(newRecipeX, newRecipeY);

            int skillX = newRecipeX + this.damagecore$recipeButton.getWidth() + 2;
            this.damagecore$skillTreeButton.setPosition(skillX, newRecipeY);
        }

        if (!this.skillTreeVisible) return;

        // Кастомный фон
        gui.blit(
                DAMAGE_CORE_INTERFACE,
                leftPos, topPos,
                INV_BG_U, INV_BG_V,
                INV_BG_W, INV_BG_H,
                ATLAS_SIZE, ATLAS_SIZE
        );
// ---- Полоска (позиция зависит от drag-offset) ----
        gui.blit(
                DAMAGE_CORE_INTERFACE,
                leftPos + STRIP_X,
                topPos  + STRIP_Y_MIN_OFF + damagecore$stripOffsetY,
                STRIP_U, STRIP_V,
                STRIP_W, STRIP_H,
                ATLAS_SIZE, ATLAS_SIZE
        );

// ---- 6 строк плюсиков со scissor-обрезкой по Y6..Y60 ----
        Component[] rowLabels = {
                Component.translatable("damagecore.stat.live_forge"),
                Component.translatable("damagecore.stat.endurance"),
                Component.translatable("damagecore.stat.mind"),
                Component.translatable("damagecore.stat.strength"),
                Component.translatable("damagecore.stat.dexterity"),
                Component.translatable("damagecore.stat.wisdom")
        };
        int scrollPx = STRIP_DRAG_RANGE > 0
                ? (damagecore$stripOffsetY * SCROLL_MAX_PX) / STRIP_DRAG_RANGE
                : 0;
        gui.enableScissor(
                leftPos + 97,  topPos + 6,
                leftPos + 163, topPos + 60
        );
        for (int i = 0; i < PLUS_ROWS_TOTAL; i++) {
            int plusScreenX = leftPos + PLUS_X;
            int plusScreenY = topPos + PLUS_Y + i * PLUS_STEP - scrollPx;

            // Проверяем наведение на строку (вся область X97..X163, высота строки)
            boolean hovered = mouseX >= leftPos + 97 && mouseX < leftPos + 163
                    && mouseY >= plusScreenY   && mouseY < plusScreenY + PLUS_H;

            // Плюсик — обычный или hover
            int plusU = hovered ? PLUS_HOVER_U : PLUS_U;
            int plusV = hovered ? PLUS_HOVER_V : PLUS_V;

            gui.blit(
                    DAMAGE_CORE_INTERFACE,
                    plusScreenX, plusScreenY,
                    plusU, plusV,
                    PLUS_W, PLUS_H,
                    ATLAS_SIZE, ATLAS_SIZE
            );

            // Текст (без изменений)
            float scale = 1f / 1.5f;
            int textX = leftPos + 99;  // левый край области + 2px отступ
            int textY = topPos + 6 + i * PLUS_STEP - scrollPx + 1; // верх строки + 1px

            gui.pose().pushPose();
            gui.pose().translate(textX, textY, 0);
            gui.pose().scale(scale, scale, 1f);
            int textColor = hovered ? 0xFFFFAA : 0xFFFFFF;

            gui.drawString(
                    Minecraft.getInstance().font,
                    rowLabels[i],
                    0, 0,
                    textColor,
                    true
            );
            gui.pose().popPose();
        }
        gui.disableScissor();
        // Рендер модели игрока — те же координаты и масштаб, что и в ванилле
        if (Minecraft.getInstance().player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    gui,
                    leftPos + 51, topPos + 75,
                    30,
                    (float)(leftPos + 51) - mouseX,
                    (float)(topPos + 75 - 50) - mouseY,
                    Minecraft.getInstance().player
            );
        }

        ci.cancel();
    }

    // ---- Начало перетаскивания полоски ----
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
            damagecore$draggingStrip  = true;
            damagecore$dragMouseY0    = mouseY;
            damagecore$dragStripY0    = damagecore$stripOffsetY;
            cir.setReturnValue(true);
        }
    }



    // ---- Конец перетаскивания полоски ----
    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void damagecore$stripMouseReleased(double mouseX, double mouseY, int button,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (button == 0 && damagecore$draggingStrip) {
            damagecore$draggingStrip = false;
        }
    }
    // -------------------------------------------------------------------------
    // renderLabels — убираем заголовок "Создание"
    // -------------------------------------------------------------------------
    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void damagecore$hideLabels(GuiGraphics gui, int mouseX, int mouseY, CallbackInfo ci) {
        if (!this.skillTreeVisible) return;
        ci.cancel();
    }

    // -------------------------------------------------------------------------
    // render TAIL — все кастомные слои поверх
    // -------------------------------------------------------------------------
    @Inject(method = "render", at = @At("TAIL"))
    private void damagecore$renderAll(GuiGraphics gui, int mouseX, int mouseY,
                                      float partialTicks, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;

        int guiLeft    = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop     = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        // ---- обновляем drag прямо здесь ----
        if (damagecore$draggingStrip) {
            int delta = (int)(mouseY - damagecore$dragMouseY0);
            damagecore$stripOffsetY = Math.max(0,
                    Math.min(STRIP_DRAG_RANGE, damagecore$dragStripY0 + delta));
        }
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
            int x     = guiLeft + 62;
            int y     = guiTop  + 10;
            int iconW = 10;
            int iconH = 9;
            boolean hover = mouseX >= x && mouseX < x + iconW &&
                    mouseY >= y && mouseY < y + iconH;
            if (hover) {
                gui.blit(ARMOR_STATS, x, y, 0, 10, iconW, iconH, 10, 19);
            } else {
                gui.blit(ARMOR_STATS, x, y, 0,  0, iconW, iconH, 10, 19);
            }
        }

        // 4) ArmorStats интерфейс — поверх всего
        if (this.armorStatsVisible) {
            ItemStack hovered = damagecore$getHoveredStack(screen, mouseX, mouseY);
            ArmorStatsFieldRenderer.setHoveredArmor(hovered);
            ArmorStatsFieldRenderer.render(gui, screen);
        }
    }

    // -------------------------------------------------------------------------
    // render HEAD — определяем предмет брони под курсором
    // -------------------------------------------------------------------------
    @Inject(method = "render", at = @At("HEAD"))
    private void damagecore$detectHoveredArmor(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                               float partialTick, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        Slot hoveredSlot = ((AbstractContainerScreenAccessor) screen).getHoveredSlot();

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
        InventoryScreen screen = (InventoryScreen) (Object) this;

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

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
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
        int tabX = guiLeft - TAB_WIDTH;
        int tabY = guiTop;
        this.selectedSmall = DamageBookInputHandler.handleSmallTabsClick(
                mouseX, mouseY, screen, this.selectedSmall, tabX, tabY);
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"), cancellable = true)
    private void damagecore$skillTree_mousePressed(double mouseX, double mouseY, int button,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible) return;

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
    // mouseReleased
    // -------------------------------------------------------------------------
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

        int slotLeft   = rowBase;
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

        int slotRight   = rowBase + DamageBookRenderer.TABS_PER_ROW - 1;
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