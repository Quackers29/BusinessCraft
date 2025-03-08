package com.yourdomain.businesscraft.screen;

import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.screen.components.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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

    // Add field for modal screen
    private BCModalScreen activeModal = null;

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
        
        // Basic resource data
        String[] baseResources = {"Wood", "Stone", "Iron", "Gold", "Diamond", "Food", "Coal", "Emerald", 
                              "Copper", "Lapis", "Redstone", "Quartz", "Obsidian", "Netherite", 
                              "Wool", "Clay", "Paper", "Wheat", "Leather", "Amethyst"};
        
        // Map resources to Minecraft items
        net.minecraft.world.item.Item[] baseItems = {
            net.minecraft.world.item.Items.OAK_LOG,         // Wood
            net.minecraft.world.item.Items.STONE,           // Stone
            net.minecraft.world.item.Items.IRON_INGOT,      // Iron
            net.minecraft.world.item.Items.GOLD_INGOT,      // Gold
            net.minecraft.world.item.Items.DIAMOND,         // Diamond
            net.minecraft.world.item.Items.BREAD,           // Food
            net.minecraft.world.item.Items.COAL,            // Coal
            net.minecraft.world.item.Items.EMERALD,         // Emerald
            net.minecraft.world.item.Items.COPPER_INGOT,    // Copper
            net.minecraft.world.item.Items.LAPIS_LAZULI,    // Lapis
            net.minecraft.world.item.Items.REDSTONE,        // Redstone
            net.minecraft.world.item.Items.QUARTZ,          // Quartz
            net.minecraft.world.item.Items.OBSIDIAN,        // Obsidian
            net.minecraft.world.item.Items.NETHERITE_INGOT, // Netherite
            net.minecraft.world.item.Items.WHITE_WOOL,      // Wool
            net.minecraft.world.item.Items.CLAY_BALL,       // Clay
            net.minecraft.world.item.Items.PAPER,           // Paper
            net.minecraft.world.item.Items.WHEAT,           // Wheat
            net.minecraft.world.item.Items.LEATHER,         // Leather
            net.minecraft.world.item.Items.AMETHYST_SHARD   // Amethyst
        };
        
        // Basic Resource tooltips
        String[] baseTooltips = {
            "Used for buildings and tools",
            "Used for structures and roads",
            "Used for advanced tools and armor",
            "Valuable resource for trade",
            "Most valuable resource for trade and tools",
            "Keeps your citizens fed and happy",
            "Fuel for furnaces and machinery",
            "Special currency for trading",
            "Used for machinery and decoration",
            "Used for enchanting and decoration",
            "Used for redstone mechanisms",
            "Used for decoration and redstone components",
            "Strong material for portals and storage",
            "Strongest material for tools and armor",
            "Used for beds and decoration",
            "Used for bricks and pottery",
            "Used for books and maps",
            "Basic ingredient for food",
            "Used for armor and item frames",
            "Used for decorative blocks and telescopes"
        };
        
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
            // Random instance for generating quantities
            private final java.util.Random random = new java.util.Random();
            // Generate a random number of resources (between 1 and 20)
            private final int resourceCount = random.nextInt(20) + 1;
            // Arrays to hold the randomly selected resources
            private final String[] resources = new String[resourceCount];
            private final net.minecraft.world.item.Item[] items = new net.minecraft.world.item.Item[resourceCount];
            private final String[] tooltips = new String[resourceCount];
            // Store the quantities
            private final int[] currentQuantities = new int[resourceCount];
            // Track scrolling
            private int scrollOffset = 0;
            private final int itemHeight = 24; // Height of each row
            private int maxVisible; // Will be calculated dynamically
            private final int scrollbarWidth = 8;
            // For middle mouse scrolling
            private boolean isMiddleMouseScrolling = false;
            private double lastMouseY = 0;
            // For scrollbar dragging
            private boolean isDraggingScrollbar = false;
            // Padding values to match Population tab
            private final int padding = 8;
            private final int verticalPadding = 5;
            
            // Initialize resource data
            {
                // Randomly select resources to display
                for (int i = 0; i < resourceCount; i++) {
                    int index = random.nextInt(baseResources.length);
                    resources[i] = baseResources[index];
                    items[i] = baseItems[index];
                    tooltips[i] = baseTooltips[index];
                    
                    // Give each resource a quantity in a specific range based on its position
                    // This ensures we demonstrate different formatting (regular, K, M, etc.)
                    if (i % 6 == 0) {
                        // Small quantities (1-99)
                        currentQuantities[i] = random.nextInt(99) + 1;
                    } else if (i % 6 == 1) {
                        // Medium quantities (100-999)
                        currentQuantities[i] = random.nextInt(900) + 100;
                    } else if (i % 6 == 2) {
                        // Thousands (1,000-9,999)
                        currentQuantities[i] = random.nextInt(9000) + 1000;
                    } else if (i % 6 == 3) {
                        // Tens of thousands (10,000-99,999)
                        currentQuantities[i] = random.nextInt(90000) + 10000;
                    } else if (i % 6 == 4) {
                        // Millions (1,000,000-9,999,999)
                        currentQuantities[i] = random.nextInt(9000000) + 1000000;
                    } else {
                        // Billions (just under max int value)
                        currentQuantities[i] = random.nextInt(1000000000) + 1000000000;
                    }
                }
            }
            
            // Initialize the grid when rendering
            private UIGridBuilder createGrid() {
                // Calculate how many rows can be visible at once
                maxVisible = (height - 10) / itemHeight;
                int rowsToDisplay = Math.min(resourceCount, maxVisible);
                
                // Create the grid with component-relative positioning
                UIGridBuilder newGrid = new UIGridBuilder(x, y, width, height, rowsToDisplay, 2)
                    .withBackgroundColor(BACKGROUND_COLOR)
                    .withBorderColor(BORDER_COLOR)
                    .withMargins(15, 5)
                    .withSpacing(15, 5)
                    .drawBorder(true);
                
                // Add visible resource data to the grid
                int endIndex = Math.min(scrollOffset + rowsToDisplay, resourceCount);
                for (int i = scrollOffset; i < endIndex; i++) {
                    int gridRow = i - scrollOffset;
                    final int resourceIndex = i; // For use in the lambda
                    
                    // Click handler for the item
                    java.util.function.Consumer<Void> onClick = unused -> {
                        // Increase the quantity based on its current magnitude
                        if (currentQuantities[resourceIndex] < 100) {
                            currentQuantities[resourceIndex] += random.nextInt(10) + 1;
                        } else if (currentQuantities[resourceIndex] < 1000) {
                            currentQuantities[resourceIndex] += random.nextInt(100) + 10;
                        } else if (currentQuantities[resourceIndex] < 10000) {
                            currentQuantities[resourceIndex] += random.nextInt(1000) + 100;
                        } else if (currentQuantities[resourceIndex] < 1000000) {
                            currentQuantities[resourceIndex] += random.nextInt(10000) + 1000;
                        } else if (currentQuantities[resourceIndex] < 1000000000) {
                            currentQuantities[resourceIndex] += random.nextInt(1000000) + 100000;
                        } else {
                            currentQuantities[resourceIndex] += random.nextInt(100000000) + 10000000;
                        }
                        playButtonClickSound();
                        // Force redraw
                        grid = null;
                        return;
                    };
                    
                    // Add resource name (first column)
                    newGrid.addLabel(gridRow, 0, resources[i], TEXT_HIGHLIGHT);
                    
                    // Add item with quantity and tooltip (second column)
                    newGrid.addItemWithTooltip(gridRow, 1, items[i], currentQuantities[i], tooltips[i], onClick);
                }
                
                return newGrid;
            }
            
            @Override
            protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
                // Create/update the grid
                grid = createGrid();
                
                // Render the grid
                grid.render(guiGraphics, mouseX, mouseY);
                
                // Draw scrollbar if needed
                if (resourceCount > maxVisible) {
                    // Calculate max scroll offset
                    int maxScrollOffset = Math.max(0, resourceCount - maxVisible);
                    
                    // Draw scrollbar track
                    int trackHeight = this.height - (verticalPadding * 2);
                    guiGraphics.fill(
                        this.x + this.width - scrollbarWidth - padding,
                        this.y + verticalPadding,
                        this.x + this.width - padding,
                        this.y + this.height - verticalPadding,
                        0x40FFFFFF // Light gray semi-transparent
                    );
                    
                    // Calculate thumb size and position
                    float thumbRatio = (float)maxVisible / resourceCount;
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
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                // Check if we're clicking within this component
                if (isMouseOver((int)mouseX, (int)mouseY)) {
                    // Middle mouse button for scrolling
                    if (button == 2) { // Middle mouse button
                        isMiddleMouseScrolling = true;
                        lastMouseY = mouseY;
                        return true;
                    }
                    
                    // Left mouse button for scrollbar or clicking items
                    if (button == 0) {
                        // Check if clicking on scrollbar
                        if (resourceCount > maxVisible && 
                            mouseX >= this.x + this.width - scrollbarWidth - padding &&
                            mouseX <= this.x + this.width - padding &&
                            mouseY >= this.y + verticalPadding &&
                            mouseY <= this.y + this.height - verticalPadding) {
                            
                            isDraggingScrollbar = true;
                            
                            // Calculate new scroll position
                            int trackHeight = this.height - (verticalPadding * 2);
                            float relativeY = (float)(mouseY - (this.y + verticalPadding)) / trackHeight;
                            int maxScrollOffset = Math.max(0, resourceCount - maxVisible);
                            scrollOffset = (int)(relativeY * maxScrollOffset);
                            
                            // Clamp scroll offset
                            if (scrollOffset < 0) {
                                scrollOffset = 0;
                            } else if (scrollOffset > maxScrollOffset) {
                                scrollOffset = maxScrollOffset;
                            }
                            
                            return true;
                        }
                        
                        // Let the grid handle clicks for items
                        if (grid.mouseClicked((int)mouseX, (int)mouseY)) {
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
                    int trackHeight = this.height - (verticalPadding * 2);
                    float relativeY = (float)(mouseY - (this.y + verticalPadding)) / trackHeight;
                    int maxScrollOffset = Math.max(0, resourceCount - maxVisible);
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
                    // Calculate scroll amount based on mouse movement
                    double deltaY = mouseY - lastMouseY;
                    lastMouseY = mouseY;
                    
                    // Convert mouse movement to scroll amount (scale factor)
                    // Positive deltaY means dragging down, which should move content up (scroll down)
                    int scrollAmount = (int)(deltaY * 0.5);
                    
                    // Apply scrolling
                    int maxScrollOffset = Math.max(0, resourceCount - maxVisible);
                    scrollOffset += scrollAmount;
                    
                    // Clamp scroll offset
                    if (scrollOffset < 0) {
                        scrollOffset = 0;
                    } else if (scrollOffset > maxScrollOffset) {
                        scrollOffset = maxScrollOffset;
                    }
                    
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
                if (resourceCount <= maxVisible) {
                    return false;
                }
                
                // Calculate max scroll offset
                int maxScrollOffset = Math.max(0, resourceCount - maxVisible);
                
                // Scroll up or down based on mouse wheel direction
                if (delta > 0 && scrollOffset > 0) {
                    scrollOffset--;
                    return true;
                } else if (delta < 0 && scrollOffset < maxScrollOffset) {
                    scrollOffset++;
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
                         "Michael Brown", "Lisa Wang", 
                         "Michael Brown", "Lisa Wang", 
                         "Michael Brown", "Lisa Wang"};
        String[] jobs = {"Miner", "Farmer", "Builder", "Trader", 
                        "Blacksmith", "Scholar","Blacksmith", "Scholar","Blacksmith", "Scholar"};
        int[] levels = {3, 2, 4, 1, 5, 2, 3, 4, 2, 5};
        
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
        return "Mayor Goodway!";
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
        
        // IMPORTANT: Render modals and popups AFTER everything else
        
        // If we have an active popup, render it on top of everything with matrix transformation for Z-ordering
        if (activePopup != null && activePopup.isVisible()) {
            // The popup handles its own matrix transformations internally
            activePopup.render(graphics, 0, 0, mouseX, mouseY);
        }
        
        // If we have an active modal, render it last to ensure it's on top of everything
        if (activeModal != null) {
            // The modal now handles its own matrix transformations internally
            activeModal.render(graphics, 0, 0, mouseX, mouseY);
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
                    .addButtonWithTooltip(0, 0, "Edit Details", "Edit town details and properties", v -> {
                        showChangeTownNamePopup();
                    }, PRIMARY_COLOR)
                    .addButtonWithTooltip(0, 1, "View Visitors", "View list of visitors to your town", v -> {
                        showVisitorListModal();
                    }, SECONDARY_COLOR);
                break;
                
            case "economy":
                bottomButtonsGrid
                    .addButtonWithTooltip(0, 0, "Trade Resources", "Trade resources with other towns", v -> {
                        showTradeResourcesModal();
                    }, PRIMARY_COLOR)
                    .addButtonWithTooltip(0, 1, "Manage Storage", "Manage town storage and inventory", v -> {
                        showStorageModal();
                    }, SECONDARY_COLOR);
                break;
                
            case "population":
                bottomButtonsGrid
                    .addButtonWithTooltip(0, 0, "Assign Jobs", "Assign jobs to town citizens", v -> {
                        sendChatMessage("Button pressed: Assign Jobs");
                    }, PRIMARY_COLOR)
                    .addButtonWithTooltip(0, 1, "Recruit Citizens", "Recruit new citizens to your town", v -> {
                        sendChatMessage("Button pressed: Recruit Citizens");
                    }, SECONDARY_COLOR);
                break;
                
            case "settings":
                bottomButtonsGrid
                    .addButtonWithTooltip(0, 0, "Save Settings", "Save current town settings", v -> {
                        sendChatMessage("Button pressed: Save Settings");
                    }, SUCCESS_COLOR)
                    .addButtonWithTooltip(0, 1, "Reset Defaults", "Reset town settings to defaults", v -> {
                        sendChatMessage("Button pressed: Reset Defaults");
                    }, DANGER_COLOR);
                break;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // If modal is active, let it handle clicks first
        if (activeModal != null) {
            if (activeModal.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
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
        // If modal is active, let it handle scroll first
        if (activeModal != null) {
            if (activeModal.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        
        // If popup is active, let it handle character input first
        if (activePopup != null && activePopup.isVisible()) {
            if (activePopup.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        
        // Get active tab and forward scroll events
        String activeTabId = this.tabPanel.getActiveTabId();
        
        if ("economy".equals(activeTabId) || "population".equals(activeTabId)) {
            BCPanel panel = this.tabPanel.getTabPanel(activeTabId);
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
        
        // Let the tab panel handle scroll
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
            // Handle escape key specially to allow closing the popup
            if (keyCode == 256) { // ESCAPE key
                return activePopup.keyPressed(keyCode, scanCode, modifiers);
            }
            
            // For string input popups, completely consume 'e' key (inventory key) to prevent it from closing the popup
            if (keyCode == 69 && activePopup.isInputPopup()) { // 'e' key and input popup
                // Forward to popup for text input handling
                activePopup.keyPressed(keyCode, scanCode, modifiers);
                // Always consume the event
                return true;
            }
            
            // Let popup handle other keys normally
            if (activePopup.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            
            // If it's an input popup, consume all uncaught key events to prevent them from affecting the main screen
            if (activePopup.isInputPopup()) {
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

    /**
     * Show the visitor list modal screen
     */
    private void showVisitorListModal() {
        // Create a modal with a list of visitors
        // Use the 2-column constructor for an appropriately sized window
        activeModal = new BCModalScreen(
            "Town Visitors", 
            result -> {
                // Handle the result (OK or Back)
                if (result) {
                    sendChatMessage("Selected visitors from the list");
                }
                activeModal = null; // Clear the modal when closed
            },
            2 // Explicitly specify 2 columns for a narrower width
        );
        
        // Create some example data - just 5 items for a more compact display
        List<String> visitorNames = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            visitorNames.add("Visitor " + i + " - Town " + (i * 10));
        }
        
        // Set the data - this will adjust the height automatically
        activeModal.setData(visitorNames);
    }

    /**
     * Shows the trade resources modal screen with input and output slots
     */
    private void showTradeResourcesModal() {
        // Create and send a network packet to open the vanilla inventory trading screen
        Player player = this.minecraft.player;
        
        // Close the current screen
        this.onClose();
        
        // In a real implementation, we would send a packet to the server to open the menu
        // For now, we'll create a client-side container with a unique ID
        int containerId = player.containerMenu.containerId + 1;
        
        // Create a new inventory handler for the trade
        net.minecraftforge.items.ItemStackHandler handler = 
            new net.minecraftforge.items.ItemStackHandler(2);
        
        // Create the trade menu with a proper container ID
        com.yourdomain.businesscraft.menu.TradeMenu menu = 
            new com.yourdomain.businesscraft.menu.TradeMenu(containerId, player.getInventory(), handler);
        
        // Tell the player that this is now their active container
        // This is important for vanilla's item drag/transfer handling
        player.containerMenu = menu;
        
        // Create and open the trade screen
        com.yourdomain.businesscraft.screen.TradeScreen screen = 
            new com.yourdomain.businesscraft.screen.TradeScreen(
                menu, player.getInventory(), net.minecraft.network.chat.Component.literal("Trade Resources"));
        
        // Set the screen to be displayed
        this.minecraft.setScreen(screen);
    }
    
    /**
     * Shows the storage modal screen with 2x9 chest-like storage
     */
    private void showStorageModal() {
        // Create and send a network packet to open the vanilla inventory storage screen
        Player player = this.minecraft.player;
        
        // Close the current screen
        this.onClose();
        
        // In a real implementation, we would send a packet to the server to open the menu
        // For now, we'll create a client-side container with a unique ID
        int containerId = player.containerMenu.containerId + 1;
        
        // Create a new inventory handler for the storage (2 rows x 9 columns = 18 slots)
        net.minecraftforge.items.ItemStackHandler handler = 
            new net.minecraftforge.items.ItemStackHandler(18);
        
        // Create the storage menu with a proper container ID
        com.yourdomain.businesscraft.menu.StorageMenu menu = 
            new com.yourdomain.businesscraft.menu.StorageMenu(containerId, player.getInventory(), handler);
        
        // Tell the player that this is now their active container
        // This is important for vanilla's item drag/transfer handling
        player.containerMenu = menu;
        
        // Create and open the storage screen
        com.yourdomain.businesscraft.screen.StorageScreen screen = 
            new com.yourdomain.businesscraft.screen.StorageScreen(
                menu, player.getInventory(), net.minecraft.network.chat.Component.literal("Town Storage"));
        
        // Set the screen to be displayed
        this.minecraft.setScreen(screen);
    }
    
    /**
     * Close the active modal if one exists
     */
    private void closeActiveModal() {
        this.activeModal = null;
    }
    
    /**
     * Custom modal screen for trading resources
     */
    private class TradeModal extends BCModalScreen {
        private net.minecraft.world.item.ItemStack inputStack = net.minecraft.world.item.ItemStack.EMPTY;
        private net.minecraft.world.item.ItemStack outputStack = net.minecraft.world.item.ItemStack.EMPTY;
        private BCButton tradeButton;
        
        public TradeModal(String title, Consumer<Boolean> resultCallback) {
            super(title, resultCallback, 2);
            
            // Set sample data rows
            List<String> modalData = new ArrayList<>();
            modalData.add("Input Slot");  // Row 1
            modalData.add("Output Slot"); // Row 2
            setData(modalData);
            
            // Replace OK button with Trade button
            for (UIComponent child : getChildren()) {
                if (child instanceof BCButton) {
                    BCButton button = (BCButton) child;
                    if ("OK".equals(button.getText().getString())) {
                        button.withText(Component.literal("Trade"));
                        tradeButton = button;
                        button.addEventListener("click", component -> executeTrade());
                    }
                }
            }
        }
        
        @Override
        public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
            // First render the normal modal content using parent
            super.render(guiGraphics, x, y, mouseX, mouseY);
            
            // Then render our custom slots
            renderTradeSlots(guiGraphics, mouseX, mouseY);
        }
        
        private void renderTradeSlots(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            int slotSize = 18;
            int centerX = getWidth() / 2;
            int inputX = getX() + centerX - 40;
            int outputX = getX() + centerX + 20;
            int slotsY = getY() + 80;
            
            // Input slot
            guiGraphics.fill(inputX, slotsY, inputX + slotSize, slotsY + slotSize, 0xFF444444);
            guiGraphics.hLine(inputX - 1, inputX + slotSize, slotsY - 1, BORDER_COLOR);
            guiGraphics.hLine(inputX - 1, inputX + slotSize, slotsY + slotSize, BORDER_COLOR);
            guiGraphics.vLine(inputX - 1, slotsY - 1, slotsY + slotSize, BORDER_COLOR);
            guiGraphics.vLine(inputX + slotSize, slotsY - 1, slotsY + slotSize, BORDER_COLOR);
            
            // Draw input label
            guiGraphics.drawString(font, "Input", inputX, slotsY - 10, TEXT_COLOR);
            
            // Draw arrow
            int arrowX = getX() + centerX - 4;
            int arrowY = slotsY + slotSize/2 - 4;
            guiGraphics.drawString(font, "→", arrowX, arrowY, TEXT_HIGHLIGHT);
            
            // Output slot
            guiGraphics.fill(outputX, slotsY, outputX + slotSize, slotsY + slotSize, 0xFF444444);
            guiGraphics.hLine(outputX - 1, outputX + slotSize, slotsY - 1, BORDER_COLOR);
            guiGraphics.hLine(outputX - 1, outputX + slotSize, slotsY + slotSize, BORDER_COLOR);
            guiGraphics.vLine(outputX - 1, slotsY - 1, slotsY + slotSize, BORDER_COLOR);
            guiGraphics.vLine(outputX + slotSize, slotsY - 1, slotsY + slotSize, BORDER_COLOR);
            
            // Draw output label
            guiGraphics.drawString(font, "Output", outputX, slotsY - 10, TEXT_COLOR);
            
            // Draw items if present
            if (!inputStack.isEmpty()) {
                guiGraphics.renderItem(inputStack, inputX + 1, slotsY + 1);
            }
            
            if (!outputStack.isEmpty()) {
                guiGraphics.renderItem(outputStack, outputX + 1, slotsY + 1);
            }
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // Check for clicks on our custom slots first
            int slotSize = 18;
            int centerX = getWidth() / 2;
            int inputX = getX() + centerX - 40;
            int outputX = getX() + centerX + 20;
            int slotsY = getY() + 80;
            
            // Check input slot
            if (mouseX >= inputX && mouseX < inputX + slotSize && 
                mouseY >= slotsY && mouseY < slotsY + slotSize) {
                
                // For demo, we'll just put a stone item in the slot when clicked
                if (inputStack.isEmpty()) {
                    inputStack = new net.minecraft.world.item.ItemStack(
                        net.minecraft.world.item.Items.STONE, 1);
                    sendChatMessage("Added Stone to input slot");
                } else {
                    // Clear the slot if clicked again
                    inputStack = net.minecraft.world.item.ItemStack.EMPTY;
                    sendChatMessage("Removed item from input slot");
                }
                return true;
            }
            
            // Check output slot
            if (mouseX >= outputX && mouseX < outputX + slotSize && 
                mouseY >= slotsY && mouseY < slotsY + slotSize) {
                
                // Collect item from output slot
                if (!outputStack.isEmpty()) {
                    sendChatMessage("Collected: " + outputStack.getCount() + "x " + 
                                   outputStack.getDisplayName().getString());
                    outputStack = net.minecraft.world.item.ItemStack.EMPTY;
                    return true;
                }
            }
            
            // If not handled by our slots, let the parent handle it
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        /**
         * Execute the trade operation
         */
        private void executeTrade() {
            if (!inputStack.isEmpty()) {
                // Process trade by moving input to output
                outputStack = inputStack.copy();
                
                // Clear input slot
                inputStack = net.minecraft.world.item.ItemStack.EMPTY;
                
                sendChatMessage("Trade executed - item transferred to output slot");
            } else {
                sendChatMessage("No input item to trade");
            }
        }
    }
} 