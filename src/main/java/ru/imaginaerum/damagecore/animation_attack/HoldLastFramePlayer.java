package ru.imaginaerum.damagecore.animation_attack;

import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;

public class HoldLastFramePlayer extends KeyframeAnimationPlayer {

    private final int freezeAtTick;

    public HoldLastFramePlayer(KeyframeAnimation animation) {
        super(animation);
        this.freezeAtTick = findLastKeyframeTick(animation);
    }

    private int findLastKeyframeTick(KeyframeAnimation animation) {
        int last = 0;
        for (KeyframeAnimation.StateCollection sc : animation.getBodyParts().values()) {
            last = Math.max(last, lastTick(sc.x));
            last = Math.max(last, lastTick(sc.y));
            last = Math.max(last, lastTick(sc.z));
            last = Math.max(last, lastTick(sc.pitch));
            last = Math.max(last, lastTick(sc.yaw));
            last = Math.max(last, lastTick(sc.roll));
        }
        return last > 0 ? last : animation.endTick;
    }

    private int lastTick(KeyframeAnimation.StateCollection.State state) {
        if (state == null || !state.isEnabled() || state.length() == 0) return 0;
        return state.getKeyFrames().get(state.length() - 1).tick;
    }

    @Override
    public void tick() {
        if (getCurrentTick() < freezeAtTick) {
            super.tick();
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }
}