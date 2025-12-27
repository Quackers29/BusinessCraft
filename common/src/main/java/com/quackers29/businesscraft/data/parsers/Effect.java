package com.quackers29.businesscraft.data.parsers;

public class Effect {
    private final String target; // e.g., "storage_cap_food"
    private final float value; // e.g., 20.0 or 0.2
    private final boolean isPercentage; // true if string had '%'

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
