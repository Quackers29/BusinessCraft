package com.yourdomain.businesscraft.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.screen.components.*;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.screen.tabs.ResourcesTab;
import com.yourdomain.businesscraft.screen.tabs.OverviewTab;
import com.yourdomain.businesscraft.screen.tabs.PopulationTab;
import com.yourdomain.businesscraft.screen.tabs.SettingsTab;
import com.yourdomain.businesscraft.screen.managers.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Town Interface Screen showcases the BusinessCraft UI system capabilities.
 * This screen demonstrates various UI components and layouts using the enhanced BCTabPanel.
 */
public class TownInterfaceScreen extends AbstractContainerScreen<TownInterfaceMenu> {
    private BCTabPanel tabPanel;
    private BCTheme customTheme;

    // Use centralized theme constants
    private static final int PRIMARY_COLOR = TownInterfaceTheme.PRIMARY_COLOR;
    private static final int SECONDARY_COLOR = TownInterfaceTheme.SECONDARY_COLOR;
    private static final int BACKGROUND_COLOR = TownInterfaceTheme.BACKGROUND_COLOR;
    private static final int BORDER_COLOR = TownInterfaceTheme.BORDER_COLOR;
    private static final int ACTIVE_TAB_COLOR = TownInterfaceTheme.ACTIVE_TAB_COLOR;
    private static final int INACTIVE_TAB_COLOR = TownInterfaceTheme.INACTIVE_TAB_COLOR;
    private static final int TEXT_COLOR = TownInterfaceTheme.TEXT_COLOR;
    private static final int TEXT_HIGHLIGHT = TownInterfaceTheme.TEXT_HIGHLIGHT;
    private static final int SUCCESS_COLOR = TownInterfaceTheme.SUCCESS_COLOR;
    private static final int DANGER_COLOR = TownInterfaceTheme.DANGER_COLOR;
    
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

    // Add a field to track update intervals
    private int updateCounter = 0;
    private static final int REFRESH_INTERVAL = 100; // Ticks between forced cache refreshes (5 seconds)

    // Consolidated cache management
    private TownDataCacheManager cacheManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(TownInterfaceScreen.class);

    public TownInterfaceScreen(TownInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        
        // Set custom dimensions for the screen
        this.imageWidth = 256;
        this.imageHeight = 204;
        
        // Move the inventory label off-screen to hide it
        this.inventoryLabelY = 300;  // Position it below the visible area
        
        // Initialize cached values from the menu
        this.currentSearchRadius = menu.getSearchRadius();
        
        // Initialize the cache manager with the town data provider from the menu
        TownDataCache dataCache = null;
        if (menu instanceof TownInterfaceMenu) {
            ITownDataProvider dataProvider = menu.getTownDataProvider();
            if (dataProvider != null) {
                dataCache = new TownDataCache(dataProvider);
            }
        }
        this.cacheManager = new TownDataCacheManager(dataCache, menu);
        
        // Create a custom theme for this screen using the centralized theme
        customTheme = TownInterfaceTheme.createBCTheme();
    }

    @Override
    protected void init() {
        super.init();
        
        // Apply our custom theme for this screen
        BCTheme.setActiveTheme(customTheme);
        
        // Initialize the cached values
        currentSearchRadius = menu.getSearchRadius();
        
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
        // Create a new OverviewTab instance
        OverviewTab overviewTab = new OverviewTab(
            this, 
            this.tabPanel.getWidth(), 
            this.tabPanel.getHeight() - 20
        );
        
        // Initialize the tab
        overviewTab.init(this::addRenderableWidget);
        
        // Add the tab to the tab panel
        this.tabPanel.addTab("overview", Component.literal("Overview"), overviewTab.getPanel());
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
        // Create a new PopulationTab instance
        PopulationTab populationTab = new PopulationTab(
            this, 
            this.tabPanel.getWidth(), 
            this.tabPanel.getHeight() - 20
                    );
                    
        // Initialize the tab
        populationTab.init(this::addRenderableWidget);
                    
        // Add the tab to the tab panel
        this.tabPanel.addTab("population", Component.literal("Population"), populationTab.getPanel());
    }
    
