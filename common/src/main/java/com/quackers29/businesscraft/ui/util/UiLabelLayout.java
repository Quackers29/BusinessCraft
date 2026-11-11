package com.quackers29.businesscraft.ui.util;

import java.util.List;

/**
 * Nudges a label downward until it no longer overlaps labels already placed.
 * Used by the town map so stacked town names stay readable.
 */
public final class UiLabelLayout {
    private UiLabelLayout() {
    }

    public record Rect(int x, int y, int w, int h) {
    }

    public static int placeY(int x, int y, int w, int h, List<Rect> placed) {
        return place(x, y, w, h, placed)[1];
    }

    /**
     * Places a label. A collision first tries just to the right of the blocking label,
     * then a full line below the original x. Returns {@code {x, y}}.
     */
    public static int[] place(int x, int y, int w, int h, List<Rect> placed) {
        int nextX = x;
        int nextY = y;
        int boxH = Math.max(1, h);
        int boxW = Math.max(1, w);
        for (int guard = 0; guard < 12; guard++) {
            Rect hit = null;
            for (Rect rect : placed) {
                if (overlaps(nextX, nextY, boxW, boxH, rect)) {
                    hit = rect;
                    break;
                }
            }
            if (hit == null) {
                break;
            }
            int toTheRight = hit.x + hit.w + 4;
            if (!overlapsAny(toTheRight, y, boxW, boxH, placed)) {
                nextX = toTheRight;
                nextY = y;
            } else {
                nextX = x;
                nextY = hit.y + hit.h + 4;
            }
        }
        placed.add(new Rect(nextX, nextY, boxW, boxH));
        return new int[] { nextX, nextY };
    }

    private static boolean overlapsAny(int x, int y, int w, int h, List<Rect> placed) {
        for (Rect rect : placed) {
            if (overlaps(x, y, w, h, rect)) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlaps(int x, int y, int w, int h, Rect rect) {
        return x < rect.x + rect.w && x + w > rect.x && y < rect.y + rect.h && y + h > rect.y;
    }
}