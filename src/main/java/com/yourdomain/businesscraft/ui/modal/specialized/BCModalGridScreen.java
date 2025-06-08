package com.yourdomain.businesscraft.ui.modal.specialized;

import com.yourdomain.businesscraft.ui.builders.UIGridBuilder;
import com.yourdomain.businesscraft.ui.builders.BCComponentFactory;
import com.yourdomain.businesscraft.ui.components.basic.BCLabel;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A reusable modal grid screen component that can display various types of data
 * in a scrollable grid with customizable appearance.
 * 
 * @param <T> The type of data to be displayed in the grid
 */
public class BCModalGridScreen<T> extends Screen {
    // Screen properties
    private final Screen parentScreen;
    private final Consumer<BCModalGridScreen<T>> onCloseCallback;
    
    // Data and display properties
    private List<T> dataItems = new ArrayList<>();
    private List<ColumnDefinition<T>> columns = new ArrayList<>();
    private String backButtonText = "Back";
    
    // Panel dimensions and position
    private int panelWidth;
    private int panelHeight;
    private int panelLeft;
    private int panelTop;
    
    // Colors (with defaults)
    private int backgroundColor = 0xFF000000;    // Black
    private int borderColor = 0xFFDDDDDD;        // Light gray
    private int titleColor = 0xFFFFFFFF;         // White
    private int headerColor = 0xFFDDFFFF;        // Light cyan
    private int textColor = 0xFFFFFFFF;          // White
    private int altRowColor1 = 0x30FFFFFF;       // Alternating row color 1
    private int altRowColor2 = 0x20FFFFFF;       // Alternating row color 2
    private int scrollbarTrackColor = 0x40FFFFFF; // Light gray semi-transparent
    private int scrollbarThumbColor = 0xA0CCDDFF; // Light blue semi-transparent
    private int scrollbarActiveColor = 0xFFCCDDFF; // Light blue opaque
    
    // Scrolling properties
    private int scrollOffset = 0;
    private final int scrollbarWidth = 8;
    private boolean isDraggingScrollbar = false;
    
    // Content dimensions
    private int rowHeight = 20;
    private int headerHeight = 45; // Space for title and header
    private int titleHeight = 30;  // Height of the title text area
    private int visibleRows;
    private int totalRows;
    
    // UI components
    private Button closeButton;
    private UIGridBuilder contentGrid;
    
    // Customizable row click handler
    private Consumer<T> onRowClickHandler;
    
    // Title scale factor
    private float titleScale = 1.5f;
    
    /**
     * Constructor for the modal grid screen
     * 
     * @param title The title to display at the top of the screen
     * @param parentScreen The parent screen to return to when closing
     * @param onCloseCallback Optional callback to execute when closing (can be null)
     */
    public BCModalGridScreen(Component title, Screen parentScreen, Consumer<BCModalGridScreen<T>> onCloseCallback) {
        super(title);
        this.parentScreen = parentScreen;
        this.onCloseCallback = onCloseCallback;
    }
    
    /**
     * Initialize the screen layout and components
     */
    @Override
    protected void init() {
        super.init();
        
        // Calculate panel dimensions - default to 70% of screen height
        this.panelWidth = Math.min(400, this.width - 40);
        this.panelHeight = (int)(this.height * 0.7) - 40;
        this.panelLeft = (this.width - panelWidth) / 2;
        this.panelTop = (this.height - panelHeight) / 2 - 10;
        
        // Calculate button position
        int buttonX = this.width / 2 - 50;
        int buttonY = panelTop + panelHeight + 5;
        
        // Create close button
        this.closeButton = this.addRenderableWidget(Button.builder(
            Component.literal(backButtonText), 
            button -> this.onClose())
            .pos(buttonX, buttonY)
            .size(100, 20)
            .build()
        );
        
        // Create the content grid
        createContentGrid();
    }
    