    private void createSettingsTab() {
        // Create a new SettingsTab instance
        SettingsTab settingsTab = new SettingsTab(
            this, 
            this.tabPanel.getWidth(), 
            this.tabPanel.getHeight() - 20
        );
        
        // Initialize the tab
        settingsTab.init(this::addRenderableWidget);
        
        // Add the tab to the tab panel
        this.tabPanel.addTab("settings", Component.literal("Settings"), settingsTab.getPanel());
    }
    
    /**
     * Opens the platform management screen
     * This method is used by tab implementations.
     */
    public void openPlatformManagementScreen() {
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
     * This method is used by tab implementations.
     */
    public void handleRadiusChange(int mouseButton) {
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
        cacheManager.updateCachedSearchRadius(newRadius);
        
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
    
    /**
     * Gets the current search radius for UI display
     * @return The current search radius
     */
    public int getCurrentSearchRadius() {
        return currentSearchRadius;
    }
    
    // Helper methods to get data from the cache manager
    public String getCachedTownName() {
        return cacheManager.getCachedTownName();
    }
    
    public int getCachedPopulation() {
        return cacheManager.getCachedPopulation();
    }
    
    // Changed from private to public to allow access from tab implementations
    public Map<Item, Integer> getCachedResources() {
        return cacheManager.getCachedResources();
    }
    
    public String getTouristString() {
        return cacheManager.getTouristString();
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Update the cached values periodically
        updateCounter++;
        if (updateCounter >= REFRESH_INTERVAL) { // Every 5 seconds (100 ticks)
            updateCounter = 0;
            
            // Refresh cache if available
            if (cacheManager != null) {
                cacheManager.invalidateCache();
                cacheManager.refreshCachedValues();
                this.currentSearchRadius = cacheManager.getLocalCachedSearchRadius();
            } else {
                // Fallback to direct menu updates if cache isn't available
                this.currentSearchRadius = menu.getSearchRadius();
            }
            
            // Update the active tab if exists
            String activeTabId = this.tabPanel.getActiveTabId();
            if ("overview".equals(activeTabId)) {
                // Get the panel, which should be created by our OverviewTab
                BCPanel panel = this.tabPanel.getTabPanel(activeTabId);
                if (panel != null && panel instanceof BCPanel) {
                    // Find the OverviewTab instance
                    for (UIComponent child : panel.getChildren()) {
                        if (child instanceof BCComponent) {
                            // Force a refresh of the component
                            child.setVisible(false);
                            child.setVisible(true);
                        }
                    }
                }
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
     * This method is used by tab implementations.
     */
    public void playButtonClickSound() {
        Minecraft.getInstance().getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }
    
    /**
     * Helper method to send a chat message to the player
     * This method is used by tab implementations.
     */
    public void sendChatMessage(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        }
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
        
        // Special case for scrollable tabs (resources and population)
        if ("resources".equals(activeTabId) || "population".equals(activeTabId)) {
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
        
        // Let the tab panel handle scroll - this will properly forward to our tabs
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
     * Show the change town name popup
     */
    private void showChangeTownNamePopup() {
        activePopup = TownNamePopupManager.showChangeTownNamePopup(
            getCachedTownName(),
            menu.getBlockPos(),
            popup -> activePopup = null
        );
        
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
        activeModal = VisitorModalManager.showVisitorListModal(
            modal -> activeModal = null
        );
    }

    /**
     * Shows the trade resources modal screen with input and output slots
     */
    private void showTradeResourcesModal() {
        TradeModalManager.showTradeResourcesModal(
            this,
            this.menu.getBlockPos(),
            screen -> {
                // Optional callback when screen is closed
                // We can use this to refresh data if needed
            }
        );
    }
    
    /**
     * Shows the storage modal screen with 2x9 chest-like storage
     */
    private void showStorageModal() {
        StorageModalManager.showStorageModal(
            this,
            this.menu.getBlockPos(),
            this.menu,
            screen -> {
                // Optional callback when screen is closed
                // We can use this to refresh data if needed
            }
        );
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
        VisitorHistoryManager.showVisitorHistoryScreen(
            this,
            menu.getBlockPos(),
            this.tabPanel,
            screen -> {
                // Optional callback when screen is closed
            }
        );
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