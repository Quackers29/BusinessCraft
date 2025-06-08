package com.yourdomain.businesscraft.ui.components.display;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that displays a scrollable list of resources (items and their quantities)
 * Refactored to use BCScrollableListComponent base class
 */
public class ResourceListComponent extends BCScrollableListComponent<ResourceListComponent.ResourceEntry> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceListComponent.class);
    private static final int ITEM_HEIGHT = 16;
    
    private final Supplier<Map<Item, Integer>> resourcesSupplier;
    private final boolean showItemGraphics;

    public ResourceListComponent(Supplier<Map<Item, Integer>> resourcesSupplier, int width) {
        this(resourcesSupplier, width, true);
    }
    
    public ResourceListComponent(Supplier<Map<Item, Integer>> resourcesSupplier, int width, boolean showItemGraphics) {
        super(width, 
              ITEM_HEIGHT * 8 + 25, // Display area height (8 items plus title)
              ITEM_HEIGHT, 
              entry -> formatResourceText(entry, showItemGraphics));
        
        this.resourcesSupplier = resourcesSupplier;
        this.showItemGraphics = showItemGraphics;
        
        // Style customization
        withBorderColor(0x80FFFFFF);
        withBackgroundColor(0x80000000);
        withItemBackgroundColor(0x20FFFFFF);
        withHoveredItemBackgroundColor(0x40FFFFFF);
        withSelectedItemBackgroundColor(0x60FFFFFF);
        withItemSpacing(1);
        withCornerRadius(4);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Draw the title with a divider
        guiGraphics.drawString(Minecraft.getInstance().font, "Resources", x + 5, y + 5, 0xFFFFFF);
        guiGraphics.fill(x + 5, y + 18, x + width - 5, y + 19, 0x80FFFFFF); // Divider line
        
        // Update the resource list
        updateResourceList();
        
        // Render the scrollable content below the title
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 25, 0); // Move down past the title
        super.renderContent(guiGraphics, mouseX, mouseY);
        guiGraphics.pose().popPose();
    }
    
    @Override
    protected void renderScrollContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render each resource item with item graphics if enabled
        super.renderScrollContent(guiGraphics, mouseX, mouseY);
        
        // If showing item graphics, overlay the item icons
        if (showItemGraphics) {
            renderItemGraphics(guiGraphics);
        }
    }
    
    private void renderItemGraphics(GuiGraphics guiGraphics) {
        // Calculate visible range
        int visibleStartIndex = (int) (getScrollOffset() / (ITEM_HEIGHT + 1));
        int visibleEndIndex = Math.min(
            getItems().size(),
            visibleStartIndex + (getHeight() / (ITEM_HEIGHT + 1)) + 2
        );
        
        // Render item icons for visible items
        int startY = y;
        for (int i = visibleStartIndex; i < visibleEndIndex; i++) {
            if (i >= getItems().size()) break;
            
            ResourceEntry entry = getItems().get(i);
            int itemY = startY + (i * (ITEM_HEIGHT + 1));
            
            // Draw the item
            ItemStack stack = new ItemStack(entry.getItem(), 1);
            guiGraphics.renderItem(stack, x + 2, itemY);
            
            // Draw the quantity
            String countText = String.valueOf(entry.getCount());
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                countText,
                x + 20 + Minecraft.getInstance().font.width(entry.getItem().getDescription().getString()),
                itemY + 4,
                0xFFFFFF
            );
        }
    }
    
    private static String formatResourceText(ResourceEntry entry, boolean showItemGraphics) {
        // If showing item graphics, indent the text to make room for the icon
        String prefix = showItemGraphics ? "   " : "";
        return prefix + entry.getItem().getDescription().getString() + ": " + entry.getCount();
    }
    
    private List<ResourceEntry> getItems() {
        return items;
    }
    
    private void updateResourceList() {
        // Get the current resources
        Map<Item, Integer> resources = resourcesSupplier.get();
        
        // Create a new list
        List<ResourceEntry> newList = new ArrayList<>();
        
        // Add all resources to the list
        resources.forEach((item, count) -> {
            newList.add(new ResourceEntry(item, count));
        });
        
        // Sort the list by quantity (highest first)
        newList.sort(Comparator.comparing(ResourceEntry::getCount).reversed());
        
        // Update our list
        setItems(newList);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Forward the drag event to the scrollable list implementation
        // Adjust mouse Y to account for the title area offset (25 pixels for the title)
        return super.mouseDragged(mouseX, mouseY - 25, button, dragX, dragY);
    }
    
    /**
     * Helper class to store an item and its count
     */
    public static class ResourceEntry {
        private final Item item;
        private final int count;
        
        public ResourceEntry(Item item, int count) {
            this.item = item;
            this.count = count;
        }
        
        public Item getItem() {
            return item;
        }
        
        public int getCount() {
            return count;
        }
    }
} 