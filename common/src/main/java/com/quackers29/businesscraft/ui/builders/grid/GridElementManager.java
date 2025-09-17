package com.quackers29.businesscraft.ui.builders.grid;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages grid elements and their data.
 * Handles creation, storage, and retrieval of grid content items.
 * Extracted from UIGridBuilder to improve single responsibility principle.
 */
public class GridElementManager {
    
    private final List<GridElement> elements = new ArrayList<>();
    
    /**
     * Adds a text element to the grid.
     * 
     * @param row Row position (0-based)
     * @param column Column position (0-based)
     * @param text Text content
     * @param textColor Color of the text
     */
    public void addText(int row, int column, String text, int textColor) {
        GridElement element = new GridElement(row, column, GridContentType.TEXT);
        element.setValue(text);
        element.setTextColor(textColor);
        elements.add(element);
    }
    
    /**
     * Adds a button element to the grid.
     * 
     * @param row Row position (0-based)
     * @param column Column position (0-based)
     * @param text Button text
     * @param onClick Click handler
     * @param buttonColor Button color
     */
    public void addButton(int row, int column, String text, Consumer<Void> onClick, int buttonColor) {
        GridElement element = new GridElement(row, column, GridContentType.BUTTON);
        element.setValue(text);
        element.setOnClick(onClick);
        element.setBgColor(buttonColor);
        elements.add(element);
    }
    
    /**
     * Adds an item element to the grid.
     * 
     * @param row Row position (0-based)
     * @param column Column position (0-based)
     * @param itemStack ItemStack to display
     */
    public void addItem(int row, int column, ItemStack itemStack) {
        GridElement element = new GridElement(row, column, GridContentType.ITEM);
        element.setValue(itemStack);
        elements.add(element);
    }
    
    /**
     * Adds an item with quantity element to the grid.
     * 
     * @param row Row position (0-based)
     * @param column Column position (0-based)
     * @param itemStack ItemStack to display
     * @param quantity Quantity to show
     */
    public void addItemWithQuantity(int row, int column, ItemStack itemStack, int quantity) {
        GridElement element = new GridElement(row, column, GridContentType.ITEM_WITH_QUANTITY);
        element.setValue(itemStack);
        element.setQuantity(quantity);
        elements.add(element);
    }
    
    /**
     * Adds a toggle element to the grid.
     * 
     * @param row Row position (0-based)
     * @param column Column position (0-based)
     * @param text Toggle text
     * @param initialState Initial toggle state
     * @param onToggle Toggle handler
     */
    public void addToggle(int row, int column, String text, boolean initialState, Consumer<Boolean> onToggle) {
        GridElement element = new GridElement(row, column, GridContentType.TOGGLE);
        element.setValue(text);
        element.setToggleState(initialState);
        element.setOnToggle(onToggle);
        elements.add(element);
    }
    
