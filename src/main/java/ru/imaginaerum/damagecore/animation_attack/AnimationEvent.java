package ru.imaginaerum.damagecore.animation_attack;

public class AnimationEvent {

    public enum Type {
        HIT,
        SWING,
        RECOVERY
    }

    private final float time; // 0.0 - 1.0 (нормализованное время)
    private final Type type;

    public AnimationEvent(float time, Type type) {
        this.time = time;
        this.type = type;
    }

    public float getTime() {
        return time;
    }

    public Type getType() {
        return type;
    }
}