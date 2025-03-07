package com.yourdomain.businesscraft.screen;

import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.screen.components.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.Optional;

/**
 * The Town Interface Screen showcases the BusinessCraft UI system capabilities.
 * This screen demonstrates various UI components and layouts using the enhanced BCTabPanel.
 */
public class TownInterfaceScreen extends AbstractContainerScreen<TownInterfaceMenu> {
    private BCTabPanel tabPanel;
    private BCTheme customTheme;

    // UI colors - lighter and more visible
    private static final int PRIMARY_COLOR = 0xA0335599;       // Semi-transparent blue
    private static final int SECONDARY_COLOR = 0xA0884466;     // Semi-transparent purple
    private static final int BACKGROUND_COLOR = 0x80222222;    // Semi-transparent dark gray
    private static final int BORDER_COLOR = 0xA0AAAAAA;        // Light gray
    private static final int ACTIVE_TAB_COLOR = 0xA0CCDDFF;    // Light blue for active tab
    private static final int INACTIVE_TAB_COLOR = 0x80555555;  // Medium gray for inactive tabs
    private static final int TEXT_COLOR = 0xFFFFFFFF;          // White text
    private static final int TEXT_HIGHLIGHT = 0xFFDDFFFF;      // Light cyan highlight text
    private static final int SUCCESS_COLOR = 0xA0339944;       // Green
    private static final int DANGER_COLOR = 0xA0993333;        // Red
    
    // Grid builder for bottom buttons
    private UIGridBuilder bottomButtonsGrid;
    
    // State tracking for toggle buttons
    private boolean pvpEnabled = false;
    private boolean publicTownEnabled = true;

    // Add a field for the active popup
    private BCPopupScreen activePopup = null;

    public TownInterfaceScreen(TownInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        
        // Set custom dimensions for the screen
        this.imageWidth = 256;
        this.imageHeight = 204;
        
        // Move the inventory label off-screen to hide it
        this.inventoryLabelY = 300;  // Position it below the visible area
        
        // Create a custom theme for this screen with lighter colors
        customTheme = BCTheme.builder()
            .primaryColor(PRIMARY_COLOR)
            .secondaryColor(SECONDARY_COLOR)
            .successColor(0xA0339944)
            .dangerColor(0xA0993333)
            .textLight(TEXT_COLOR)
            .textDark(0xFF202020)
            .panelBackground(BACKGROUND_COLOR)
            .panelBorder(BORDER_COLOR)
            .roundedCorners(true)
            .build();
    }

    @Override
    protected void init() {
        super.init();
        
        // Apply our custom theme for this screen
        BCTheme.setActiveTheme(customTheme);
        
        // Calculate screen dimensions and padding
        int screenPadding = 10;
        int screenContentWidth = this.imageWidth - (screenPadding * 2);
        int screenContentHeight = this.imageHeight - (screenPadding * 2);
        
        // Create tab panel with proper dimensions, positioned with padding
        this.tabPanel = new BCTabPanel(screenContentWidth, screenContentHeight, 20);
        this.tabPanel.position(this.leftPos + screenPadding, this.topPos + screenPadding);
        
        // Set tab styling with our lighter colors
        this.tabPanel.withTabStyle(ACTIVE_TAB_COLOR, INACTIVE_TAB_COLOR, TEXT_COLOR)
                     .withTabBorder(true, BORDER_COLOR)
                     .withContentStyle(BACKGROUND_COLOR, BORDER_COLOR);
        
        // Create and configure tabs with proper spacing
        createOverviewTab();
        createEconomyTab();
        createPopulationTab();
        createSettingsTab();
        
        // Make sure Overview is the default active tab
        if (this.tabPanel.getActiveTabId() == null) {
            this.tabPanel.setActiveTab("overview");
        }
        
        // Initialize the bottom buttons grid
        createBottomButtonsGrid();
        
        // Initialize the tab panel
        this.tabPanel.init(this::addRenderableWidget);
    }
    
    /**
     * Create the grid builder for bottom buttons
     */
    private void createBottomButtonsGrid() {
        // Calculate button panel dimensions
        int buttonPanelWidth = this.imageWidth - 40; // Leave 20px margin on each side
        int buttonPanelHeight = 40;
        int buttonPanelX = this.leftPos + (this.imageWidth - buttonPanelWidth) / 2;
        int buttonPanelY = this.topPos + this.imageHeight + 10;
        
        // Create a 1x2 grid (1 row, 2 columns)
        bottomButtonsGrid = new UIGridBuilder(buttonPanelX, buttonPanelY, buttonPanelWidth, buttonPanelHeight, 1, 2)
            .withBackgroundColor(BACKGROUND_COLOR)
            .withBorderColor(BORDER_COLOR)
            .withMargins(15, 10)
            .withSpacing(20, 0);
    }
    
