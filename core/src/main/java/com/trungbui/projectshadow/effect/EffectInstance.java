package com.trungbui.projectshadow.effect;

public class EffectInstance {

    public static final int PERMANENT = -1;

    private final String effectId;
    private final String sourceId;
    private int remainingDuration;
    private int stacks;

    public EffectInstance(String effectId, String sourceId, int duration, int stacks) {
        if (effectId == null || effectId.isBlank()) {
            throw new IllegalArgumentException("effectId must not be blank");
        }
        this.effectId = effectId;
        this.sourceId = sourceId;
        this.remainingDuration = duration;
        this.stacks = Math.max(1, stacks);
    }

    public String effectId() {
        return effectId;
    }

    public String sourceId() {
        return sourceId;
    }

    public int remainingDuration() {
        return remainingDuration;
    }

    public int stacks() {
        return stacks;
    }

    public boolean isPermanent() {
        return remainingDuration == PERMANENT;
    }

    public boolean isExpired() {
        return remainingDuration == 0;
    }

    public void tickDuration() {
        if (remainingDuration > 0) remainingDuration--;
    }

    public void refreshDuration(int duration) {
        this.remainingDuration = duration;
    }

    public void addStack(int max) {
        if (max <= 0) max = 1;
        if (stacks < max) stacks++;
    }

    @Override
    public String toString() {
        return "EffectInstance{" + effectId + ", stacks=" + stacks + ", dur=" + remainingDuration + "}";
    }
}
