package ru.imaginaerum.damagecore.animation_attack;

import net.minecraft.resources.ResourceLocation;

public class AttackAnimationData {

    private final ResourceLocation animationId;
    private final AnimationTimeline timeline;

    public AttackAnimationData(ResourceLocation animationId, AnimationTimeline timeline) {
        this.animationId = animationId;
        this.timeline = timeline;
    }

    public ResourceLocation getAnimationId() {
        return animationId;
    }

    public AnimationTimeline getTimeline() {
        return timeline;
    }
}