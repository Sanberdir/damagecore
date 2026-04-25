package ru.imaginaerum.damagecore.animation_attack;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class CustomAnimationPlayer extends KeyframeAnimationPlayer {

    private final AnimationTimeline timeline;
    private final Set<AnimationEvent> triggered = new HashSet<>();

    public CustomAnimationPlayer(KeyframeAnimation anim, AnimationTimeline timeline) {
        super(anim);
        this.timeline = timeline;
    }

    public boolean isInfluencing(float tickDelta) {
        return (getTick() + tickDelta) < getData().stopTick;
    }

    public float getNormalizedTime(float tickDelta) {
        return (getTick() + tickDelta) / getData().stopTick;
    }

    public void handleEvents(float tickDelta, Runnable onHit) {
        float t = getNormalizedTime(tickDelta);

        for (AnimationEvent event : timeline.getEvents()) {
            if (!triggered.contains(event) && t >= event.getTime()) {
                triggered.add(event);

                if (event.getType() == AnimationEvent.Type.HIT) {
                    onHit.run();
                }
            }
        }
    }

    @Override
    public @NotNull FirstPersonMode getFirstPersonMode(float tickDelta) {
        if (!isInfluencing(tickDelta)) {
            return FirstPersonMode.NONE;
        }
        return FirstPersonMode.THIRD_PERSON_MODEL; // ← показывает полную модель с анимацией
    }
}