    private void createOverviewTab() {
        // Create panel for content
        BCPanel panel = new BCPanel(this.tabPanel.getWidth(), this.tabPanel.getHeight() - 20);
        panel.withPadding(10)
             .withBackgroundColor(0x00000000) // Transparent background
             .withCornerRadius(3);
        
        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
        
        // Add title
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("TOWN OVERVIEW", panel.getInnerWidth());
        titleLabel.withTextColor(TEXT_HIGHLIGHT).withShadow(true);
        panel.addChild(titleLabel);
        
        // Calculate dimensions for the town info grid - ensure it fits within panel boundaries
        // Account for the panel's padding and the title height
        int titleHeight = 20; // Approximate height of the header
        int verticalSpacing = 10; // Space between title and grid
        
        // Calculate available space for the grid
        int availableWidth = panel.getInnerWidth();
        int availableHeight = panel.getInnerHeight() - titleHeight - verticalSpacing;
        
        // Add animated town info component with appropriate dimensions
        BCComponent gridHost = new BCComponent(availableWidth, availableHeight) {
            // Internal grid instance
            private UIGridBuilder grid;
            private float alpha = 0.0f;
            private long startTime;
            private boolean animationStarted = false;
            
            // Initialize the grid when rendering
            private UIGridBuilder createGrid() {
                // Create the grid with component-relative positioning (3 rows x 2 columns)
                UIGridBuilder newGrid = new UIGridBuilder(x, y, getWidth(), getHeight(), 3, 2)
                    .withBackgroundColor(BACKGROUND_COLOR)
                    .withBorderColor(BORDER_COLOR)
                    .withMargins(15, 10)
                    .withSpacing(15, 10)
                    .drawBorder(true);
                
                // Add town info data
                // First row: Town Name
                newGrid.addLabel(0, 0, "Town Name:", TEXT_COLOR);
                newGrid.addLabel(0, 1, getTownName(), TEXT_HIGHLIGHT);
                
                // Second row: Mayor
                newGrid.addLabel(1, 0, "Mayor:", TEXT_COLOR);
                newGrid.addLabel(1, 1, getMayorName(), TEXT_HIGHLIGHT);
                
                // Third row: Population
                newGrid.addLabel(2, 0, "Population:", TEXT_COLOR);
                newGrid.addLabel(2, 1, String.valueOf(getTownPopulation()), TEXT_HIGHLIGHT);
                
                return newGrid;
            }
            
            @Override
            protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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
                if (alpha > 0.01f) {
                    // Save current pose
                    guiGraphics.pose().pushPose();
                    
                    // Apply alpha transformation
                    int alphaInt = (int)(alpha * 255.0f);
                    guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
                    
                    // Create/update the grid
                    grid = createGrid();
                    
                    // Render the grid with alpha applied
                    grid.render(guiGraphics, mouseX, mouseY);
                    
                    // Restore original pose and color
                    guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    guiGraphics.pose().popPose();
                }
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                // Only handle clicks if animation is complete
                if (alpha >= 0.99f) {
                    // Make sure we have a grid to click on
                    if (grid == null) {
                        grid = createGrid();
                    }
                    
                    // Let the grid handle clicks
                    if (grid.mouseClicked((int)mouseX, (int)mouseY)) {
                        playButtonClickSound();
                        return true;
                    }
                }
                return false;
            }
        };
        
        // Add the grid host component to the panel
        panel.addChild(gridHost);
        
