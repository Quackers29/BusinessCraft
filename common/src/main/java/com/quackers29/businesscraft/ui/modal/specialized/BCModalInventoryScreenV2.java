package com.quackers29.businesscraft.ui.modal.specialized;

import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.menu.TradeMenu;
import com.quackers29.businesscraft.ui.modal.components.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Refactored modal inventory screen using component-based architecture.
 * This class serves as a coordinator for the specialized components:
 * - ModalRenderingEngine: Handles all rendering operations
 * - TradeOperationsManager: Manages trade-specific functionality
 * - StorageOperationsManager: Manages storage-specific functionality  
 * - ModalEventHandler: Handles mouse events and user interactions
 * 
 * This approach follows the Single Responsibility Principle and makes the code
 * much more maintainable and testable compared to the original 1400+ line class.
 */
public class BCModalInventoryScreenV2<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BCModalInventoryScreenV2.class);
    
    // Component managers
    private final ModalRenderingEngine renderingEngine;
    private final TradeOperationsManager tradeManager;
    private final StorageOperationsManager storageManager;
    private final ModalEventHandler eventHandler;
    
    // Screen properties
    private final Screen parentScreen;
    private final Consumer<BCModalInventoryScreenV2<T>> onCloseCallback;
    private final InventoryType inventoryType;
    
    // UI elements
    private Button closeButton;
    private Component displayTitle;
    private String backButtonText = "Back";
    
    /**
     * Inventory type enumeration for different modal behaviors.
     */
    public enum InventoryType {
        TRADE,
        STORAGE,
        TOWN_INTERFACE,
        TOWN_BLOCK
    }
    
    /**
     * Creates a new modal inventory screen with component-based architecture.
     */
    public BCModalInventoryScreenV2(T menu, Inventory playerInventory, Component title, Screen parentScreen,
                                   InventoryType inventoryType, Consumer<BCModalInventoryScreenV2<T>> onCloseCallback) {
        super(menu, playerInventory, title);
        
        this.parentScreen = parentScreen;
        this.inventoryType = inventoryType;
        this.onCloseCallback = onCloseCallback;
        this.displayTitle = title;
        
        // Initialize component managers
        this.renderingEngine = new ModalRenderingEngine();
        this.tradeManager = new TradeOperationsManager(renderingEngine);
        this.storageManager = new StorageOperationsManager(renderingEngine);
        this.eventHandler = new ModalEventHandler(renderingEngine);
        
        // Configure screen properties
        setupScreenDimensions();
        hideVanillaLabels();
        
        LOGGER.debug("Created BCModalInventoryScreenV2 for type: {}", inventoryType);
    }
    
    /**
     * Sets up screen dimensions and positioning.
     */
    private void setupScreenDimensions() {
        this.imageWidth = 176;
        this.imageHeight = 166;
    }
    
    /**
     * Hides vanilla labels by moving them off-screen.
     */
    private void hideVanillaLabels() {
        this.titleLabelX = -9999;
        this.titleLabelY = -9999;
        this.inventoryLabelX = -9999;
        this.inventoryLabelY = -9999;
    }
    
    /**
     * Initialize the screen layout and components.
     */
    @Override
    protected void init() {
        super.init();
        
        // Reset component state
        eventHandler.reset();
        
        // Create close button
        createCloseButton();
        
        // Initialize type-specific components
        initializeTypeSpecificComponents();
        
        LOGGER.debug("Initialized modal screen for type: {}", inventoryType);
    }
    
    /**
     * Creates the close button.
     */
    private void createCloseButton() {
        int buttonX = this.width / 2 - 50;
        int buttonY = this.topPos + this.imageHeight + 5;
        
        this.closeButton = this.addRenderableWidget(Button.builder(
            Component.literal(backButtonText), 
            button -> this.onClose())
            .pos(buttonX, buttonY)
            .size(100, 20)
            .build()
        );
    }
    
    /**
     * Initializes components specific to the inventory type.
     */
    private void initializeTypeSpecificComponents() {
        switch (inventoryType) {
            case STORAGE:
                if (menu instanceof StorageMenu storageMenu) {
                    if (storageMenu.isPersonalStorageMode()) {
                        storageManager.loadPersonalStorageItems(storageMenu);
                    }
                }
                break;
                
            case TRADE:
                // Trade-specific initialization if needed
                break;
                
            case TOWN_INTERFACE:
            case TOWN_BLOCK:
                // Town-specific initialization if needed
                break;
        }
    }
    
    /**
     * Main render method coordinating all rendering operations.
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Render modal background
        renderModalBackground(guiGraphics);
        
        // Render the main panel
        renderMainPanel(guiGraphics, partialTicks, mouseX, mouseY);
        
        // Render inventory slots and items
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        
        // Render type-specific elements
        renderTypeSpecificElements(guiGraphics, mouseX, mouseY, partialTicks);
        
        // Render tooltips and overlays
        renderTooltipsAndOverlays(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * Renders the modal background.
     */
    private void renderModalBackground(GuiGraphics guiGraphics) {
        if (parentScreen != null) {
            this.renderBackground(guiGraphics);
            renderingEngine.renderModalBackground(guiGraphics, this.width, this.height, true);
        }
    }
    
    /**
     * Renders the main panel background and title.
     */
    private void renderMainPanel(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // Render panel background first
        renderBg(guiGraphics, partialTicks, mouseX, mouseY);
        
        // Render custom title
        renderingEngine.renderTitle(guiGraphics, this.font, displayTitle, this.leftPos, this.topPos, this.imageWidth);
        
        // Render header separator
        renderingEngine.renderSeparator(guiGraphics, this.leftPos, this.topPos, this.imageWidth, 25);
        
        // Render inventory label
        renderingEngine.renderLabel(guiGraphics, this.font, "Inventory", 
                                   this.leftPos + 8, this.topPos + this.imageHeight - 94, 0xFFDDDDDD);
    }
    
    /**
     * Renders elements specific to the inventory type.
     */
    private void renderTypeSpecificElements(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        switch (inventoryType) {
            case TRADE:
                if (menu instanceof TradeMenu tradeMenu) {
                    tradeManager.renderTradeElements(guiGraphics, this.font, tradeMenu, 
                                                   this.leftPos, this.topPos, this.imageWidth, mouseX, mouseY);
                }
                break;
                
            case STORAGE:
                if (menu instanceof StorageMenu storageMenu) {
                    storageManager.renderStorageElements(guiGraphics, this.font, storageMenu,
                                                       this.leftPos, this.topPos, this.imageWidth, mouseX, mouseY);
                }
                break;
                
            case TOWN_INTERFACE:
            case TOWN_BLOCK:
                // Town-specific rendering if needed
                renderTownElements(guiGraphics, mouseX, mouseY);
                break;
        }
    }
    
    /**
     * Renders town-specific elements (placeholder for future expansion).
     */
    private void renderTownElements(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Placeholder for town-specific rendering
        // Could render town status, population info, etc.
    }
    
    /**
     * Renders tooltips and overlay elements.
     */
    private void renderTooltipsAndOverlays(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render standard tooltips
        renderTooltip(guiGraphics, mouseX, mouseY);
        
        // Render custom tooltips and event overlays
        eventHandler.renderCustomTooltips(guiGraphics, this.font, mouseX, mouseY);
        
        // Render type-specific tooltips
        renderTypeSpecificTooltips(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * Renders tooltips specific to the inventory type.
     */
    private void renderTypeSpecificTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String tooltip = null;
        
        switch (inventoryType) {
            case TRADE:
                if (menu instanceof TradeMenu tradeMenu) {
                    tooltip = tradeManager.getTradeTooltip(tradeMenu, mouseX, mouseY, this.leftPos, this.topPos);
                }
                break;
                
            case STORAGE:
                if (menu instanceof StorageMenu storageMenu) {
                    tooltip = storageManager.getStorageTooltip(storageMenu, mouseX, mouseY, this.leftPos, this.topPos);
                }
                break;
        }
        
        if (tooltip != null) {
            eventHandler.setCustomTooltip(tooltip, mouseX, mouseY);
        }
    }
    
    /**
     * Handles mouse click events.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Let event handler process the click first
        if (eventHandler.handleMouseClick(mouseX, mouseY, button)) {
            return true;
        }
        
        // Handle type-specific clicks
        if (handleTypeSpecificClick(mouseX, mouseY, button)) {
            return true;
        }
        
        // Fall back to superclass handling
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Handles type-specific mouse clicks.
     */
    private boolean handleTypeSpecificClick(double mouseX, double mouseY, int button) {
        switch (inventoryType) {
            case TRADE:
                if (menu instanceof TradeMenu tradeMenu) {
                    // Calculate trade button position (this would need to be coordinated with TradeOperationsManager)
                    int buttonX = this.leftPos + 58; // Approximate position
                    int buttonY = this.topPos + 50;
                    return tradeManager.handleTradeButtonClick(tradeMenu, (int) mouseX, (int) mouseY, buttonX, buttonY);
                }
                break;
                
            case STORAGE:
                if (menu instanceof StorageMenu storageMenu) {
                    return storageManager.handleStorageModeToggle(storageMenu, (int) mouseX, (int) mouseY, 
                                                                this.leftPos, this.topPos + 30);
                }
                break;
        }
        
        return false;
    }
    
    /**
     * Handles mouse drag events.
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (eventHandler.handleMouseDrag(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    /**
     * Handles mouse release events.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (eventHandler.handleMouseRelease(mouseX, mouseY, button)) {
            return true;
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    /**
     * Handles slot click events.
     */
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (eventHandler.handleSlotClick(slot, slotId, mouseButton, clickType)) {
            return; // Event was handled by our custom logic
        }
        
        // Fall back to superclass handling
        super.slotClicked(slot, slotId, mouseButton, clickType);
    }
    
    /**
     * Handles keyboard input.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (eventHandler.handleKeyPressed(keyCode, scanCode, modifiers)) {
            // Close the screen if requested by event handler
            if (keyCode == 256 || keyCode == 69) { // Escape or E key
                this.onClose();
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    /**
     * Handles screen closure.
     */
    @Override
    public void onClose() {
        // Execute callback before switching screens
        if (onCloseCallback != null) {
            onCloseCallback.accept(this);
        }
        
        // Switch back to parent screen
        if (parentScreen != null) {
            minecraft.setScreen(parentScreen);
        } else {
            super.onClose();
        }
    }
    
    /**
     * Renders the background (overridden to use our rendering engine).
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        renderingEngine.renderPanelBackground(guiGraphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
    }
    
    // Configuration methods
    
    /**
     * Sets the back button text.
     */
    public void setBackButtonText(String text) {
        this.backButtonText = text;
        if (closeButton != null) {
            closeButton.setMessage(Component.literal(text));
        }
    }
    
    /**
     * Sets the display title.
     */
    public void setDisplayTitle(Component title) {
        this.displayTitle = title;
    }
    
    /**
     * Sets custom rendering colors.
     */
    public void setColors(int backgroundColor, int borderColor, int titleColor, int overlayColor) {
        renderingEngine.setColors(backgroundColor, borderColor, titleColor, overlayColor);
    }
    
    // Component access methods (for advanced usage)
    
    /**
     * Gets the rendering engine for advanced rendering operations.
     */
    public ModalRenderingEngine getRenderingEngine() {
        return renderingEngine;
    }
    
    /**
     * Gets the trade manager for advanced trade operations.
     */
    public TradeOperationsManager getTradeManager() {
        return tradeManager;
    }
    
    /**
     * Gets the storage manager for advanced storage operations.
     */
    public StorageOperationsManager getStorageManager() {
        return storageManager;
    }
    
    /**
     * Gets the event handler for advanced event processing.
     */
    public ModalEventHandler getEventHandler() {
        return eventHandler;
    }
}
