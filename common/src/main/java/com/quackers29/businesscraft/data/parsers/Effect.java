package com.quackers29.businesscraft.data.parsers;

public class Effect {
    private final String target;
    private final float value;
    private final boolean isPercentage;

    public Effect(String target, float value, boolean isPercentage) {
        this.target = target;
        this.value = value;
        this.isPercentage = isPercentage;
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

    @Override
    public String toString() {
        return target + ":" + value + (isPercentage ? "%" : "");
    }
}
