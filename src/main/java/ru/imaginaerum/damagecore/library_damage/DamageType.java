package ru.imaginaerum.damagecore.library_damage;

public enum DamageType {
    PIERCING("piercing"),
    SLASHING("slashing"),
    FIRE("fire"),
    BLUDGEONING("bludgeoning");

    private final String damageName;

    DamageType(String damageName) {
        this.damageName = damageName;
    }

    public String getDamageName() {
        return damageName;
    }

    @Override
    public String toString() {
        return damageName;
    }
}