package com.quackers29.businesscraft.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Pure formatting for tourist name tags (client + unit tests).
 */
public final class TouristDisplayFormat {
    /** Name tags fade beyond this distance (blocks). */
    public static final double NAME_TAG_MAX_DISTANCE = 48.0;

    private TouristDisplayFormat() {
    }

    public static MutableComponent primaryLine(String destinationName, boolean anyTown) {
        String dest = sanitize(destinationName, "Unknown");
        if (anyTown) {
            return Component.literal("To Any Town").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        }
        return Component.literal("To ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(dest).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    /** Second line: origin + type (kept short so it fits above the head). */
    public static MutableComponent secondaryLine(String originName, TouristType type) {
        String origin = sanitize(originName, "Unknown");
        MutableComponent line = Component.literal("from ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(origin).withStyle(ChatFormatting.WHITE));
        if (type != null) {
            line = line.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(type.displayName()).withStyle(ChatFormatting.AQUA));
        }
        return line;
    }

    /** Optional third line: distance traveled. */
    public static MutableComponent distanceLine(double distanceMeters) {
        return Component.literal(formatDistance(distanceMeters) + " traveled")
                .withStyle(ChatFormatting.YELLOW);
    }

    public static String formatDistance(double meters) {
        if (meters >= 1000) {
            return String.format("%.1f km", meters / 1000.0);
        }
        return String.format("%d m", (int) Math.round(meters));
    }

    public static String sanitize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 24) {
            return trimmed.substring(0, 21) + "...";
        }
        return trimmed;
    }

    public static boolean withinNameTagRange(double distanceSq) {
        double max = NAME_TAG_MAX_DISTANCE;
        return distanceSq <= max * max;
    }
}
