package com.trungbui.projectshadow.effect;

public class EffectInstance {

    public static final int PERMANENT = -1;
    /** Sentinel for "applied round unknown" (e.g. legacy state, save/load).
     *  An effect with {@code appliedRound == UNKNOWN_ROUND} is eligible to tick
     *  immediately rather than being skipped as "just applied". */
    public static final int UNKNOWN_ROUND = -1;

    private final String effectId;
    private final String sourceId;
    private int remainingDuration;
    private int stacks;
    /** Round number when this effect was first applied (Sprint 9+ B2: effect-tick hybrid).
     *  Used by {@link ActiveEffects#onTurnStart} to skip the first tick on the same round
     *  an effect was applied. */
    private int appliedRound;

    public EffectInstance(String effectId, String sourceId, int duration, int stacks) {
        this(effectId, sourceId, duration, stacks, UNKNOWN_ROUND);
    }

    public EffectInstance(String effectId, String sourceId, int duration, int stacks, int appliedRound) {
        if (effectId == null || effectId.isBlank()) {
            throw new IllegalArgumentException("effectId must not be blank");
        }
        this.effectId = effectId;
        this.sourceId = sourceId;
        this.remainingDuration = duration;
        this.stacks = Math.max(1, stacks);
        this.appliedRound = appliedRound;
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

    public int appliedRound() {
        return appliedRound;
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

    /** Refresh remaining duration. Sprint 9+ B2: takes the max of current and incoming
     *  so re-applying a 1-turn effect can never shorten a still-running 5-turn buff. */
    public void refreshDuration(int duration) {
        if (this.remainingDuration == PERMANENT || duration == PERMANENT) {
            this.remainingDuration = PERMANENT;
            return;
        }
        this.remainingDuration = Math.max(this.remainingDuration, duration);
    }

    public void addStack(int max) {
        if (max <= 0) max = 1;
        if (stacks < max) stacks++;
    }

    @Override
    public String toString() {
        return "EffectInstance{" + effectId + ", stacks=" + stacks
                + ", dur=" + remainingDuration + ", round=" + appliedRound + "}";
    }
}