    /**
     * Create the content grid with current data
     */
    private void createContentGrid() {
        // Calculate grid dimensions
        int gridContentWidth = panelWidth - 20; // Leave padding on sides
        int gridContentHeight = panelHeight - headerHeight - 10; // Leave space for header and padding
        int gridLeft = panelLeft + 10;
        int gridTop = panelTop + headerHeight + 5;
        
        // Calculate visible rows
        visibleRows = (gridContentHeight / rowHeight);
        
        // Calculate total rows (header + data)
        totalRows = dataItems.isEmpty() ? 2 : dataItems.size() + 1; // +1 for header
        
        // Create grid with column count
        contentGrid = UIGridBuilder.create(gridLeft, gridTop, gridContentWidth, gridContentHeight, columns.size())
            .withBackgroundColor(0x00000000) // Transparent background
            .withBorderColor(0x00000000) // No border
            .withMargins(10, 5)
            .withSpacing(10, 0)
            .withRowHeight(rowHeight)
            .drawBorder(false);
        
        // Add header row
        for (int i = 0; i < columns.size(); i++) {
            contentGrid.addLabel(0, i, columns.get(i).title, headerColor);
        }
        
        // Add data rows
        if (dataItems.isEmpty()) {
            // Show empty message in the middle column or span across all columns
            int messageColumn = Math.max(0, columns.size() / 2);
            contentGrid.addLabel(1, messageColumn, "No items to display", textColor);
        } else {
            // Add data rows, considering scroll offset
            int displayedRows = Math.min(visibleRows - 1, dataItems.size()); // -1 for header
            int startIndex = Math.min(scrollOffset, Math.max(0, dataItems.size() - displayedRows));
            
            for (int i = 0; i < displayedRows; i++) {
                int dataIndex = startIndex + i;
                if (dataIndex >= dataItems.size()) break;
                
                T item = dataItems.get(dataIndex);
                int row = i + 1; // Skip header row
                
                // Set alternating row background color
                int rowColor = (dataIndex % 2 == 0) ? altRowColor1 : altRowColor2;
                
                // Add data for each column
                for (int col = 0; col < columns.size(); col++) {
                    String cellContent = columns.get(col).valueExtractor.apply(item);
                    contentGrid.addLabel(row, col, cellContent, textColor);
                }
            }
        }
    }
    
