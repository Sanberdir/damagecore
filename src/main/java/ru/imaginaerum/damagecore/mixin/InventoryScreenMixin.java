package ru.imaginaerum.damagecore.mixin;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    private static final ResourceLocation DAMAGE_TYPE_BUTTON = new ResourceLocation("damagecore", "textures/gui/damage_type_button.png");
    private static final ResourceLocation DAMAGE_BOOK_TAB = new ResourceLocation("damagecore", "textures/gui/container/creative_inventory/damage_book.png");

    @Unique
    private ImageButton damagecore$button;
    @Unique
    private ImageButton damagecore$recipeButton;
    @Unique
    private static final int BUTTON_WIDTH = 20;
    @Unique
    private static final int BUTTON_HEIGHT = 18;
    @Unique
    private boolean damageBookVisible = false;
    @Unique
    private static final int TAB_WIDTH = 150;
    @Unique
    private int recipeButtonOffsetX = 0;
    @Unique
    private int recipeButtonOffsetY = 0;

    @Inject(method = "init", at = @At("TAIL"))
    private void damagecore$init(CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        for (var child : screen.children()) {
            if (child instanceof ImageButton btn && btn.getWidth() == BUTTON_WIDTH && btn.getHeight() == BUTTON_HEIGHT) {
                this.damagecore$recipeButton = btn;
                // Используем аксессор для получения leftPos/topPos
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
                // Закрываем книгу рецептов, если она открыта
                if (s.getRecipeBookComponent().isVisible()) {
                    s.getRecipeBookComponent().toggleVisibility();
                }
            }

            this.damagecore$updateInventoryPosition(s);
        }
        );

        ((ScreenInvoker) screen).damagecore$addRenderableWidget(this.damagecore$button);
    }

    /**
     * После обработки клика ванилой — если рецепт-книга теперь видима,
     * закрываем damage book и корректируем позицию GUI.
     * (инжектируем в TAIL, чтобы видеть итоговое состояние после ванильной обработки)
     */
    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void damagecore$closeDamageBookWhenRecipeOpen(
            double mouseX, double mouseY, int button,
            CallbackInfoReturnable<Boolean> cir
    ) {
        InventoryScreen screen = (InventoryScreen) (Object) this;

        if (screen.getRecipeBookComponent().isVisible()) {
            // Рецепты открыты — закрываем damage book (если он был открыт)
            if (this.damageBookVisible) {
                this.damageBookVisible = false;
                this.damagecore$updateInventoryPosition(screen);
            }
        }
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void damagecore$updateButtonPosition(GuiGraphics gui, float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        if (this.damagecore$recipeButton == null || this.damagecore$button == null) return;

        // Используем аксессор для получения позиций
        int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
        int guiTop = ((AbstractContainerScreenAccessor) screen).getTopPos();

        int newRecipeX = guiLeft + this.recipeButtonOffsetX;
        int newRecipeY = guiTop + this.recipeButtonOffsetY;
        this.damagecore$recipeButton.setPosition(newRecipeX, newRecipeY);

        int buttonX = newRecipeX + this.damagecore$recipeButton.getWidth() + 2;
        int buttonY = newRecipeY;
        this.damagecore$button.setPosition(buttonX, buttonY);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void damagecore$renderDamageBook(GuiGraphics gui, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        if (this.damageBookVisible) {
            // Используем аксессор для получения позиций
            int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
            int guiTop = ((AbstractContainerScreenAccessor) screen).getTopPos();

            int tabX = guiLeft - TAB_WIDTH;
            int tabY = guiTop;
            gui.blit(DAMAGE_BOOK_TAB, tabX, tabY - 1, 0, 0, TAB_WIDTH, ((AbstractContainerScreenAccessor) screen).damagecore$getImageHeight());
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void damagecore$handleDamageBookClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.damageBookVisible) {
            InventoryScreen screen = (InventoryScreen) (Object) this;
            // Используем аксессор для получения позиций
            int guiLeft = ((AbstractContainerScreenAccessor) screen).getLeftPos();
            int guiTop = ((AbstractContainerScreenAccessor) screen).getTopPos();

            int tabX = guiLeft - TAB_WIDTH;
            int tabY = guiTop;
            int tabRight = tabX + TAB_WIDTH;
            int tabBottom = tabY + ((AbstractContainerScreenAccessor) screen).damagecore$getImageHeight();

            if (mouseX >= tabX && mouseX < tabRight && mouseY >= tabY && mouseY < tabBottom) {
                // Обработка клика по вкладке (если нужно)
            }
        }
    }

    @Unique
    private void damagecore$updateInventoryPosition(InventoryScreen screen) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;

        // Если открыта ванильная книга — НЕ ЛЕЗЕМ
        if (screen.getRecipeBookComponent().isVisible()) {
            return;
        }

        int totalWidth = accessor.damagecore$getImageWidth();
        if (this.damageBookVisible) {
            totalWidth += TAB_WIDTH;
        }

        int newLeftPos = (screen.width - totalWidth) / 2 + 2;

        if (this.damageBookVisible) {
            accessor.setLeftPos(newLeftPos + TAB_WIDTH);
        } else {
            accessor.setLeftPos(newLeftPos);
        }
    }

}
