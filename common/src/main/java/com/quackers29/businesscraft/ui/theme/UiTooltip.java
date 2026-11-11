package com.quackers29.businesscraft.ui.theme;

/**
 * Keeps a tooltip box inside the scaled screen.
 */
public final class UiTooltip {
    private UiTooltip() {
    }

    public record Box(int x, int y) {
    }

    public static Box place(int screenW, int screenH, int preferX, int preferY, int boxW, int boxH) {
        int x = preferX;
        int y = preferY;
        int maxX = Math.max(2, screenW - 2 - Math.max(0, boxW));
        int maxY = Math.max(2, screenH - 2 - Math.max(0, boxH));
        if (x > maxX) {
            x = maxX;
        }
        if (x < 2) {
            x = 2;
        }
        if (y > maxY) {
            y = maxY;
        }
        if (y < 2) {
            y = 2;
        }
        return new Box(x, y);
    }
}
