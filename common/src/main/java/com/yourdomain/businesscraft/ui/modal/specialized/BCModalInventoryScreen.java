package com.yourdomain.businesscraft.ui.modal.specialized;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourdomain.businesscraft.menu.StorageMenu;
import com.yourdomain.businesscraft.menu.TradeMenu;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.packets.storage.PersonalStorageRequestPacket;
import com.yourdomain.businesscraft.network.packets.storage.CommunalStoragePacket;
import com.yourdomain.businesscraft.ui.util.InventoryRenderer;
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
import net.minecraft.world.item.Item;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.function.Consumer;
import java.util.function.BiFunction;
import java.util.UUID;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.debug.DebugConfig;

/**
 * A modal inventory screen that allows for inventory-based interactions
 * within a modal overlay, rather than opening a separate screen.
 * This provides a consistent UI experience with other modal components.
 * 
 * @deprecated This class has been refactored into component-based architecture.
 *             Use BCModalInventoryScreenV2 instead, which provides the same functionality
 *             with improved maintainability through focused components:
 *             - ModalRenderingEngine: Handles all rendering operations
 *             - TradeOperationsManager: Manages trade-specific functionality
 *             - StorageOperationsManager: Manages storage-specific functionality
 *             - ModalEventHandler: Handles mouse events and user interactions
 */
