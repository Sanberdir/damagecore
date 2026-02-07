package ru.imaginaerum.damagecore.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.imaginaerum.damagecore.api.damage_book_protection.DamageBookInputHandler;
import ru.imaginaerum.damagecore.api.damage_book_protection.DamageBookPositionHelper;
import ru.imaginaerum.damagecore.api.damage_book_protection.DamageBookRenderer;
import ru.imaginaerum.damagecore.api.damage_book_protection.DamageBookStateCollector;
import ru.imaginaerum.damagecore.api.damage_book_protection.pages_book.RenderActiveEffects;
import ru.imaginaerum.damagecore.api.damage_book_protection.pages_book.RenderDamageIconsAndTexts;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    private static final ResourceLocation DAMAGE_TYPE_BUTTON = new ResourceLocation("damagecore", "textures/gui/damage_type_button.png");
    private static final ResourceLocation SKILL_TREE_BUTTON = new ResourceLocation("damagecore", "textures/gui/skill_tree_button.png");

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
    private static final int RIGHT_INTERFACE_WIDTH = 289; // 468 - 179
    @Unique
    private int recipeButtonOffsetX = 0;
    @Unique
    private int recipeButtonOffsetY = 0;

    @Unique
    private int selectedSmall = 0; // 0 = верхняя активная, 1 = нижняя активная

    @Inject(method = "init", at = @At("TAIL"))
    private void damagecore$init(CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
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
                // если открывается левая панель, закрываем правую (чтобы не налезало)
                this.skillTreeVisible = false;
            }

            this.damagecore$updateInventoryPosition(s);
        });

        this.damagecore$skillTreeButton = new ImageButton(
                0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, 0, 0, 19, SKILL_TREE_BUTTON, btn -> {
            InventoryScreen s = (InventoryScreen) (Object) this;
            this.skillTreeVisible = !this.skillTreeVisible;

            if (this.skillTreeVisible) {
                // если открывается правая панель — закроем рецепт и левую книгу
                if (s.getRecipeBookComponent().isVisible()) {
                    s.getRecipeBookComponent().toggleVisibility();
                }
                this.damageBookVisible = false;
            }

            this.damagecore$updateInventoryPosition(s);
        });

        ((ScreenInvoker) screen).damagecore$addRenderableWidget(this.damagecore$button);
        ((ScreenInvoker) screen).damagecore$addRenderableWidget(this.damagecore$skillTreeButton);
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

        // skill tree button — справа от кнопки брони
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

        // Левая панель (Damage Book) — старая логика
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

        // Правая панель (Skill Tree заглушка) — новая
        if (this.skillTreeVisible) {
            // рисуем справа от инвентаря
            int imageWidth = ((AbstractContainerScreenAccessor) screen).damagecore$getImageWidth();
            int tabX = guiLeft + imageWidth;
            int tabY = guiTop;

            DamageBookRenderer.renderRightInterface(gui, screen, tabX, tabY);
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

    @Unique
    private void damagecore$updateInventoryPosition(InventoryScreen screen) {
        DamageBookPositionHelper.updateInventoryPosition(screen, this.damageBookVisible, this.skillTreeVisible, TAB_WIDTH, RIGHT_INTERFACE_WIDTH);
    }
}
