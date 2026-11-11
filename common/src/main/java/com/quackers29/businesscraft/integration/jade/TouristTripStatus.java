package com.quackers29.businesscraft.integration.jade;

public enum TouristTripStatus {
    RIDING,
    WAITING,
    TRAVELING;

    public static TouristTripStatus from(boolean riding, boolean stationary) {
        if (riding) {
            return RIDING;
        }
        if (stationary) {
            return WAITING;
        }
        return TRAVELING;
    }

    public String translationKey() {
        return switch (this) {
            case RIDING -> "businesscraft.jade.tourist.status.riding";
            case WAITING -> "businesscraft.jade.tourist.status.waiting";
            case TRAVELING -> "businesscraft.jade.tourist.status.traveling";
        };
    }

    public static TouristTripStatus parseNbt(String raw) {
        if (raw == null || raw.isBlank()) {
            return WAITING;
        }
        return switch (raw) {
            case "riding" -> RIDING;
            case "traveling" -> TRAVELING;
            default -> WAITING;
        };
    }

    public String toNbt() {
        return switch (this) {
            case RIDING -> "riding";
            case WAITING -> "waiting";
            case TRAVELING -> "traveling";
        };
    }
}
