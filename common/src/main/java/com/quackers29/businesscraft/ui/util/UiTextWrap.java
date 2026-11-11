package com.quackers29.businesscraft.ui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Greedy word wrap for narrow GUI cells. Width is measured by the caller so
 * unit tests can use a stand-in for the Minecraft font.
 */
public final class UiTextWrap {
    private UiTextWrap() {
    }

    public static List<String> wrap(String text, int maxWidth, ToIntFunction<String> width) {
        if (text == null || text.isEmpty()) {
            return List.of("");
        }
        if (maxWidth <= 0) {
            return List.of(text);
        }
        if (width.applyAsInt(text) <= maxWidth) {
            return List.of(text);
        }
        List<String> lines = new ArrayList<>();
        String remaining = text.trim();
        while (!remaining.isEmpty() && lines.size() < 8) {
            if (width.applyAsInt(remaining) <= maxWidth) {
                lines.add(remaining);
                remaining = "";
                break;
            }
            int cut = longestFit(remaining, maxWidth, width);
            int space = remaining.lastIndexOf(' ', cut);
            int split = (space > 0 && space >= Math.max(1, cut / 2)) ? space : Math.max(1, cut);
            String piece = remaining.substring(0, split).trim();
            if (piece.isEmpty()) {
                piece = remaining.substring(0, Math.max(1, cut));
                split = piece.length();
            }
            lines.add(piece);
            remaining = remaining.substring(Math.min(remaining.length(), split)).trim();
        }
        if (!remaining.isEmpty()) {
            lines.add(remaining);
        }
        return lines;
    }

    private static int longestFit(String text, int maxWidth, ToIntFunction<String> width) {
        int low = 1;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (width.applyAsInt(text.substring(0, mid)) <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /**
     * Shortens {@code text} with an ellipsis so {@code width} of the result is at most {@code maxWidth}.
     * A non-positive width yields an empty string. Text that already fits is returned unchanged.
     */
    public static String ellipsize(String text, int maxWidth, ToIntFunction<String> width) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (width.applyAsInt(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int ellipsisWidth = width.applyAsInt(ellipsis);
        if (maxWidth <= ellipsisWidth) {
            return ellipsis;
        }
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (width.applyAsInt(text.substring(0, mid)) + ellipsisWidth <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return text.substring(0, low) + ellipsis;
    }
}