        // Add the panel to the tab
        this.tabPanel.addTab("overview", Component.literal("Overview"), panel);
    }
    
    private void createEconomyTab() {
        // Create panel for content
        BCPanel panel = new BCPanel(this.tabPanel.getWidth(), this.tabPanel.getHeight() - 20);
        panel.withPadding(10)
             .withBackgroundColor(0x00000000) // Transparent background
             .withCornerRadius(3);
        
        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
        
        // Add title
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("ECONOMY", panel.getInnerWidth());
        titleLabel.withTextColor(TEXT_HIGHLIGHT).withShadow(true);
        panel.addChild(titleLabel);
        
        // Sample resource data
        String[] resources = {"Wood", "Stone", "Iron", "Gold", "Diamond", "Food", "Coal", "Emerald"};
        int[] amounts = {128, 64, 32, 16, 8, 256, 48, 24};
        
        // Calculate dimensions for the resource grid - ensure it fits within panel boundaries
        // Account for the panel's padding and the title height
        int titleHeight = 20; // Approximate height of the header
        int verticalSpacing = 10; // Space between title and grid
        
        // Calculate available space for the grid
        int availableWidth = panel.getInnerWidth();
        int availableHeight = panel.getInnerHeight() - titleHeight - verticalSpacing;
        
        // Create a custom component to host the resource grid
        BCComponent gridHost = new BCComponent(availableWidth, availableHeight) {
            // Internal grid instance
            private UIGridBuilder grid;
            
            // Initialize the grid when rendering
            private UIGridBuilder createGrid() {
                // Create the grid with component-relative positioning (8 rows x 2 columns)
                UIGridBuilder newGrid = new UIGridBuilder(x, y, getWidth(), getHeight(), 8, 2)
                    .withBackgroundColor(BACKGROUND_COLOR)
                    .withBorderColor(BORDER_COLOR)
                    .withMargins(15, 5)
                    .withSpacing(15, 5)
                    .drawBorder(true);
                
                // Add resource data to the grid
                for (int i = 0; i < resources.length; i++) {
                    // Add resource name (first column)
                    newGrid.addLabel(i, 0, resources[i], TEXT_HIGHLIGHT);
                    
                    // Add resource amount (second column) - right-aligned effect
                    newGrid.addLabel(i, 1, String.valueOf(amounts[i]), TEXT_COLOR);
                }
                
                return newGrid;
            }
            
            @Override
            protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
                // Create/update the grid
                grid = createGrid();
                
                // Render the grid
                grid.render(guiGraphics, mouseX, mouseY);
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                // Make sure we have a grid to click on
                if (grid == null) {
                    grid = createGrid();
                }
                
                // Let the grid handle clicks
                if (grid.mouseClicked((int)mouseX, (int)mouseY)) {
                    playButtonClickSound();
                    return true;
                }
                return false;
            }
        };
        
        // Add the grid host component to the panel
        panel.addChild(gridHost);
        
        // Add the panel to the tab
        this.tabPanel.addTab("economy", Component.literal("Economy"), panel);
    }
    
    private void createPopulationTab() {
        // Create panel for content
        BCPanel panel = new BCPanel(this.tabPanel.getWidth(), this.tabPanel.getHeight() - 20);
        panel.withPadding(10)
             .withBackgroundColor(0x00000000) // Transparent background
             .withCornerRadius(3);
        
        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
        
        // Add title
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("POPULATION", panel.getInnerWidth());
        titleLabel.withTextColor(TEXT_HIGHLIGHT).withShadow(true);
        panel.addChild(titleLabel);
        
        // Sample citizen data (expanded)
        String[] names = {"John Smith", "Emma Johnson", "Alex Lee", "Sofia Garcia", 
                         "Michael Brown", "Lisa Wang", "David Miller", "James Wilson",
                         "Olivia Martinez", "William Taylor", "Ava Robinson", "Benjamin Clark",
                         "Charlotte Lewis", "Henry Walker", "Mia Allen", "Ethan Young"};
        String[] jobs = {"Miner", "Farmer", "Builder", "Trader", 
                        "Blacksmith", "Scholar", "Guard", "Carpenter",
                        "Baker", "Hunter", "Tailor", "Alchemist",
                        "Scribe", "Tanner", "Jeweler", "Beekeeper"};
        int[] levels = {3, 2, 4, 1, 5, 2, 3, 4, 2, 5, 3, 1, 4, 2, 3, 5};
        
        // Calculate dimensions for the citizens grid - ensure it fits within panel boundaries
        // Account for the panel's padding and the title height
        int titleHeight = 20; // Approximate height of the header
        int verticalSpacing = 10; // Space between title and grid
        
        // Calculate available space for the grid
        int availableWidth = panel.getInnerWidth();
        int availableHeight = panel.getInnerHeight() - titleHeight - verticalSpacing;
        
        // Create a direct implementation for the citizen list with scrolling
        BCComponent citizenList = new BCComponent(availableWidth, availableHeight) {
            // Scrolling state
            private int scrollOffset = 0;
            private final int itemHeight = 16; // Reduced item height for more compact display
            private final int padding = 8; // Reduced padding for more content space
            private final int verticalPadding = 5; // Even smaller vertical padding
            private int maxVisible; // Will be calculated based on available height
            private boolean isDraggingScrollbar = false;
            private boolean isMiddleMouseScrolling = false; // Track middle mouse scrolling
            private double lastMouseY = 0; // Last mouse Y position for middle mouse scrolling
            private final int scrollbarWidth = 8;
            
            @Override
            protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
                // Calculate max visible items based on available height
                int contentHeight = this.height - (verticalPadding * 2);
                maxVisible = contentHeight / itemHeight;
                
                // Draw the background panel
                guiGraphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, BACKGROUND_COLOR);
                
                // Draw border
                guiGraphics.hLine(this.x, this.x + this.width - 1, this.y, BORDER_COLOR);
                guiGraphics.hLine(this.x, this.x + this.width - 1, this.y + this.height - 1, BORDER_COLOR);
                guiGraphics.vLine(this.x, this.y, this.y + this.height - 1, BORDER_COLOR);
                guiGraphics.vLine(this.x + this.width - 1, this.y, this.y + this.height - 1, BORDER_COLOR);
                
                // Calculate column widths (3 columns: name, job, level)
                int contentWidth = this.width - scrollbarWidth - (padding * 2);
                int nameWidth = (int)(contentWidth * 0.4);
                int jobWidth = (int)(contentWidth * 0.35);
                int levelWidth = (int)(contentWidth * 0.25);
                
                // Calculate max scroll offset
                int maxScrollOffset = Math.max(0, names.length - maxVisible);
                if (scrollOffset > maxScrollOffset) {
                    scrollOffset = maxScrollOffset;
                }
                
                // Draw the scrollbar if needed
                if (names.length > maxVisible) {
                    // Draw scrollbar track
                    int trackHeight = this.height - (verticalPadding * 2);
                    guiGraphics.fill(
                        this.x + this.width - scrollbarWidth - padding,
                        this.y + verticalPadding,
                        this.x + this.width - padding,
                        this.y + this.height - verticalPadding,
                        0x40FFFFFF // Light gray semi-transparent
                    );
                    
                    // Draw scrollbar thumb
                    float thumbRatio = (float)maxVisible / names.length;
                    int thumbHeight = Math.max(20, (int)(trackHeight * thumbRatio));
                    int thumbY = this.y + verticalPadding + (int)((trackHeight - thumbHeight) * ((float)scrollOffset / maxScrollOffset));
                    
                    // Highlight if mouse is over
                    boolean isOverScrollbar = mouseX >= this.x + this.width - scrollbarWidth - padding &&
                                            mouseX <= this.x + this.width - padding &&
                                            mouseY >= this.y + verticalPadding &&
                                            mouseY <= this.y + this.height - verticalPadding;
                    
                    guiGraphics.fill(
                        this.x + this.width - scrollbarWidth - padding,
                        thumbY,
                        this.x + this.width - padding,
                        thumbY + thumbHeight,
                        isOverScrollbar ? 0xFFCCDDFF : 0xA0CCDDFF // Light blue with variable opacity
                    );
                }
                
                // Draw only visible citizens
                int startY = this.y + verticalPadding;
                for (int i = 0; i < Math.min(maxVisible, names.length - scrollOffset); i++) {
                    int dataIndex = i + scrollOffset;
                    if (dataIndex >= names.length) break; // Safety check
                    
                    int rowY = startY + (i * itemHeight);
                    
                    // Make sure this row is fully visible
                    if (rowY + itemHeight > this.y + this.height - verticalPadding) {
                        break;
                    }
                    
                    // Draw name (first column)
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        names[dataIndex],
                        this.x + padding,
                        rowY + 3, // Slight vertical centering
                        TEXT_HIGHLIGHT
                    );
                    
                    // Draw job (second column)
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        jobs[dataIndex],
                        this.x + padding + nameWidth,
                        rowY + 3, // Slight vertical centering
                        TEXT_COLOR
                    );
                    
                    // Draw level (third column)
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        "Level " + levels[dataIndex],
                        this.x + padding + nameWidth + jobWidth,
                        rowY + 3, // Slight vertical centering
                        TEXT_COLOR
                    );
                }
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (isMouseOver((int)mouseX, (int)mouseY)) {
                    // Calculate max visible items based on available height
                    int contentHeight = this.height - (verticalPadding * 2);
                    maxVisible = contentHeight / itemHeight;
                    
                    // Middle mouse button for scrolling
                    if (button == 2) { // Middle mouse button
                        isMiddleMouseScrolling = true;
                        lastMouseY = mouseY;
                        return true;
                    }
                    
                    // Left mouse button
                    if (button == 0) {
                        // Check if clicking on scrollbar
                        if (names.length > maxVisible && 
                            mouseX >= this.x + this.width - scrollbarWidth - padding &&
                            mouseX <= this.x + this.width - padding &&
                            mouseY >= this.y + verticalPadding &&
                            mouseY <= this.y + this.height - verticalPadding) {
                            
                            isDraggingScrollbar = true;
                            
                            // Calculate new scroll position
                            int trackHeight = this.height - (verticalPadding * 2);
                            float relativeY = (float)(mouseY - (this.y + verticalPadding)) / trackHeight;
                            int maxScrollOffset = Math.max(0, names.length - maxVisible);
                            scrollOffset = (int)(relativeY * maxScrollOffset);
                            
                            // Clamp scroll offset
                            if (scrollOffset < 0) {
                                scrollOffset = 0;
                            } else if (scrollOffset > maxScrollOffset) {
                                scrollOffset = maxScrollOffset;
                            }
                            
                            return true;
                        }
                        
                        // Check if clicking on a citizen
                        int clickedItem = -1;
                        int startY = this.y + verticalPadding;
                        for (int i = 0; i < Math.min(maxVisible, names.length - scrollOffset); i++) {
                            int dataIndex = i + scrollOffset;
                            int rowY = startY + (i * itemHeight);
                            
                            if (mouseY >= rowY && mouseY < rowY + itemHeight) {
                                clickedItem = dataIndex;
                                break;
                            }
                        }
                        
                        if (clickedItem != -1 && clickedItem < names.length) {
                            // Handle citizen click
                            playButtonClickSound();
                            sendChatMessage("Selected citizen: " + names[clickedItem] + " (" + jobs[clickedItem] + ")");
                            return true;
                        }
                    }
                }
                return false;
            }
            
            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                // Left mouse button dragging (scrollbar)
                if (isDraggingScrollbar && button == 0) {
                    // Calculate max visible items based on available height
                    int contentHeight = this.height - (verticalPadding * 2);
                    maxVisible = contentHeight / itemHeight;
                    
                    // Calculate new scroll position based on drag
                    int trackHeight = this.height - (verticalPadding * 2);
                    float relativeY = (float)(mouseY - (this.y + verticalPadding)) / trackHeight;
                    int maxScrollOffset = Math.max(0, names.length - maxVisible);
                    scrollOffset = (int)(relativeY * maxScrollOffset);
                    
                    // Clamp scroll offset
                    if (scrollOffset < 0) {
                        scrollOffset = 0;
                    } else if (scrollOffset > maxScrollOffset) {
                        scrollOffset = maxScrollOffset;
                    }
                    
                    return true;
                }
                
                // Middle mouse button dragging (direct scrolling)
                if (isMiddleMouseScrolling && button == 2) {
                    // Calculate max visible items based on available height
                    int contentHeight = this.height - (verticalPadding * 2);
                    maxVisible = contentHeight / itemHeight;
                    
                    // Calculate scroll amount based on mouse movement
                    double deltaY = mouseY - lastMouseY;
                    lastMouseY = mouseY;
                    
                    // Convert mouse movement to scroll amount (scale factor)
                    // Positive deltaY means dragging down, which should move content up (scroll down)
                    int scrollAmount = (int)(deltaY * 0.5);
                    
                    // Apply scrolling
                    int maxScrollOffset = Math.max(0, names.length - maxVisible);
                    scrollOffset += scrollAmount;
                    
                    // Clamp scroll offset
                    if (scrollOffset < 0) {
                        scrollOffset = 0;
                    } else if (scrollOffset > maxScrollOffset) {
                        scrollOffset = maxScrollOffset;
                    }
                    
                    // Middle mouse scrolling active
                    return true;
                }
                
                return false;
            }
            
            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                if (isDraggingScrollbar && button == 0) {
                    isDraggingScrollbar = false;
                    return true;
                }
                
                if (isMiddleMouseScrolling && button == 2) {
                    isMiddleMouseScrolling = false;
                    return true;
                }
                
                return false;
            }
            
            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
                // Calculate max visible items and offset
                int contentHeight = this.height - (verticalPadding * 2);
                maxVisible = contentHeight / itemHeight;
                int maxScrollOffset = Math.max(0, names.length - maxVisible);
                
                // Apply scrolling directly based on delta sign (delta > 0 means scroll up)
                scrollOffset -= (int)Math.signum(delta);
                
                // Clamp scroll position
                if (scrollOffset < 0) {
                    scrollOffset = 0;
                }
                if (scrollOffset > maxScrollOffset) {
                    scrollOffset = maxScrollOffset;
                }
                
                return true;
            }
        };
        
        // Add the citizen list to the panel
        panel.addChild(citizenList);
        
        // Add the panel to the tab
        this.tabPanel.addTab("population", Component.literal("Population"), panel);
    }
    
    private void createSettingsTab() {
        // Create panel for content
        BCPanel panel = new BCPanel(this.tabPanel.getWidth(), this.tabPanel.getHeight() - 20);
        panel.withPadding(10)
             .withBackgroundColor(0x00000000) // Transparent background
             .withCornerRadius(3);
        
        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
        
        // Add title
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("SETTINGS", panel.getInnerWidth());
        titleLabel.withTextColor(TEXT_HIGHLIGHT).withShadow(true);
        panel.addChild(titleLabel);
        
        // Calculate dimensions for the settings grid - ensure it fits within panel boundaries
        // Account for the panel's padding and the title height
        int titleHeight = 20; // Approximate height of the header
        int verticalSpacing = 10; // Space between title and grid
        
        // Calculate available space for the grid
        int availableWidth = panel.getInnerWidth();
        int availableHeight = panel.getInnerHeight() - titleHeight - verticalSpacing;
        
        // Create a custom component to host the settings grid
        BCComponent gridHost = new BCComponent(availableWidth, availableHeight) {
            // Internal grid instance
            private UIGridBuilder grid;
            
            // Initialize the grid when rendering
            private UIGridBuilder createGrid() {
                // Create the grid with component-relative positioning
                UIGridBuilder newGrid = new UIGridBuilder(x, y, getWidth(), getHeight(), 4, 2)
                    .withBackgroundColor(BACKGROUND_COLOR)
                    .withBorderColor(BORDER_COLOR)
                    .withMargins(15, 10)
                    .withSpacing(15, 10)
                    .drawBorder(true);
                
                // Add town settings
                
                // Row 1: PVP Toggle
                newGrid.addLabel(0, 0, "PVP:", TEXT_COLOR);
                newGrid.addToggle(0, 1, pvpEnabled ? "Enabled" : "Disabled", pvpEnabled, toggled -> {
                    // Update the persistent state
                    pvpEnabled = toggled;
                    String state = toggled ? "Enabled" : "Disabled";
                    sendChatMessage("Button pressed: PVP Toggle " + state);
                }, SUCCESS_COLOR, DANGER_COLOR);
                
                // Row 2: Public Town Toggle
                newGrid.addLabel(1, 0, "Public Town:", TEXT_COLOR);
                newGrid.addToggle(1, 1, publicTownEnabled ? "Enabled" : "Disabled", publicTownEnabled, toggled -> {
                    // Update the persistent state
                    publicTownEnabled = toggled;
                    String state = toggled ? "Enabled" : "Disabled";
                    sendChatMessage("Button pressed: Public Town Toggle " + state);
                }, SUCCESS_COLOR, DANGER_COLOR);
                
                // Row 3: Town Rank
                newGrid.addLabel(2, 0, "Town Rank:", TEXT_COLOR);
                newGrid.addLabel(2, 1, "Level 3", TEXT_HIGHLIGHT);
                
                // Row 4: Founded Date
                newGrid.addLabel(3, 0, "Founded:", TEXT_COLOR);
                newGrid.addLabel(3, 1, "Day 42", TEXT_HIGHLIGHT);
                
                return newGrid;
            }
            
            @Override
            protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
                // Create/update the grid
                grid = createGrid();
                
                // Render the grid
                grid.render(guiGraphics, mouseX, mouseY);
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                // Make sure we have a grid to click on
                if (grid == null) {
                    grid = createGrid();
                }
                
                // Let the grid handle clicks
                if (grid.mouseClicked((int)mouseX, (int)mouseY)) {
                    playButtonClickSound();
                    return true;
                }
                return false;
            }
        };
        
        // Add the grid host component to the panel
        panel.addChild(gridHost);
        
        // Add the panel to the tab
        this.tabPanel.addTab("settings", Component.literal("Settings"), panel);
    }
    
    // Helper methods to get data from the menu
    private String getTownName() {
        return "Prosperityville";
    }
    
    private String getMayorName() {
        return "Mayor Goodway";
    }
    
    private int getTownPopulation() {
        return 42;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Draw the dimmed background
        this.renderBackground(graphics);
        
        // Draw a semi-transparent background for the entire window
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0x80222222);
        
        // Draw border
        graphics.hLine(this.leftPos, this.leftPos + this.imageWidth - 1, this.topPos, BORDER_COLOR);
        graphics.hLine(this.leftPos, this.leftPos + this.imageWidth - 1, this.topPos + this.imageHeight - 1, BORDER_COLOR);
        graphics.vLine(this.leftPos, this.topPos, this.topPos + this.imageHeight - 1, BORDER_COLOR);
        graphics.vLine(this.leftPos + this.imageWidth - 1, this.topPos, this.topPos + this.imageHeight - 1, BORDER_COLOR);
        
        // Render the tab panel
        this.tabPanel.render(graphics, this.tabPanel.getX(), this.tabPanel.getY(), mouseX, mouseY);
        
        // Update and render the bottom buttons based on active tab
        updateBottomButtons();
        bottomButtonsGrid.render(graphics, mouseX, mouseY);
        
        // Render the screen title
        graphics.drawCenteredString(this.font, this.title, this.leftPos + this.imageWidth / 2, this.topPos - 12, TEXT_COLOR);

        // Draw any tooltips
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);  // Move tooltips to front layer
        this.renderTooltip(graphics, mouseX, mouseY);
        graphics.pose().popPose();
        
        // If we have an active popup, render it on top of everything with matrix transformation for Z-ordering
        if (activePopup != null && activePopup.isVisible()) {
            // Save the current render state
            graphics.pose().pushPose();
            
            // Move forward in Z-buffer to ensure it renders on top of everything else
            graphics.pose().translate(0, 0, 1000);
            
            // Draw a darker semi-transparent overlay for the entire screen first
            graphics.fill(0, 0, this.width, this.height, 0xD0000000); // 80% opacity black
            
            // Now render the actual popup content
            // We pass null for font because the popup will use Minecraft.getInstance().font directly
            activePopup.render(graphics, 0, 0, mouseX, mouseY);
            
            // Restore the render state
            graphics.pose().popPose();
        }
    }
    
    /**
     * Updates the bottom buttons based on the active tab
     */
    private void updateBottomButtons() {
        // Calculate button panel dimensions (must be recalculated in case window was resized)
        int buttonPanelWidth = this.imageWidth - 40;
        int buttonPanelHeight = 40;
        int buttonPanelX = this.leftPos + (this.imageWidth - buttonPanelWidth) / 2;
        int buttonPanelY = this.topPos + this.imageHeight + 10;
        
        // Create a new grid builder with current dimensions
        bottomButtonsGrid = new UIGridBuilder(buttonPanelX, buttonPanelY, buttonPanelWidth, buttonPanelHeight, 1, 2)
            .withBackgroundColor(BACKGROUND_COLOR)
            .withBorderColor(BORDER_COLOR)
            .withMargins(15, 10)
            .withSpacing(20, 0);
        
        // Get the active tab ID
        String activeTab = this.tabPanel.getActiveTabId();
        if (activeTab == null) {
            activeTab = "overview"; // Default
        }
        
        // Configure buttons based on active tab
        switch (activeTab) {
            case "overview":
                bottomButtonsGrid
                    .addButton(0, 0, "Edit Details", v -> {
                        showChangeTownNamePopup();
                    }, PRIMARY_COLOR)
                    .addButton(0, 1, "Visit Center", v -> {
                        sendChatMessage("Button pressed: Visit Center");
                    }, SECONDARY_COLOR);
                break;
                
            case "economy":
                bottomButtonsGrid
                    .addButton(0, 0, "Trade Resources", v -> {
                        sendChatMessage("Button pressed: Trade Resources");
                    }, PRIMARY_COLOR)
                    .addButton(0, 1, "Manage Storage", v -> {
                        sendChatMessage("Button pressed: Manage Storage");
                    }, SECONDARY_COLOR);
                break;
                
            case "population":
                bottomButtonsGrid
                    .addButton(0, 0, "Assign Jobs", v -> {
                        sendChatMessage("Button pressed: Assign Jobs");
                    }, PRIMARY_COLOR)
                    .addButton(0, 1, "Recruit Citizens", v -> {
                        sendChatMessage("Button pressed: Recruit Citizens");
                    }, SECONDARY_COLOR);
                break;
                
            case "settings":
                bottomButtonsGrid
                    .addButton(0, 0, "Save Settings", v -> {
                        sendChatMessage("Button pressed: Save Settings");
                    }, SUCCESS_COLOR)
                    .addButton(0, 1, "Reset Defaults", v -> {
                        sendChatMessage("Button pressed: Reset Defaults");
                    }, DANGER_COLOR);
                break;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // If popup is active, let it handle clicks first
        if (activePopup != null && activePopup.isVisible()) {
            if (activePopup.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        // Let the tab panel handle clicks first
        if (this.tabPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Check if bottom buttons grid handled the click
        if (button == 0 && bottomButtonsGrid.mouseClicked((int)mouseX, (int)mouseY)) {
            // Play button click sound
            playButtonClickSound();
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Plays the button click sound effect
     */
    private void playButtonClickSound() {
        Minecraft.getInstance().getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }
    
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // Background rendering is handled in render() method
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Let the tab panel handle drags first
        if (this.tabPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Let the tab panel handle releases first
        if (this.tabPanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Check if population tab is active and forward directly to citizenList
        String activeTabId = this.tabPanel.getActiveTabId();
        if ("population".equals(activeTabId)) {
            BCPanel panel = this.tabPanel.getTabPanel("population");
            if (panel != null) {
                for (UIComponent child : panel.getChildren()) {
                    if (child instanceof BCComponent) {
                        if (child.mouseScrolled(mouseX, mouseY, delta)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        // Let tab panel handle scroll
        if (this.tabPanel != null && this.tabPanel.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /**
     * Create a modified button with chat message notification
     * 
     * @param text The button text
     * @param onClick The original click handler
     * @param width The button width
     * @return A button that shows a chat message when clicked
     */
    private BCButton createNotifyButton(String text, Consumer<BCButton> onClick, int width) {
        // We need to bridge between different consumer types
        Consumer<Button> buttonHandler = b -> {
            // Send chat message
            sendChatMessage("Button pressed: " + text);
            
            // Call original handler if provided
            if (onClick != null) {
                onClick.accept(null);
            }
        };
        
        // Create button with the wrapped handler
        BCButton button;
        
        if (text.contains("Save")) {
            button = BCComponentFactory.createSuccessButton(text, buttonHandler, width);
        } else if (text.contains("Reset")) {
            button = BCComponentFactory.createDangerButton(text, buttonHandler, width);
        } else {
            button = BCComponentFactory.createPrimaryButton(text, buttonHandler, width);
        }
        
        return button.withText(Component.literal(text));
    }
    
    /**
     * Helper method to send a chat message to the player
     */
    private void sendChatMessage(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        }
    }

    /**
     * Show the change town name popup
     */
    private void showChangeTownNamePopup() {
        // Create a popup for changing the town name
        activePopup = BCComponentFactory.createStringInputPopup(
            "Change Town Name", 
            getTownName(), // Initial value
            result -> {
                // Handle the result
                if (result.isConfirmed() && !result.getStringValue().isEmpty()) {
                    // Send chat message with the new town name
                    sendChatMessage("Town name changed to: " + result.getStringValue());
                }
            }
        );
        
        // Get screen dimensions
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        // Calculate exact center position
        int popupWidth = 300; // Same as in createStringInputPopup
        int popupHeight = 150; // Same as in createStringInputPopup
        int centerX = screenWidth / 2 - popupWidth / 2;
        int centerY = screenHeight / 2 - popupHeight / 2;
        
        // Directly position the popup at the center of the screen
        activePopup.position(centerX, centerY);
        
        // Set close handler
        activePopup.setClosePopupHandler(button -> {
            activePopup = null; // Clear the popup when closed
        });
        
        // Initialize the popup
        activePopup.init(this::addRenderableWidget);
        
        // Focus the input field
        activePopup.focusInput();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // If popup is active, let it handle keyboard input first
        if (activePopup != null && activePopup.isVisible()) {
            if (activePopup.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        // If popup is active, let it handle character input first
        if (activePopup != null && activePopup.isVisible()) {
            if (activePopup.charTyped(c, modifiers)) {
                return true;
            }
        }
        
        return super.charTyped(c, modifiers);
    }
} 