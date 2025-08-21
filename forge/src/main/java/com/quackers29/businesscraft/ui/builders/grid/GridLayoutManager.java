package com.quackers29.businesscraft.ui.builders.grid;

/**
 * Manages grid layout calculations and positioning.
 * Extracted from UIGridBuilder to improve single responsibility principle.
 */
public class GridLayoutManager {
    
    // Grid dimensions and positioning
    private int x, y, width, height;
    private int rows, columns;
    private int horizontalSpacing, verticalSpacing;
    private int horizontalMargin, verticalMargin;
    private Integer customRowHeight;
    
    // Default values
    private static final int DEFAULT_HORIZONTAL_SPACING = 10;
    private static final int DEFAULT_VERTICAL_SPACING = 2;
    private static final int DEFAULT_HORIZONTAL_MARGIN = 15;
    private static final int DEFAULT_VERTICAL_MARGIN = 6;
    private static final int DEFAULT_ROW_HEIGHT = 14;
    
    public GridLayoutManager(int x, int y, int width, int height, int rows, int columns) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rows = rows;
        this.columns = columns;
        this.horizontalSpacing = DEFAULT_HORIZONTAL_SPACING;
        this.verticalSpacing = DEFAULT_VERTICAL_SPACING;
        this.horizontalMargin = DEFAULT_HORIZONTAL_MARGIN;
        this.verticalMargin = DEFAULT_VERTICAL_MARGIN;
        this.customRowHeight = DEFAULT_ROW_HEIGHT;
    }
    
    /**
     * Sets the spacing between grid elements.
     * 
     * @param horizontalSpacing Horizontal spacing in pixels
     * @param verticalSpacing Vertical spacing in pixels
     */
    public void setSpacing(int horizontalSpacing, int verticalSpacing) {
        this.horizontalSpacing = horizontalSpacing;
        this.verticalSpacing = verticalSpacing;
    }
    
    /**
     * Sets the margins around the grid.
     * 
     * @param horizontalMargin Horizontal margin in pixels
     * @param verticalMargin Vertical margin in pixels
     */
    public void setMargins(int horizontalMargin, int verticalMargin) {
        this.horizontalMargin = horizontalMargin;
        this.verticalMargin = verticalMargin;
    }
    
    /**
     * Sets a custom row height.
     * 
     * @param height Height in pixels for each row
     */
    public void setRowHeight(int height) {
        this.customRowHeight = height;
    }
    
    /**
     * Calculates the position of a grid cell.
     * 
     * @param row The row index (0-based)
     * @param column The column index (0-based)
     * @return GridPosition containing x, y coordinates and dimensions
     */
    public GridPosition calculateCellPosition(int row, int column) {
        // Calculate available space for content
        int contentWidth = width - (2 * horizontalMargin);
        int contentHeight = height - (2 * verticalMargin);
        
        // Calculate cell dimensions
        int cellWidth = (contentWidth - ((columns - 1) * horizontalSpacing)) / columns;
        int cellHeight = customRowHeight != null ? customRowHeight : 
            (contentHeight - ((rows - 1) * verticalSpacing)) / rows;
        
        // Calculate cell position
        int cellX = x + horizontalMargin + (column * (cellWidth + horizontalSpacing));
        int cellY = y + verticalMargin + (row * (cellHeight + verticalSpacing));
        
        return new GridPosition(cellX, cellY, cellWidth, cellHeight);
    }
    
    /**
     * Calculates the content area dimensions.
     * 
     * @return GridPosition representing the content area
     */
    public GridPosition getContentArea() {
        int contentX = x + horizontalMargin;
        int contentY = y + verticalMargin;
        int contentWidth = width - (2 * horizontalMargin);
        int contentHeight = height - (2 * verticalMargin);
        
        return new GridPosition(contentX, contentY, contentWidth, contentHeight);
    }
    
    /**
     * Gets the total grid area.
     * 
     * @return GridPosition representing the entire grid area
     */
    public GridPosition getGridArea() {
        return new GridPosition(x, y, width, height);
    }
    
    /**
     * Calculates the minimum height needed for the given number of rows.
     * 
     * @param numRows Number of rows
     * @return Minimum height in pixels
     */
    public int calculateMinimumHeight(int numRows) {
        int rowHeight = customRowHeight != null ? customRowHeight : DEFAULT_ROW_HEIGHT;
        return (2 * verticalMargin) + (numRows * rowHeight) + ((numRows - 1) * verticalSpacing);
    }
    
    /**
     * Calculates the minimum width needed for the given number of columns.
     * 
     * @param numColumns Number of columns
     * @return Minimum width in pixels
     */
    public int calculateMinimumWidth(int numColumns) {
        // Estimate minimum cell width
        int estimatedCellWidth = 50; // Minimum reasonable cell width
        return (2 * horizontalMargin) + (numColumns * estimatedCellWidth) + ((numColumns - 1) * horizontalSpacing);
    }
    
    /**
     * Updates the grid dimensions.
     * 
     * @param newRows New number of rows
     * @param newColumns New number of columns
     */
    public void updateDimensions(int newRows, int newColumns) {
        this.rows = newRows;
        this.columns = newColumns;
    }
    
    /**
     * Updates the grid position and size.
     * 
     * @param newX New X position
     * @param newY New Y position
     * @param newWidth New width
     * @param newHeight New height
     */
    public void updatePosition(int newX, int newY, int newWidth, int newHeight) {
        this.x = newX;
        this.y = newY;
        this.width = newWidth;
        this.height = newHeight;
    }
    
    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getRows() { return rows; }
    public int getColumns() { return columns; }
    public int getHorizontalSpacing() { return horizontalSpacing; }
    public int getVerticalSpacing() { return verticalSpacing; }
    public int getHorizontalMargin() { return horizontalMargin; }
    public int getVerticalMargin() { return verticalMargin; }
    public Integer getCustomRowHeight() { return customRowHeight; }
    
    /**
     * Represents a position and dimensions within the grid.
     */
    public static class GridPosition {
        private final int x, y, width, height;
        
        public GridPosition(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        
        public int getRight() { return x + width; }
        public int getBottom() { return y + height; }
        public int getCenterX() { return x + width / 2; }
        public int getCenterY() { return y + height / 2; }
        
        @Override
        public String toString() {
            return String.format("GridPosition{x=%d, y=%d, width=%d, height=%d}", x, y, width, height);
        }
    }
}