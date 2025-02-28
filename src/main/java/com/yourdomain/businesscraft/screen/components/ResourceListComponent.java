package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
 */
public class ResourceListComponent implements UIComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceListComponent.class);
    private static final int ITEM_HEIGHT = 16;
    private static final int MAX_VISIBLE_ITEMS = 8;
    private static final int SCROLL_AMOUNT = 1;
    
    private final Supplier<Map<Item, Integer>> resourcesSupplier;
    private final int width;
    private boolean visible = true;
    private int scrollOffset = 0;
    private List<ResourceEntry> sortedResources = new ArrayList<>();
    private Button scrollUpButton;
    private Button scrollDownButton;

    public ResourceListComponent(Supplier<Map<Item, Integer>> resourcesSupplier, int width) {
        this.resourcesSupplier = resourcesSupplier;
        this.width = width;
    }

    @Override
    public void init(Consumer<Button> register) {
        scrollUpButton = new Button.Builder(Component.literal("Up"), button -> scrollUp())
            .pos(0, 0) // Position will be set in render
            .size(20, 20)
            .build();
        
        scrollDownButton = new Button.Builder(Component.literal("Down"), button -> scrollDown())
            .pos(0, 0) // Position will be set in render
            .size(20, 20)
            .build();
        
        register.accept(scrollUpButton);
        register.accept(scrollDownButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        if (!visible) return;
        
        // Update the list of resources
        updateResourceList();
        
        // Update button positions
        scrollUpButton.setX(x + width - 24);
        scrollUpButton.setY(y);
        scrollDownButton.setX(x + width - 24);
        scrollDownButton.setY(y + getHeight() - 20);
        
        // Enable/disable scroll buttons based on scroll position
        scrollUpButton.active = scrollOffset > 0;
        scrollDownButton.active = scrollOffset < Math.max(0, sortedResources.size() - MAX_VISIBLE_ITEMS);
        
        // Draw background for the list
        int listHeight = getHeight();
        guiGraphics.fill(x, y, x + width, y + listHeight, 0x80000000); // Semi-transparent background
        
        // Draw the title with a divider
        guiGraphics.drawString(Minecraft.getInstance().font, "Resources", x + 5, y + 5, 0xFFFFFF);
        guiGraphics.fill(x + 5, y + 18, x + width - 5, y + 19, 0x80FFFFFF); // Divider line
        
        // Check if we have any resources
        if (sortedResources.isEmpty()) {
            // Draw a message when no resources are available
            guiGraphics.drawString(Minecraft.getInstance().font, 
                "No resources available", x + 20, y + 40, 0xAAAAAA);
            return;
        }
        
        // Draw the resources list
        int yOffset = y + 24; // Start below the title and divider
        int count = 0;
        for (int i = scrollOffset; i < sortedResources.size() && count < MAX_VISIBLE_ITEMS; i++) {
            ResourceEntry entry = sortedResources.get(i);
            
            // Draw row background with alternating colors
            int rowColor = count % 2 == 0 ? 0x30FFFFFF : 0x20FFFFFF;
            guiGraphics.fill(x + 5, yOffset, x + width - 28, yOffset + ITEM_HEIGHT, rowColor);
            
            // Draw item icon
            guiGraphics.renderItem(entry.getItemStack(), x + 7, yOffset);
            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, entry.getItemStack(), x + 7, yOffset);
            
            // Draw item name and count
            String displayText = entry.getItemName() + ": " + entry.getCount();
            guiGraphics.drawString(Minecraft.getInstance().font, displayText, x + 28, yOffset + 4, 0xFFFFFF);
            
            yOffset += ITEM_HEIGHT;
            count++;
        }
    }

    @Override
    public void tick() {
        // Update the list each tick to ensure it's current
        updateResourceList();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return 20 + (MAX_VISIBLE_ITEMS * ITEM_HEIGHT);
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (scrollUpButton != null) scrollUpButton.visible = visible;
        if (scrollDownButton != null) scrollDownButton.visible = visible;
    }
    
    private void scrollUp() {
        scrollOffset = Math.max(0, scrollOffset - SCROLL_AMOUNT);
    }
    
    private void scrollDown() {
        scrollOffset = Math.min(
            Math.max(0, sortedResources.size() - MAX_VISIBLE_ITEMS), 
            scrollOffset + SCROLL_AMOUNT
        );
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
        
        // Sort the list by name
        newList.sort(Comparator.comparing(ResourceEntry::getItemName));
        
        // Update our list
        sortedResources = newList;
    }
    
    // Add mouse wheel scrolling support
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (isMouseOver(mouseX, mouseY)) {
            if (scrollAmount > 0) {
                scrollUp();
                return true;
            } else if (scrollAmount < 0) {
                scrollDown();
                return true;
            }
        }
        return false;
    }
    
    // Check if mouse is over the component
    private boolean isMouseOver(double mouseX, double mouseY) {
        // These are set during render and might not be up-to-date
        // but should be close enough for mouse wheel interactions
        int x = scrollUpButton.getX() - (width - 24);
        int y = scrollUpButton.getY();
        int height = getHeight();
        
        return mouseX >= x && mouseX <= x + width && 
               mouseY >= y && mouseY <= y + height;
    }
    
    /**
     * Helper class to store an item and its count
     */
    private static class ResourceEntry {
        private final Item item;
        private final int count;
        
        ResourceEntry(Item item, int count) {
            this.item = item;
            this.count = count;
        }
        
        public Item getItem() {
            return item;
        }
        
        public int getCount() {
            return count;
        }
        
        public String getItemName() {
            // Use the item's display name for better formatting
            return Component.translatable(item.getDescriptionId()).getString();
        }
        
        public net.minecraft.world.item.ItemStack getItemStack() {
            return new net.minecraft.world.item.ItemStack(item);
        }
    }
} 