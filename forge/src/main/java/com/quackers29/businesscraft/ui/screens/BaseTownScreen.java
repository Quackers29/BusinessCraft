package com.quackers29.businesscraft.ui.screens;

import com.quackers29.businesscraft.ui.templates.TownInterfaceTheme;
import com.quackers29.businesscraft.ui.templates.BCTheme;
import com.quackers29.businesscraft.ui.interfaces.ScreenRenderingCapabilities;
import com.quackers29.businesscraft.ui.interfaces.ScreenEventCapabilities;
// TownInterfaceMenu has been migrated to common module
// import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.ui.modal.core.BCModalScreen;
import com.quackers29.businesscraft.ui.modal.core.BCPopupScreen;
import com.quackers29.businesscraft.ui.components.containers.BCTabPanel;
import com.quackers29.businesscraft.ui.managers.*;
import com.quackers29.businesscraft.data.cache.TownDataCache;
import com.quackers29.businesscraft.api.ITownDataProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;
import java.util.function.Consumer;

/**
 * Base class for town-related screens providing common functionality.
 * Extracted from TownInterfaceScreen to improve code reusability.
 * Refactored to use composite interfaces and reduce coupling.
 */
public abstract class BaseTownScreen<T extends com.quackers29.businesscraft.menu.TownInterfaceMenu> extends AbstractContainerScreen<T> 
        implements ScreenRenderingCapabilities,
                   ScreenEventCapabilities,
                   TownTabController.TabDataProvider {
    
    // Static field to preserve active tab across screen switches
    private static String lastActiveTab = "overview";
    
    // Theme access - use static constants directly from TownInterfaceTheme
    // No need for instance since it's a utility class
    
    // Common screen components
    protected BCTabPanel tabPanel;
    protected BCTheme customTheme;
    
    // Modal and popup management
    protected BCPopupScreen activePopup = null;
    protected BCModalScreen activeModal = null;
    
    // Manager instances - common to all town screens
    protected TownScreenEventHandler eventHandler;
    protected TownScreenRenderManager renderManager;
    protected TownTabController tabController;
    
    // Cache management
    protected int updateCounter = 0;
    protected static final int REFRESH_INTERVAL = 600; // Ticks between forced cache refreshes (30 seconds - reduced from 5s after fixing sync issues)
    protected TownDataCacheManager cacheManager;
    
    // Resource cleanup management
    private boolean isCleanedUp = false;
    
    // Logger for all town screens
    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseTownScreen.class);
    
    /**
     * Creates a new base town screen.
     * 
     * @param menu The menu instance
     * @param inventory Player inventory
     * @param title Screen title
     */
    public BaseTownScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        
        // Set standard dimensions for town screens
        this.imageWidth = 256;
        this.imageHeight = 204;
        
        // Move the inventory label off-screen to hide it
        this.inventoryLabelY = 300;  // Position it below the visible area
        
        // Initialize cache management
        initializeCacheManager();
        
        // Create custom theme
        customTheme = TownInterfaceTheme.createBCTheme();
        
        // Initialize common managers
        initializeManagers();
        
        // Initialize specific managers for subclasses
        initializeSpecificManagers();
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "BaseTownScreen initialized: {}", this.getClass().getSimpleName());
    }
    
    /**
     * Initializes the cache manager with town data.
     */
    protected void initializeCacheManager() {
        TownDataCache dataCache = null;
        if (menu instanceof com.quackers29.businesscraft.menu.TownInterfaceMenu) {
            ITownDataProvider dataProvider = ((com.quackers29.businesscraft.menu.TownInterfaceMenu) menu).getTownDataProvider();
            if (dataProvider != null) {
                dataCache = new TownDataCache(dataProvider);
            }
        }
        this.cacheManager = new TownDataCacheManager(dataCache, menu);
    }
    
    /**
     * Initializes common managers. Subclasses can override to customize.
     */
    protected void initializeManagers() {
        // Event handler and render manager are initialized by subclasses
        // since they need specific button action handlers
        this.tabController = new TownTabController(this);
    }
    
    /**
     * Template method for subclasses to initialize their specific managers.
     * Called after common managers are initialized.
     */
    protected abstract void initializeSpecificManagers();
    
    @Override
    protected void init() {
        super.init();
        
        // Apply our custom theme for this screen
        BCTheme.setActiveTheme(customTheme);
        
        // Initialize tabs using the tab controller
        this.tabPanel = tabController.initializeTabs();
        
        // Restore the last active tab after initialization
        restoreActiveTab();
        
        // Allow subclasses to perform additional initialization
        performAdditionalInit();
    }
    
    /**
     * Template method for subclasses to perform additional initialization.
     */
    protected abstract void performAdditionalInit();
    
    /**
     * Saves the currently active tab for restoration after screen switches.
     */
    public void saveActiveTab() {
        if (tabPanel != null && tabPanel.getActiveTabId() != null) {
            lastActiveTab = tabPanel.getActiveTabId();
            DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "Saved active tab: {}", lastActiveTab);
        }
    }
    
    /**
     * Restores the previously saved active tab.
     */
    private void restoreActiveTab() {
        if (tabPanel != null && lastActiveTab != null) {
            tabPanel.setActiveTab(lastActiveTab);
            DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "Restored active tab: {}", lastActiveTab);
        }
    }
    
    // ===== Common Interface Implementations =====
    
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
    
    // CacheUpdateProvider implementation
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
    
    // ScreenLayoutProvider implementation
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
    
    // TabDataProvider implementation
    @Override
    public int getScreenPadding() {
        return 10; // Standard screen padding
    }
    
    @Override
    public void addRenderableWidget(Button widget) {
        super.addRenderableWidget(widget);
    }
    
    /**
     * Returns this screen as the tab content provider.
     * Subclasses can override if they need to provide a different implementation.
     */
    @Override
    public Object getTabContentProvider() {
        return this;
    }
    
    // ===== Common Rendering =====
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Delegate all rendering to the render manager
        if (renderManager != null) {
            renderManager.render(graphics, mouseX, mouseY, partialTicks);
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // Background rendering is handled in render() method
    }
    
    // ===== Common Event Handling =====
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (eventHandler != null) {
            return eventHandler.handleMouseClicked(mouseX, mouseY, button, 
                b -> super.mouseClicked(mouseX, mouseY, b));
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (eventHandler != null) {
            return eventHandler.handleMouseDragged(mouseX, mouseY, button, dragX, dragY,
                b -> super.mouseDragged(mouseX, mouseY, b, dragX, dragY));
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (eventHandler != null) {
            return eventHandler.handleMouseReleased(mouseX, mouseY, button,
                b -> super.mouseReleased(mouseX, mouseY, b));
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (eventHandler != null) {
            return eventHandler.handleMouseScrolled(mouseX, mouseY, delta,
                d -> super.mouseScrolled(mouseX, mouseY, d));
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (eventHandler != null) {
            return eventHandler.handleKeyPressed(keyCode, scanCode, modifiers,
                k -> super.keyPressed(k, scanCode, modifiers));
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char c, int modifiers) {
        if (eventHandler != null) {
            return eventHandler.handleCharTyped(c, modifiers,
                ch -> super.charTyped(ch, modifiers));
        }
        return super.charTyped(c, modifiers);
    }
    
    // ===== Common Utility Methods =====
    
    /**
     * Helper method to send a chat message to the player.
     * This method is used by tab implementations and subclasses.
     */
    public void sendChatMessage(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        }
    }
    
    /**
     * Gets the menu instance. Provides protected access to coordinators.
     * 
     * @return The menu instance
     */
    public T getMenu() {
        return this.menu;
    }
    
    /**
     * Closes the active modal if one exists.
     */
    protected void closeActiveModal() {
        this.activeModal = null;
    }
    
    // ===== Lifecycle Management =====
    
    @Override
    public void onClose() {
        if (!isCleanedUp) {
            try {
                performSafeCleanup();
            } catch (Exception e) {
                LOGGER.error("Error during screen cleanup", e);
            } finally {
                isCleanedUp = true;
                super.onClose();
            }
        }
    }
    
    /**
     * Performs safe cleanup of all resources.
     */
    private void performSafeCleanup() {
        DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "Starting cleanup for {}", this.getClass().getSimpleName());
        
        // Clean up managers
        cleanupManagers();
        
        // Clean up modals and popups
        cleanupUIComponents();
        
        // Allow subclasses to perform additional cleanup
        performAdditionalCleanup();
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "Cleanup completed for {}", this.getClass().getSimpleName());
    }
    
    /**
     * Safely cleans up all manager instances.
     */
    private void cleanupManagers() {
        safeCleanup(tabController, "TabController", TownTabController::cleanup);
        // Note: RenderManager and EventHandler don't currently have cleanup methods
        // but this provides a place to add them in the future if needed
        safeCleanup(renderManager, "RenderManager", mgr -> {
            DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "RenderManager cleanup placeholder");
        });
        safeCleanup(eventHandler, "EventHandler", mgr -> {
            DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "EventHandler cleanup placeholder");
        });
    }
    
    /**
     * Cleans up UI components like modals and popups.
     */
    private void cleanupUIComponents() {
        if (activeModal != null) {
            DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "Closing active modal");
            activeModal = null;
        }
        
        if (activePopup != null) {
            DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "Closing active popup");
            activePopup = null;
        }
    }
    
    /**
     * Safely executes cleanup actions with error handling.
     * 
     * @param resource The resource to clean up
     * @param resourceName Name for logging
     * @param cleanupAction The cleanup action to perform
     */
    private <T> void safeCleanup(T resource, String resourceName, Consumer<T> cleanupAction) {
        if (resource != null) {
            try {
                cleanupAction.accept(resource);
                DebugConfig.debug(LOGGER, DebugConfig.UI_BASE_SCREEN, "{} cleanup completed", resourceName);
            } catch (Exception e) {
                LOGGER.warn("Failed to cleanup {}: {}", resourceName, e.getMessage());
            }
        }
    }
    
    /**
     * Template method for subclasses to perform additional cleanup.
     */
    protected abstract void performAdditionalCleanup();
} 