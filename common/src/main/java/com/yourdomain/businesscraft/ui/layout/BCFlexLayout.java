package com.yourdomain.businesscraft.ui.layout;

import com.yourdomain.businesscraft.ui.layout.BCLayout;
import com.yourdomain.businesscraft.ui.components.basic.BCPanel;
import com.yourdomain.businesscraft.ui.components.basic.UIComponent;
import com.yourdomain.businesscraft.ui.components.basic.BCComponent;
import java.util.List;

/**
 * Flexible layout manager that supports both horizontal and vertical arrangements
 * with various alignment and distribution options.
 */
public class BCFlexLayout implements BCLayout {
    public enum Direction {
        ROW,            // Left to right
        COLUMN,         // Top to bottom
        ROW_REVERSE,    // Right to left
        COLUMN_REVERSE  // Bottom to top
    }
    
    public enum Alignment {
        START,          // Pack to start
        CENTER,         // Center align
        END,           // Pack to end
        SPACE_BETWEEN, // Distribute with space between
        SPACE_AROUND,  // Distribute with space around
        STRETCH        // Stretch to fill
    }
    
    private final Direction direction;
    private final Alignment mainAxisAlignment;
    private final Alignment crossAxisAlignment;
    private final int gap;
    private final int padding;
    
    public BCFlexLayout(Direction direction, Alignment mainAxisAlignment, 
                       Alignment crossAxisAlignment, int gap, int padding) {
        this.direction = direction;
        this.mainAxisAlignment = mainAxisAlignment;
        this.crossAxisAlignment = crossAxisAlignment;
        this.gap = gap;
        this.padding = padding;
    }
    
    @Override
    public void layout(BCPanel container, List<UIComponent> components) {
        if (components.isEmpty()) return;
        
        int availableWidth = container.getInnerWidth();
        int availableHeight = container.getInnerHeight();
        
        // Apply stretching first if needed
        if (crossAxisAlignment == Alignment.STRETCH) {
            applyStretch(components, availableWidth, availableHeight);
        }
        
        // Calculate total content size and spacing
        int totalMainAxisSize = 0;
        int maxCrossAxisSize = 0;
        
        for (UIComponent component : components) {
            if (!component.isVisible()) continue;
            
            if (isHorizontal()) {
                totalMainAxisSize += component.getWidth();
                maxCrossAxisSize = Math.max(maxCrossAxisSize, component.getHeight());
            } else {
                totalMainAxisSize += component.getHeight();
                maxCrossAxisSize = Math.max(maxCrossAxisSize, component.getWidth());
            }
        }
        
        // Add gaps between components
        if (components.size() > 1) {
            totalMainAxisSize += gap * (components.size() - 1);
        }
        
        // Calculate spacing based on alignment
        int mainAxisSpace = (isHorizontal() ? availableWidth : availableHeight) - totalMainAxisSize;
        float startOffset = calculateStartOffset(mainAxisSpace, components.size());
        float spacing = calculateSpacing(mainAxisSpace, components.size());
        
        // Position components
        float currentPos = startOffset;
        for (UIComponent component : components) {
            if (!component.isVisible()) continue;
            
            int x = container.getInnerLeft();
            int y = container.getInnerTop();
            
            if (isHorizontal()) {
                x += (int)currentPos;
                y += calculateCrossAxisPosition(component, availableHeight, maxCrossAxisSize);
                if (direction == Direction.ROW_REVERSE) {
                    x = container.getInnerLeft() + availableWidth - x - component.getWidth();
                }
                currentPos += component.getWidth() + spacing;
            } else {
                x += calculateCrossAxisPosition(component, availableWidth, maxCrossAxisSize);
                y += (int)currentPos;
                if (direction == Direction.COLUMN_REVERSE) {
                    y = container.getInnerTop() + availableHeight - y - component.getHeight();
                }
                currentPos += component.getHeight() + spacing;
            }
            
            if (component instanceof BCPanel) {
                ((BCPanel)component).positionWithChildren(x, y);
            } else if (component instanceof BCComponent) {
                ((BCComponent)component).position(x, y);
            } else {
                component.render(null, x, y, 0, 0); // Update position
            }
        }
    }
    
    /**
     * Apply stretching to components if needed
     */
    private void applyStretch(List<UIComponent> components, int availableWidth, int availableHeight) {
        for (UIComponent component : components) {
            if (!component.isVisible() || !(component instanceof BCComponent)) {
                continue;
            }
            
            BCComponent bcComponent = (BCComponent) component;
            
            if (isHorizontal()) {
                // For horizontal layouts, stretch height
                bcComponent.size(component.getWidth(), availableHeight - (padding * 2));
            } else {
                // For vertical layouts, stretch width
                bcComponent.size(availableWidth - (padding * 2), component.getHeight());
            }
        }
    }
    
    private boolean isHorizontal() {
        return direction == Direction.ROW || direction == Direction.ROW_REVERSE;
    }
    
    private float calculateStartOffset(int availableSpace, int componentCount) {
        if (availableSpace <= 0) return padding;
        
        switch (mainAxisAlignment) {
            case CENTER:
                return availableSpace / 2.0f;
            case END:
                return availableSpace;
            case SPACE_BETWEEN:
                return padding;
            case SPACE_AROUND:
                return availableSpace / (componentCount + 1.0f);
            default: // START or STRETCH
                return padding;
        }
    }
    
    private float calculateSpacing(int availableSpace, int componentCount) {
        if (availableSpace <= 0 || componentCount <= 1) return gap;
        
        switch (mainAxisAlignment) {
            case SPACE_BETWEEN:
                return availableSpace / (float)(componentCount - 1);
            case SPACE_AROUND:
                return availableSpace / (float)(componentCount + 1);
            default:
                return gap;
        }
    }
    
    private int calculateCrossAxisPosition(UIComponent component, int availableSize, int maxSize) {
        int size = isHorizontal() ? component.getHeight() : component.getWidth();
        
        switch (crossAxisAlignment) {
            case CENTER:
                return (availableSize - size) / 2;
            case END:
                return availableSize - size - padding;
            case STRETCH:
                // Already handled in applyStretch
                return padding;
            default: // START
                return padding;
        }
    }
    
    /**
     * Create a horizontal layout with default settings
     */
    public static BCFlexLayout createHorizontal(int gap) {
        return new BCFlexLayout(Direction.ROW, Alignment.START, Alignment.START, gap, 0);
    }
    
    /**
     * Create a vertical layout with default settings
     */
    public static BCFlexLayout createVertical(int gap) {
        return new BCFlexLayout(Direction.COLUMN, Alignment.START, Alignment.START, gap, 0);
    }
} 