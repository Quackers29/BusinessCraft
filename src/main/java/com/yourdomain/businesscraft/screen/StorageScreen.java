package com.yourdomain.businesscraft.screen;

import com.yourdomain.businesscraft.menu.StorageMenu;
import com.yourdomain.businesscraft.screen.util.InventoryRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public class StorageScreen extends AbstractContainerScreen<StorageMenu> {
    // The location of the storage GUI texture
    private static final ResourceLocation TEXTURE = 
            new ResourceLocation("businesscraft", "textures/gui/storage_screen.png");
    
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
    
    // Storage grid positions
    private static final int STORAGE_START_X = 8;
    private static final int STORAGE_START_Y = 28;
    private static final int STORAGE_ROWS = 2;
    private static final int STORAGE_COLS = 9;
    
    // Track if we're currently dragging items
    private boolean isDragging = false;
    private Slot currentDragSlot = null;
    
    public StorageScreen(StorageMenu menu, Inventory inventory, Component title) {
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
        InventoryRenderer.drawLabel(guiGraphics, this.font, "Town Storage", 
                x + this.titleLabelX, y + this.titleLabelY);
        
        // Draw inventory label with improved visibility
        InventoryRenderer.drawLabel(guiGraphics, this.font, "Inventory", 
                x + this.inventoryLabelX, y + this.inventoryLabelY);
                
        // Draw storage grid section
        // Background and border for storage grid
        guiGraphics.fill(x + STORAGE_START_X - 2, y + STORAGE_START_Y - 2, 
                x + STORAGE_START_X + (STORAGE_COLS * InventoryRenderer.SLOT_SIZE) + 2, 
                y + STORAGE_START_Y + (STORAGE_ROWS * InventoryRenderer.SLOT_SIZE) + 2, 
                0x70000000); // Darker background behind slots
        
        InventoryRenderer.drawBorder(guiGraphics, 
                x + STORAGE_START_X - 2, y + STORAGE_START_Y - 2, 
                STORAGE_COLS * InventoryRenderer.SLOT_SIZE + 4, 
                STORAGE_ROWS * InventoryRenderer.SLOT_SIZE + 4, 
                InventoryRenderer.INVENTORY_BORDER_COLOR, 2);
        
        // Draw storage slot grid
        for (int row = 0; row < STORAGE_ROWS; row++) {
            for (int col = 0; col < STORAGE_COLS; col++) {
                int slotX = x + STORAGE_START_X + (col * InventoryRenderer.SLOT_SIZE);
                int slotY = y + STORAGE_START_Y + (row * InventoryRenderer.SLOT_SIZE);
                
                // Draw slot background
                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, InventoryRenderer.SLOT_BG_COLOR);
                
                // Draw slot border
                InventoryRenderer.drawSlotBorder(guiGraphics, slotX - 1, slotY - 1, 18, 18, InventoryRenderer.SLOT_BORDER_COLOR);
            }
        }
        
        // Draw player inventory using the utility class
        InventoryRenderer.drawInventoryWithHotbar(guiGraphics, 
                x + INV_START_X, y + INV_START_Y, 
                9, 3, 1, INVENTORY_SPACING);
        
        // Draw the back button
        boolean isBackButtonHovered = isMouseOverBackButton(mouseX, mouseY);
        InventoryRenderer.drawButton(guiGraphics, 
                x + BACK_BUTTON_X, y + BACK_BUTTON_Y, 
                BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, 
                "B", this.font, isBackButtonHovered);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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