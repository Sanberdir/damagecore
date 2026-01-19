package ru.imaginaerum.damagecore.entities;

import net.minecraft.world.entity.Entity;

public enum DCEntityType {

    UNDEAD("Undead",        0xAA3333, DCEntityTagList.UNDEAD),        // тёмно-красный, кровь и тлен
    INFERNAL("Infernal",    0xFF6600, DCEntityTagList.INFERNAL),      // огненно-оранжевый, адское пламя
    ABERRATION("Aberration",0x9966FF, DCEntityTagList.ABERRATION),    // фиолетовый, чуждая магия
    ARTHROPOD("Arthropod",  0x66CC66, DCEntityTagList.ARTHROPOD),     // ядовито-зелёный, хитин
    CONSTRUCTION("Construction", 0xAAAAAA, DCEntityTagList.CONSTRUCTION), // серый металл
    ANIMAL("Animal",        0x55FF55, DCEntityTagList.ANIMAL),        // природный зелёный

    PLAINTS("Plaints",      0xE5D37A, DCEntityTagList.PLAINTS),       // песочно-жёлтый, степи/поля
    SLIME("Slime",          0x33FFAA, DCEntityTagList.SLIME),       // кислотно-бирюзовый, слизь
    HUMAN("Human",          0x918151, DCEntityTagList.HUMAN),       // кислотно-бирюзовый, слизь


    UNKNOWN("Unknown", 0xFFFFFF, null);

    public final String title;
    public final int color;
    private final DCEntityTagList tag;

    DCEntityType(String title, int color, DCEntityTagList tag) {
        this.title = title;
        this.color = color;
        this.tag = tag;
    }

    public static DCEntityType resolve(Entity entity) {
        for (DCEntityType type : values()) {
            if (type.tag != null && type.tag.matches(entity)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}