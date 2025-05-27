package com.yourdomain.businesscraft.screen.components;

import com.yourdomain.businesscraft.screen.TownInterfaceTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.Item;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standardized tab content component that provides unified rendering patterns
 * while maintaining the visual appearance of existing tabs.
 */
public class StandardTabContent extends BCComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(StandardTabContent.class);
    
    // Theme constants
    private static final int TEXT_COLOR = TownInterfaceTheme.TEXT_COLOR;
    private static final int TEXT_HIGHLIGHT = TownInterfaceTheme.TEXT_HIGHLIGHT;
    private static final int BACKGROUND_COLOR = TownInterfaceTheme.BACKGROUND_COLOR;
    private static final int BORDER_COLOR = TownInterfaceTheme.BORDER_COLOR;
    
    // Content types
    public enum ContentType {
        LABEL_VALUE_GRID,    // For Overview tab - animated label-value pairs
        ITEM_LIST,           // For Resources tab - scrollable item list
        CUSTOM_LIST,         // For Population tab - custom rendered list
        BUTTON_GRID          // For Settings tab - label-button pairs
    }
    
    private final ContentType contentType;
    private final String title;
    private UIGridBuilder grid;
    private PopulationListComponent customListComponent;
    
    // Animation state for LABEL_VALUE_GRID
    private float alpha = 0.0f;
    private long startTime;
    private boolean animationStarted = false;
    
    // Data suppliers for different content types
    private Supplier<Map<String, String>> labelValueSupplier;
    private Supplier<Map<Item, Integer>> itemListSupplier;
    private Supplier<Object[]> customDataSupplier;
    private Supplier<Map<String, Object[]>> buttonGridSupplier;
    
    // Custom click handler for button grids (for special cases like right-click)
    private java.util.function.Function<Integer, Boolean> customClickHandler;
    
    /**
     * Creates a new standardized tab content component.
     */
    public StandardTabContent(int width, int height, ContentType contentType, String title) {
        super(width, height);
        this.contentType = contentType;
        this.title = title;
    }
    
    /**
     * Configure for label-value grid (Overview tab style)
     */
    public StandardTabContent withLabelValueData(Supplier<Map<String, String>> dataSupplier) {
        this.labelValueSupplier = dataSupplier;
        return this;
    }
    
    /**
     * Configure for item list (Resources tab style)
     */
    public StandardTabContent withItemListData(Supplier<Map<Item, Integer>> dataSupplier) {
        this.itemListSupplier = dataSupplier;
        return this;
    }
    
    /**
     * Configure for custom list (Population tab style)
     */
    public StandardTabContent withCustomData(Supplier<Object[]> dataSupplier) {
        this.customDataSupplier = dataSupplier;
        return this;
    }
    
    /**
     * Configure for button grid (Settings tab style)
     */
    public StandardTabContent withButtonGridData(Supplier<Map<String, Object[]>> dataSupplier) {
        this.buttonGridSupplier = dataSupplier;
        return this;
    }
    
    /**
     * Configure custom click handler for button grids (for special cases like right-click)
     */
    public StandardTabContent withCustomClickHandler(java.util.function.Function<Integer, Boolean> clickHandler) {
        this.customClickHandler = clickHandler;
        return this;
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        switch (contentType) {
            case LABEL_VALUE_GRID:
                renderLabelValueGrid(guiGraphics, mouseX, mouseY);
                break;
            case ITEM_LIST:
                renderItemList(guiGraphics, mouseX, mouseY);
                break;
            case CUSTOM_LIST:
                renderCustomList(guiGraphics, mouseX, mouseY);
                break;
            case BUTTON_GRID:
                renderButtonGrid(guiGraphics, mouseX, mouseY);
                break;
        }
    }
    
    /**
     * Render animated label-value grid (Overview tab style)
     */
    private void renderLabelValueGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Start animation if not started
        if (!animationStarted) {
            startTime = System.currentTimeMillis();
            animationStarted = true;
        }
        
        // Calculate alpha based on time (fade in over 500ms, starting after title appears)
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > 300) { // Start after title animation
            alpha = Math.min(1.0f, (elapsed - 300) / 500.0f);
        }
        
        // Only render if we have some visibility
        if (alpha > 0.01f && labelValueSupplier != null) {
            // Save current pose
            guiGraphics.pose().pushPose();
            
            // Apply alpha transformation
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
            
            // Create/update the grid
            Map<String, String> data = labelValueSupplier.get();
            grid = UIGridBuilder.createLabelValueGrid(
                x, y, width, height,
                TEXT_COLOR, TEXT_HIGHLIGHT,
                data)
                .withBackgroundColor(BACKGROUND_COLOR)
                .withBorderColor(BORDER_COLOR)
                .withMargins(15, 10)
                .withSpacing(15, 10)
                .drawBorder(true);
            
            // Render the grid with alpha applied
            grid.render(guiGraphics, mouseX, mouseY);
            
            // Restore original pose and color
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            guiGraphics.pose().popPose();
        }
    }
    
    /**
     * Render scrollable item list (Resources tab style)
     */
    private void renderItemList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (itemListSupplier != null) {
            Map<Item, Integer> items = itemListSupplier.get();
            
            // Create the grid only once to preserve scroll state
            if (grid == null) {
                LOGGER.debug("Creating new UIGridBuilder for item list with {} items", items.size());
                grid = UIGridBuilder.create(x, y, width, height, 2)
                    .withBackgroundColor(BACKGROUND_COLOR)
                    .withBorderColor(BORDER_COLOR)
                    .withMargins(15, 10)
                    .withSpacing(15, 10)
                    .withRowHeight(14)
                    .drawBorder(true);
                
                // This method automatically enables scrolling if needed
                grid.withItemQuantityPairs(items, TEXT_HIGHLIGHT);
                LOGGER.debug("Grid created with scrolling enabled: {}", items.size() > 4);
            }
            
            // Render the grid (preserves scroll state)
            grid.render(guiGraphics, mouseX, mouseY);
        }
    }
    
    /**
     * Render custom list (Population tab style) - preserves all original functionality
     */
    private void renderCustomList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (customDataSupplier != null) {
            Object[] data = customDataSupplier.get();
            
            // Extract data arrays from supplier
            String[] names = (String[]) data[0];
            String[] jobs = (String[]) data[1];
            int[] levels = (int[]) data[2];
            
            // Create the custom list component only once to preserve scroll state
            if (customListComponent == null) {
                LOGGER.debug("Creating new custom list component for population with {} citizens", names.length);
                customListComponent = new PopulationListComponent(x, y, width, height, names, jobs, levels);
            }
            
            // Render the custom list component (x, y are already set in constructor)
            customListComponent.render(guiGraphics, customListComponent.getX(), customListComponent.getY(), mouseX, mouseY);
        }
    }
    
    /**
     * Render button grid (Settings tab style)
     */
    private void renderButtonGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (buttonGridSupplier != null) {
            Map<String, Object[]> buttonData = buttonGridSupplier.get();
            
            // Create/update the grid every time to ensure dynamic button text updates
            grid = UIGridBuilder.createLabelButtonGrid(
                x, y, width, height,
                TEXT_COLOR, TownInterfaceTheme.PRIMARY_COLOR,
                buttonData)
                .withBackgroundColor(BACKGROUND_COLOR)
                .withBorderColor(BORDER_COLOR)
                .withMargins(15, 10)
                .withSpacing(15, 10)
                .drawBorder(true);
            
            // Render the grid
            grid.render(guiGraphics, mouseX, mouseY);
        }
    }
    

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (contentType == ContentType.CUSTOM_LIST && customListComponent != null) {
            return customListComponent.mouseClicked(mouseX, mouseY, button);
        }
        
        // For button grids, check if we have a custom click handler first
        if (contentType == ContentType.BUTTON_GRID && customClickHandler != null) {
            if (customClickHandler.apply(button)) {
                return true; // Custom handler handled the click
            }
        }
        
        if (grid != null) {
            return grid.mouseClicked((int)mouseX, (int)mouseY, button);
        }
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (contentType == ContentType.CUSTOM_LIST && customListComponent != null) {
            return customListComponent.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (contentType == ContentType.CUSTOM_LIST && customListComponent != null) {
            return customListComponent.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (contentType == ContentType.CUSTOM_LIST && customListComponent != null) {
            return customListComponent.mouseScrolled(mouseX, mouseY, delta);
        }
        if (grid != null) {
            return grid.mouseScrolled(mouseX, mouseY, delta);
        }
        return false;
    }
    
    /**
     * Force refresh of the content (for external updates)
     */
    public void refresh() {
        // Reset animation for label-value grids
        if (contentType == ContentType.LABEL_VALUE_GRID) {
            animationStarted = false;
            alpha = 0.0f;
        }
        
        // Force grid recreation
        grid = null;
        
        // Force custom list recreation
        customListComponent = null;
    }
} 