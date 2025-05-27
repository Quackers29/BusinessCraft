package com.yourdomain.businesscraft.screen.managers;

import com.yourdomain.businesscraft.screen.TownInterfaceTheme;
import com.yourdomain.businesscraft.screen.components.UIGridBuilder;
import net.minecraft.client.gui.GuiGraphics;
import java.util.function.Consumer;

/**
 * Manages bottom button grid creation and configuration for the Town Interface.
 * Extracted from TownInterfaceScreen to improve code organization.
 */
public class BottomButtonManager {
    
    // Button configuration interface
    public interface ButtonActionHandler {
        void onEditDetails();
        void onViewVisitors();
        void onTradeResources();
        void onManageStorage();
        void onAssignJobs();
        void onViewVisitorHistory();
        void onSaveSettings();
        void onResetDefaults();
        void onManagePlatforms();
        void onGenericAction(String action);
    }
    
    private UIGridBuilder bottomButtonsGrid;
    private final ButtonActionHandler actionHandler;
    
    // Screen positioning data
    private int screenLeftPos;
    private int screenTopPos;
    private int screenWidth;
    private int screenHeight;
    
    /**
     * Creates a new bottom button manager.
     * 
     * @param actionHandler Handler for button actions
     */
    public BottomButtonManager(ButtonActionHandler actionHandler) {
        this.actionHandler = actionHandler;
    }
    
    /**
     * Updates the screen positioning for button calculations.
     * 
     * @param leftPos Screen left position
     * @param topPos Screen top position
     * @param width Screen width
     * @param height Screen height
     */
    public void updateScreenPosition(int leftPos, int topPos, int width, int height) {
        this.screenLeftPos = leftPos;
        this.screenTopPos = topPos;
        this.screenWidth = width;
        this.screenHeight = height;
    }
    
    /**
     * Creates the bottom button grid with proper positioning.
     */
    public void createBottomButtonsGrid() {
        // Calculate button panel dimensions
        int buttonPanelWidth = this.screenWidth - 40; // Leave 20px margin on each side
        int buttonPanelHeight = 30; // Reduced by 50% from 60px to 30px
        int buttonPanelX = this.screenLeftPos + (this.screenWidth - buttonPanelWidth) / 2;
        int buttonPanelY = this.screenTopPos + this.screenHeight + 10;
        
        // Create a 1x2 grid (1 row, 2 columns)
        bottomButtonsGrid = new UIGridBuilder(buttonPanelX, buttonPanelY, buttonPanelWidth, buttonPanelHeight, 1, 2)
            .withBackgroundColor(TownInterfaceTheme.BACKGROUND_COLOR)
            .withBorderColor(TownInterfaceTheme.BORDER_COLOR)
            .withMargins(5, 5) // Reduce margins for smaller height
            .withSpacing(20, 0)
            .withRowHeight(20); // Reduced by 50% from 40px to 20px
    }
    
    /**
     * Updates the bottom buttons based on the active tab.
     * 
     * @param activeTab The currently active tab ID
     */
    public void updateBottomButtons(String activeTab) {
        // Calculate button panel dimensions (must be recalculated in case window was resized)
        int buttonPanelWidth = this.screenWidth - 40;
        int buttonPanelHeight = 30;
        int buttonPanelX = this.screenLeftPos + (this.screenWidth - buttonPanelWidth) / 2;
        int buttonPanelY = this.screenTopPos + this.screenHeight + 10;
        
        // Create a new grid builder with current dimensions
        bottomButtonsGrid = new UIGridBuilder(buttonPanelX, buttonPanelY, buttonPanelWidth, buttonPanelHeight, 1, 2)
            .withBackgroundColor(TownInterfaceTheme.BACKGROUND_COLOR)
            .withBorderColor(TownInterfaceTheme.BORDER_COLOR)
            .withMargins(5, 5)
            .withSpacing(20, 0)
            .withRowHeight(20);
        
        // Default to overview if no active tab
        if (activeTab == null) {
            activeTab = "overview";
        }
        
        // Configure buttons based on active tab
        configureButtonsForTab(activeTab);
    }
    
