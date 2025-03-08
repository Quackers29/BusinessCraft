package com.yourdomain.businesscraft.screen;

import com.yourdomain.businesscraft.menu.TradeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    // The location of the trade GUI texture
    private static final ResourceLocation TEXTURE = 
            new ResourceLocation("businesscraft", "textures/gui/trade_screen.png");
    
    // UI colors - matching the main interface theme
    private static final int PRIMARY_COLOR = 0xA0335599;       // Semi-transparent blue
    private static final int SECONDARY_COLOR = 0xA0884466;     // Semi-transparent purple
    private static final int BACKGROUND_COLOR = 0x80222222;    // Semi-transparent dark gray
    private static final int BORDER_COLOR = 0xA0AAAAAA;        // Light gray
    private static final int BUTTON_COLOR = 0xA0446688;        // Button blue
    private static final int BUTTON_HOVER_COLOR = 0xA066AADD;  // Button hover blue
    private static final int TEXT_COLOR = 0xFFFFFFFF;          // White text
    private static final int TEXT_SHADOW_COLOR = 0xFF000000;   // Black text shadow
    private static final int LABEL_BG_COLOR = 0x80000000;      // Semi-transparent black for text background
    private static final int SLOT_BORDER_COLOR = 0xFF666666;   // Darker gray for slot borders
    private static final int INVENTORY_BORDER_COLOR = 0xFFAAAAAA; // Light gray for inventory border
    
    // Trade button coordinates
    private static final int TRADE_BUTTON_X = 80;
    private static final int TRADE_BUTTON_Y = 35;
    private static final int TRADE_BUTTON_WIDTH = 20;
    private static final int TRADE_BUTTON_HEIGHT = 20;
    
    // Back button coordinates
    private static final int BACK_BUTTON_X = 8;
    private static final int BACK_BUTTON_Y = 6;
    private static final int BACK_BUTTON_WIDTH = 20;
    private static final int BACK_BUTTON_HEIGHT = 20;
    
    // Slot labels
    private static final String INPUT_LABEL = "Input";
    private static final String OUTPUT_LABEL = "Output";
    
    // Inventory positions (matching vanilla layout)
    private static final int INV_START_X = 8;
    private static final int INV_START_Y = 84;
    private static final int HOTBAR_START_Y = 142;
    private static final int SLOT_SIZE = 18;
    
    // Track if we're currently dragging items
    private boolean isDragging = false;
    private Slot currentDragSlot = null;
    
    public TradeScreen(TradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        
        // Set the size of the screen
        this.imageWidth = 176;
        this.imageHeight = 166;
        
        // Position the title and inventory text
        this.titleLabelX = 8 + BACK_BUTTON_WIDTH + 4; // Move title to make room for back button
        this.titleLabelY = 8;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94; // Just above player inventory
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render the background
        this.renderBackground(guiGraphics);
        
        // Render the screen elements
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render tooltips for slots
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // Render tooltip for trade button if mouse is over it
        if (isMouseOverTradeButton(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Process the trade"), mouseX, mouseY);
        }
        
        // Render tooltip for back button if mouse is over it
        if (isMouseOverBackButton(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Return to Town Interface"), mouseX, mouseY);
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Calculate position for centered interface
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Draw the background panel
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, BACKGROUND_COLOR);
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, BORDER_COLOR);
        guiGraphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, BACKGROUND_COLOR);
        
        // Draw screen title with improved visibility
        // Background for title
        int titleWidth = this.font.width("Trade Resources");
        guiGraphics.fill(x + this.titleLabelX - 2, y + this.titleLabelY - 2, 
                 x + this.titleLabelX + titleWidth + 2, y + this.titleLabelY + 10, LABEL_BG_COLOR);
        
        // Title with shadow for better visibility
        guiGraphics.drawString(this.font, "Trade Resources", 
                x + this.titleLabelX, y + this.titleLabelY, TEXT_COLOR, true);
        
        // Draw inventory label with improved visibility
        int invWidth = this.font.width("Inventory");
        guiGraphics.fill(x + this.inventoryLabelX - 2, y + this.inventoryLabelY - 2, 
                 x + this.inventoryLabelX + invWidth + 2, y + this.inventoryLabelY + 10, LABEL_BG_COLOR);
        
        guiGraphics.drawString(this.font, "Inventory", 
                x + this.inventoryLabelX, y + this.inventoryLabelY, TEXT_COLOR, true);
        
        // Draw player inventory section borders
        // Draw border around main inventory (3 rows of 9)
        drawInventoryBorder(guiGraphics, x + INV_START_X - 1, y + INV_START_Y - 1, 
                9 * SLOT_SIZE + 2, 3 * SLOT_SIZE + 2, INVENTORY_BORDER_COLOR);
        
        // Draw border around hotbar (1 row of 9)
        drawInventoryBorder(guiGraphics, x + INV_START_X - 1, y + HOTBAR_START_Y - 1, 
                9 * SLOT_SIZE + 2, SLOT_SIZE + 2, INVENTORY_BORDER_COLOR);
        
        // Draw individual slot backgrounds
        drawSlotGrid(guiGraphics, x + INV_START_X, y + INV_START_Y, 9, 3);   // Main inventory
        drawSlotGrid(guiGraphics, x + INV_START_X, y + HOTBAR_START_Y, 9, 1); // Hotbar
        
        // Draw the trade button
        int tradeButtonColor = isMouseOverTradeButton(mouseX, mouseY) ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
        guiGraphics.fill(x + TRADE_BUTTON_X, y + TRADE_BUTTON_Y, 
                 x + TRADE_BUTTON_X + TRADE_BUTTON_WIDTH, y + TRADE_BUTTON_Y + TRADE_BUTTON_HEIGHT, 
                 tradeButtonColor);
        
        // Draw border around trade button
        guiGraphics.fill(x + TRADE_BUTTON_X, y + TRADE_BUTTON_Y, 
                 x + TRADE_BUTTON_X + TRADE_BUTTON_WIDTH, y + TRADE_BUTTON_Y + 1, TEXT_COLOR);
        guiGraphics.fill(x + TRADE_BUTTON_X, y + TRADE_BUTTON_Y, 
                 x + TRADE_BUTTON_X + 1, y + TRADE_BUTTON_Y + TRADE_BUTTON_HEIGHT, TEXT_COLOR);
        guiGraphics.fill(x + TRADE_BUTTON_X, y + TRADE_BUTTON_Y + TRADE_BUTTON_HEIGHT - 1, 
                 x + TRADE_BUTTON_X + TRADE_BUTTON_WIDTH, y + TRADE_BUTTON_Y + TRADE_BUTTON_HEIGHT, TEXT_COLOR);
        guiGraphics.fill(x + TRADE_BUTTON_X + TRADE_BUTTON_WIDTH - 1, y + TRADE_BUTTON_Y, 
                 x + TRADE_BUTTON_X + TRADE_BUTTON_WIDTH, y + TRADE_BUTTON_Y + TRADE_BUTTON_HEIGHT, TEXT_COLOR);
        
        // Draw trade button label
        drawCenteredString(guiGraphics, this.font, "T", 
                x + TRADE_BUTTON_X + TRADE_BUTTON_WIDTH / 2, 
                y + TRADE_BUTTON_Y + (TRADE_BUTTON_HEIGHT - 8) / 2, TEXT_COLOR, true);
        
        // Draw the back button
        int backButtonColor = isMouseOverBackButton(mouseX, mouseY) ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
        guiGraphics.fill(x + BACK_BUTTON_X, y + BACK_BUTTON_Y, 
                 x + BACK_BUTTON_X + BACK_BUTTON_WIDTH, y + BACK_BUTTON_Y + BACK_BUTTON_HEIGHT, 
                 backButtonColor);
        
        // Draw border around back button
        guiGraphics.fill(x + BACK_BUTTON_X, y + BACK_BUTTON_Y, 
                 x + BACK_BUTTON_X + BACK_BUTTON_WIDTH, y + BACK_BUTTON_Y + 1, TEXT_COLOR);
        guiGraphics.fill(x + BACK_BUTTON_X, y + BACK_BUTTON_Y, 
                 x + BACK_BUTTON_X + 1, y + BACK_BUTTON_Y + BACK_BUTTON_HEIGHT, TEXT_COLOR);
        guiGraphics.fill(x + BACK_BUTTON_X, y + BACK_BUTTON_Y + BACK_BUTTON_HEIGHT - 1, 
                 x + BACK_BUTTON_X + BACK_BUTTON_WIDTH, y + BACK_BUTTON_Y + BACK_BUTTON_HEIGHT, TEXT_COLOR);
        guiGraphics.fill(x + BACK_BUTTON_X + BACK_BUTTON_WIDTH - 1, y + BACK_BUTTON_Y, 
                 x + BACK_BUTTON_X + BACK_BUTTON_WIDTH, y + BACK_BUTTON_Y + BACK_BUTTON_HEIGHT, TEXT_COLOR);
        
        // Draw back button label (changed to just "B")
        drawCenteredString(guiGraphics, this.font, "B", 
                x + BACK_BUTTON_X + BACK_BUTTON_WIDTH / 2, 
                y + BACK_BUTTON_Y + (BACK_BUTTON_HEIGHT - 8) / 2, TEXT_COLOR, true);
        
        // Draw slot highlights and labels with improved visibility
        // Input slot background
        guiGraphics.fill(x + TradeMenu.SLOT_INPUT_X - 1, y + TradeMenu.SLOT_INPUT_Y - 1,
                 x + TradeMenu.SLOT_INPUT_X + 17, y + TradeMenu.SLOT_INPUT_Y + 17, 
                 PRIMARY_COLOR);
        
        // Draw slot border for input slot
        drawSlotBorder(guiGraphics, x + TradeMenu.SLOT_INPUT_X - 1, y + TradeMenu.SLOT_INPUT_Y - 1, 
                18, 18, SLOT_BORDER_COLOR);
        
        // Input label background
        int inputWidth = this.font.width(INPUT_LABEL);
        guiGraphics.fill(x + TradeMenu.SLOT_INPUT_X - 2, y + TradeMenu.SLOT_INPUT_Y - 12, 
                 x + TradeMenu.SLOT_INPUT_X + inputWidth + 2, y + TradeMenu.SLOT_INPUT_Y - 2, LABEL_BG_COLOR);
        
        // Input label
        guiGraphics.drawString(this.font, INPUT_LABEL, 
                x + TradeMenu.SLOT_INPUT_X, y + TradeMenu.SLOT_INPUT_Y - 12, TEXT_COLOR, true);
        
        // Output slot background
        guiGraphics.fill(x + TradeMenu.SLOT_OUTPUT_X - 1, y + TradeMenu.SLOT_OUTPUT_Y - 1,
                 x + TradeMenu.SLOT_OUTPUT_X + 17, y + TradeMenu.SLOT_OUTPUT_Y + 17, 
                 SECONDARY_COLOR);
                 
        // Draw slot border for output slot
        drawSlotBorder(guiGraphics, x + TradeMenu.SLOT_OUTPUT_X - 1, y + TradeMenu.SLOT_OUTPUT_Y - 1, 
                18, 18, SLOT_BORDER_COLOR);
        
        // Output label background
        int outputWidth = this.font.width(OUTPUT_LABEL);
        guiGraphics.fill(x + TradeMenu.SLOT_OUTPUT_X - 2, y + TradeMenu.SLOT_OUTPUT_Y - 12, 
                 x + TradeMenu.SLOT_OUTPUT_X + outputWidth + 2, y + TradeMenu.SLOT_OUTPUT_Y - 2, LABEL_BG_COLOR);
        
        // Output label
        guiGraphics.drawString(this.font, OUTPUT_LABEL, 
                x + TradeMenu.SLOT_OUTPUT_X, y + TradeMenu.SLOT_OUTPUT_Y - 12, TEXT_COLOR, true);
        
        // Draw arrow between slots
        drawArrow(guiGraphics, 
                x + TradeMenu.SLOT_INPUT_X + 20, 
                y + TradeMenu.SLOT_INPUT_Y + 8,
                x + TradeMenu.SLOT_OUTPUT_X - 4, 
                y + TradeMenu.SLOT_OUTPUT_Y + 8,
                0xFFFFFFFF);
    }
    
    /**
     * Draws a grid of inventory slots
     */
    private void drawSlotGrid(GuiGraphics guiGraphics, int startX, int startY, int columns, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int x = startX + col * SLOT_SIZE;
                int y = startY + row * SLOT_SIZE;
                
                // Draw darker slot background
                guiGraphics.fill(x, y, x + 16, y + 16, 0x50000000);
                
                // Draw slot border
                drawSlotBorder(guiGraphics, x - 1, y - 1, 18, 18, SLOT_BORDER_COLOR);
            }
        }
    }
    
    /**
     * Draws a border around an inventory section
     */
    private void drawInventoryBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        // Top border
        guiGraphics.fill(x, y, x + width, y + 1, color);
        // Left border
        guiGraphics.fill(x, y, x + 1, y + height, color);
        // Bottom border
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        // Right border
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }
    
    /**
     * Draws a border around a slot
     */
    private void drawSlotBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        // Top border
        guiGraphics.fill(x, y, x + width, y + 1, color);
        // Left border
        guiGraphics.fill(x, y, x + 1, y + height, color);
        // Bottom border
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        // Right border
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }
    
    private void drawArrow(GuiGraphics guiGraphics, int startX, int startY, int endX, int endY, int color) {
        // Draw the arrow line
        guiGraphics.fill(startX, startY - 1, endX, startY + 1, color);
        
        // Draw the arrow head
        for (int i = 0; i < 4; i++) {
            guiGraphics.fill(endX - i, startY - i - 1, endX - i + 1, startY + i + 2, color);
        }
    }
    
    private void drawCenteredString(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, String text, int x, int y, int color, boolean withShadow) {
        int textX = x - font.width(text) / 2;
        if (withShadow) {
            guiGraphics.drawString(font, text, textX, y, color, true);
        } else {
            guiGraphics.drawString(font, text, textX, y, color);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if the trade button was clicked
        if (button == 0 && isMouseOverTradeButton((int)mouseX, (int)mouseY)) {
            // Process the trade
            this.menu.processTrade();
            // Play a click sound
            net.minecraft.client.resources.sounds.SimpleSoundInstance sound = 
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F);
            this.minecraft.getSoundManager().play(sound);
            return true;
        }
        
        // Check if the back button was clicked
        if (button == 0 && isMouseOverBackButton((int)mouseX, (int)mouseY)) {
            // Play a click sound
            net.minecraft.client.resources.sounds.SimpleSoundInstance sound = 
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F);
            this.minecraft.getSoundManager().play(sound);
            
            // Return to the main UI
            returnToMainUI();
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        super.slotClicked(slot, slotId, mouseButton, type);
        // Make sure to properly handle slot clicks for better item handling
        if (slot != null) {
            this.currentDragSlot = slot;
        }
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Ensure proper item dragging between slots
        boolean result = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        
        // Start dragging if not already dragging
        if (button == 0 && !this.isDragging && this.menu.getCarried().isEmpty()) {
            Slot slot = this.findSlot(mouseX, mouseY);
            if (slot != null && slot.hasItem()) {
                this.isDragging = true;
            }
        }
        
        return result;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Reset dragging state when mouse is released
        if (button == 0) {
            this.isDragging = false;
            this.currentDragSlot = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    // Helper method to find a slot at the given mouse coordinates
    private Slot findSlot(double mouseX, double mouseY) {
        for (Slot slot : this.menu.slots) {
            if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }
    
    private boolean isMouseOverTradeButton(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        return mouseX >= x + TRADE_BUTTON_X && mouseX < x + TRADE_BUTTON_X + TRADE_BUTTON_WIDTH &&
               mouseY >= y + TRADE_BUTTON_Y && mouseY < y + TRADE_BUTTON_Y + TRADE_BUTTON_HEIGHT;
    }
    
    private boolean isMouseOverBackButton(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        return mouseX >= x + BACK_BUTTON_X && mouseX < x + BACK_BUTTON_X + BACK_BUTTON_WIDTH &&
               mouseY >= y + BACK_BUTTON_Y && mouseY < y + BACK_BUTTON_Y + BACK_BUTTON_HEIGHT;
    }
    
    private void returnToMainUI() {
        // Close the current screen
        this.onClose();
        
        // Open the town interface screen
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = this.minecraft.level.getBlockEntity(this.minecraft.player.blockPosition());
        
        // Create the menu with just the player's current position
        net.minecraft.world.inventory.AbstractContainerMenu containerMenu = 
            new com.yourdomain.businesscraft.menu.TownInterfaceMenu(
                0, this.minecraft.player.getInventory(), 
                this.minecraft.player.blockPosition());
        
        this.minecraft.setScreen(
            new TownInterfaceScreen(
                (com.yourdomain.businesscraft.menu.TownInterfaceMenu) containerMenu,
                this.minecraft.player.getInventory(),
                Component.literal("Town Interface")));
    }
}