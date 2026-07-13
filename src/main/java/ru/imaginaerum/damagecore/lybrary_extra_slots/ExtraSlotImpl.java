package ru.imaginaerum.damagecore.lybrary_extra_slots;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.ItemStackHandler;

public class ExtraSlotImpl implements IExtraSlot {
    private final ItemStackHandler handler = new ItemStackHandler(3);
    private boolean combatMode = false;

    @Override
    public ItemStackHandler getHandler() { return handler; }

    @Override
    public boolean isCombatMode() { return combatMode; }

    @Override
    public void setCombatMode(boolean value) { combatMode = value; }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("Handler", handler.serializeNBT());
        tag.putBoolean("CombatMode", combatMode);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("Handler")) {
            handler.deserializeNBT(tag.getCompound("Handler"));
        } else {
            // обратная совместимость со старым форматом, где корень = хендлер
            handler.deserializeNBT(tag);
        }
        combatMode = tag.getBoolean("CombatMode");
    }
}