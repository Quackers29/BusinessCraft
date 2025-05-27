package com.yourdomain.businesscraft.screen;

import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.screen.components.BCModalScreen;
import com.yourdomain.businesscraft.screen.components.BCPopupScreen;
import com.yourdomain.businesscraft.screen.components.BCTabPanel;
import com.yourdomain.businesscraft.screen.components.BCTheme;
import com.yourdomain.businesscraft.screen.managers.*;
import com.yourdomain.businesscraft.data.cache.TownDataCache;
import com.yourdomain.businesscraft.api.ITownDataProvider;
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

/**
 * Base class for town-related screens providing common functionality.
 * Extracted from TownInterfaceScreen to improve code reusability.
 */
public abstract class BaseTownScreen<T extends TownInterfaceMenu> extends AbstractContainerScreen<T> 
        implements TownScreenEventHandler.SoundHandler,
                   TownScreenEventHandler.ModalStateProvider,
                   TownScreenRenderManager.CacheUpdateProvider,
                   TownScreenRenderManager.ScreenLayoutProvider,
                   TownScreenRenderManager.ComponentProvider,
                   TownTabController.TabDataProvider {
    
    // Theme constants - centralized for all town screens
    protected static final int PRIMARY_COLOR = TownInterfaceTheme.PRIMARY_COLOR;
    protected static final int SECONDARY_COLOR = TownInterfaceTheme.SECONDARY_COLOR;
    protected static final int BACKGROUND_COLOR = TownInterfaceTheme.BACKGROUND_COLOR;
    protected static final int BORDER_COLOR = TownInterfaceTheme.BORDER_COLOR;
    protected static final int ACTIVE_TAB_COLOR = TownInterfaceTheme.ACTIVE_TAB_COLOR;
    protected static final int INACTIVE_TAB_COLOR = TownInterfaceTheme.INACTIVE_TAB_COLOR;
    protected static final int TEXT_COLOR = TownInterfaceTheme.TEXT_COLOR;
    protected static final int TEXT_HIGHLIGHT = TownInterfaceTheme.TEXT_HIGHLIGHT;
    protected static final int SUCCESS_COLOR = TownInterfaceTheme.SUCCESS_COLOR;
    protected static final int DANGER_COLOR = TownInterfaceTheme.DANGER_COLOR;
    
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
    protected static final int REFRESH_INTERVAL = 100; // Ticks between forced cache refreshes (5 seconds)
    protected TownDataCacheManager cacheManager;
    
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
    }
    
    /**
     * Initializes the cache manager with town data.
     */
    protected void initializeCacheManager() {
        TownDataCache dataCache = null;
        if (menu instanceof TownInterfaceMenu) {
            ITownDataProvider dataProvider = ((TownInterfaceMenu) menu).getTownDataProvider();
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
        
        // Allow subclasses to perform additional initialization
        performAdditionalInit();
    }
    
    /**
     * Template method for subclasses to perform additional initialization.
     */
    protected abstract void performAdditionalInit();
    
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
     * Closes the active modal if one exists.
     */
    protected void closeActiveModal() {
        this.activeModal = null;
    }
    
    // ===== Lifecycle Management =====
    
    @Override
    public void onClose() {
        // Clean up tab controller resources
        if (tabController != null) {
            tabController.cleanup();
        }
        
        // Allow subclasses to perform additional cleanup
        performAdditionalCleanup();
        
        super.onClose();
    }
    
    /**
     * Template method for subclasses to perform additional cleanup.
     */
    protected abstract void performAdditionalCleanup();
} 