    /**
     * Render the screen
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Render darkened background
        if (parentScreen != null) {
            this.renderBackground(guiGraphics);
            // Additional dark overlay for better contrast
            guiGraphics.fill(0, 0, this.width, this.height, 0xB0000000);
        }
        
        // Draw panel background
        guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, backgroundColor);
        
        // Draw panel border
        guiGraphics.hLine(panelLeft, panelLeft + panelWidth - 1, panelTop, borderColor);
        guiGraphics.hLine(panelLeft, panelLeft + panelWidth - 1, panelTop + panelHeight - 1, borderColor);
        guiGraphics.vLine(panelLeft, panelTop, panelTop + panelHeight - 1, borderColor);
        guiGraphics.vLine(panelLeft + panelWidth - 1, panelTop, panelTop + panelHeight - 1, borderColor);
        
        // Draw title with scale
        guiGraphics.pose().pushPose();
        
        // Position title in center of panel
        float titleX = panelLeft + panelWidth / 2;
        float titleY = panelTop + 18;
        
        // Apply transformations for scaling centered on text position
        guiGraphics.pose().translate(titleX, titleY, 0);
        guiGraphics.pose().scale(titleScale, titleScale, 1.0f);
        guiGraphics.pose().translate(-titleX, -titleY, 0);
        
        // Draw title with shadow
        guiGraphics.drawCenteredString(
            this.font,
            this.title,
            (int)titleX,
            (int)titleY,
            titleColor
        );
        
        // Restore transformation
        guiGraphics.pose().popPose();
        
        // Draw header separator
        guiGraphics.hLine(
            panelLeft + 10, 
            panelLeft + panelWidth - 10, 
            panelTop + headerHeight - 5,
            borderColor
        );
        
        // Draw scrollbar if needed
        int gridContentHeight = panelHeight - headerHeight - 10;
        if (totalRows > visibleRows) {
            // Scrollbar position
            int scrollbarX = panelLeft + panelWidth - scrollbarWidth - 5;
            int scrollbarY = panelTop + headerHeight + 5;
            int scrollbarHeight = gridContentHeight;
            
            // Draw scrollbar track
            guiGraphics.fill(
                scrollbarX,
                scrollbarY,
                scrollbarX + scrollbarWidth,
                scrollbarY + scrollbarHeight,
                scrollbarTrackColor
            );
            
            // Draw scrollbar thumb
            int maxScrollOffset = Math.max(0, totalRows - visibleRows);
            float thumbRatio = (float)visibleRows / totalRows;
            int thumbHeight = Math.max(20, (int)(scrollbarHeight * thumbRatio));
            int thumbY = scrollbarY + (int)((scrollbarHeight - thumbHeight) * ((float)scrollOffset / maxScrollOffset));
            
            // Highlight if mouse is over
            boolean isOverScrollbar = mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                                    mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight;
            
            guiGraphics.fill(
                scrollbarX,
                thumbY,
                scrollbarX + scrollbarWidth,
                thumbY + thumbHeight,
                isOverScrollbar ? scrollbarActiveColor : scrollbarThumbColor
            );
        }
        
        // Render the content grid
        if (contentGrid != null) {
            contentGrid.render(guiGraphics, mouseX, mouseY);
        }
        
        // Render buttons and other widgets
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
    
    /**
     * Handle mouse clicks
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle scrollbar clicks
        if (button == 0 && totalRows > visibleRows) {
            int scrollbarX = panelLeft + panelWidth - scrollbarWidth - 5;
            int scrollbarY = panelTop + headerHeight + 5;
            int scrollbarHeight = panelHeight - headerHeight - 10;
            
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth &&
                mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
                
                isDraggingScrollbar = true;
                
                // Calculate new scroll position
                float relativeY = (float)(mouseY - scrollbarY) / scrollbarHeight;
                int maxScrollOffset = Math.max(0, totalRows - visibleRows);
                scrollOffset = (int)(relativeY * maxScrollOffset);
                
                // Clamp scroll offset
                scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
                
                // Refresh the grid
                createContentGrid();
                
                return true;
            }
        }
        
        // Handle grid clicks to identify row selection
        if (button == 0 && onRowClickHandler != null && !dataItems.isEmpty()) {
            int gridTop = panelTop + headerHeight + 5;
            int gridLeft = panelLeft + 10;
            int gridWidth = panelWidth - 20;
            int gridHeight = panelHeight - headerHeight - 10;
            
            // Check if click is within grid bounds
            if (mouseX >= gridLeft && mouseX <= gridLeft + gridWidth &&
                mouseY >= gridTop && mouseY <= gridTop + gridHeight) {
                
                // Calculate which row was clicked (subtract header row)
                int clickedRow = (int)((mouseY - gridTop) / rowHeight);
                
                // Adjust for scrolling and header
                int dataIndex = clickedRow + scrollOffset - 1; // -1 for header
                
                // Verify valid data index
                if (dataIndex >= 0 && dataIndex < dataItems.size()) {
                    // Trigger row click handler
                    onRowClickHandler.accept(dataItems.get(dataIndex));
                    return true;
                }
            }
        }
        
        // Let the grid handle other clicks
        if (contentGrid != null && contentGrid.mouseClicked((int)mouseX, (int)mouseY, button)) {
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Handle mouse dragging for scrollbar
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Handle scrollbar dragging
        if (isDraggingScrollbar && button == 0) {
            int scrollbarY = panelTop + headerHeight + 5;
            int scrollbarHeight = panelHeight - headerHeight - 10;
            
            // Calculate new scroll position
            float relativeY = (float)(mouseY - scrollbarY) / scrollbarHeight;
            int maxScrollOffset = Math.max(0, totalRows - visibleRows);
            scrollOffset = (int)(relativeY * maxScrollOffset);
            
            // Clamp scroll offset
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
            
            // Refresh the grid
            createContentGrid();
            
            return true;
        }
        
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    /**
     * Handle mouse release
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDraggingScrollbar && button == 0) {
            isDraggingScrollbar = false;
            return true;
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    /**
     * Handle mouse scrolling
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Handle mouse wheel scrolling within panel bounds
        if (mouseX >= panelLeft && mouseX <= panelLeft + panelWidth &&
            mouseY >= panelTop && mouseY <= panelTop + panelHeight) {
            
            // Calculate max scroll offset
            int maxScrollOffset = Math.max(0, totalRows - visibleRows);
            
            // Apply scrolling (delta > 0 means scroll up)
            scrollOffset -= (int)Math.signum(delta);
            
            // Clamp scroll position
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
            
            // Refresh the grid
            createContentGrid();
            
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    /**
     * Handle screen close
     */
    @Override
    public void onClose() {
        // Return to parent screen
        this.minecraft.setScreen(parentScreen);
        
        // Execute close callback if provided
        if (onCloseCallback != null) {
            onCloseCallback.accept(this);
        }
    }
    
