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
    
    // Trade button coordinates
    private static final int TRADE_BUTTON_X = 88;
    private static final int TRADE_BUTTON_Y = 35;
    private static final int TRADE_BUTTON_WIDTH = 20;
    private static final int TRADE_BUTTON_HEIGHT = 18;
    
    // Back button coordinates
    private static final int BACK_BUTTON_X = 8;
    private static final int BACK_BUTTON_Y = 6;
    private static final int BACK_BUTTON_WIDTH = 20;
    private static final int BACK_BUTTON_HEIGHT = 18;
    
    // Track if we're currently dragging items
    private boolean isDragging = false;
    private Slot currentDragSlot = null;
    
    public TradeScreen(TradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        
        // Set the size of the screen
        this.imageWidth = 176;
        this.imageHeight = 166;
        
        // Position the title and inventory text
        this.titleLabelX = 32; // Move title to make room for back button
        this.titleLabelY = 6;
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
            guiGraphics.renderTooltip(this.font, Component.literal("Trade"), mouseX, mouseY);
        }
        
        // Render tooltip for back button if mouse is over it
        if (isMouseOverBackButton(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Back"), mouseX, mouseY);
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Set the texture location
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Draw the background texture
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        
        // Draw the trade button
        // Determine which texture coordinates to use based on mouse hover
        int buttonTextureY = isMouseOverTradeButton(mouseX, mouseY) ? 
                             this.imageHeight : this.imageHeight + TRADE_BUTTON_HEIGHT;
        guiGraphics.blit(TEXTURE, x + TRADE_BUTTON_X, y + TRADE_BUTTON_Y,
                         0, buttonTextureY, TRADE_BUTTON_WIDTH, TRADE_BUTTON_HEIGHT);
        
        // Draw the back button (using arrow texture from the vanilla GUI)
        int backButtonTextureY = isMouseOverBackButton(mouseX, mouseY) ? 
                             this.imageHeight + TRADE_BUTTON_HEIGHT*2 : this.imageHeight + TRADE_BUTTON_HEIGHT*3;
        guiGraphics.blit(TEXTURE, x + BACK_BUTTON_X, y + BACK_BUTTON_Y,
                         0, backButtonTextureY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
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