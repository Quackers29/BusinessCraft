package com.quackers29.businesscraft.data.parsers;

public class Condition {
    private final String target;
    private final String operator;
    private final String value;
    private final boolean isPercentage;

    public Condition(String target, String operator, String value, boolean isPercentage) {
        this.target = target;
        this.operator = operator;
        this.value = value;
        this.isPercentage = isPercentage;
    }

    public String getTarget() {
        return target;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public boolean isPercentage() {
        return isPercentage;
    }

    @Override
    public String toString() {
        return target + ":" + operator + value + (isPercentage ? "%" : "");
    }
}
