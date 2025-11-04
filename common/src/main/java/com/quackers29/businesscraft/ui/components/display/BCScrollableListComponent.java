package com.quackers29.businesscraft.ui.components.display;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A scrollable list component that renders a list of items with a consistent appearance.
 * This component handles virtualization, item rendering, and selection.
 * 
 * @param <T> The type of items in the list
 */
public class BCScrollableListComponent<T> extends BCScrollableComponent {
    // Item rendering
    protected final List<T> items = new ArrayList<>();
    private final Function<T, String> itemTextProvider;
    private final Function<T, Integer> itemColorProvider;
    private final int itemHeight;
    
    // Selection
    private T selectedItem = null;
    private int hoveredIndex = -1;
    private boolean selectionEnabled = true;
    
    // Styling
    private int itemBackgroundColor = 0x30FFFFFF;
    private int hoveredItemBackgroundColor = 0x50FFFFFF;
    private int selectedItemBackgroundColor = 0x80FFFFFF;
    private int itemSpacing = 1;
    
    /**
     * Create a new scrollable list component
     * 
     * @param width The width of the list
     * @param height The height of the list view area
     * @param itemHeight The height of each item in the list
     * @param itemTextProvider Function to extract display text from an item
     * @param itemColorProvider Function to determine text color for an item (can be null)
     */
    public BCScrollableListComponent(
        int width, 
        int height, 
        int itemHeight,
        Function<T, String> itemTextProvider,
        Function<T, Integer> itemColorProvider
    ) {
        super(width, height);
        this.itemHeight = itemHeight;
        this.itemTextProvider = itemTextProvider;
        this.itemColorProvider = itemColorProvider != null ? itemColorProvider : item -> 0xFFFFFF;
    }
    
    /**
     * Create a new scrollable list with default white text color
     */
    public BCScrollableListComponent(
        int width,
        int height,
        int itemHeight,
        Function<T, String> itemTextProvider
    ) {
        this(width, height, itemHeight, itemTextProvider, item -> 0xFFFFFF);
    }
    
    /**
     * Set the items to display in the list
     */
    public BCScrollableListComponent<T> setItems(List<T> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        
        // Update content height
        setContentHeight(getContentHeight());
        
        return this;
    }
    
    /**
     * Calculate the total content height
     */
    private int getContentHeight() {
        return items.size() * (itemHeight + itemSpacing);
    }
    
    @Override
    protected void renderScrollContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (items.isEmpty()) {
            // Render empty message
            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper != null) {
                Object fontObj = clientHelper.getFont();
                if (fontObj instanceof net.minecraft.client.gui.Font font) {
                    guiGraphics.drawString(
                        font,
                        "No items",
                        x + 5,
                        y + 5,
                        0x808080
                    );
                }
            }
            return;
        }
        
        // Calculate visible range based on scroll position
        int startY = y;
        int visibleStartIndex = (int) (scrollOffset / (itemHeight + itemSpacing));
        int visibleEndIndex = Math.min(
            items.size(),
            visibleStartIndex + (height / (itemHeight + itemSpacing)) + 2
        );
        
        // Update hovered item index
        hoveredIndex = -1;
        if (isMouseOver(mouseX, mouseY) && mouseX >= x && mouseX <= x + width) {
            for (int i = visibleStartIndex; i < visibleEndIndex; i++) {
                int itemY = startY + (i * (itemHeight + itemSpacing));
                if (mouseY >= itemY && mouseY < itemY + itemHeight) {
                    hoveredIndex = i;
                    break;
                }
            }
        }
        
        // Render visible items
        for (int i = visibleStartIndex; i < visibleEndIndex; i++) {
            if (i >= items.size()) break;
            
            T item = items.get(i);
            int itemY = startY + (i * (itemHeight + itemSpacing));
            
            // Determine background color
            int backgroundColor;
            if (item.equals(selectedItem)) {
                backgroundColor = selectedItemBackgroundColor;
            } else if (i == hoveredIndex) {
                backgroundColor = hoveredItemBackgroundColor;
            } else {
                backgroundColor = itemBackgroundColor;
            }
            
            // Draw item background
            guiGraphics.fill(
                x,
                itemY,
                x + width - scrollbarWidth - 2,
                itemY + itemHeight,
                backgroundColor
            );
            
            // Draw item text
            String text = itemTextProvider.apply(item);
            int color = itemColorProvider.apply(item);
            
            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper != null) {
                Object fontObj = clientHelper.getFont();
                if (fontObj instanceof net.minecraft.client.gui.Font font) {
                    guiGraphics.drawString(
                        font,
                        text,
                        x + 5,
                        itemY + (itemHeight - 8) / 2,
                        color
                    );
                }
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle selection first
        if (selectionEnabled && hoveredIndex >= 0 && hoveredIndex < items.size()) {
            T clicked = items.get(hoveredIndex);
            selectedItem = clicked;
            triggerEvent("select");
        }
        
        // Then handle normal click behaviors (scrolling, etc.)
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Get the currently selected item, or null if nothing is selected
     */
    public T getSelectedItem() {
        return selectedItem;
    }
    
    /**
     * Set the selected item programmatically
     */
    public BCScrollableListComponent<T> setSelectedItem(T item) {
        this.selectedItem = item;
        
        // If the item is not currently visible, scroll to it
        if (item != null) {
            int index = items.indexOf(item);
            if (index >= 0) {
                ensureItemVisible(index);
            }
        }
        
        return this;
    }
    
    /**
     * Scroll to make the item at the specified index visible
     */
    public void ensureItemVisible(int index) {
        if (index < 0 || index >= items.size()) return;
        
        int itemTop = index * (itemHeight + itemSpacing);
        int itemBottom = itemTop + itemHeight;
        
        if (itemTop < scrollOffset) {
            // Scroll up to show item
            smoothScrollTo(itemTop);
        } else if (itemBottom > scrollOffset + height) {
            // Scroll down to show item
            smoothScrollTo(itemBottom - height);
        }
    }
    
    /**
     * Set whether item selection is enabled
     */
    public BCScrollableListComponent<T> setSelectionEnabled(boolean enabled) {
        this.selectionEnabled = enabled;
        return this;
    }
    
    /**
     * Set styling properties
     */
    public BCScrollableListComponent<T> withItemBackgroundColor(int color) {
        this.itemBackgroundColor = color;
        return this;
    }
    
    public BCScrollableListComponent<T> withHoveredItemBackgroundColor(int color) {
        this.hoveredItemBackgroundColor = color;
        return this;
    }
    
    public BCScrollableListComponent<T> withSelectedItemBackgroundColor(int color) {
        this.selectedItemBackgroundColor = color;
        return this;
    }
    
    public BCScrollableListComponent<T> withItemSpacing(int spacing) {
        this.itemSpacing = spacing;
        setContentHeight(getContentHeight());
        return this;
    }
} 
