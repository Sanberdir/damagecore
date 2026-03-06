package ru.imaginaerum.damagecore.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.damage_book_protection.*;
import ru.imaginaerum.damagecore.api.damage_book_protection.pages_book.RenderActiveEffects;
import ru.imaginaerum.damagecore.api.damage_book_protection.pages_book.RenderDamageIconsAndTexts;
import ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer.Render;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin implements ISkillTreeAccessor {
    private static final ResourceLocation DAMAGE_TYPE_BUTTON = new ResourceLocation("damagecore", "textures/gui/damage_type_button.png");
    private static final ResourceLocation SKILL_TREE_BUTTON = new ResourceLocation("damagecore", "textures/gui/skill_tree_button.png");
    @Unique
    private static boolean syncRequested = false;
    @Unique
    private ImageButton damagecore$button;
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

    @Override
    public boolean damagecore$isSkillTreeVisible() {
        return this.skillTreeVisible;
    }
    // Клик по вкладкам внизу
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void damagecore$bottomTabsClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;

        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop  = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int panelLeft = guiLeft + imageWidth + 2;
        int panelTop  = guiTop;

        int PANEL_W = 289;
        int TAB_W = 28;

        int bottomY = panelTop + 163;
        int topY = panelTop - 25;

        // ---- стрелки (проверяем первым — чтобы клики по стрелкам НЕ попадали в табы) ----
        if (DamageBookRenderer.handleArrowClick(mouseX, mouseY, panelLeft, panelTop, PANEL_W)) {
            cir.setReturnValue(true);
            return;
        }

        // ---- нижний ряд ----
        if (handleRowClick(mouseX, mouseY, panelLeft, PANEL_W, TAB_W, bottomY, true)) {
            cir.setReturnValue(true);
            return;
        }

        // ---- верхний ряд ----
        if (handleRowClick(mouseX, mouseY, panelLeft, PANEL_W, TAB_W, topY, false)) {
            cir.setReturnValue(true);
        }
    }
    @Unique
    private boolean clickTab(double mx, double my, int x, int y, int w, int globalId) {
        if (!inside(mx, my, x, y - 3, w, 32)) return false;

        // globalId уже глобальный (с учётом страницы)
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
    private boolean handleRowClick(double mx, double my, int panelLeft, int panelW, int tabW, int y, boolean bottom) {
        int rowBase = bottom ? 0 : DamageBookRenderer.TABS_PER_ROW;

        // левая
        int slotLeft = rowBase;
        int globalLeft = DamageBookRenderer.globalIdForSlot(slotLeft);
        if (SkillTreeRenderer.hasTreeForTab(globalLeft)) {
            if (clickTab(mx, my, panelLeft, y, tabW, globalLeft)) return true;
        }

        // средние
        for (int i = 0; i < DamageBookRenderer.MIDDLE_TABS; i++) {
            int slotId = rowBase + 1 + i;
            int globalId = DamageBookRenderer.globalIdForSlot(slotId);
            if (!SkillTreeRenderer.hasTreeForTab(globalId)) continue;

            int x = DamageBookRenderer.calcMiddleX(i, panelLeft, panelW, tabW);
            if (clickTab(mx, my, x, y, tabW, globalId)) return true;
        }

        // правая
        int slotRight = rowBase + DamageBookRenderer.TABS_PER_ROW - 1;
        int globalRight = DamageBookRenderer.globalIdForSlot(slotRight);
        return SkillTreeRenderer.hasTreeForTab(globalRight) && clickTab(mx, my, panelLeft + panelW - tabW, y, tabW, globalRight);
    }

    @Unique
    private void selectTab(int id) {
        if (DamageBookRenderer.selectedBottomTab == id) return;

        DamageBookRenderer.setBottomTab(id);
        SkillTreeRenderer.setActiveTree(id);
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }



    // Проверка попадания мыши в прямоугольник
    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }




    @Inject(method = "init", at = @At("TAIL"))
    private void damagecore$init(CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;

        // Загружаем все деревья из папки skill_tree (ТОЛЬКО ОДИН РАЗ)
        SkillTreeRenderer.loadAllTrees("skill_tree"); // Уже содержит проверку на повторную загрузку

        for (var child : screen.children()) {
            if (child instanceof ImageButton btn && btn.getWidth() == BUTTON_WIDTH && btn.getHeight() == BUTTON_HEIGHT) {
                this.damagecore$recipeButton = btn;
                this.recipeButtonOffsetX = btn.getX() - ((AbstractContainerScreenAccessor) screen).getLeftPos();
                this.recipeButtonOffsetY = btn.getY() - ((AbstractContainerScreenAccessor) screen).getTopPos();
                break;
            }
        }

        this.damagecore$button = new ImageButton(
                0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, 0, 0, 19, DAMAGE_TYPE_BUTTON, btn -> {
            InventoryScreen s = (InventoryScreen) (Object) this;
            this.damageBookVisible = !this.damageBookVisible;

            if (this.damageBookVisible) {
                if (s.getRecipeBookComponent().isVisible()) {
                    s.getRecipeBookComponent().toggleVisibility();
                }
                this.skillTreeVisible = false;
            }

            this.damagecore$updateInventoryPosition(s);
        });

        this.damagecore$skillTreeButton = new ImageButton(
                0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, 0, 0, 19, SKILL_TREE_BUTTON, btn -> {
            InventoryScreen s = (InventoryScreen) (Object) this;
            this.skillTreeVisible = !this.skillTreeVisible;

            if (this.skillTreeVisible) {
                if (s.getRecipeBookComponent().isVisible()) {
                    s.getRecipeBookComponent().toggleVisibility();
                }
                this.damageBookVisible = false;

                // Устанавливаем активное дерево на текущую вкладку
                int currentTab = DamageBookRenderer.selectedBottomTab;
                if (currentTab < SkillTreeRenderer.getTotalTrees()) {
                    SkillTreeRenderer.setActiveTree(currentTab);
                }
            } else {
                SkillTreeRenderer.resetTreePosition();
            }

            this.damagecore$updateInventoryPosition(s);
        });

        ((ScreenInvoker) screen).damagecore$addRenderableWidget(this.damagecore$button);
        ((ScreenInvoker) screen).damagecore$addRenderableWidget(this.damagecore$skillTreeButton);

        // Запрашиваем синхронизацию только один раз
        if (Minecraft.getInstance().player != null && !syncRequested) {
            System.out.println("Requesting full sync from server after tree load");
            ModNetwork.CHANNEL.sendToServer(new RequestFullSyncPacket());
            syncRequested = true; // Нужно добавить поле
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void damagecore$closeDamageBookWhenRecipeOpen(
            double mouseX, double mouseY, int button,
            CallbackInfoReturnable<Boolean> cir
    ) {
        InventoryScreen screen = (InventoryScreen) (Object) this;

        if (screen.getRecipeBookComponent().isVisible()) {
            boolean changed = false;
            if (this.damageBookVisible) {
                this.damageBookVisible = false;
                changed = true;
            }
            if (this.skillTreeVisible) {
                this.skillTreeVisible = false;
                changed = true;
            }
            if (changed) {
                this.damagecore$updateInventoryPosition(screen);
            }
        }
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void damagecore$updateButtonPosition(GuiGraphics gui, float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        if (this.damagecore$recipeButton == null || this.damagecore$button == null || this.damagecore$skillTreeButton == null) return;

        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop = ((AbstractContainerScreenAccessor) screen).getTopPos();

        int newRecipeX = guiLeft + this.recipeButtonOffsetX;
        int newRecipeY = guiTop + this.recipeButtonOffsetY;
        this.damagecore$recipeButton.setPosition(newRecipeX, newRecipeY);

        int buttonX = newRecipeX + this.damagecore$recipeButton.getWidth() + 2;
        int buttonY = newRecipeY;
        this.damagecore$button.setPosition(buttonX, buttonY);

        int skillX = buttonX + this.damagecore$button.getWidth() + 2;
        int skillY = buttonY;
        this.damagecore$skillTreeButton.setPosition(skillX, skillY);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void damagecore$renderDamageBook(
            GuiGraphics gui, int mouseX, int mouseY, float partialTicks, CallbackInfo ci
    ) {
        InventoryScreen screen = (InventoryScreen) (Object) this;

        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop  = ((AbstractContainerScreenAccessor) screen).getTopPos();

        if (this.damageBookVisible) {
            int tabX = guiLeft - TAB_WIDTH;
            int tabY = guiTop;

            DamageBookRenderer.renderMainTab(gui, screen, tabX, tabY);
            DamageBookRenderer.renderSmallTabs(gui, screen, tabX, tabY, this.selectedSmall);

            if (this.selectedSmall == 0) {
                DamageBookStateCollector.ProtectionData protectionData =
                        DamageBookStateCollector.collectProtectionData(Minecraft.getInstance().player);

                RenderDamageIconsAndTexts.renderDamageIconsAndTexts(gui, tabX, tabY, protectionData, mouseX, mouseY);
            } else if (this.selectedSmall == 1) {
                DamageBookStateCollector.ProtectionData protectionData =
                        DamageBookStateCollector.collectProtectionData(Minecraft.getInstance().player);

                RenderActiveEffects.renderActiveEffects(gui, tabX, tabY, protectionData, mouseX, mouseY);
            }
        }

        if (this.skillTreeVisible) {
            int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();
            int tabX = guiLeft + imageWidth;
            int tabY = guiTop;

            DamageBookRenderer.renderRightInterface(gui, screen, tabX, tabY, mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void damagecore$handleSmallClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!this.damageBookVisible) return;

        InventoryScreen screen = (InventoryScreen) (Object) this;
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int tabX = guiLeft - TAB_WIDTH;
        int tabY = guiTop;

        this.selectedSmall = DamageBookInputHandler.handleSmallTabsClick(mouseX, mouseY, screen, this.selectedSmall, tabX, tabY);
    }
    @Inject(method = "render", at = @At("TAIL"))
    private void damagecore$renderSkillTreeHold(GuiGraphics gui, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!this.skillTreeVisible) return;

        int guiLeft = ((AbstractContainerScreenAccessor) (InventoryScreen)(Object)this).getLeftPos();
        int guiTop = ((AbstractContainerScreenAccessor) (InventoryScreen)(Object)this).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) (InventoryScreen)(Object)this).damagecore$getImageWidth();
        int panelScreenX = guiLeft + imageWidth + 2;
        int panelScreenY = guiTop;

        // 1) Обновляем drag каждый кадр (если isDragging==true внутри SkillTreeData — метод выполнит смещение)
        //    Это заменяет отсутствие mouseDragged в InventoryScreen — вызывать безопасно всегда.
        SkillTreeRenderer.mouseDragged((int) mouseX, (int) mouseY, 0, panelScreenX, panelScreenY);

        // 2) После возможного перемещения дерева — обновляем hovered-ноду (учитывает offset/scale внутри Render)
        Render.currentHoveredNode = Render.getHoveredNodeUnderMouse(mouseX, mouseY);

        // 3) (опционально) — если хочешь показывать hold-бар прямо сейчас, можно вызвать
        // Render.renderHoldProgressOverlay(gui);
    }
    // --- mousePressed (заменяет текущую реализацию) ---
    @Inject(method = "mouseClicked", at = @At("TAIL"), cancellable = true)
    private void damagecore$skillTree_mousePressed(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!this.skillTreeVisible) return;

        InventoryScreen screen = (InventoryScreen) (Object) this;

        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop  = ((AbstractContainerScreenAccessor) screen).getTopPos();
        int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();

        int panelScreenX = guiLeft + imageWidth + 2;
        int panelScreenY = guiTop;

        boolean consumed = SkillTreeRenderer.mousePressed(
                (int) mouseX, (int) mouseY, button,
                panelScreenX, panelScreenY
        );

        if (button == 0) {
            SkillTreeNode hovered = Render.getHoveredNodeUnderMouse((int) mouseX, (int) mouseY);
            if (hovered != null) {
                if (hovered.learned) {
                    // игнорируем: уже изучена
                    Render.currentHoveredNode = null;
                    Render.mousePressTime = 0L;
                } else {
                    Render.currentHoveredNode = hovered;
                    Render.mousePressTime = System.currentTimeMillis();
                }
            }
            if (hovered != null && !hovered.learned) {
                Render.currentHoveredNode = hovered;
                Render.mousePressTime = System.currentTimeMillis();
            } else {
                Render.currentHoveredNode = null;
                Render.mousePressTime = 0L;
            }
        }

        if (consumed) {
            cir.setReturnValue(true);
        }
    }

    // --- mouseReleased (заменяет текущую реализацию) ---
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void damagecore$skillTree_mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        if (!this.skillTreeVisible) return;

        boolean consumed = SkillTreeRenderer.mouseReleased((int) mouseX, (int) mouseY, button);

        if (button == 0) {
            if (Render.currentHoveredNode != null && Render.mousePressTime > 0) {
                long elapsed = System.currentTimeMillis() - Render.mousePressTime;
            }
            // Сбрасываем hold
            Render.currentHoveredNode = null;
            Render.mousePressTime = 0L;
        }

        if (consumed) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void damagecore$updateInventoryPosition(InventoryScreen screen) {
        DamageBookPositionHelper.updateInventoryPosition(screen, this.damageBookVisible, this.skillTreeVisible, TAB_WIDTH, RIGHT_INTERFACE_WIDTH);
    }
}