@Deprecated
public class BCModalInventoryScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BCModalInventoryScreen.class);
    
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
    
    // Storage mode labels
    private static final String COMMUNAL_LABEL = "Communal";
    private static final String PERSONAL_LABEL = "Personal";
    
    // Toggle button coordinates for storage mode
    private static final int TOGGLE_BUTTON_X = 136;
    private static final int TOGGLE_BUTTON_Y = 6;
    private static final int TOGGLE_BUTTON_WIDTH = 32;
    private static final int TOGGLE_BUTTON_HEIGHT = 20;
    
    // Display title that can be updated (unlike the final 'title' field inherited from Screen)
    private Component displayTitle;
    
    // InventoryType determines the layout and functionality
    public enum InventoryType {
        TRADE,      // Trade interface with input/output slots
        STORAGE     // Storage interface with multiple item slots
    }
    
    private final InventoryType inventoryType;
    
    // Functions for special actions
    private BiFunction<BlockPos, ItemStack, Boolean> tradeFunction;
    
    // Keep track of all slots affected during a drag operation
    private final java.util.Set<Integer> affectedDragSlots = new java.util.HashSet<>();
    
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
        this.displayTitle = title; // Initialize display title with the original title
        
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
        
        // If this is a storage screen, initialize with the correct mode data
        if (this.inventoryType == InventoryType.STORAGE && 
            this.menu instanceof StorageMenu storageMenu) {
            if (storageMenu.isPersonalStorageMode()) {
                // Load personal storage items if in personal mode
                loadPersonalStorageItems(storageMenu);
            }
        }
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
        int titleWidth = this.font.width(this.displayTitle);
        float scale = this.titleScale;
        
        // Adjust title position
        int titleX = (int)((this.width / 2) - (titleWidth * scale / 2));
        int titleY = this.topPos - 22;
        
        // Push matrix to apply transformations
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(titleX, titleY, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        
        // Draw title with shadow
        guiGraphics.drawString(this.font, this.displayTitle, 0, 0, titleColor, true);
        
        // Pop matrix to restore original state
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
        
        // Enhanced input section with better styling
        renderTradeInputSection(guiGraphics, mouseX, mouseY);
        
        // Enhanced output section with better styling
        renderTradeOutputSection(guiGraphics, mouseX, mouseY);
        
        // Enhanced trade processing area
        renderTradeProcessingArea(guiGraphics, mouseX, mouseY);
        
        // Trade information panel
        renderTradeInfoPanel(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * Render the enhanced input section
     */
    private void renderTradeInputSection(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int inputX = this.leftPos + TradeMenu.SLOT_INPUT_X;
        int inputY = this.topPos + TradeMenu.SLOT_INPUT_Y;
        
        // Draw input section background with gradient effect
        guiGraphics.fill(inputX - 8, inputY - 8, inputX + 24, inputY + 24, 0x40336699);
        guiGraphics.fill(inputX - 7, inputY - 7, inputX + 23, inputY + 23, 0x20FFFFFF);
        
        // Draw input label with better styling - positioned above the slot
        String inputLabel = "Sell";
        int labelWidth = this.font.width(inputLabel);
        int labelX = inputX + 8 - (labelWidth / 2); // Center the label above the slot
        guiGraphics.drawString(this.font, inputLabel, labelX, inputY - 10, 0xFFFFAA00, true);
        
        // Draw helpful hint below input slot - positioned below the slot
        String hintText = "Items";
        int hintWidth = this.font.width(hintText);
        int hintX = inputX + 8 - (hintWidth / 2); // Center the hint below the slot
        guiGraphics.drawString(this.font, hintText, hintX, inputY + 18, 0xFFAAAAAA, false);
    }
    
    /**
     * Render the enhanced output section
     */
    private void renderTradeOutputSection(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int outputX = this.leftPos + TradeMenu.SLOT_OUTPUT_X;
        int outputY = this.topPos + TradeMenu.SLOT_OUTPUT_Y;
        
        // Draw output section background with different color
        guiGraphics.fill(outputX - 8, outputY - 8, outputX + 24, outputY + 24, 0x40669933);
        guiGraphics.fill(outputX - 7, outputY - 7, outputX + 23, outputY + 23, 0x20FFFFFF);
        
        // Draw output label - positioned above the slot
        String outputLabel = "Earn";
        int labelWidth = this.font.width(outputLabel);
        int labelX = outputX + 8 - (labelWidth / 2); // Center the label above the slot
        guiGraphics.drawString(this.font, outputLabel, labelX, outputY - 10, 0xFF00FF00, true);
        
        // Draw helpful hint below output slot - positioned below the slot
        String hintText = "Emeralds";
        int hintWidth = this.font.width(hintText);
        int hintX = outputX + 8 - (hintWidth / 2); // Center the hint below the slot
        guiGraphics.drawString(this.font, hintText, hintX, outputY + 18, 0xFFAAAAAA, false);
    }
    
    /**
     * Render the enhanced trade processing area
     */
    private void renderTradeProcessingArea(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int centerX = this.leftPos + 88; // Center of the screen
        int centerY = this.topPos + 65;  // Below the slots
        
        // Draw enhanced trade button centered below the slots
        boolean isTradeButtonHovered = isMouseOverTradeButton(mouseX, mouseY);
        renderEnhancedTradeButton(guiGraphics, centerX - 16, centerY, isTradeButtonHovered);
    }
    

    
    /**
     * Render enhanced trade button with better styling and animation
     */
    private void renderEnhancedTradeButton(GuiGraphics guiGraphics, int x, int y, boolean isHovered) {
        // Check if there are items ready to trade for animation
        boolean hasItemsToTrade = false;
        if (menu instanceof TradeMenu tradeMenu) {
            hasItemsToTrade = !tradeMenu.getSlot(0).getItem().isEmpty();
        }
        
        // Calculate pulsing effect for when items are ready
        float pulseIntensity = 1.0f;
        if (hasItemsToTrade) {
            long time = System.currentTimeMillis();
            pulseIntensity = 1.0f + 0.2f * (float) Math.sin(time * 0.005);
        }
        
        int buttonColor = isHovered ? 0xFF66AA44 : 0xFF448822;
        int borderColor = isHovered ? 0xFF88CC66 : 0xFF66AA44;
        
        // Apply pulse effect to colors when items are ready
        if (hasItemsToTrade && !isHovered) {
            int pulseGreen = (int) (0x44 + (0x22 * pulseIntensity));
            buttonColor = 0xFF000000 | (pulseGreen << 8) | 0x22;
            borderColor = 0xFF000000 | (Math.min(255, pulseGreen + 0x22) << 8) | 0x44;
        }
        
        // Make button appropriately sized
        int buttonWidth = 32;
        int buttonHeight = 16;
        
        // Draw button background with gradient
        guiGraphics.fill(x, y, x + buttonWidth, y + buttonHeight, buttonColor);
        guiGraphics.fill(x + 1, y + 1, x + buttonWidth - 1, y + buttonHeight / 2, 0x40FFFFFF);
        
        // Draw button border
        InventoryRenderer.drawBorder(guiGraphics, x, y, buttonWidth, buttonHeight, borderColor, 1);
        
        // Draw button text centered
        String buttonText = "TRADE";
        int textWidth = this.font.width(buttonText);
        int textX = x + (buttonWidth - textWidth) / 2; // Center horizontally
        int textY = y + (buttonHeight - 8) / 2; // Center vertically
        
        if (hasItemsToTrade) {
            guiGraphics.drawString(this.font, buttonText, textX, textY, 0xFFFFFFFF, true);
        } else {
            guiGraphics.drawString(this.font, buttonText, textX, textY, 0xFF999999, true);
        }
        
        // Add hover glow effect
        if (isHovered) {
            guiGraphics.fill(x - 1, y - 1, x + buttonWidth + 1, y + buttonHeight + 1, 0x20FFFFFF);
        }
        
        // Add ready-to-trade glow effect
        if (hasItemsToTrade && !isHovered) {
            int glowAlpha = (int) (0x10 + (0x10 * pulseIntensity));
            guiGraphics.fill(x - 2, y - 2, x + buttonWidth + 2, y + buttonHeight + 2, (glowAlpha << 24) | 0x00FF00);
        }
    }
    
    /**
     * Render enhanced trade information panel with status indicator
     */
    private void renderTradeInfoPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Get current input item for rate calculation
        ItemStack inputStack = ItemStack.EMPTY;
        if (menu instanceof TradeMenu tradeMenu) {
            inputStack = tradeMenu.getSlot(0).getItem();
        }
        
        // Draw info panel background with dynamic color based on trade readiness
        int panelX = this.leftPos + 8;
        int panelY = this.topPos + 8;
        int panelWidth = 160;
        int panelHeight = 20;
        
        boolean hasItemsToTrade = !inputStack.isEmpty();
        int panelBgColor = hasItemsToTrade ? 0x80004400 : 0x60000000;
        int borderColor = hasItemsToTrade ? 0xFF448822 : 0xFF666666;
        
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, panelBgColor);
        InventoryRenderer.drawBorder(guiGraphics, panelX, panelY, panelWidth, panelHeight, borderColor, 1);
        
        // Draw status indicator
        int indicatorX = panelX + panelWidth - 12;
        int indicatorY = panelY + 6;
        int indicatorSize = 8;
        
        if (hasItemsToTrade) {
            // Green ready indicator with pulse
            long time = System.currentTimeMillis();
            float pulse = 0.8f + 0.2f * (float) Math.sin(time * 0.008);
            int pulseAlpha = (int) (255 * pulse);
            guiGraphics.fill(indicatorX, indicatorY, indicatorX + indicatorSize, indicatorY + indicatorSize, 
                    (pulseAlpha << 24) | 0x00FF00);
        } else {
            // Gray not ready indicator
            guiGraphics.fill(indicatorX, indicatorY, indicatorX + indicatorSize, indicatorY + indicatorSize, 0xFF666666);
        }
        
        // Display trade rate information
        if (hasItemsToTrade) {
            String itemName = inputStack.getHoverName().getString();
            int itemCount = inputStack.getCount();
            int emeraldsEarned = Math.max(1, itemCount / 10); // 10 items = 1 emerald
            
            // Shorten item name to fit better
            String shortName = itemName.length() > 6 ? itemName.substring(0, 6) + "..." : itemName;
            String rateText = String.format("%d %s = %d emerald%s", 
                    itemCount, shortName, emeraldsEarned, emeraldsEarned == 1 ? "" : "s");
            
            // Draw just the trade rate text
            guiGraphics.drawString(this.font, rateText, panelX + 4, panelY + 6, 0xFFFFFFFF, false);
        } else {
            guiGraphics.drawString(this.font, "Add items to sell", 
                    panelX + 4, panelY + 6, 0xFFAAAAAA, false);
        }
    }
    
    /**
     * Render storage-specific UI elements
     */
    private void renderStorageElements(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // The storage slots are already handled by the AbstractContainerScreen's rendering
    }
    
    /**
     * Check if mouse is over the enhanced trade button
     */
    private boolean isMouseOverTradeButton(int mouseX, int mouseY) {
        int centerX = this.leftPos + 88; // Center of the screen
        int centerY = this.topPos + 65;  // Below the slots
        int buttonX = centerX - 16;
        int buttonY = centerY;
        int buttonWidth = 32;
        int buttonHeight = 16;
        
        return mouseX >= buttonX && mouseX < buttonX + buttonWidth && 
               mouseY >= buttonY && mouseY < buttonY + buttonHeight;
    }
    
    /**
     * Render custom tooltips for UI elements
     */
    private void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // For trade screen, add enhanced tooltip for the trade button
        if (this.inventoryType == InventoryType.TRADE && isMouseOverTradeButton(mouseX, mouseY)) {
            ItemStack inputStack = ItemStack.EMPTY;
            if (menu instanceof TradeMenu tradeMenu) {
                inputStack = tradeMenu.getSlot(0).getItem();
            }
            
            if (!inputStack.isEmpty()) {
                int itemCount = inputStack.getCount();
                int emeraldsEarned = Math.max(1, itemCount / 10);
                String tooltipText = String.format("Click to trade!\nGet: %d emerald%s", 
                        emeraldsEarned, emeraldsEarned == 1 ? "" : "s");
                guiGraphics.renderTooltip(this.font, Component.literal(tooltipText), mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(this.font, Component.literal("Add items first"), mouseX, mouseY);
            }
        }
        
        // For storage screen, add tooltip for the storage mode toggle button
        if (this.inventoryType == InventoryType.STORAGE && isMouseOverStorageModeToggle(mouseX, mouseY) && 
            this.menu instanceof StorageMenu storageMenu) {
            String currentMode = storageMenu.isPersonalStorageMode() ? COMMUNAL_LABEL : PERSONAL_LABEL;
            String nextMode = storageMenu.isPersonalStorageMode() ? PERSONAL_LABEL : COMMUNAL_LABEL;
            guiGraphics.renderTooltip(this.font, 
                Component.literal("Current: " + currentMode + "\nClick to switch to " + nextMode), 
                mouseX, mouseY);
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
        
        // Draw enhanced input slot with blue theme
        int inputX = this.leftPos + TradeMenu.SLOT_INPUT_X;
        int inputY = this.topPos + TradeMenu.SLOT_INPUT_Y;
        InventoryRenderer.drawSlot(guiGraphics, inputX, inputY, 0x60336699, 0xFF4477BB);
        
        // Add subtle glow around input slot
        guiGraphics.fill(inputX - 2, inputY - 2, inputX + 18, inputY + 18, 0x20336699);
        
        // Draw enhanced output slot with green theme
        int outputX = this.leftPos + TradeMenu.SLOT_OUTPUT_X;
        int outputY = this.topPos + TradeMenu.SLOT_OUTPUT_Y;
        InventoryRenderer.drawSlot(guiGraphics, outputX, outputY, 0x60669933, 0xFF77BB44);
        
        // Add subtle glow around output slot
        guiGraphics.fill(outputX - 2, outputY - 2, outputX + 18, outputY + 18, 0x20669933);
        
        // Draw connecting flow line between slots
        renderTradeFlowLine(guiGraphics, inputX + 16, inputY + 8, outputX, outputY + 8);
    }
    
    /**
     * Render a flowing connection line between input and output with animation
     */
    private void renderTradeFlowLine(GuiGraphics guiGraphics, int startX, int startY, int endX, int endY) {
        // Check if there are items to trade for flow animation
        boolean hasItemsToTrade = false;
        if (menu instanceof TradeMenu tradeMenu) {
            hasItemsToTrade = !tradeMenu.getSlot(0).getItem().isEmpty();
        }
        
        // Draw main flow line with different opacity based on trade readiness
        int lineOpacity = hasItemsToTrade ? 0xC0DDDDDD : 0x60DDDDDD;
        guiGraphics.fill(startX, startY - 1, endX, startY + 1, lineOpacity);
        
        // Add animated flow particles
        if (hasItemsToTrade) {
            long time = System.currentTimeMillis();
            int particleSpacing = 12;
            float animationOffset = (time * 0.01f) % particleSpacing;
            
            for (float x = startX + 4 + animationOffset; x < endX - 4; x += particleSpacing) {
                int particleX = (int) x;
                
                // Create flowing particle effect with varying opacity
                float distanceFromStart = (particleX - startX) / (float) (endX - startX);
                int particleAlpha = (int) (255 * (0.5f + 0.5f * Math.sin(time * 0.01 + distanceFromStart * 6)));
                int particleColor = (particleAlpha << 24) | 0xFFFFFF;
                
                guiGraphics.fill(particleX, startY - 1, particleX + 2, startY + 1, particleColor);
            }
        } else {
            // Static particles when no items to trade
            int particleSpacing = 12;
            for (int x = startX + 6; x < endX - 4; x += particleSpacing) {
                guiGraphics.fill(x, startY - 1, x + 1, startY + 1, 0x80AAAAAA);
            }
        }
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
        
        // Draw the toggle button for storage mode if this is a storage menu
        if (this.menu instanceof StorageMenu storageMenu) {
            boolean isToggleButtonHovered = isMouseOverStorageModeToggle(mouseX, mouseY);
            InventoryRenderer.drawButton(guiGraphics, 
                    this.leftPos + TOGGLE_BUTTON_X, this.topPos + TOGGLE_BUTTON_Y, 
                    TOGGLE_BUTTON_WIDTH, TOGGLE_BUTTON_HEIGHT, 
                    storageMenu.isPersonalStorageMode() ? "P" : "C", this.font, isToggleButtonHovered);
        }
    }
    
    /**
     * Check if mouse is over the storage mode toggle button
     */
    private boolean isMouseOverStorageModeToggle(int mouseX, int mouseY) {
        return mouseX >= this.leftPos + TOGGLE_BUTTON_X && 
               mouseX < this.leftPos + TOGGLE_BUTTON_X + TOGGLE_BUTTON_WIDTH && 
               mouseY >= this.topPos + TOGGLE_BUTTON_Y && 
               mouseY < this.topPos + TOGGLE_BUTTON_Y + TOGGLE_BUTTON_HEIGHT;
    }
    
    /**
     * Helper method to get the StorageMenu from the current menu
     * 
     * @return The StorageMenu if available, null otherwise
     */
    private StorageMenu getStorageMenu() {
        if (this.inventoryType == InventoryType.STORAGE && 
            this.menu instanceof StorageMenu storageMenu) {
            return storageMenu;
        }
        return null;
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
        
        // Check if storage mode toggle button was clicked (for storage screen only)
        if (button == 0 && this.inventoryType == InventoryType.STORAGE && 
            isMouseOverStorageModeToggle((int)mouseX, (int)mouseY) && 
            this.menu instanceof StorageMenu storageMenu) {
            
            // Toggle storage mode
            boolean isPersonal = storageMenu.toggleStorageMode();
            
            // Play a click sound
            playClickSound();
            
            // Update the display title based on the storage mode
            this.displayTitle = Component.literal("Town " + (isPersonal ? "Personal" : "Communal") + " Storage");
            
            // Refresh the storage data based on the new mode
            if (isPersonal) {
                loadPersonalStorageItems(storageMenu);
            } else {
                loadCommunalStorageItems(storageMenu);
            }
            
            return true;
        }
        
        // Let AbstractContainerScreen handle normal inventory interactions
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Helper method to load personal storage items
     */
    private void loadPersonalStorageItems(StorageMenu storageMenu) {
        if (this.minecraft != null && this.minecraft.player != null) {
            // Get personal storage data for the current player
            UUID playerId = this.minecraft.player.getUUID();
            try {
                BlockPos townPos = storageMenu.getTownBlockPos();
                if (townPos != null) {
                    // First, request personal storage data from the server
                    DebugConfig.debug(LOGGER, DebugConfig.STORAGE_OPERATIONS, "Requesting personal storage data for player {} at position {}", playerId, townPos);
                    ModMessages.sendToServer(new PersonalStorageRequestPacket(townPos, playerId));
                    
                    // Then try to fetch from local cache as a fallback
                    var townMenu = storageMenu.getTownInterfaceMenu();
                    if (townMenu != null) {
                        // Get the personal storage items through the town data provider
                        var townDataProvider = townMenu.getTownDataProvider();
                        Map<Item, Integer> personalItems = townDataProvider != null ? 
                            townDataProvider.getPersonalStorageItems(playerId) : java.util.Collections.emptyMap();
                        
                        // Update with personal storage items
                        storageMenu.updatePersonalStorageItems(personalItems);
                        DebugConfig.debug(LOGGER, DebugConfig.STORAGE_OPERATIONS, "Updated personal storage display with {} items from local cache", personalItems.size());
                    } else {
                        LOGGER.warn("Could not access TownInterfaceMenu for personal storage");
                    }
                } else {
                    LOGGER.warn("No town position available for personal storage request");
                }
            } catch (Exception e) {
                LOGGER.error("Error loading personal storage items", e);
            }
        }
    }
    
    /**
     * Helper method to load communal storage items
     */
    private void loadCommunalStorageItems(StorageMenu storageMenu) {
        if (this.minecraft != null && this.minecraft.player != null) {
            try {
                // Get the town menu to access storage data
                var townMenu = storageMenu.getTownInterfaceMenu();
                if (townMenu != null) {
                    // Update with communal storage items through the town data provider or menu method
                    Map<Item, Integer> communalItems = townMenu.getAllCommunalStorageItems();
                    storageMenu.updateStorageItems(communalItems);
                } else {
                    LOGGER.warn("Could not access TownInterfaceMenu for communal storage");
                }
            } catch (Exception e) {
                LOGGER.error("Error loading communal storage items", e);
            }
        }
    }
    
    /**
     * Override slotClicked to handle custom slot behavior for storage and trade
     */
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        StorageMenu storageMenu = getStorageMenu();
        if (storageMenu == null) {
            super.slotClicked(slot, slotId, mouseButton, type);
            return;
        }
        
        boolean isPersonalMode = storageMenu.isPersonalStorageMode();
        
        // Handle double-click to collect all items of same type (DOUBLE_CLICK)
        if (type == ClickType.PICKUP && mouseButton == 0 && 
            !this.menu.getCarried().isEmpty() && slot == null) {
            
            // This is a special case - double-click collecting operation
            // Track the item before the operation
            ItemStack carriedBefore = this.menu.getCarried().copy();
            
            // Let Minecraft handle the double-click collection
            super.slotClicked(slot, slotId, mouseButton, type);
            
            // Find out what changed by comparing carried item before and after
            ItemStack carriedAfter = this.menu.getCarried();
            int itemsCollected = carriedAfter.getCount() - carriedBefore.getCount();
            
            if (itemsCollected > 0) {
                LOGGER.debug("Collected {} items via double-click", itemsCollected);
                
                // Request updated storage data to keep the backend in sync
                BlockPos townPos = storageMenu.getTownBlockPos();
                if (townPos != null && this.minecraft != null && this.minecraft.player != null) {
                    UUID playerId = this.minecraft.player.getUUID();
                    if (isPersonalMode) {
                        // Request personal storage data refresh
                        ModMessages.sendToServer(new PersonalStorageRequestPacket(townPos, playerId));
                    } else {
                        // Request communal storage data refresh
                        ModMessages.sendToServer(new CommunalStoragePacket(townPos, ItemStack.EMPTY, -1, true));
                    }
                }
            }
            
            return;
        }
        
        // Handle dragging items across slots (QUICK_CRAFT)
        if (type == ClickType.QUICK_CRAFT) {
            // Track drag stages for both left-click and right-click dragging:
            // Left-click dragging (distribute evenly):
            //   mouseButton == 0: start dragging
            //   mouseButton == 1: add slot
            //   mouseButton == 2: end dragging
            // Right-click dragging (place one per slot):
            //   mouseButton == 4: start dragging
            //   mouseButton == 5: add slot
            //   mouseButton == 6: end dragging
            
            // If we're starting a drag operation, clear the affected slots set
            if (mouseButton == 0 || mouseButton == 4) {
                this.affectedDragSlots.clear();
            }
            
            // If we're adding a slot to the drag operation, track it
            if ((mouseButton == 1 || mouseButton == 5) && slot != null) {
                // Only track storage slots (within the storage grid)
                // Storage is always first 18 slots (2x9 grid)
                if (slotId < 18) {
                    this.affectedDragSlots.add(slotId);
                }
            }
            
            // Let Minecraft handle the drag operation
            super.slotClicked(slot, slotId, mouseButton, type);
            
            // If this is the end of a drag operation
            if (mouseButton == 2 || mouseButton == 6) {
                // Process all affected slots individually
                BlockPos townPos = storageMenu.getTownBlockPos();
                if (townPos != null && this.minecraft != null && this.minecraft.player != null) {
                    boolean anySlotUpdated = false;
                    
                    LOGGER.debug("Processing {} affected slots after drag operation", affectedDragSlots.size());
                    for (int affectedSlotId : affectedDragSlots) {
                        Slot affectedSlot = this.menu.slots.get(affectedSlotId);
                        if (affectedSlot != null) {
                            ItemStack slotStack = affectedSlot.getItem();
                            if (!slotStack.isEmpty()) {
                                // Process the storage add operation based on mode
                                if (isPersonalMode) {
                                    storageMenu.processPersonalStorageAdd(this.minecraft.player, affectedSlotId, slotStack.copy());
                                } else {
                                    storageMenu.processCommunalStorageAdd(this.minecraft.player, affectedSlotId, slotStack.copy());
                                }
                                anySlotUpdated = true;
                            }
                        }
                    }
                    
                    // Request a full refresh only if we didn't update any slots
                    if (!anySlotUpdated && !affectedDragSlots.isEmpty()) {
                        if (isPersonalMode) {
                            // Request personal storage data refresh
                            ModMessages.sendToServer(new PersonalStorageRequestPacket(townPos, this.minecraft.player.getUUID()));
                        } else {
                            // Request communal storage data refresh
                            ModMessages.sendToServer(new CommunalStoragePacket(townPos, ItemStack.EMPTY, -1, true));
                        }
                    }
                    
                    // Clear the affected slots for the next operation
                    this.affectedDragSlots.clear();
                }
            }
            
            return;
        }
        
        // Check if it's a storage slot (in the 2x9 grid)
        if (slot != null && slotId < 18) {
            // If player is adding an item to storage (left click with carried item)
            if (type == ClickType.PICKUP && mouseButton == 0 && !this.menu.getCarried().isEmpty()) {
                // Store information about the state before clicking
                ItemStack carriedBefore = this.menu.getCarried().copy();
                ItemStack slotBefore = slot.getItem().isEmpty() ? ItemStack.EMPTY : slot.getItem().copy();
                
                // Let the standard handling happen first
                super.slotClicked(slot, slotId, mouseButton, type);
                
                // Calculate what actually changed
                ItemStack carriedAfter = this.menu.getCarried();
                ItemStack slotAfter = slot.getItem();
                
                // Only send packet if items were actually added to storage
                if (slotAfter.getCount() > slotBefore.getCount() || 
                    (slotBefore.isEmpty() && !slotAfter.isEmpty())) {
                    
                    // Calculate how many items were actually added
                    int itemsAdded;
                    if (slotBefore.isEmpty()) {
                        // New stack was created
                        itemsAdded = slotAfter.getCount();
                    } else {
                        // Existing stack was increased
                        itemsAdded = slotAfter.getCount() - slotBefore.getCount();
                    }
                    
                    // If items were actually added, send the packet with the correct item count
                    if (itemsAdded > 0) {
                        ItemStack itemToAdd = slotAfter.copy();
                        itemToAdd.setCount(itemsAdded);
                        
                        DebugConfig.debug(LOGGER, DebugConfig.STORAGE_OPERATIONS, "Adding {} items to storage at slot {}", itemsAdded, slotId);
                        
                        // Process the storage add operation based on mode
                        if (isPersonalMode) {
                            storageMenu.processPersonalStorageAdd(this.minecraft.player, slotId, itemToAdd);
                        } else {
                            storageMenu.processCommunalStorageAdd(this.minecraft.player, slotId, itemToAdd);
                        }
                    }
                }
                // Handle the case where we swapped different items (not just adding more of the same type)
                else if (!ItemStack.isSameItemSameTags(slotBefore, slotAfter) && 
                        !slotBefore.isEmpty() && !slotAfter.isEmpty()) {
                    
                    LOGGER.debug("Swapping different items in slot {}: {} -> {}", 
                        slotId, slotBefore.getHoverName().getString(), slotAfter.getHoverName().getString());
                    
                    // First, remove the original item from storage
                    if (isPersonalMode) {
                        storageMenu.processPersonalStorageRemove(this.minecraft.player, slotId, slotBefore);
                    } else {
                        storageMenu.processCommunalStorageRemove(this.minecraft.player, slotId, slotBefore);
                    }
                    
                    // Then, add the new item to storage
                    if (isPersonalMode) {
                        storageMenu.processPersonalStorageAdd(this.minecraft.player, slotId, slotAfter.copy());
                    } else {
                        storageMenu.processCommunalStorageAdd(this.minecraft.player, slotId, slotAfter.copy());
                    }
                }
                return;
            }
            // If player is right-clicking to split a stack in storage
            else if (type == ClickType.PICKUP && mouseButton == 1 && !slot.getItem().isEmpty()) {
                // Store information about the state before clicking
                ItemStack slotBefore = slot.getItem().copy();
                int countBefore = slotBefore.getCount();
                
                // Let the standard handling happen first
                super.slotClicked(slot, slotId, mouseButton, type);
                
                // After splitting, get the remaining items in the slot
                ItemStack slotAfter = slot.getItem();
                ItemStack carriedAfter = this.menu.getCarried();
                
                // Calculate how many items were actually removed
                int itemsRemoved = carriedAfter.getCount();
                
                LOGGER.debug("Right-click split: {} items remaining in slot, {} items in hand", 
                    slotAfter.getCount(), carriedAfter.getCount());
                
                // Create a copy of the item stack with the split amount
                ItemStack splitStack = slotBefore.copy();
                splitStack.setCount(itemsRemoved);
                
                // Process the storage remove operation based on mode
                if (isPersonalMode) {
                    storageMenu.processPersonalStorageRemove(this.minecraft.player, slotId, splitStack);
                } else {
                    storageMenu.processCommunalStorageRemove(this.minecraft.player, slotId, splitStack);
                }
                
                return;
            }
            // If player is removing an item from storage (left click on item in slot)
            else if (type == ClickType.PICKUP && mouseButton == 0 && !slot.getItem().isEmpty()) {
                // Get a copy of the item before it's removed
                var itemStack = slot.getItem().copy();
                
                // Let the standard handling happen first
                super.slotClicked(slot, slotId, mouseButton, type);
                
                // Then process the storage remove operation based on mode
                if (isPersonalMode) {
                    storageMenu.processPersonalStorageRemove(this.minecraft.player, slotId, itemStack);
                } else {
                    storageMenu.processCommunalStorageRemove(this.minecraft.player, slotId, itemStack);
                }
                return;
            }

            // Add a new case for right-click placing a single item from carried stack
            // If player is right-clicking with carried item to place just one item
            else if (type == ClickType.PICKUP && mouseButton == 1 && !this.menu.getCarried().isEmpty()) {
                // Store information about the state before clicking
                ItemStack carriedBefore = this.menu.getCarried().copy();
                ItemStack slotBefore = slot.getItem().isEmpty() ? ItemStack.EMPTY : slot.getItem().copy();
                
                // Let the standard handling happen first
                super.slotClicked(slot, slotId, mouseButton, type);
                
                // Calculate what actually changed
                ItemStack carriedAfter = this.menu.getCarried();
                ItemStack slotAfter = slot.getItem();
                
                // Only send packet if items were actually added to storage
                if (slotAfter.getCount() > slotBefore.getCount() || 
                    (slotBefore.isEmpty() && !slotAfter.isEmpty())) {
                    
                    // Calculate how many items were actually added (should be 1 for right-click)
                    int itemsAdded;
                    if (slotBefore.isEmpty()) {
                        // New stack was created
                        itemsAdded = slotAfter.getCount();
                    } else {
                        // Existing stack was increased
                        itemsAdded = slotAfter.getCount() - slotBefore.getCount();
                    }
                    
                    // If items were actually added, send the packet with the correct item count
                    if (itemsAdded > 0) {
                        ItemStack itemToAdd = slotAfter.copy();
                        itemToAdd.setCount(itemsAdded);
                        
                        LOGGER.debug("Right-click adding {} items to storage at slot {}", itemsAdded, slotId);
                        
                        // Process the storage add operation based on mode
                        if (isPersonalMode) {
                            storageMenu.processPersonalStorageAdd(this.minecraft.player, slotId, itemToAdd);
                        } else {
                            storageMenu.processCommunalStorageAdd(this.minecraft.player, slotId, itemToAdd);
                        }
                    }
                }
                return;
            }
        }
        
        // Handle shift-clicking (QUICK_MOVE) to properly track moved items
        if (type == ClickType.QUICK_MOVE && slot != null && storageMenu != null) {
            boolean isStorageSlot = slotId < 18; // First 18 slots are storage slots
            
            // Store the state before the quick move
            ItemStack slotBefore = slot.getItem().copy();
            
            // Let the standard handling happen
            super.slotClicked(slot, slotId, mouseButton, type);
            
            // Store the state after the quick move
            ItemStack slotAfter = slot.getItem().copy();
            
            // If moving from storage to player inventory
            if (isStorageSlot && !slotBefore.isEmpty() && (slotAfter.isEmpty() || slotAfter.getCount() < slotBefore.getCount())) {
                // Calculate how many items were actually moved
                int itemsMoved;
                if (slotAfter.isEmpty()) {
                    // Full stack was moved
                    itemsMoved = slotBefore.getCount();
                } else {
                    // Partial stack was moved
                    itemsMoved = slotBefore.getCount() - slotAfter.getCount();
                }
                
                LOGGER.debug("Shift-click removed {} items from storage slot {}", itemsMoved, slotId);
                
                // Create a copy of the item stack with the moved count
                ItemStack itemsToRemove = slotBefore.copy();
                itemsToRemove.setCount(itemsMoved);
                
                // Process the storage remove operation based on mode
                if (isPersonalMode) {
                    storageMenu.processPersonalStorageRemove(this.minecraft.player, slotId, itemsToRemove);
                } else {
                    storageMenu.processCommunalStorageRemove(this.minecraft.player, slotId, itemsToRemove);
                }
            }
            // If moving from player inventory to storage
            else if (!isStorageSlot && !slotBefore.isEmpty()) {
                LOGGER.debug("Shift-click from player inventory slot {} to storage", slotId);
                
                // Capture the storage state before the operation
                ItemStack[] storageSlotsBefore = new ItemStack[18]; // 18 storage slots
                for (int i = 0; i < 18; i++) {
                    Slot storageSlot = this.menu.slots.get(i);
                    storageSlotsBefore[i] = storageSlot.hasItem() ? storageSlot.getItem().copy() : ItemStack.EMPTY;
                }
                
                // Let the standard handling happen
                super.slotClicked(slot, slotId, mouseButton, type);
                
                // Now compare storage slots before and after to see what changed
                boolean anySlotUpdated = false;
                for (int i = 0; i < 18; i++) {
                    Slot storageSlot = this.menu.slots.get(i);
                    ItemStack storageSlotAfter = storageSlot.hasItem() ? storageSlot.getItem().copy() : ItemStack.EMPTY;
                    ItemStack storageSlotBefore = storageSlotsBefore[i];
                    
                    // Case 1: Existing stack size increased (same item type)
                    if (!storageSlotBefore.isEmpty() && !storageSlotAfter.isEmpty() && 
                        ItemStack.isSameItemSameTags(storageSlotBefore, storageSlotAfter)) {
                        
                        if (storageSlotAfter.getCount() > storageSlotBefore.getCount()) {
                            // Normal case - stack was increased
                            int itemsAdded = storageSlotAfter.getCount() - storageSlotBefore.getCount();
                            
                            // Create a copy with just the added items
                            ItemStack itemsToAdd = storageSlotAfter.copy();
                            itemsToAdd.setCount(itemsAdded);
                            
                            // Process the storage add operation based on mode
                            if (isPersonalMode) {
                                storageMenu.processPersonalStorageAdd(this.minecraft.player, i, itemsToAdd);
                            } else {
                                storageMenu.processCommunalStorageAdd(this.minecraft.player, i, itemsToAdd);
                            }
                            anySlotUpdated = true;
                        } 
                        else if (storageSlotAfter.getCount() == storageSlotBefore.getCount() && 
                                 ItemStack.isSameItemSameTags(storageSlotAfter, slotBefore)) {
                            // Special case: Same count but potentially a new item instance replacing the old one
                            // This happens when Minecraft replaces the item instead of stacking it
                            
                            // Check if this slot already received items in this operation
                            boolean slotAlreadyUpdated = false;
                            for (int j = 0; j < i; j++) {
                                if (affectedDragSlots.contains(j)) {
                                    Slot prevSlot = this.menu.slots.get(j);
                                    if (ItemStack.isSameItemSameTags(prevSlot.getItem(), storageSlotAfter)) {
                                        slotAlreadyUpdated = true;
                                        break;
                                    }
                                }
                            }
                                    
                            if (!slotAlreadyUpdated) {
                                // Get the itemstack that was shift-clicked
                                ItemStack itemToAdd = slotBefore.copy();
                                
                                // Process the server update
                                if (isPersonalMode) {
                                    storageMenu.processPersonalStorageAdd(this.minecraft.player, i, itemToAdd);
                                } else {
                                    storageMenu.processCommunalStorageAdd(this.minecraft.player, i, itemToAdd);
                                }
                                anySlotUpdated = true;
                                affectedDragSlots.add(i);
                            }
                        }
                    }
                    // Case 2: New stack created in empty slot
                    else if (storageSlotBefore.isEmpty() && !storageSlotAfter.isEmpty()) {
                        // Check if this matches what we were shift-clicking
                        boolean isMatchingItem = ItemStack.isSameItemSameTags(storageSlotAfter, slotBefore);
                        
                        if (isMatchingItem) {
                            // Process the storage add operation based on mode
                            if (isPersonalMode) {
                                storageMenu.processPersonalStorageAdd(this.minecraft.player, i, storageSlotAfter.copy());
                            } else {
                                storageMenu.processCommunalStorageAdd(this.minecraft.player, i, storageSlotAfter.copy());
                            }
                            anySlotUpdated = true;
                        } else {
                            // This slot changed but doesn't match our shift-clicked item - this shouldn't happen
                            LOGGER.warn("Unexpected item type in storage slot {} after shift-click", i);
                            
                            // Send an update anyway to be safe
                            if (isPersonalMode) {
                                storageMenu.processPersonalStorageAdd(this.minecraft.player, i, storageSlotAfter.copy());
                            } else {
                                storageMenu.processCommunalStorageAdd(this.minecraft.player, i, storageSlotAfter.copy());
                            }
                            anySlotUpdated = true;
                        }
                    }
                }
                
                // If no specific slot updates were found, request a full refresh
                if (!anySlotUpdated) {
                    BlockPos townPos = storageMenu.getTownBlockPos();
                    if (townPos != null && this.minecraft != null && this.minecraft.player != null) {
                        if (isPersonalMode) {
                            ModMessages.sendToServer(new PersonalStorageRequestPacket(townPos, this.minecraft.player.getUUID()));
                        } else {
                            ModMessages.sendToServer(new CommunalStoragePacket(townPos, ItemStack.EMPTY, -1, true));
                        }
                    }
                }
                return;
            }
            
            return;
        }
        
        // For other slots or click types, use default behavior
        super.slotClicked(slot, slotId, mouseButton, type);
    }
    
    /**
     * Process a trade action with enhanced feedback
     */
    private boolean processTrade() {
        // Only proceed if we have a TradeMenu
        if (!(menu instanceof TradeMenu tradeMenu)) {
            return false;
        }
        
        // Check if there's an item to trade
        ItemStack inputStack = tradeMenu.getSlot(0).getItem();
        if (inputStack.isEmpty()) {
            // Play error sound and show message
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    Component.literal("Add items first!"), true);
            }
            return false;
        }
        
        // Calculate trade value for feedback
        int itemCount = inputStack.getCount();
        int emeraldsEarned = Math.max(1, itemCount / 10);
        String itemName = inputStack.getHoverName().getString();
        
        boolean success = tradeMenu.processTrade();
        if (success) {
            playClickSound();
            
            // Show success message
            if (this.minecraft != null && this.minecraft.player != null) {
                String message = String.format("Trade Complete! Got %d emerald%s", 
                        emeraldsEarned, emeraldsEarned == 1 ? "" : "s");
                this.minecraft.player.displayClientMessage(Component.literal(message), true);
            }
        } else {
            // Show error message
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                    Component.literal("Trade failed! Town doesn't want this."), true);
            }
        }
        return success;
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
        // Execute callback BEFORE switching screens to preserve state
        if (this.onCloseCallback != null) {
            this.onCloseCallback.accept(this);
        }
        
        // Return to parent screen AFTER callback executes
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