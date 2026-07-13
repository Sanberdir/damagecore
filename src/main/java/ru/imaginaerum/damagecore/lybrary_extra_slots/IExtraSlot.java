package ru.imaginaerum.damagecore.lybrary_extra_slots;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.ItemStackHandler;

public interface IExtraSlot extends INBTSerializable<CompoundTag> {
    ItemStackHandler getHandler();
    boolean isCombatMode();
    void setCombatMode(boolean value);
}