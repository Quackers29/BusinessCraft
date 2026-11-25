package com.quackers29.businesscraft.data.parsers;

public class Effect {
    private final String target;
    private final float value;
    private final boolean isPercentage;
    /** Bare effect name (`farming`, `border_expansion_production`): turns something on. Not a scaled bonus. */
    private final boolean unlock;

    public Effect(String target, float value, boolean isPercentage) {
        this(target, value, isPercentage, false);
    }

    public Effect(String target, float value, boolean isPercentage, boolean unlock) {
        this.target = target;
        this.value = value;
        this.isPercentage = isPercentage;
        this.unlock = unlock;
    }

    public String getTarget() {
        return target;
    }

    public float getValue() {
        return value;
    }

    public boolean isPercentage() {
        return isPercentage;
    }

    /** True for a bare effect name. The value stays put at every level; it is not a multiplier. */
    public boolean isUnlock() {
        return unlock;
    }

    @Override
    public String toString() {
        return target + ":" + value + (isPercentage ? "%" : "");
    }
}
