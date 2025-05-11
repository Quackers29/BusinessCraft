package com.yourdomain.businesscraft.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.screen.components.*;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.screen.tabs.ResourcesTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.opengl.GL11;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.SetTownNamePacket;
import com.yourdomain.businesscraft.network.PlayerExitUIPacket;
import com.yourdomain.businesscraft.network.SetSearchRadiusPacket;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import com.yourdomain.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import net.minecraft.client.gui.screens.Screen;
import com.yourdomain.businesscraft.screen.components.BCModalGridScreen;
import com.yourdomain.businesscraft.screen.components.BCModalGridFactory;
import com.yourdomain.businesscraft.screen.components.BCModalInventoryScreen;
import com.yourdomain.businesscraft.screen.components.BCModalInventoryFactory;
import com.yourdomain.businesscraft.menu.TradeMenu;
import com.yourdomain.businesscraft.menu.StorageMenu;
import com.yourdomain.businesscraft.data.cache.TownDataCache;
import com.yourdomain.businesscraft.api.ITownDataProvider;
import java.util.Collections;

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
    
    // Cache the current search radius for UI updates
    private int currentSearchRadius;
    
    // Cache values for population and tourists for UI updates
    private int cachedPopulation;
    private int cachedTourists;
    private int cachedMaxTourists;

    // Add a field to track update intervals
    private int updateCounter = 0;
    private static final int REFRESH_INTERVAL = 100; // Ticks between forced cache refreshes (5 seconds)

    // State management
    private TownDataCache dataCache;

    public TownInterfaceScreen(TownInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        
        // Set custom dimensions for the screen
        this.imageWidth = 256;
        this.imageHeight = 204;
        
        // Move the inventory label off-screen to hide it
        this.inventoryLabelY = 300;  // Position it below the visible area
        
        // Initialize cached values from the menu
        this.cachedPopulation = menu.getTownPopulation();
        this.cachedTourists = menu.getCurrentTourists();
        this.cachedMaxTourists = menu.getMaxTourists();
        this.currentSearchRadius = menu.getSearchRadius();
        
        // Initialize the data cache with the town data provider from the menu
        if (menu instanceof TownInterfaceMenu) {
            ITownDataProvider dataProvider = menu.getTownDataProvider();
            if (dataProvider != null) {
                this.dataCache = new TownDataCache(dataProvider);
            }
        }
        
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
        
        // Initialize the cached values
        currentSearchRadius = menu.getSearchRadius();
        cachedPopulation = menu.getTownPopulation();
        cachedTourists = menu.getCurrentTourists();
        cachedMaxTourists = menu.getMaxTourists();
        
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
        createResourcesTab();
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
        int buttonPanelHeight = 30; // Reduced by 50% from 60px to 30px
        int buttonPanelX = this.leftPos + (this.imageWidth - buttonPanelWidth) / 2;
        int buttonPanelY = this.topPos + this.imageHeight + 10;
        
        // Create a 1x2 grid (1 row, 2 columns)
        bottomButtonsGrid = new UIGridBuilder(buttonPanelX, buttonPanelY, buttonPanelWidth, buttonPanelHeight, 1, 2)
            .withBackgroundColor(BACKGROUND_COLOR)
            .withBorderColor(BORDER_COLOR)
            .withMargins(5, 5) // Reduce margins for smaller height
            .withSpacing(20, 0)
            .withRowHeight(20); // Reduced by 50% from 40px to 20px
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
                // Use the new utility method for label-value pairs
                Map<String, String> overviewData = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order
                overviewData.put("Town Name:", getCachedTownName());
                overviewData.put("Population:", String.valueOf(getCachedPopulation()));
                overviewData.put("Tourists:", getTouristString());
                
                return UIGridBuilder.createLabelValueGrid(
                    x, y, getWidth(), getHeight(),
                    TEXT_COLOR, TEXT_HIGHLIGHT,
                    overviewData)
                    .withBackgroundColor(BACKGROUND_COLOR)
                    .withBorderColor(BORDER_COLOR)
                    .withMargins(15, 10)
                    .withSpacing(15, 10)
                    .drawBorder(true);
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
                // Pass the event to the grid if it was created
                if (grid != null) {
                    return grid.mouseClicked((int)mouseX, (int)mouseY, button);
                }
                return false;
            }
        };
        
        // Add the grid host component to the panel
        panel.addChild(gridHost);
        
        // Add the panel to the tab
        this.tabPanel.addTab("overview", Component.literal("Overview"), panel);
    }
    
    private void createResourcesTab() {
        // Create a new ResourcesTab instance
        ResourcesTab resourcesTab = new ResourcesTab(
            this, 
            this.tabPanel.getWidth(), 
            this.tabPanel.getHeight() - 20
        );
        
        // Initialize the tab
        resourcesTab.init(this::addRenderableWidget);
        
        // Add the tab to the tab panel
        this.tabPanel.addTab("resources", Component.literal("Resources"), resourcesTab.getPanel());
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
                    int rowY = startY + (i * itemHeight);
                    
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
                // Use the new utility method for label-button pairs
                Map<String, Object[]> settingsData = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order
                
                // Add platforms row
                settingsData.put("Platforms:", new Object[] {
                    "Set Platforms", 
                    (Consumer<Void>) button -> {
                        playButtonClickSound();
                        // Open the platform management screen instead of just showing a message
                        openPlatformManagementScreen();
                    }
                });
                
                // Add search radius row - use the cached value for display
                settingsData.put("Search Radius:", new Object[] {
                    "Radius: " + currentSearchRadius, 
                    (Consumer<Void>) button -> {
                        // In TownBlockScreen, this would increase the radius
                        handleRadiusChange(0); // 0 = left click (increase)
                    }
                });
                
                return UIGridBuilder.createLabelButtonGrid(
                    x, y, getWidth(), getHeight(),
                    TEXT_COLOR, PRIMARY_COLOR,
                    settingsData)
                    .withBackgroundColor(BACKGROUND_COLOR)
                    .withBorderColor(BORDER_COLOR)
                    .withMargins(15, 10)
                    .withSpacing(15, 10)
                    .drawBorder(true);
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
                // If this is a right-click, handle the radius change manually
                if (button == 1) {
                    // Approximate if the click is in the button area
                    // This is a simplified approach that assumes the radius button is at a specific position
                    handleRadiusChange(1); // Handle right click
                    return true;
                }
                
                // For all other cases, delegate to the grid
                if (grid != null) {
                    return grid.mouseClicked((int)mouseX, (int)mouseY, button);
                }
                return false;
            }
        };
        
        // Add the grid host to the panel
        panel.addChild(gridHost);
        
        // Add the panel to the tab
        this.tabPanel.addTab("settings", Component.literal("Settings"), panel);
    }
    
    /**
     * Opens the platform management screen
     */
    private void openPlatformManagementScreen() {
        // Get real platform data from the menu rather than using sample data
        List<Platform> platforms = menu.getPlatforms();
        
        // Create the platform management screen
        PlatformManagementScreen platformScreen = new PlatformManagementScreen(
            menu.getBlockPos(), platforms);
        
        // Close this screen and open the platform screen
        // This ensures when the user navigates back from the platform screen,
        // they return to the town interface
        this.minecraft.setScreen(platformScreen);
    }
    
    /**
     * Handles changes to the search radius
     * Implements the behavior from TownBlockScreen
     */
    private void handleRadiusChange(int mouseButton) {
        // Get the current radius from our cached value
        int newRadius = currentSearchRadius;
        
        // Calculate new radius based on key combinations
        boolean isShift = hasShiftDown();
        
        // Use mouseButton to determine increase/decrease
        // mouseButton 0 = left click (increase), 1 = right click (decrease)
        boolean isDecrease = (mouseButton == 1);
        
        if (isShift && isDecrease) {
            newRadius -= 10;
        } else if (isDecrease) {
            newRadius -= 1;
        } else if (isShift) {
            newRadius += 10;
        } else {
            newRadius += 1;
        }
        
        // Clamp to reasonable values
        newRadius = Math.max(1, Math.min(newRadius, 100));
        
        // Update our cached value immediately for UI feedback
        currentSearchRadius = newRadius;
        
        // Send packet to update the server
        ModMessages.sendToServer(new SetSearchRadiusPacket(menu.getBlockPos(), newRadius));
        
        // Use feedback message since we can't update the UI directly
        String message = "Search radius " + (isDecrease ? "decreased" : "increased") + " to " + newRadius;
        sendChatMessage(message);
        
        // Also update the menu's cached value if the method is available
        if (menu instanceof TownInterfaceMenu) {
            ((TownInterfaceMenu) menu).setClientSearchRadius(newRadius);
        }
        
        // Play a click sound for feedback
        playButtonClickSound();
    }
    
    // Helper methods to get data from the cache or fall back to menu
    private String getCachedTownName() {
        if (dataCache != null) {
            return dataCache.getTownName();
        }
        return menu.getTownName();
    }
    
    private int getCachedPopulation() {
        if (dataCache != null) {
            return dataCache.getPopulation();
        }
        return menu.getTownPopulation();
    }
    
    private int getCachedTouristCount() {
        if (dataCache != null) {
            return dataCache.getTouristCount();
        }
        return menu.getCurrentTourists();
    }
    
    private int getCachedMaxTourists() {
        if (dataCache != null) {
            return dataCache.getMaxTourists();
        }
        return menu.getMaxTourists();
    }
    
    private int getCachedSearchRadius() {
        if (dataCache != null) {
            return dataCache.getSearchRadius();
        }
        return menu.getSearchRadius();
    }
    
    // Changed from private to public to allow access from tab implementations
    public Map<Item, Integer> getCachedResources() {
        if (dataCache != null) {
            return dataCache.getAllResources();
        }
        return menu.getAllResources();
    }
    
    private List<ITownDataProvider.VisitHistoryRecord> getCachedVisitHistory() {
        if (dataCache != null) {
            return dataCache.getVisitHistory();
        }
        return Collections.emptyList(); // Fallback when cache is not available
    }
    
    private String getTouristString() {
        return cachedTourists + "/" + cachedMaxTourists;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Update the cached values periodically
        updateCounter++;
        if (updateCounter >= REFRESH_INTERVAL) { // Every 5 seconds (100 ticks)
            updateCounter = 0;
            
            // Refresh cache if available
            if (dataCache != null) {
                dataCache.invalidateAll();
                
                // Update our local cached values from the refreshed cache
                this.cachedPopulation = getCachedPopulation();
                this.cachedTourists = getCachedTouristCount();
                this.cachedMaxTourists = getCachedMaxTourists();
                this.currentSearchRadius = getCachedSearchRadius();
            } else {
                // Fallback to direct menu updates if cache isn't available
                this.cachedPopulation = menu.getTownPopulation();
                this.cachedTourists = menu.getCurrentTourists();
                this.cachedMaxTourists = menu.getMaxTourists();
                this.currentSearchRadius = menu.getSearchRadius();
            }
        }
        
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
        int buttonPanelHeight = 30; // Reduced by 50% from 60px to 30px
        int buttonPanelX = this.leftPos + (this.imageWidth - buttonPanelWidth) / 2;
        int buttonPanelY = this.topPos + this.imageHeight + 10;
        
        // Create a new grid builder with current dimensions
        bottomButtonsGrid = new UIGridBuilder(buttonPanelX, buttonPanelY, buttonPanelWidth, buttonPanelHeight, 1, 2)
            .withBackgroundColor(BACKGROUND_COLOR)
            .withBorderColor(BORDER_COLOR)
            .withMargins(5, 5) // Reduce margins for smaller height
            .withSpacing(20, 0)
            .withRowHeight(20); // Reduced by 50% from 40px to 20px
        
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
                
            case "resources":
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
                    .addButtonWithTooltip(0, 1, "View Visitors", "View history of visitors to your town", v -> {
                        showVisitorHistoryScreen();
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
        // Handle clicks on modal screens first, which take priority
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
        if (button == 0 && bottomButtonsGrid.mouseClicked((int)mouseX, (int)mouseY, button)) {
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
        // If modal is active, let it handle drag first
        if (activeModal != null && activeModal.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        
        // If popup is active, let it handle drag events
        if (activePopup != null && activePopup.isVisible() && activePopup.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        
        // Get active tab and forward drag events
        String activeTabId = this.tabPanel.getActiveTabId();
        
        // For scrollable tabs (Resources, Population) - forward drag events to children
        if ("resources".equals(activeTabId) || "population".equals(activeTabId)) {
            BCPanel panel = this.tabPanel.getTabPanel(activeTabId);
            if (panel != null) {
                // Forward to all children that are visible components
                for (UIComponent child : panel.getChildren()) {
                    if (child instanceof BCComponent && ((BCComponent) child).isVisible()) {
                        if (((BCComponent) child).mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        // Let the tab panel handle drag events
        if (this.tabPanel != null && this.tabPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
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
        if (activeModal != null && activeModal.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        
        // If popup is active, let it handle scroll events
        if (activePopup != null && activePopup.isVisible() && activePopup.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        
        // Get active tab and forward scroll events to all scrollable tabs in the same way
        String activeTabId = this.tabPanel.getActiveTabId();
        
        // Special case for resources tab since it has custom scrolling
        if ("resources".equals(activeTabId)) {
            BCPanel panel = this.tabPanel.getTabPanel(activeTabId);
            if (panel != null) {
                // Attempt direct scrolling on the panel's children
                for (UIComponent child : panel.getChildren()) {
                    if (child instanceof BCComponent && ((BCComponent) child).isVisible()) {
                        if (child.mouseScrolled(mouseX, mouseY, delta)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        // Let the tab panel handle scroll - this will properly forward to our ResourcesTab
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
            getCachedTownName(), // Initial value
            result -> {
                // Handle the result
                if (result.isConfirmed() && !result.getStringValue().isEmpty()) {
                    String newName = result.getStringValue().trim();
                    
                    // Send packet to update town name on the server
                    ModMessages.sendToServer(
                        new SetTownNamePacket(menu.getBlockPos(), newName)
                    );
                    
                    // Provide immediate client-side feedback
                    sendChatMessage("Changing town name to: " + newName);
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
        // Create a modal trade screen using the factory
        BCModalInventoryScreen<TradeMenu> tradeScreen = BCModalInventoryFactory.createTradeScreen(
            Component.literal("Trade Resources"),
            this,
            this.menu.getBlockPos(),
            screen -> {
                // Optional callback when screen is closed
                // We can use this to refresh data if needed
            }
        );
        
        // Show the trade screen as a modal overlay
        this.minecraft.setScreen(tradeScreen);
    }
    
    /**
     * Shows the storage modal screen with 2x9 chest-like storage
     */
    private void showStorageModal() {
        // Create a modal storage screen using the factory
        BCModalInventoryScreen<StorageMenu> storageScreen = BCModalInventoryFactory.createStorageScreen(
            Component.literal("Town Storage"),
            this,
            this.menu.getBlockPos(),
            screen -> {
                // Optional callback when screen is closed
                // We can use this to refresh data if needed
            }
        );
        
        // Initialize the storage inventory with the town's communal storage items
        if (storageScreen.getMenu() != null) {
            StorageMenu storageMenu = storageScreen.getMenu();
            
            // Start with communal storage by default
            boolean isPersonalMode = false;
            storageMenu.updateStorageItems(this.menu.getAllCommunalStorageItems());
            
            // If at some point we want to start with personal mode, we would do:
            // isPersonalMode = true;
            // storageMenu.toggleStorageMode();
            // storageMenu.updatePersonalStorageItems(this.menu.getPersonalStorageItems(this.minecraft.player.getUUID()));
        }
        
        // Show the storage screen as a modal overlay
        this.minecraft.setScreen(storageScreen);
    }
    
    /**
     * Close the active modal if one exists
     */
    private void closeActiveModal() {
        this.activeModal = null;
    }
    
    /**
     * Show the visitor history screen
     */
    private void showVisitorHistoryScreen() {
        // Get the block entity and visit history
        List<VisitHistoryRecord> visitHistory = new ArrayList<>();
        BlockPos townPos = menu.getBlockPos();
        
        if (this.minecraft != null && this.minecraft.level != null) {
            BlockEntity blockEntity = this.minecraft.level.getBlockEntity(townPos);
            if (blockEntity instanceof TownBlockEntity townBlock) {
                // Request the town block entity to sync its town data with the server
                ModMessages.sendToServer(new PlayerExitUIPacket(townPos));
                
                // Get the visit history
                visitHistory.addAll(townBlock.getVisitHistory());
            }
        }
        
        // Create a town name lookup function
        Function<UUID, String> townNameLookup = townId -> {
            if (townId == null) return "Unknown";
            
            try {
                // On the client side, we need to rely on the TownBlockEntity to get the town name
                if (this.minecraft != null && this.minecraft.level != null) {
                    BlockEntity blockEntity = this.minecraft.level.getBlockEntity(townPos);
                    if (blockEntity instanceof TownBlockEntity townBlock) {
                        // Use the getTownNameFromId method to look up the town name
                        String townName = townBlock.getTownNameFromId(townId);
                        
                        // If we got a name, return it
                        if (townName != null) {
                            return townName;
                        }
                    }
                }
            } catch (Exception e) {
                // Log the error but don't crash
                System.out.println("Error looking up town name for UUID " + townId + ": " + e.getMessage());
            }
            
            // Fallback if town not found
            return "Town-" + townId.toString().substring(0, 8);
        };
        
        // Create modal grid screen with the visitor history data
        BCModalGridScreen<VisitHistoryRecord> visitorScreen = BCModalGridFactory.createVisitorHistoryScreen(
            Component.literal("Town Visitor History"),
            this, // Parent screen
            visitHistory,
            screen -> {
                // When closing, ensure we're on the population tab
                if (this.tabPanel != null) {
                    this.tabPanel.setActiveTab("population");
                }
            },
            townNameLookup // Pass our town name lookup function
        );
        
        // Customize appearance if needed
        visitorScreen.withBackButtonText("Back")
                     .withTitleScale(1.5f)
                     .withRowHeight(20);
        
        // Show the visitor history screen
        this.minecraft.setScreen(visitorScreen);
    }

    @Override
    public void onClose() {
        // Send a packet to register the player exit UI to show platform indicators
        BlockPos blockPos = this.menu.getBlockPos();
        if (blockPos != null) {
            // Send a packet to the server to register player exit UI
            ModMessages.sendToServer(new PlayerExitUIPacket(blockPos));
        }
        
        super.onClose();
    }
} 