    /**
     * Gets an element at the specified position.
     * 
     * @param row Row position
     * @param column Column position
     * @return GridElement at the position, or null if not found
     */
    public GridElement getElement(int row, int column) {
        return elements.stream()
            .filter(element -> element.getRow() == row && element.getColumn() == column)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Gets all elements in the specified row.
     * 
     * @param row Row index
     * @return List of elements in the row
     */
    public List<GridElement> getElementsInRow(int row) {
        return elements.stream()
            .filter(element -> element.getRow() == row)
            .toList();
    }
    
    /**
     * Gets all elements in the specified column.
     * 
     * @param column Column index
     * @return List of elements in the column
     */
    public List<GridElement> getElementsInColumn(int column) {
        return elements.stream()
            .filter(element -> element.getColumn() == column)
            .toList();
    }
    
    /**
     * Gets all elements that are currently visible based on scroll offsets.
     * 
     * @param rowOffset Vertical scroll offset
     * @param columnOffset Horizontal scroll offset
     * @param visibleRows Number of visible rows
     * @param visibleColumns Number of visible columns
     * @return List of visible elements
     */
    public List<GridElement> getVisibleElements(int rowOffset, int columnOffset, int visibleRows, int visibleColumns) {
        int maxRow = rowOffset + visibleRows;
        int maxColumn = columnOffset + visibleColumns;
        
        return elements.stream()
            .filter(element -> element.getRow() >= rowOffset && element.getRow() < maxRow &&
                              element.getColumn() >= columnOffset && element.getColumn() < maxColumn)
            .toList();
    }
    
    /**
     * Removes an element at the specified position.
     * 
     * @param row Row position
     * @param column Column position
     * @return True if element was removed
     */
    public boolean removeElement(int row, int column) {
        return elements.removeIf(element -> element.getRow() == row && element.getColumn() == column);
    }
    
    /**
     * Clears all elements.
     */
    public void clear() {
        elements.clear();
    }
    
    /**
     * Gets the total number of elements.
     * 
     * @return Number of elements
     */
    public int getElementCount() {
        return elements.size();
    }
    
    /**
     * Gets all elements.
     * 
     * @return List of all elements
     */
    public List<GridElement> getAllElements() {
        return new ArrayList<>(elements);
    }
    
    /**
     * Handles a click event at the specified grid position.
     * 
     * @param row Row position
     * @param column Column position
     * @return True if click was handled
     */
    public boolean handleClick(int row, int column) {
        GridElement element = getElement(row, column);
        if (element != null && element.getOnClick() != null) {
            element.getOnClick().accept(null);
            return true;
        }
        return false;
    }
    
    /**
     * Handles a toggle event at the specified grid position.
     * 
     * @param row Row position
     * @param column Column position
     * @return True if toggle was handled
     */
    public boolean handleToggle(int row, int column) {
        GridElement element = getElement(row, column);
        if (element != null && element.getType() == GridContentType.TOGGLE && element.getOnToggle() != null) {
            boolean newState = !element.getToggleState();
            element.setToggleState(newState);
            element.getOnToggle().accept(newState);
            return true;
        }
        return false;
    }
    
    /**
     * Enum for different types of grid content.
     */
    public enum GridContentType {
        TEXT,
        ITEM,
        ITEM_WITH_QUANTITY,
        TOGGLE,
        BUTTON
    }
    
    /**
     * Represents a single element in the grid.
     */
    public static class GridElement {
        private final int row;
        private final int column;
        private final GridContentType type;
        
        private Object value;
        private int textColor = 0xFFFFFFFF;  // Default white text
        private int bgColor = 0xA0335599;    // Default blue background
        private String tooltip;
        private Consumer<Void> onClick;
        private Consumer<Boolean> onToggle;
        private boolean toggleState = false;
        private int quantity = 0;
        
        public GridElement(int row, int column, GridContentType type) {
            this.row = row;
            this.column = column;
            this.type = type;
        }
        
        // Getters and setters
        public int getRow() { return row; }
        public int getColumn() { return column; }
        public GridContentType getType() { return type; }
        
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
        
        public int getTextColor() { return textColor; }
        public void setTextColor(int textColor) { this.textColor = textColor; }
        
        public int getBgColor() { return bgColor; }
        public void setBgColor(int bgColor) { this.bgColor = bgColor; }
        
        public String getTooltip() { return tooltip; }
        public void setTooltip(String tooltip) { this.tooltip = tooltip; }
        
        public Consumer<Void> getOnClick() { return onClick; }
        public void setOnClick(Consumer<Void> onClick) { this.onClick = onClick; }
        
        public Consumer<Boolean> getOnToggle() { return onToggle; }
        public void setOnToggle(Consumer<Boolean> onToggle) { this.onToggle = onToggle; }
        
        public boolean getToggleState() { return toggleState; }
        public void setToggleState(boolean toggleState) { this.toggleState = toggleState; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        
        @Override
        public String toString() {
            return String.format("GridElement{row=%d, col=%d, type=%s, value=%s}", 
                                row, column, type, value);
        }
    }
}
