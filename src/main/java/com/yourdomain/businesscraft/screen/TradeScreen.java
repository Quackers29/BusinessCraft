package com.yourdomain.businesscraft.screen;

import com.yourdomain.businesscraft.menu.TradeMenu;
import com.yourdomain.businesscraft.screen.util.InventoryRenderer;
import com.yourdomain.businesscraft.screen.util.ScreenNavigationHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    // The location of the trade GUI texture
    private static final ResourceLocation TEXTURE = 
            new ResourceLocation("businesscraft", "textures/gui/trade_screen.png");
    
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
    
    // Inventory positions (matching vanilla layout)
    private static final int INV_START_X = 8;
    private static final int INV_START_Y = 84;
    private static final int HOTBAR_START_Y = 142;
    private static final int INVENTORY_SPACING = 4; // Spacing between inventory sections
    
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
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, InventoryRenderer.BACKGROUND_COLOR);
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, InventoryRenderer.BORDER_COLOR);
        guiGraphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, InventoryRenderer.BACKGROUND_COLOR);
        
        // Draw screen title with improved visibility
        InventoryRenderer.drawLabel(guiGraphics, this.font, "Trade Resources", 
                x + this.titleLabelX, y + this.titleLabelY);
        
        // Draw inventory label with improved visibility
        InventoryRenderer.drawLabel(guiGraphics, this.font, "Inventory", 
                x + this.inventoryLabelX, y + this.inventoryLabelY);
        
        // Draw player inventory using the utility class
        InventoryRenderer.drawInventoryWithHotbar(guiGraphics, 
                x + INV_START_X, y + INV_START_Y, 
                9, 3, 1, INVENTORY_SPACING);
        
        // Draw the trade button
        boolean isTradeButtonHovered = isMouseOverTradeButton(mouseX, mouseY);
        InventoryRenderer.drawButton(guiGraphics, 
                x + TRADE_BUTTON_X, y + TRADE_BUTTON_Y, 
                TRADE_BUTTON_WIDTH, TRADE_BUTTON_HEIGHT, 
                "T", this.font, isTradeButtonHovered);
        
        // Draw the back button
        boolean isBackButtonHovered = isMouseOverBackButton(mouseX, mouseY);
        InventoryRenderer.drawButton(guiGraphics, 
                x + BACK_BUTTON_X, y + BACK_BUTTON_Y, 
                BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, 
                "B", this.font, isBackButtonHovered);
        
        // Draw slot highlights and labels with improved visibility
        // Input slot
        InventoryRenderer.drawSlot(guiGraphics, 
                x + TradeMenu.SLOT_INPUT_X, y + TradeMenu.SLOT_INPUT_Y, 
                InventoryRenderer.PRIMARY_COLOR, InventoryRenderer.SLOT_BORDER_COLOR);
        
        // Input label
        InventoryRenderer.drawLabel(guiGraphics, this.font, "Input", 
                x + TradeMenu.SLOT_INPUT_X, y + TradeMenu.SLOT_INPUT_Y - 12);
        
        // Output slot
        InventoryRenderer.drawSlot(guiGraphics, 
                x + TradeMenu.SLOT_OUTPUT_X, y + TradeMenu.SLOT_OUTPUT_Y, 
                InventoryRenderer.SECONDARY_COLOR, InventoryRenderer.SLOT_BORDER_COLOR);
        
        // Output label
        InventoryRenderer.drawLabel(guiGraphics, this.font, "Output", 
                x + TradeMenu.SLOT_OUTPUT_X, y + TradeMenu.SLOT_OUTPUT_Y - 12);
        
        // Draw arrow between slots
        InventoryRenderer.drawArrow(guiGraphics, 
                x + TradeMenu.SLOT_INPUT_X + 20, 
                y + TradeMenu.SLOT_INPUT_Y + 8,
                x + TradeMenu.SLOT_OUTPUT_X - 4, 
                y + TradeMenu.SLOT_OUTPUT_Y + 8,
                0xFFFFFFFF);
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
        
        return InventoryRenderer.isMouseOverElement(mouseX, mouseY, x, y, 
                BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
    }
    
    private void returnToMainUI() {
        // Close the current screen
        this.onClose();
        
        // Use the utility method to return to the main interface
        // Pass the town block position from the menu to ensure we maintain the town reference
        ScreenNavigationHelper.returnToTownInterface(this.minecraft, this.minecraft.player, this.menu.getTownBlockPos());
    }
    
    /**
     * Sets an item in the output slot after a trade has been processed on the server
     */
    public void setOutputItem(ItemStack itemStack) {
        // Update the output slot in the menu directly
        if (this.menu != null) {
            // Update the item in the trade inventory
            this.menu.setOutputItem(itemStack);
        }
    }
}