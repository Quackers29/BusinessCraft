package com.quackers29.businesscraft.ui.layout;

import com.quackers29.businesscraft.ui.components.basic.BCPanel;
import com.quackers29.businesscraft.ui.components.basic.UIComponent;
import com.quackers29.businesscraft.ui.components.basic.BCComponent;

import java.util.List;

/**
 * Flow layout implementation for BusinessCraft UI system.
 * Arranges components horizontally or vertically with optional wrapping.
 */
public class BCFlowLayout implements BCLayout {
    public enum Direction {
        HORIZONTAL,
        VERTICAL
    }
    
    private final Direction direction;
    private final int spacing;
    private final boolean wrap;
    private final int wrapWidth;
    private final int wrapHeight;
    
    /**
     * Create a new flow layout with the specified direction and spacing
     */
    public BCFlowLayout(Direction direction, int spacing) {
        this(direction, spacing, false, 0, 0);
    }
    
    /**
     * Create a new flow layout with wrapping support
     */
    public BCFlowLayout(Direction direction, int spacing, boolean wrap, int wrapWidth, int wrapHeight) {
        this.direction = direction;
        this.spacing = spacing;
        this.wrap = wrap;
        this.wrapWidth = wrapWidth;
        this.wrapHeight = wrapHeight;
    }
    
    @Override
    public void layout(BCPanel container, List<UIComponent> components) {
        int startX = container.getInnerLeft();
        int startY = container.getInnerTop();
        int currentX = startX;
        int currentY = startY;
        int rowHeight = 0;
        int columnWidth = 0;
        
        for (UIComponent component : components) {
            if (!component.isVisible()) {
                continue;
            }
            
            if (direction == Direction.HORIZONTAL) {
                // Check if we need to wrap
                if (wrap && currentX + component.getWidth() > startX + container.getInnerWidth()) {
                    currentX = startX;
                    currentY += rowHeight + spacing;
                    rowHeight = 0;
                }
                
                // Position component
                if (component instanceof BCComponent) {
                    ((BCComponent) component).position(currentX, currentY);
                }
                
                // Update position for next component
                currentX += component.getWidth() + spacing;
                rowHeight = Math.max(rowHeight, component.getHeight());
            } else {
                // Check if we need to wrap
                if (wrap && currentY + component.getHeight() > startY + container.getInnerHeight()) {
                    currentY = startY;
                    currentX += columnWidth + spacing;
                    columnWidth = 0;
                }
                
                // Position component
                if (component instanceof BCComponent) {
                    ((BCComponent) component).position(currentX, currentY);
                }
                
                // Update position for next component
                currentY += component.getHeight() + spacing;
                columnWidth = Math.max(columnWidth, component.getWidth());
            }
        }
    }
} 
