package ru.imaginaerum.damagecore.animation_attack;

import ru.imaginaerum.damagecore.library_damage.DamageType;

public interface ICurrentAttackTypeServer {
    void damagecore$setAttackType(DamageType type);
    DamageType damagecore$getAttackType();
}