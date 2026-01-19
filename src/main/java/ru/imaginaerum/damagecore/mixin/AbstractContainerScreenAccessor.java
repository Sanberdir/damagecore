package ru.imaginaerum.damagecore.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int getLeftPos();
    @Accessor("imageWidth")
    int getImageWidth();
    @Accessor("imageWidth")
    int damagecore$getImageWidth();

    @Accessor("imageHeight")
    int damagecore$getImageHeight();
    @Accessor("imageHeight")
    int getImageHeight();
    @Accessor("leftPos")
    void setLeftPos(int leftPos);

    @Accessor("topPos")
    int getTopPos();

    @Accessor("topPos")
    void setTopPos(int topPos);
}
