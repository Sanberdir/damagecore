package ru.imaginaerum.damagecore.animation_attack;

import java.util.ArrayList;
import java.util.List;

public class AnimationTimeline {

    private final List<AnimationEvent> events = new ArrayList<>();

    public AnimationTimeline add(float time, AnimationEvent.Type type) {
        events.add(new AnimationEvent(time, type));
        return this;
    }

    public List<AnimationEvent> getEvents() {
        return events;
    }
}