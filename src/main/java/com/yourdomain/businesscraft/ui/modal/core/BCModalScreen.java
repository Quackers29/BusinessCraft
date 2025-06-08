package com.yourdomain.businesscraft.ui.modal.core;

import com.yourdomain.businesscraft.ui.builders.UIGridBuilder;
import com.yourdomain.businesscraft.ui.builders.BCComponentFactory;
import com.yourdomain.businesscraft.ui.components.basic.BCLabel;

import com.yourdomain.businesscraft.ui.components.basic.BCPanel;
import com.yourdomain.businesscraft.ui.components.basic.BCButton;
import com.yourdomain.businesscraft.ui.builders.UIGridBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.yourdomain.businesscraft.ui.components.basic.UIComponent;

/**
 * A full-page modal window component for BusinessCraft.
 * Unlike popups, this component can host complex layouts like grids and is designed
 * for more detailed interactions requiring more screen real estate.
 * 
 * Features:
 * - Responsive sizing based on content
 * - Auto-centering on screen
 * - Scrollable content with grid layout
 * - Standard OK/Back buttons
 * - Customizable column layout
 */
public class BCModalScreen extends BCPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(BCModalScreen.class);
    
    // Constants for layout
    private static final int HEADER_HEIGHT = 40;
    private static final int FOOTER_HEIGHT = 50;
    private static final int CONTENT_PADDING = 20;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_SPACING = 20;
    private static final int ITEM_HEIGHT = 24;
    private static final int MIN_ROWS = 5;
    private static final int MAX_ROWS = 10;
    
    private final String title;
    private final Consumer<Boolean> resultCallback; // Boolean: true for OK, false for Back/Cancel
    private List<String> exampleData; // Example data for demo purposes
    
    private BCButton backButton;
    private BCButton okButton;
    private UIGridBuilder contentGrid;
    private int scrollOffset = 0;
    private int maxVisibleItems = MAX_ROWS;
    private boolean isScrolling = false;
    private int columnCount = 2; // Default column count
    private float[] columnWidths = null; // Optional column width distribution
    
    /**
     * Create a new modal window with default 2 columns
     * 
     * @param title The title of the window
     * @param resultCallback Callback that receives the result when closed (true=OK, false=Back)
     */
    public BCModalScreen(String title, Consumer<Boolean> resultCallback) {
        this(title, resultCallback, 2);
    }
    
    /**
     * Create a new modal window with specified column count
     * 
     * @param title The title of the window
     * @param resultCallback Callback that receives the result when closed (true=OK, false=Back)
     * @param columnCount Number of columns to display
     */
    public BCModalScreen(String title, Consumer<Boolean> resultCallback, int columnCount) {
        this(title, resultCallback, columnCount, null);
    }
    
    /**
     * Create a new modal window with specified column count and width distribution
     * 
     * @param title The title of the window
     * @param resultCallback Callback that receives the result when closed (true=OK, false=Back)
     * @param columnCount Number of columns to display
     * @param columnWidths Relative widths for each column (null for equal widths)
     */
    public BCModalScreen(String title, Consumer<Boolean> resultCallback, int columnCount, float[] columnWidths) {
        // Create a panel with appropriate size for content
        super(
            calculateAppropriateWidth(columnCount),
            calculateAppropriateHeight(0) // Initial height calculation, will be updated when data is set
        );
        
        this.title = title != null ? title : "Modal";
        this.resultCallback = resultCallback;
        this.exampleData = new ArrayList<>();
        this.columnCount = Math.max(1, columnCount); // Ensure at least 1 column
        this.columnWidths = columnWidths;
        
        // Style the modal
        this.withBackgroundColor(0xFF222222) // Fully opaque background
            .withBorderColor(0xFFAAAAAA)     // Fully opaque border
            .withCornerRadius(0);            // No rounded corners for full page modal
        
        // Set up example data (will be updated later)
        initExampleData();
        
        // Center the modal on the screen - this will be called again when data is set
        centerOnScreen();
        
        // Initialize content
        createContent();
    }
    
    /**
     * Calculate an appropriate width based on the number of columns
     * 
     * @param columns Number of columns in the content
     * @return Appropriate width for the modal
     */
    private static int calculateAppropriateWidth(int columns) {
        // Base width plus column-dependent width
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        
        // Ensure columns is at least 1
        columns = Math.max(1, columns);
        
        // For a two-column layout with item numbers and names, we don't need much width
        if (columns == 2) {
            // For 2 columns, use a more compact width - about 36% of screen width (reduced by 20% from previous 45%)
            return Math.min(400, (int)(screenWidth * 0.36f));
        } else {
            // For other column counts, scale appropriately, also reduced by 20%
            return Math.min(480, (int)(screenWidth * 0.4f + (columns - 2) * 64));
        }
    }
    
    /**
     * Calculate an appropriate height based on the number of rows
     * 
     * @param rowCount Number of rows in the content (0 to use default)
     * @return Appropriate height for the modal
     */
    private static int calculateAppropriateHeight(int rowCount) {
        // Base height calculation
        Minecraft minecraft = Minecraft.getInstance();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        // Fixed components heights
        int padding = CONTENT_PADDING;        // Padding (top and bottom)
        
        // Calculate content height based on rows
        int contentRows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, rowCount == 0 ? MAX_ROWS : rowCount));
        int contentHeight = contentRows * ITEM_HEIGHT;
        
        // Calculate total height
        int totalHeight = HEADER_HEIGHT + contentHeight + FOOTER_HEIGHT + padding * 2;
        
        // Limit to screen height if needed
        return Math.min(totalHeight, (int)(screenHeight * 0.8f));
    }
    
    /**
     * Center the modal on the screen
     */
    private void centerOnScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int x = (screenWidth - this.width) / 2;
        int y = (screenHeight - this.height) / 2;
        
        // Set position with child updates
        this.positionWithChildren(x, y);
        
        // Ensure buttons are properly positioned
        ensureButtonsPositioned();
    }
    
    /**
     * Ensure buttons are properly positioned even if children update fails
     */
    private void ensureButtonsPositioned() {
        if (backButton != null && okButton != null) {
            int buttonsY = this.y + this.height - FOOTER_HEIGHT + 10;
            
            // Position Back button
            backButton.position(
                this.x + (this.width / 2) - BUTTON_WIDTH - (BUTTON_SPACING / 2),
                buttonsY
            );
            
            // Position OK button
            okButton.position(
                this.x + (this.width / 2) + (BUTTON_SPACING / 2),
                buttonsY
            );
        }
    }
    
    /**
     * Populate with example data
     */
    private void initExampleData() {
        // Populate with some example data - only 5 items now
        for (int i = 1; i <= 5; i++) {
            exampleData.add("Item " + i + " - Example Name");
        }
    }
    
    /**
     * Set custom data for the grid and adjust size
     * 
     * @param data List of string data to display in the grid
     */
    public void setData(List<String> data) {
        if (data == null) {
            this.exampleData = new ArrayList<>();
        } else {
            this.exampleData = new ArrayList<>(data); // Create a copy to avoid external modifications
        }
        
        // Update height based on row count
        int newHeight = calculateAppropriateHeight(this.exampleData.size());
        
        // Only resize if height changed
        if (this.height != newHeight) {
            // Update height with event notification
            this.sizeWithUpdates(this.width, newHeight);
            
            // Re-center on screen with new size
            centerOnScreen();
        }
        
        // Reset scroll position when data changes
        scrollOffset = 0;
        
        // Update content grid
        updateContentGrid();
        
        // Trigger data changed event
        triggerEvent("dataChanged");
    }
    
    /**
     * Reposition buttons after modal size or position changes
     */
    private void repositionButtons() {
        if (backButton != null && okButton != null) {
            int buttonsY = this.y + this.height - FOOTER_HEIGHT + 10;
            
            // Reposition Back button
            backButton.position(
                this.x + (this.width / 2) - BUTTON_WIDTH - (BUTTON_SPACING / 2),
                buttonsY
            );
            
            // Reposition OK button
            okButton.position(
                this.x + (this.width / 2) + (BUTTON_SPACING / 2),
                buttonsY
            );
        }
    }
    
    /**
     * Create the content of the modal window
     */
    private void createContent() {
        // Calculate content area dimensions
        int contentWidth = this.width - (CONTENT_PADDING * 2);
        int contentHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT - (CONTENT_PADDING * 2);
        
        // Create and position the content grid
        contentGrid = new UIGridBuilder(
            this.x + CONTENT_PADDING,
            this.y + HEADER_HEIGHT + CONTENT_PADDING,
            contentWidth,
            contentHeight,
            1, // 1 row initially, will add more
            1  // 1 column
        )
        .withBackgroundColor(0x40000000)
        .withBorderColor(0x80AAAAAA)
        .withMargins(5, 5)
        .withSpacing(5, 5)
        .drawBorder(true);
        
        // Update the grid with data
        updateContentGrid();
        
        // Add navigation buttons
        int buttonsY = this.y + this.height - FOOTER_HEIGHT + 10;
        
        // Back button
        backButton = BCComponentFactory.createSecondaryButton(
            "Back", 
            button -> {
                if (resultCallback != null) {
                    try {
                        resultCallback.accept(false);
                    } catch (Exception e) {
                        LOGGER.error("Error in Back button callback: {}", e.getMessage());
                    }
                }
            }, 
            BUTTON_WIDTH
        );
        backButton.position(
            this.x + (this.width / 2) - BUTTON_WIDTH - (BUTTON_SPACING / 2),
            buttonsY
        );
        this.addChild(backButton);
        
        // OK button
        okButton = BCComponentFactory.createPrimaryButton(
            "OK", 
            button -> {
                if (resultCallback != null) {
                    try {
                        resultCallback.accept(true);
                    } catch (Exception e) {
                        LOGGER.error("Error in OK button callback: {}", e.getMessage());
                    }
                }
            }, 
            BUTTON_WIDTH
        );
        okButton.position(
            this.x + (this.width / 2) + (BUTTON_SPACING / 2),
            buttonsY
        );
        this.addChild(okButton);
    }
    
    /**
     * Update the content grid with current data
     */
    private void updateContentGrid() {
        if (contentGrid == null || exampleData.isEmpty()) {
            return;
        }
        
        // Calculate grid position and dimensions
        int contentWidth = this.width - (CONTENT_PADDING * 2);
        int contentHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT - (CONTENT_PADDING * 2);
        
        // Create a grid builder with the specified column count
        UIGridBuilder builder = new UIGridBuilder(
            this.x + CONTENT_PADDING,
            this.y + HEADER_HEIGHT + CONTENT_PADDING,
            contentWidth,
            contentHeight,
            Math.min(maxVisibleItems, exampleData.size() - scrollOffset), // Set rows based on visible items
            columnCount
        )
        .withBackgroundColor(0x80000000)
        .withBorderColor(0x80AAAAAA)
        .withMargins(5, 5)
        .withSpacing(5, 5)
        .drawBorder(true);
        
        // Set column widths if specified - adjust cell sizes manually
        // Note: UIGridBuilder doesn't have withColumns method, so we'll adjust indirectly
        if (columnWidths != null && columnWidths.length == columnCount) {
            // Can't set column widths directly as there's no withColumns method
            // Leave it with default uniform column distribution
        } 
        
        // Recreate the grid with current data
        contentGrid = builder;
        
        // Add visible items based on scroll position
        int endIndex = Math.min(scrollOffset + maxVisibleItems, exampleData.size());
        for (int i = scrollOffset; i < endIndex; i++) {
            int rowIndex = i - scrollOffset;
            
            try {
                String item = exampleData.get(i);
                
                if (columnCount == 2) {
                    // Default two-column display (index and name)
                    contentGrid.addLabel(rowIndex, 0, (i + 1) + ".", 0xFFAAAAAA);
                    contentGrid.addLabel(rowIndex, 1, item, 0xFFFFFFFF);
                } else {
                    // For other column counts, distribute the item across columns
                    // This is a simple example - you might want to parse the data differently
                    String[] parts = item.split(" - ");
                    contentGrid.addLabel(rowIndex, 0, (i + 1) + ".", 0xFFAAAAAA);
                    
                    for (int col = 0; col < Math.min(parts.length, columnCount - 1); col++) {
                        contentGrid.addLabel(rowIndex, col + 1, parts[col], 0xFFFFFFFF);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error adding item to grid at index {}: {}", i, e.getMessage());
                contentGrid.addLabel(rowIndex, 0, "Error displaying item " + (i + 1), 0xFFFF0000);
            }
        }
    }
    
    /**
     * Handle scrolling functionality
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Check if mouse is over the grid area
        int contentX = this.x + CONTENT_PADDING;
        int contentY = this.y + HEADER_HEIGHT + CONTENT_PADDING;
        int contentWidth = this.width - (CONTENT_PADDING * 2);
        int contentHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT - (CONTENT_PADDING * 2);
        
        if (mouseX >= contentX && mouseX < contentX + contentWidth &&
            mouseY >= contentY && mouseY < contentY + contentHeight) {
            
            // Scroll up or down based on delta
            if (delta > 0 && scrollOffset > 0) {
                scrollOffset--;
                updateContentGrid();
                return true;
            } else if (delta < 0 && scrollOffset + maxVisibleItems < exampleData.size()) {
                scrollOffset++;
                updateContentGrid();
                return true;
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        // Draw semi-transparent overlay for the entire screen
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        
        // First, push the matrix to ensure proper Z-ordering
        guiGraphics.pose().pushPose();
        
        // Move forward in Z-buffer to ensure it renders on top
        guiGraphics.pose().translate(0, 0, 1000);
        
        // Fill the entire screen with a dark overlay
        guiGraphics.fill(0, 0, screenWidth, screenHeight, 0xD0000000);
        
        // Draw window background with FULLY OPAQUE background
        // Don't use UIDirectRenderer here to ensure full control
        // Main background - fully opaque
        guiGraphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0xFF222222);
        
        // Draw border
        int borderColor = 0xFFAAAAAA; // Fully opaque
        guiGraphics.hLine(this.x, this.x + this.width - 1, this.y, borderColor);
        guiGraphics.hLine(this.x, this.x + this.width - 1, this.y + this.height - 1, borderColor);
        guiGraphics.vLine(this.x, this.y, this.y + this.height - 1, borderColor);
        guiGraphics.vLine(this.x + this.width - 1, this.y, this.y + this.height - 1, borderColor);
        
        // Draw header
        guiGraphics.fillGradient(
            this.x + 1, 
            this.y + 1, 
            this.x + this.width - 1, 
            this.y + HEADER_HEIGHT, 
            0xFF335599, 
            0xFF223366
        );
        
        // Draw title with explicit positioning
        guiGraphics.drawCenteredString(
            Minecraft.getInstance().font,
            Component.literal(title),
            this.x + this.width / 2,
            this.y + 20,
            0xFFFFFFFF
        );
        
        // Render content grid
        if (contentGrid != null) {
            contentGrid.render(guiGraphics, mouseX, mouseY);
            
            // Draw scroll indicators if needed
            if (scrollOffset > 0) {
                // Up arrow
                drawScrollArrow(guiGraphics, this.x + this.width - 25, this.y + 70, true);
            }
            
            if (scrollOffset + maxVisibleItems < exampleData.size()) {
                // Down arrow
                drawScrollArrow(guiGraphics, this.x + this.width - 25, this.y + this.height - 90, false);
            }
        }
        
        // Render the back and OK buttons from the panel
        for (UIComponent child : children) {
            if (child.isVisible()) {
                // Use absolute positioning
                child.render(guiGraphics, child.getX(), child.getY(), mouseX, mouseY);
            }
        }
        
        // Pop the pose after we're done rendering everything
        guiGraphics.pose().popPose();
    }
    
    /**
     * Draw a scroll arrow indicator
     */
    private void drawScrollArrow(GuiGraphics guiGraphics, int x, int y, boolean isUp) {
        int color = 0xFFAAAAAA;
        int size = 10;
        
        if (isUp) {
            // Draw up arrow
            for (int i = 0; i < size; i++) {
                int lineWidth = size - i;
                int lineX = x - lineWidth / 2;
                guiGraphics.hLine(lineX, lineX + lineWidth, y + i, color);
            }
        } else {
            // Draw down arrow
            for (int i = 0; i < size; i++) {
                int lineWidth = i + 1;
                int lineX = x - lineWidth / 2;
                guiGraphics.hLine(lineX, lineX + lineWidth, y + i, color);
            }
        }
    }
} 