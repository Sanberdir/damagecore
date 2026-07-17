package ru.imaginaerum.damagecore.library_extra_slots.creative;

import net.minecraft.world.inventory.Slot;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeFieldUtil {
    private static final Unsafe UNSAFE;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setInt(Object target, String fieldName, int value) {
        try {
            Field field = target.getClass().getSuperclass() == Slot.class
                    ? Slot.class.getDeclaredField(fieldName)
                    : target.getClass().getDeclaredField(fieldName); // на случай анонимного подкласса
            long offset = UNSAFE.objectFieldOffset(field);
            UNSAFE.putInt(target, offset, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}