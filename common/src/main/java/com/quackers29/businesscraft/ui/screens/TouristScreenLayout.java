package com.quackers29.businesscraft.ui.screens;

/**
 * Pixel layout for the tourist screen. The header stays compact and the trade
 * list takes the remaining height. When the rows do not fit, {@link #maxScroll}
 * is the extra content the list can scroll.
 */
public final class TouristScreenLayout {
    public final int width;
    public final int height;
    public final int rowHeight;
    public final int rowsTop;
    public final int listHeight;
    public final int contentHeight;
    public final int maxScroll;
    public final boolean roomy;
    public final int titleY;
    public final int identityY;
    public final int fromY;
    public final int toY;
    public final int detailY;
    public final int detailLines;
    public final int progressY;
    public final int barY;
    public final int waitingY;
    public final int tradesY;

    private TouristScreenLayout(int width, int height, int rowHeight, int rowsTop, int listHeight, int contentHeight,
            int maxScroll, boolean roomy, int titleY, int identityY, int fromY, int toY, int detailY, int detailLines,
            int progressY, int barY, int waitingY, int tradesY) {
        this.width = width;
        this.height = height;
        this.rowHeight = rowHeight;
        this.rowsTop = rowsTop;
        this.listHeight = listHeight;
        this.contentHeight = contentHeight;
        this.maxScroll = maxScroll;
        this.roomy = roomy;
        this.titleY = titleY;
        this.identityY = identityY;
        this.fromY = fromY;
        this.toY = toY;
        this.detailY = detailY;
        this.detailLines = detailLines;
        this.progressY = progressY;
        this.barY = barY;
        this.waitingY = waitingY;
        this.tradesY = tradesY;
    }

    public static TouristScreenLayout choose(int screenWidth, int screenHeight, int rows) {
        int safeRows = Math.max(1, rows);
        int width = Math.min(420, Math.max(200, screenWidth - 8));
        boolean roomy = screenHeight >= 420;
        int titleY = roomy ? 8 : 4;
        int line = roomy ? 13 : 11;
        int identityY = titleY + line;
        int fromY = identityY + line + (roomy ? 2 : 0);
        int toY = fromY + line;
        int detailY = toY + line;
        int detailLines = roomy ? 2 : 1;
        int progressY = detailY + detailLines * 11 + 2;
        int barY = progressY + 11;
        int waitingY = barY + 8;
        int tradesY = waitingY + line;
        int rowsTop = tradesY + line;
        int rowHeight = roomy ? 32 : 22;
        int bottomPad = 6;
        int margin = 4;
        int content = safeRows * rowHeight;
        int limit = Math.max(rowsTop + rowHeight + bottomPad, screenHeight - margin);
        int available = limit - rowsTop - bottomPad;
        int listHeight = Math.min(content, Math.max(rowHeight, available));
        int height = rowsTop + listHeight + bottomPad;
        if (height > screenHeight - margin) {
            listHeight = Math.max(rowHeight, screenHeight - margin - rowsTop - bottomPad);
            if (rowsTop + listHeight + bottomPad > screenHeight - margin) {
                int overflow = rowsTop + listHeight + bottomPad - (screenHeight - margin);
                rowsTop = Math.max(titleY + line, rowsTop - overflow);
                tradesY = Math.max(titleY, tradesY - overflow);
                listHeight = Math.max(rowHeight, screenHeight - margin - rowsTop - bottomPad);
            }
            height = Math.min(screenHeight - margin, rowsTop + listHeight + bottomPad);
            listHeight = Math.max(rowHeight, height - rowsTop - bottomPad);
            height = rowsTop + listHeight + bottomPad;
        }
        int maxScroll = Math.max(0, content - listHeight);
        return new TouristScreenLayout(width, height, rowHeight, rowsTop, listHeight, content, maxScroll, roomy,
                titleY, identityY, fromY, toY, detailY, detailLines, progressY, barY, waitingY, tradesY);
    }
}
