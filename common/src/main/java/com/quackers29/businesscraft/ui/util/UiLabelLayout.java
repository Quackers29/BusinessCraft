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
        int nextY = y;
        int boxH = Math.max(1, h);
        int boxW = Math.max(1, w);
        for (int guard = 0; guard < 12; guard++) {
            Rect hit = null;
            for (Rect rect : placed) {
                if (overlaps(x, nextY, boxW, boxH, rect)) {
                    hit = rect;
                    break;
                }
            }
            if (hit == null) {
                break;
            }
            nextY = hit.y + hit.h + 1;
        }
        placed.add(new Rect(x, nextY, boxW, boxH));
        return nextY;
    }

    private static boolean overlaps(int x, int y, int w, int h, Rect rect) {
        return x < rect.x + rect.w && x + w > rect.x && y < rect.y + rect.h && y + h > rect.y;
    }
}