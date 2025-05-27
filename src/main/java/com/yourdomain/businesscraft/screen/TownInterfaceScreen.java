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
import net.minecraft.client.gui.Font;
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
public class TownInterfaceScreen extends AbstractContainerScreen<TownInterfaceMenu> 
        implements BottomButtonManager.ButtonActionHandler, 
                   TownScreenEventHandler.SoundHandler,
                   TownScreenEventHandler.ModalStateProvider,
                   TownScreenRenderManager.CacheUpdateProvider,
                   TownScreenRenderManager.ScreenLayoutProvider,
                   TownScreenRenderManager.ComponentProvider,
                   TownTabController.TabDataProvider {
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
    
    // State tracking for toggle buttons
    private boolean pvpEnabled = false;
    private boolean publicTownEnabled = true;

    // Add a field for the active popup
    private BCPopupScreen activePopup = null;

    // Add field for modal screen
    private BCModalScreen activeModal = null;
    
    // Manager instances
    private BottomButtonManager buttonManager;
    private TownScreenEventHandler eventHandler;
    private TownScreenRenderManager renderManager;
    private TownTabController tabController;
    
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
        
        // Initialize managers
        this.buttonManager = new BottomButtonManager(this);
        this.eventHandler = new TownScreenEventHandler(this, this, this.buttonManager);
        this.renderManager = new TownScreenRenderManager(this, this, this);
        this.tabController = new TownTabController(this);
    }
    
    // ===== Interface Implementations =====
    
    // ButtonActionHandler implementation
    @Override
    public void onEditDetails() {
        showChangeTownNamePopup();
    }
    
    @Override
    public void onViewVisitors() {
        showVisitorListModal();
    }
    
    @Override
    public void onTradeResources() {
        showTradeResourcesModal();
    }
    
    @Override
    public void onManageStorage() {
        showStorageModal();
    }
    
    @Override
    public void onAssignJobs() {
        sendChatMessage("Button pressed: Assign Jobs");
    }
    
    @Override
    public void onViewVisitorHistory() {
        showVisitorHistoryScreen();
    }
    
    @Override
    public void onSaveSettings() {
        sendChatMessage("Button pressed: Save Settings");
    }
    
    @Override
    public void onResetDefaults() {
        sendChatMessage("Button pressed: Reset Defaults");
    }
    
    @Override
    public void onManagePlatforms() {
        openPlatformManagementScreen();
    }
    
    @Override
    public void onGenericAction(String action) {
        sendChatMessage("Button pressed: " + action);
    }
    
    // SoundHandler implementation
    @Override
    public void playButtonClickSound() {
        Minecraft.getInstance().getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }
    
    // ModalStateProvider implementation
    @Override
    public BCModalScreen getActiveModal() {
        return activeModal;
    }
    
    @Override
    public BCPopupScreen getActivePopup() {
        return activePopup;
    }
    
    @Override
    public BCTabPanel getTabPanel() {
        return tabPanel;
    }
    
    // ===== CacheUpdateProvider Implementation =====
    
    @Override
    public TownDataCacheManager getCacheManager() {
        return cacheManager;
    }
    
    @Override
    public int getUpdateCounter() {
        return updateCounter;
    }
    
    @Override
    public void setUpdateCounter(int counter) {
        this.updateCounter = counter;
    }
    
    @Override
    public int getRefreshInterval() {
        return REFRESH_INTERVAL;
    }
    
    @Override
    public void setCurrentSearchRadius(int radius) {
        this.currentSearchRadius = radius;
    }
    
    @Override
    public int getSearchRadiusFromMenu() {
        return menu.getSearchRadius();
    }
    
    // ===== ScreenLayoutProvider Implementation =====
    
    @Override
    public int getLeftPos() {
        return this.leftPos;
    }
    
    @Override
    public int getTopPos() {
        return this.topPos;
    }
    
    @Override
    public int getImageWidth() {
        return this.imageWidth;
    }
    
    @Override
    public int getImageHeight() {
        return this.imageHeight;
    }
    
    @Override
    public Font getFont() {
        return this.font;
    }
    
    @Override
    public Component getTitle() {
        return this.title;
    }
    
    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);
    }
    
    @Override
    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
    }
    
    // ===== ComponentProvider Implementation =====
    
    @Override
    public BottomButtonManager getButtonManager() {
        return buttonManager;
    }
    
    // ===== TabDataProvider Implementation =====
    
    @Override
    public int getScreenPadding() {
        return 10; // Standard screen padding
    }
    
    @Override
    public void addRenderableWidget(Button widget) {
        super.addRenderableWidget(widget);
    }
    
    @Override
    public TownInterfaceScreen getTabContentProvider() {
        return this;
    }

    @Override
    protected void init() {
        super.init();
        
        // Apply our custom theme for this screen
        BCTheme.setActiveTheme(customTheme);
        
        // Initialize the cached values
        currentSearchRadius = menu.getSearchRadius();
        
        // Initialize tabs using the tab controller
        this.tabPanel = tabController.initializeTabs();
        
        // Initialize the button manager with screen position
        buttonManager.updateScreenPosition(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        buttonManager.createBottomButtonsGrid();
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
        // Delegate all rendering to the render manager
        renderManager.render(graphics, mouseX, mouseY, partialTicks);
    }
    

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return eventHandler.handleMouseClicked(mouseX, mouseY, button, 
            b -> super.mouseClicked(mouseX, mouseY, b));
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
        return eventHandler.handleMouseDragged(mouseX, mouseY, button, dragX, dragY,
            b -> super.mouseDragged(mouseX, mouseY, b, dragX, dragY));
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return eventHandler.handleMouseReleased(mouseX, mouseY, button,
            b -> super.mouseReleased(mouseX, mouseY, b));
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return eventHandler.handleMouseScrolled(mouseX, mouseY, delta,
            d -> super.mouseScrolled(mouseX, mouseY, d));
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
        return eventHandler.handleKeyPressed(keyCode, scanCode, modifiers,
            k -> super.keyPressed(k, scanCode, modifiers));
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return eventHandler.handleCharTyped(c, modifiers,
            ch -> super.charTyped(ch, modifiers));
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
        // Clean up tab controller resources
        if (tabController != null) {
            tabController.cleanup();
        }
        
        // Send a packet to register the player exit UI to show platform indicators
        BlockPos blockPos = this.menu.getBlockPos();
        if (blockPos != null) {
            // Send a packet to the server to register player exit UI
            ModMessages.sendToServer(new PlayerExitUIPacket(blockPos));
        }
        
        super.onClose();
    }
} 