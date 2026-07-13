package ru.imaginaerum.damagecore.mixin.inventory_screen.key_mixins;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBindsList.KeyEntry.class)
public interface AccessorKeyEntry {
    @Accessor("key")
    KeyMapping getKey();
}