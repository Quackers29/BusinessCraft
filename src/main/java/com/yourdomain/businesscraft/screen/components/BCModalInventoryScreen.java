package com.yourdomain.businesscraft.screen.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourdomain.businesscraft.menu.StorageMenu;
import com.yourdomain.businesscraft.menu.TradeMenu;
import com.yourdomain.businesscraft.screen.util.InventoryRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.function.Consumer;
import java.util.function.BiFunction;

/**
 * A modal inventory screen that allows for inventory-based interactions
 * within a modal overlay, rather than opening a separate screen.
 * This provides a consistent UI experience with other modal components.
 */
public class BCModalInventoryScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    // Screen properties
    private final Screen parentScreen;
    private final Consumer<BCModalInventoryScreen<T>> onCloseCallback;
    
    // Colors (with defaults)
    private int backgroundColor = 0xD0000000;    // Darker semi-transparent black
    private int borderColor = 0xFFDDDDDD;        // Light gray
    private int titleColor = 0xFFFFFFFF;         // White
    
    // Button properties
    private Button closeButton;
    private String backButtonText = "Back";
    
    // Title scale factor
    private float titleScale = 1.5f;
    
    // InventoryType determines the layout and functionality
    public enum InventoryType {
        TRADE,      // Trade interface with input/output slots
        STORAGE     // Storage interface with multiple item slots
    }
    
    private final InventoryType inventoryType;
    
    // Functions for special actions
    private BiFunction<BlockPos, ItemStack, Boolean> tradeFunction;
    
    /**
     * Constructor for the modal inventory screen
     * 
     * @param title The title to display at the top of the screen
     * @param parentScreen The parent screen to return to when closing
     * @param menu The container menu for this inventory
     * @param playerInventory The player's inventory
     * @param inventoryType The type of inventory to display
     * @param onCloseCallback Optional callback to execute when closing (can be null)
     */
    public BCModalInventoryScreen(
            Component title,
            Screen parentScreen,
            T menu,
            Inventory playerInventory,
            InventoryType inventoryType,
            Consumer<BCModalInventoryScreen<T>> onCloseCallback) {
        super(menu, playerInventory, title);
        this.parentScreen = parentScreen;
        this.inventoryType = inventoryType;
        this.onCloseCallback = onCloseCallback;
        
        // Move the title and inventory label off-screen to hide them (we'll draw our own)
        this.titleLabelX = -9999;
        this.titleLabelY = -9999;
        this.inventoryLabelX = -9999;
        this.inventoryLabelY = -9999;
        
        // Adjust dimensions to match standard inventory screens
        this.imageWidth = 176;
        this.imageHeight = 166;
    }
    
    /**
     * Initialize the screen layout and components
     */
    @Override
    protected void init() {
        super.init();
        
        // Calculate button position at the bottom of our panel
        int buttonX = this.width / 2 - 50;
        int buttonY = this.topPos + this.imageHeight + 5;
        
        // Create close button
        this.closeButton = this.addRenderableWidget(Button.builder(
            Component.literal(backButtonText), 
            button -> this.onClose())
            .pos(buttonX, buttonY)
            .size(100, 20)
            .build()
        );
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
        
        // First render the background container
        renderBg(guiGraphics, partialTicks, mouseX, mouseY);
        
        // Draw our custom title (not using AbstractContainerScreen's title)
        renderTitle(guiGraphics);
        
        // Draw the header separator
        guiGraphics.hLine(
            this.leftPos + 10, 
            this.leftPos + this.imageWidth - 10, 
            this.topPos + 25,
            borderColor
        );
        
        // Draw inventory label
        InventoryRenderer.drawLabel(guiGraphics, this.font, "Inventory", 
                this.leftPos + 8, this.topPos + this.imageHeight - 94);
        
        // Handle foreground rendering (slots, items, etc.)
        super.renderLabels(guiGraphics, mouseX, mouseY);
        
        // Call the super render which will handle slot rendering
        // But skip the label rendering by moving the labels offscreen
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        
        // Render additional UI elements after slot rendering
        renderAdditionalElements(guiGraphics, mouseX, mouseY, partialTicks);
        
        // Render tooltips on top of everything
        renderTooltip(guiGraphics, mouseX, mouseY);
        
        // Render any custom tooltips for UI elements
        renderCustomTooltips(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * Render our custom title
     */
    private void renderTitle(GuiGraphics guiGraphics) {
        // Draw title with scale
        guiGraphics.pose().pushPose();
        
        // Position title in center of panel
        float titleX = this.leftPos + this.imageWidth / 2;
        float titleY = this.topPos + 10;
        
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
    }
    
    /**
     * Override the label rendering to prevent default title from showing
     */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Do nothing - we'll draw our own labels
    }
    
    /**
     * Render additional UI elements based on inventory type
     */
    private void renderAdditionalElements(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (inventoryType == InventoryType.TRADE) {
            renderTradeElements(guiGraphics, mouseX, mouseY);
        } else if (inventoryType == InventoryType.STORAGE) {
            renderStorageElements(guiGraphics, mouseX, mouseY);
        }
    }
    
    /**
     * Render trade-specific UI elements
     */
    private void renderTradeElements(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Only proceed if we have a TradeMenu
        if (!(menu instanceof TradeMenu tradeMenu)) {
            return;
        }
        
        // Draw input and output labels
        InventoryRenderer.drawLabel(guiGraphics, this.font, "Input", 
                this.leftPos + TradeMenu.SLOT_INPUT_X, this.topPos + TradeMenu.SLOT_INPUT_Y - 12);
        
        InventoryRenderer.drawLabel(guiGraphics, this.font, "Output", 
                this.leftPos + TradeMenu.SLOT_OUTPUT_X, this.topPos + TradeMenu.SLOT_OUTPUT_Y - 12);
        
        // Draw arrow between slots
        InventoryRenderer.drawArrow(guiGraphics, 
                this.leftPos + TradeMenu.SLOT_INPUT_X + 20, 
                this.topPos + TradeMenu.SLOT_INPUT_Y + 8,
                this.leftPos + TradeMenu.SLOT_OUTPUT_X - 4, 
                this.topPos + TradeMenu.SLOT_OUTPUT_Y + 8,
                0xFFFFFFFF);
                
        // Draw trade button
        boolean isTradeButtonHovered = isMouseOverTradeButton(mouseX, mouseY);
        InventoryRenderer.drawButton(guiGraphics, 
                this.leftPos + 80, this.topPos + 35, 
                20, 20, 
                "T", this.font, isTradeButtonHovered);
    }
    
    /**
     * Render storage-specific UI elements
     */
    private void renderStorageElements(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // The storage slots are already handled by the AbstractContainerScreen's rendering
    }
    
    /**
     * Check if mouse is over the trade button
     */
    private boolean isMouseOverTradeButton(int mouseX, int mouseY) {
        return InventoryRenderer.isMouseOverElement(mouseX, mouseY, this.leftPos, this.topPos, 
                80, 35, 20, 20);
    }
    
    /**
     * Render custom tooltips for UI elements
     */
    private void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render tooltip for trade button if mouse is over it and it's a trade screen
        if (this.inventoryType == InventoryType.TRADE && isMouseOverTradeButton(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Process the trade"), mouseX, mouseY);
        }
    }

    /**
     * Draw the background of the inventory screen
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // Draw Minecraft-style dark panel background with a semi-transparent overlay
        int x = this.leftPos;
        int y = this.topPos;
        int width = this.imageWidth;
        int height = this.imageHeight;
        
        // Draw panel background (darker, more opaque)
        guiGraphics.fill(x, y, x + width, y + height, backgroundColor);
        
        // Draw panel border
        guiGraphics.hLine(x, x + width - 1, y, borderColor);
        guiGraphics.hLine(x, x + width - 1, y + height - 1, borderColor);
        guiGraphics.vLine(x, y, y + height - 1, borderColor);
        guiGraphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        
        // Draw custom backgrounds for specific inventory types
        if (inventoryType == InventoryType.TRADE) {
            renderTradeBackground(guiGraphics, mouseX, mouseY);
        } else if (inventoryType == InventoryType.STORAGE) {
            renderStorageBackground(guiGraphics, mouseX, mouseY);
        }
        
        // Draw player inventory background and slot borders
        renderPlayerInventorySlots(guiGraphics);
    }
    
    /**
     * Render the player inventory slots with clear borders
     */
    private void renderPlayerInventorySlots(GuiGraphics guiGraphics) {
        // Player inventory area starts at y = imageHeight - 94 typically
        
        // Constants for player inventory layout
        final int HOTBAR_Y = this.topPos + this.imageHeight - 24;
        final int INVENTORY_Y = this.topPos + this.imageHeight - 84;
        final int SLOT_SIZE = 18; // Standard slot size
        final int SLOTS_PER_ROW = 9;
        
        // Draw background for main inventory (3 rows x 9 slots)
        guiGraphics.fill(
            this.leftPos + 7, 
            INVENTORY_Y + 1,
            this.leftPos + 7 + SLOTS_PER_ROW * SLOT_SIZE + 2,
            INVENTORY_Y + 1 + 3 * SLOT_SIZE,
            0x50000000 // Semi-transparent dark background
        );
        
        // Draw border around main inventory
        InventoryRenderer.drawBorder(guiGraphics,
            this.leftPos + 7,
            INVENTORY_Y + 2,
            SLOTS_PER_ROW * SLOT_SIZE + 2,
            3 * SLOT_SIZE,
            InventoryRenderer.INVENTORY_BORDER_COLOR,
            1
        );
        
        // Draw background for hotbar (1 row x 9 slots)
        guiGraphics.fill(
            this.leftPos + 7,
            HOTBAR_Y - 1,
            this.leftPos + 7 + SLOTS_PER_ROW * SLOT_SIZE + 2,
            HOTBAR_Y - 1 + SLOT_SIZE + 2,
            0x50000000 // Semi-transparent dark background
        );
        
        // Draw border around hotbar
        InventoryRenderer.drawBorder(guiGraphics,
            this.leftPos + 7,
            HOTBAR_Y - 1,
            SLOTS_PER_ROW * SLOT_SIZE + 2,
            SLOT_SIZE + 2,
            InventoryRenderer.INVENTORY_BORDER_COLOR,
            1
        );
        
        // Draw individual slot borders for main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int x = this.leftPos + 8 + col * SLOT_SIZE;
                int y = INVENTORY_Y + 2 + row * SLOT_SIZE;
                
                // Draw individual slot
                InventoryRenderer.drawSlot(guiGraphics, x, y, 0x30FFFFFF, 0xFFAAAAAA);
            }
        }
        
        // Draw individual slot borders for hotbar
        for (int col = 0; col < SLOTS_PER_ROW; col++) {
            int x = this.leftPos + 8 + col * SLOT_SIZE;
            int y = HOTBAR_Y;
            
            // Draw individual slot with slightly different color to show it's the hotbar
            InventoryRenderer.drawSlot(guiGraphics, x, y, 0x30FFFFFF, 0xFFBBBBBB);
        }
    }
    
    /**
     * Render trade-specific background elements
     */
    private void renderTradeBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Only proceed if we have a TradeMenu
        if (!(menu instanceof TradeMenu)) {
            return;
        }
        
        // Draw input slot with special background
        InventoryRenderer.drawSlot(guiGraphics, 
                this.leftPos + TradeMenu.SLOT_INPUT_X, this.topPos + TradeMenu.SLOT_INPUT_Y, 
                InventoryRenderer.PRIMARY_COLOR, InventoryRenderer.SLOT_BORDER_COLOR);
        
        // Draw output slot with special background
        InventoryRenderer.drawSlot(guiGraphics, 
                this.leftPos + TradeMenu.SLOT_OUTPUT_X, this.topPos + TradeMenu.SLOT_OUTPUT_Y, 
                InventoryRenderer.SECONDARY_COLOR, InventoryRenderer.SLOT_BORDER_COLOR);
    }
    
    /**
     * Render storage-specific background elements
     */
    private void renderStorageBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Storage grid positions
        final int STORAGE_START_X = 8;
        final int STORAGE_START_Y = 28;
        final int STORAGE_ROWS = 2;
        final int STORAGE_COLS = 9;
        
        // Draw storage grid section background
        guiGraphics.fill(this.leftPos + STORAGE_START_X - 2, this.topPos + STORAGE_START_Y - 2, 
                this.leftPos + STORAGE_START_X + (STORAGE_COLS * InventoryRenderer.SLOT_SIZE) + 2, 
                this.topPos + STORAGE_START_Y + (STORAGE_ROWS * InventoryRenderer.SLOT_SIZE) + 2, 
                0x70000000); // Darker background behind slots
        
        // Draw storage grid border
        InventoryRenderer.drawBorder(guiGraphics, 
                this.leftPos + STORAGE_START_X - 2, this.topPos + STORAGE_START_Y - 2, 
                STORAGE_COLS * InventoryRenderer.SLOT_SIZE + 4, 
                STORAGE_ROWS * InventoryRenderer.SLOT_SIZE + 4, 
                InventoryRenderer.INVENTORY_BORDER_COLOR, 2);
        
        // Draw storage slot backgrounds
        InventoryRenderer.drawSlotGrid(guiGraphics, 
                this.leftPos + STORAGE_START_X, this.topPos + STORAGE_START_Y, 
                STORAGE_COLS, STORAGE_ROWS);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if trade button was clicked (for trade screen only)
        if (button == 0 && this.inventoryType == InventoryType.TRADE && isMouseOverTradeButton((int)mouseX, (int)mouseY)) {
            // Process the trade
            processTrade();
            
            // Play a click sound
            playClickSound();
            return true;
        }
        
        // Let AbstractContainerScreen handle normal inventory interactions
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Process a trade action
     */
    private boolean processTrade() {
        // Only proceed if we have a TradeMenu
        if (!(menu instanceof TradeMenu tradeMenu)) {
            return false;
        }
        
        return tradeMenu.processTrade();
    }
    
    /**
     * Play a button click sound for feedback
     */
    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }
    
    /**
     * Set the trade function for processing trades
     */
    public BCModalInventoryScreen<T> withTradeFunction(BiFunction<BlockPos, ItemStack, Boolean> tradeFunction) {
        this.tradeFunction = tradeFunction;
        return this;
    }
    
    /**
     * Customize the back button text
     */
    public BCModalInventoryScreen<T> withBackButtonText(String text) {
        this.backButtonText = text;
        return this;
    }
    
    /**
     * Customize the title scale
     */
    public BCModalInventoryScreen<T> withTitleScale(float scale) {
        this.titleScale = scale;
        return this;
    }
    
    /**
     * Called when the screen is closed
     */
    @Override
    public void onClose() {
        // Notify callback if provided
        if (this.onCloseCallback != null) {
            this.onCloseCallback.accept(this);
        }
        
        // Return to parent screen
        this.minecraft.setScreen(this.parentScreen);
    }
    
    /**
     * Determine whether the game should be paused when this screen is displayed
     */
    @Override
    public boolean isPauseScreen() {
        // Don't pause the game when this screen is open
        return false;
    }
    
    /**
     * Create a trade inventory screen
     */
    public static <T extends AbstractContainerMenu> BCModalInventoryScreen<T> createTradeScreen(
            Component title,
            Screen parentScreen,
            T menu,
            Inventory playerInventory,
            Consumer<BCModalInventoryScreen<T>> onCloseCallback) {
        
        return new BCModalInventoryScreen<>(
                title,
                parentScreen,
                menu,
                playerInventory,
                InventoryType.TRADE,
                onCloseCallback)
                .withBackButtonText("Back")
                .withTitleScale(1.5f);
    }
    
    /**
     * Create a storage inventory screen
     */
    public static <T extends AbstractContainerMenu> BCModalInventoryScreen<T> createStorageScreen(
            Component title,
            Screen parentScreen,
            T menu,
            Inventory playerInventory,
            Consumer<BCModalInventoryScreen<T>> onCloseCallback) {
        
        return new BCModalInventoryScreen<>(
                title,
                parentScreen,
                menu,
                playerInventory,
                InventoryType.STORAGE,
                onCloseCallback)
                .withBackButtonText("Back")
                .withTitleScale(1.5f);
    }
} 