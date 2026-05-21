package ru.imaginaerum.damagecore.animation_attack;

import ru.imaginaerum.damagecore.library_damage.DamageType;

public interface ICurrentAttackType {

    void damagecore$setCurrentAttackType(DamageType type);

    DamageType damagecore$getCurrentAttackType();
}