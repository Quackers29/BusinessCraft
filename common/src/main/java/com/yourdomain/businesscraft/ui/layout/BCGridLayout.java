package com.yourdomain.businesscraft.ui.layout;

import com.yourdomain.businesscraft.ui.components.basic.BCPanel;
import com.yourdomain.businesscraft.ui.components.basic.UIComponent;
import com.yourdomain.businesscraft.ui.components.basic.BCComponent;

import java.util.List;

/**
 * Grid layout implementation for BusinessCraft UI system.
 * Arranges components in a grid with a specified number of columns.
 */
public class BCGridLayout implements BCLayout {
    private final int columns;
    private final int horizontalSpacing;
    private final int verticalSpacing;
    private final boolean equalWidth;
    private final boolean equalHeight;
    
    /**
     * Create a new grid layout with the specified number of columns and spacing
     */
    public BCGridLayout(int columns, int horizontalSpacing, int verticalSpacing) {
        this(columns, horizontalSpacing, verticalSpacing, false, false);
    }
    
    /**
     * Create a new grid layout with options for equal sizing
     */
    public BCGridLayout(int columns, int horizontalSpacing, int verticalSpacing, 
                         boolean equalWidth, boolean equalHeight) {
        this.columns = columns;
        this.horizontalSpacing = horizontalSpacing;
        this.verticalSpacing = verticalSpacing;
        this.equalWidth = equalWidth;
        this.equalHeight = equalHeight;
    }
    
    @Override
    public void layout(BCPanel container, List<UIComponent> components) {
        if (components.isEmpty()) return;
        
        int startX = container.getInnerLeft();
        int startY = container.getInnerTop();
        int availableWidth = container.getInnerWidth();
        
        // Calculate column width if equal width is requested
        int columnWidth = availableWidth / columns;
        if (equalWidth) {
            columnWidth = (availableWidth - (horizontalSpacing * (columns - 1))) / columns;
        }
        
        // First pass: determine row heights if needed
        int[] rowHeights = null;
        if (equalHeight) {
            // Find the number of rows we'll need
            int visibleCount = (int) components.stream().filter(UIComponent::isVisible).count();
            int rows = (visibleCount + columns - 1) / columns; // Ceiling division
            rowHeights = new int[rows];
            
            // Initialize with the first visible component's height
            for (UIComponent component : components) {
                if (component.isVisible()) {
                    for (int i = 0; i < rows; i++) {
                        rowHeights[i] = component.getHeight();
                    }
                    break;
                }
            }
        } else {
            // Find the number of rows we'll need
            int visibleCount = (int) components.stream().filter(UIComponent::isVisible).count();
            int rows = (visibleCount + columns - 1) / columns; // Ceiling division
            rowHeights = new int[rows];
            
            // Calculate the maximum height for each row
            int row = 0;
            int col = 0;
            for (UIComponent component : components) {
                if (!component.isVisible()) continue;
                
                rowHeights[row] = Math.max(rowHeights[row], component.getHeight());
                
                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
            }
        }
        
        // Second pass: position components
        int row = 0;
        int col = 0;
        int currentX = startX;
        int currentY = startY;
        
        for (UIComponent component : components) {
            if (!component.isVisible()) continue;
            
            // Position the component
            if (component instanceof BCComponent) {
                if (equalWidth) {
                    // Resize component to match column width
                    if (component instanceof BCComponent) {
                        ((BCComponent) component).size(columnWidth, component.getHeight());
                    }
                }
                
                ((BCComponent) component).position(currentX, currentY);
            }
            
            // Move to next column
            col++;
            currentX += (equalWidth ? columnWidth : component.getWidth()) + horizontalSpacing;
            
            // If we've reached the end of a row, move to the next row
            if (col >= columns) {
                col = 0;
                row++;
                currentX = startX;
                currentY += rowHeights[row - 1] + verticalSpacing;
            }
        }
    }
} 