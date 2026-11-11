package com.quackers29.businesscraft.ui.theme;

/**
 * Keeps a tooltip box inside the scaled screen.
 */
public final class UiTooltip {
    private UiTooltip() {
    }

    public record Box(int x, int y) {
    }

    /**
     * Same inset as the original screen clamp: 2px from each edge.
     */
    public static Box place(int screenW, int screenH, int preferX, int preferY, int boxW, int boxH) {
        return placeIn(2, 2, Math.max(2, screenW - 2), Math.max(2, screenH - 2), preferX, preferY, boxW, boxH);
    }

    /**
     * Pins a box inside {@code [left, right) × [top, bottom)}. A box larger than
     * the rectangle stays on the top-left of that rectangle.
     */
    public static Box placeIn(int left, int top, int right, int bottom, int preferX, int preferY, int boxW, int boxH) {
        int maxX = Math.max(left, right - Math.max(0, boxW));
        int maxY = Math.max(top, bottom - Math.max(0, boxH));
        int x = preferX;
        int y = preferY;
        if (x > maxX) {
            x = maxX;
        }
        if (x < left) {
            x = left;
        }
        if (y > maxY) {
            y = maxY;
        }
        if (y < top) {
            y = top;
        }
        return new Box(x, y);
    }
}
