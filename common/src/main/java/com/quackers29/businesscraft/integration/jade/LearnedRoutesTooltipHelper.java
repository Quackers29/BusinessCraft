package com.quackers29.businesscraft.integration.jade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure helpers for formatting AUTO learned-route summaries in Jade tooltips.
 */
public final class LearnedRoutesTooltipHelper {

    public record LearnedRouteLine(String townName, float strength) {
    }

    private LearnedRoutesTooltipHelper() {
    }

    public static List<LearnedRouteLine> buildLines(
            List<Map.Entry<UUID, Double>> topRoutes,
            double totalScore,
            java.util.function.Function<UUID, String> townNameLookup) {
        List<LearnedRouteLine> lines = new ArrayList<>();
        if (topRoutes == null || topRoutes.isEmpty()) {
            return lines;
        }
        for (Map.Entry<UUID, Double> entry : topRoutes) {
            UUID destId = entry.getKey();
            double score = entry.getValue() == null ? 0 : entry.getValue();
            String name = townNameLookup.apply(destId);
            if (name == null || name.isBlank()) {
                name = "Unknown";
            }
            float strength = totalScore > 0 ? (float) (score / totalScore) : 0f;
            lines.add(new LearnedRouteLine(name, strength));
        }
        return lines;
    }

    public static int sharePercentRounded(float strength) {
        if (strength <= 0) {
            return 0;
        }
        if (strength >= 1f) {
            return 100;
        }
        return Math.round(strength * 100f);
    }

  /**
   * Short single-line summary for Jade, e.g. {@code Oakvale (42%), Hillcrest (31%)}.
   */
    public static String formatLearnedRoutesSummary(List<LearnedRouteLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            LearnedRouteLine line = lines.get(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(line.townName())
                    .append(" (")
                    .append(sharePercentRounded(line.strength()))
                    .append("%)");
        }
        return builder.toString();
    }
}