    /**
     * Configures buttons for the specified tab.
     * 
     * @param tabId The tab ID to configure buttons for
     */
    private void configureButtonsForTab(String tabId) {
        switch (tabId) {
            case "overview":
                configureOverviewButtons();
                break;
                
            case "resources":
                configureResourcesButtons();
                break;
                
            case "population":
                configurePopulationButtons();
                break;
                
            case "settings":
                configureSettingsButtons();
                break;
                
            default:
                configureDefaultButtons();
                break;
        }
    }
    
    /**
     * Configures buttons for the Overview tab.
     */
    private void configureOverviewButtons() {
        bottomButtonsGrid
            .addButtonWithTooltip(0, 0, "Edit Details", "Edit town details and properties", 
                v -> actionHandler.onEditDetails(), TownInterfaceTheme.PRIMARY_COLOR)
            .addButtonWithTooltip(0, 1, "Manage Platforms", "Manage town platforms and connections", 
                v -> actionHandler.onManagePlatforms(), TownInterfaceTheme.SECONDARY_COLOR);
    }
    
    /**
     * Configures buttons for the Resources tab.
     */
    private void configureResourcesButtons() {
        bottomButtonsGrid
            .addButtonWithTooltip(0, 0, "Trade Resources", "Trade resources with other towns", 
                v -> actionHandler.onTradeResources(), TownInterfaceTheme.PRIMARY_COLOR)
            .addButtonWithTooltip(0, 1, "Manage Storage", "Manage town storage and inventory", 
                v -> actionHandler.onManageStorage(), TownInterfaceTheme.SECONDARY_COLOR);
    }
    
    /**
     * Configures buttons for the Population tab.
     */
    private void configurePopulationButtons() {
        bottomButtonsGrid
            .addButtonWithTooltip(0, 0, "Assign Jobs", "Assign jobs to town citizens", 
                v -> actionHandler.onAssignJobs(), TownInterfaceTheme.PRIMARY_COLOR)
            .addButtonWithTooltip(0, 1, "View Visitors", "View history of visitors to your town", 
                v -> actionHandler.onViewVisitorHistory(), TownInterfaceTheme.SECONDARY_COLOR);
    }
    
    /**
     * Configures buttons for the Settings tab.
     */
    private void configureSettingsButtons() {
        bottomButtonsGrid
            .addButtonWithTooltip(0, 0, "Save Settings", "Save current town settings", 
                v -> actionHandler.onSaveSettings(), TownInterfaceTheme.SUCCESS_COLOR)
            .addButtonWithTooltip(0, 1, "Reset Defaults", "Reset town settings to defaults", 
                v -> actionHandler.onResetDefaults(), TownInterfaceTheme.DANGER_COLOR);
    }
    
    /**
     * Configures default buttons for unknown tabs.
     */
    private void configureDefaultButtons() {
        bottomButtonsGrid
            .addButtonWithTooltip(0, 0, "Action 1", "Default action 1", 
                v -> actionHandler.onGenericAction("Action 1"), TownInterfaceTheme.PRIMARY_COLOR)
            .addButtonWithTooltip(0, 1, "Action 2", "Default action 2", 
                v -> actionHandler.onGenericAction("Action 2"), TownInterfaceTheme.SECONDARY_COLOR);
    }
    
    /**
     * Renders the bottom buttons grid.
     * 
     * @param graphics Graphics context
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (bottomButtonsGrid != null) {
            bottomButtonsGrid.render(graphics, mouseX, mouseY);
        }
    }
    
    /**
     * Handles mouse clicks on the bottom buttons.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     * @return True if the click was handled
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (bottomButtonsGrid != null && button == 0) {
            return bottomButtonsGrid.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }
    
    /**
     * Gets the current bottom buttons grid.
     * 
     * @return The bottom buttons grid
     */
    public UIGridBuilder getBottomButtonsGrid() {
        return bottomButtonsGrid;
    }
} 