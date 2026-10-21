package com.quackers29.businesscraft.ui.layout;

/**
 * Layout math for town screens: panel plus title above and bottom action row below.
 * Positions are in Minecraft GUI (scaled) pixels — the same coordinate space as
 * {@link net.minecraft.client.gui.screens.Screen#width} / {@code height}.
 */
public final class TownScreenLayout {
    public static final int PANEL_WIDTH = 256;
    public static final int PANEL_HEIGHT = 204;
    public static final int DEFAULT_TITLE_MARGIN = 12;
    public static final int DEFAULT_BOTTOM_GAP = 10;
    public static final int DEFAULT_BOTTOM_ROW_HEIGHT = 30;
    /** Vanilla {@code Window.calculateScale} minimum scaled framebuffer. */
    public static final int MIN_SCALED_FRAME_HEIGHT = 240;

    private TownScreenLayout() {
    }

    public record Chrome(int titleMargin, int bottomGap, int bottomRowHeight) {
        public int totalHeight(int panelHeight) {
            return titleMargin + panelHeight + bottomGap + bottomRowHeight;
        }
    }

    /**
     * Picks title/button chrome so the full block fits in {@code scaledHeight},
     * trimming margins before shrinking the button row.
     */
    public static Chrome fitChrome(int scaledHeight, int panelHeight) {
        int title = DEFAULT_TITLE_MARGIN;
        int gap = DEFAULT_BOTTOM_GAP;
        int row = DEFAULT_BOTTOM_ROW_HEIGHT;

        while (title + panelHeight + gap + row > scaledHeight && gap > 2) {
            gap--;
        }
        while (title + panelHeight + gap + row > scaledHeight && row > 18) {
            row--;
        }
        while (title + panelHeight + gap + row > scaledHeight && title > 4) {
            title--;
        }
        if (title + panelHeight + gap + row > scaledHeight) {
            gap = Math.min(gap, 2);
            row = Math.min(row, 18);
            title = Math.min(title, 4);
        }
        return new Chrome(title, gap, row);
    }

    public static int panelTopY(int scaledHeight, int panelHeight, Chrome chrome) {
        int total = chrome.totalHeight(panelHeight);
        int visualTop = Math.max(0, (scaledHeight - total) / 2);
        return visualTop + chrome.titleMargin;
    }

    public static int panelLeftX(int scaledWidth, int panelWidth) {
        return (scaledWidth - panelWidth) / 2;
    }

    /** Largest guiScale vanilla allows for the given window size (320×240 scaled minimum). */
    public static int maxAllowedGuiScale(int windowWidth, int windowHeight) {
        int scale = 1;
        while (windowWidth / (scale + 1) >= 320 && windowHeight / (scale + 1) >= 240) {
            scale++;
        }
        return scale;
    }

    public static int effectiveGuiScale(int windowWidth, int windowHeight, int requestedScale) {
        int max = maxAllowedGuiScale(windowWidth, windowHeight);
        if (requestedScale <= 0) {
            return max;
        }
        return Math.min(requestedScale, max);
    }
}