    /**
     * Prevent game from pausing when screen is open
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    //
    // Builder methods for configuration
    //
    
    /**
     * Set the data to display in the grid
     */
    public BCModalGridScreen<T> withData(List<T> data) {
        this.dataItems = new ArrayList<>(data);
        return this;
    }
    
    /**
     * Add a column to the grid
     * 
     * @param title The column header text
     * @param valueExtractor Function to extract the value from data items
     */
    public BCModalGridScreen<T> addColumn(String title, Function<T, String> valueExtractor) {
        this.columns.add(new ColumnDefinition<>(title, valueExtractor));
        return this;
    }
    
    /**
     * Set back button text
     */
    public BCModalGridScreen<T> withBackButtonText(String text) {
        this.backButtonText = text;
        return this;
    }
    
    /**
     * Set custom panel size (as ratio of screen size)
     */
    public BCModalGridScreen<T> withPanelSize(float widthRatio, float heightRatio) {
        if (this.minecraft != null) {
            int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
            
            this.panelWidth = (int)(screenWidth * widthRatio);
            this.panelHeight = (int)(screenHeight * heightRatio);
        }
        return this;
    }
    
    /**
     * Set row height
     */
    public BCModalGridScreen<T> withRowHeight(int height) {
        this.rowHeight = height;
        return this;
    }
    
    /**
     * Set title scale
     */
    public BCModalGridScreen<T> withTitleScale(float scale) {
        this.titleScale = scale;
        return this;
    }
    
    /**
     * Set all colors at once
     */
    public BCModalGridScreen<T> withColors(int backgroundColor, int borderColor, 
                                         int titleColor, int headerColor, int textColor) {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.titleColor = titleColor;
        this.headerColor = headerColor;
        this.textColor = textColor;
        return this;
    }
    
    /**
     * Set alternating row colors
     */
    public BCModalGridScreen<T> withAlternatingRowColors(int color1, int color2) {
        this.altRowColor1 = color1;
        this.altRowColor2 = color2;
        return this;
    }
    
    /**
     * Set scrollbar colors
     */
    public BCModalGridScreen<T> withScrollbarColors(int trackColor, int thumbColor, int activeColor) {
        this.scrollbarTrackColor = trackColor;
        this.scrollbarThumbColor = thumbColor;
        this.scrollbarActiveColor = activeColor;
        return this;
    }
    
    /**
     * Set row click handler
     */
    public BCModalGridScreen<T> withRowClickHandler(Consumer<T> handler) {
        this.onRowClickHandler = handler;
        return this;
    }
    
    /**
     * Definition of a column in the grid
     */
    private static class ColumnDefinition<T> {
        final String title;
        final Function<T, String> valueExtractor;
        
        ColumnDefinition(String title, Function<T, String> valueExtractor) {
            this.title = title;
            this.valueExtractor = valueExtractor;
        }
    }
} 