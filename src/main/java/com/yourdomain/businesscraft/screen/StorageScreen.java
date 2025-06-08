package com.yourdomain.businesscraft.screen;

import com.yourdomain.businesscraft.menu.StorageMenu;
import com.yourdomain.businesscraft.screen.util.InventoryRenderer;
import com.yourdomain.businesscraft.screen.util.ScreenNavigationHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.core.BlockPos;
import com.yourdomain.businesscraft.network.packets.storage.PersonalStorageRequestPacket;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.packets.storage.CommunalStoragePacket;
import com.yourdomain.businesscraft.network.packets.storage.PersonalStoragePacket;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

public class StorageScreen extends AbstractContainerScreen<StorageMenu> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageScreen.class);
    
    // The location of the storage GUI texture
    private static final ResourceLocation TEXTURE = 
            new ResourceLocation("businesscraft", "textures/gui/storage_screen.png");
    
    // Back button coordinates
    private static final int BACK_BUTTON_X = 8;
    private static final int BACK_BUTTON_Y = 6;
    private static final int BACK_BUTTON_WIDTH = 20;
    private static final int BACK_BUTTON_HEIGHT = 20;
    
    // Toggle button coordinates
    private static final int TOGGLE_BUTTON_X = 136;
    private static final int TOGGLE_BUTTON_Y = 6;
    private static final int TOGGLE_BUTTON_WIDTH = 32;
    private static final int TOGGLE_BUTTON_HEIGHT = 20;
    
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
    
    // Inventory size constants
    private static final int STORAGE_INVENTORY_SIZE = STORAGE_ROWS * STORAGE_COLS; // 18 slots for storage
    
    // Track if we're currently dragging items
    private boolean isDragging = false;
    private Slot currentDragSlot = null;
    
    // Keep track of all slots affected during a drag operation
    private final java.util.Set<Integer> affectedDragSlots = new java.util.HashSet<>();
    
    // Storage mode labels
    private static final String COMMUNAL_LABEL = "Communal";
    private static final String PERSONAL_LABEL = "Personal";
    
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
        
        // Render tooltip for toggle button if mouse is over it
        if (isMouseOverToggleButton(mouseX, mouseY)) {
            String currentMode = this.menu.isPersonalStorageMode() ? PERSONAL_LABEL : COMMUNAL_LABEL;
            String nextMode = this.menu.isPersonalStorageMode() ? COMMUNAL_LABEL : PERSONAL_LABEL;
            guiGraphics.renderTooltip(this.font, Component.literal("Current: " + currentMode + "\nClick to switch to " + nextMode), mouseX, mouseY);
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
        
        // Determine the title based on storage mode
        String storageTitle = "Town " + (this.menu.isPersonalStorageMode() ? "Personal" : "Communal") + " Storage";
        
        // Draw screen title with improved visibility
        InventoryRenderer.drawLabel(guiGraphics, this.font, storageTitle, 
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
                
        // Draw the toggle button
        boolean isToggleButtonHovered = isMouseOverToggleButton(mouseX, mouseY);
        InventoryRenderer.drawButton(guiGraphics, 
                x + TOGGLE_BUTTON_X, y + TOGGLE_BUTTON_Y, 
                TOGGLE_BUTTON_WIDTH, TOGGLE_BUTTON_HEIGHT, 
                this.menu.isPersonalStorageMode() ? "P" : "C", this.font, isToggleButtonHovered);
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
        
        // Check if the toggle button was clicked
        if (button == 0 && isMouseOverToggleButton((int)mouseX, (int)mouseY)) {
            // Play a click sound
            net.minecraft.client.resources.sounds.SimpleSoundInstance sound = 
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F);
            this.minecraft.getSoundManager().play(sound);
            
            // Toggle storage mode
            boolean isPersonal = this.menu.toggleStorageMode();
            
            // Request the appropriate storage data based on the new mode
            if (isPersonal) {
                // Request personal storage data
                // We don't have direct access to the player's personal storage data yet
                // The next time they interact with a slot, it will trigger a request
            } else {
                // Request communal storage data
                // We don't have direct access to the communal storage data yet
                // The next time they interact with a slot, it will trigger a request
            }
            
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slotId < 0 || slot == null) {
            super.slotClicked(slot, slotId, mouseButton, type);
            return;
        }

        // Get the item in the slot before the click
        ItemStack slotBefore = slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
        boolean isStorageSlot = slotId < STORAGE_INVENTORY_SIZE;
        boolean isPersonalMode = this.menu.isPersonalStorageMode();

        // Handle special case for shift-clicking
        if (type == ClickType.QUICK_MOVE) {
            // If shift-clicking from storage to player inventory
            if (isStorageSlot) {
                LOGGER.debug("Shift-click moving item from storage slot {} to player inventory", slotId);
                
                if (!slotBefore.isEmpty()) {
                    // Remember what we're taking out
                    ItemStack removedStack = slotBefore.copy();
                    int countBefore = removedStack.getCount();
                    
                    // Let standard handling happen
                    super.slotClicked(slot, slotId, mouseButton, type);
                    
                    // Check what's left in the slot
                    ItemStack slotAfter = slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                    int countAfter = slotAfter.isEmpty() ? 0 : slotAfter.getCount();
                    
                    // Calculate how many items were actually taken
                    int itemsTaken = countBefore - countAfter;
                    
                    if (itemsTaken > 0) {
                        LOGGER.debug("Shift-click removed {} items from storage slot {}", itemsTaken, slotId);
                        
                        // Create a copy of the removed stack with the correct count
                        ItemStack itemsToRemove = removedStack.copy();
                        itemsToRemove.setCount(itemsTaken);
                        
                        // Process the removal operation based on mode
                        if (isPersonalMode) {
                            this.menu.processPersonalStorageRemove(this.minecraft.player, slotId, itemsToRemove);
                        } else {
                            this.menu.processCommunalStorageRemove(this.minecraft.player, slotId, itemsToRemove);
                        }
                    }
                }
                return;
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
                                this.menu.processPersonalStorageAdd(this.minecraft.player, i, itemsToAdd);
                            } else {
                                this.menu.processCommunalStorageAdd(this.minecraft.player, i, itemsToAdd);
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
                                    this.menu.processPersonalStorageAdd(this.minecraft.player, i, itemToAdd);
                                } else {
                                    this.menu.processCommunalStorageAdd(this.minecraft.player, i, itemToAdd);
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
                                this.menu.processPersonalStorageAdd(this.minecraft.player, i, storageSlotAfter.copy());
                            } else {
                                this.menu.processCommunalStorageAdd(this.minecraft.player, i, storageSlotAfter.copy());
                            }
                            anySlotUpdated = true;
                        } else {
                            // This slot changed but doesn't match our shift-clicked item - this shouldn't happen
                            LOGGER.warn("Unexpected item type in storage slot {} after shift-click", i);
                            
                            // Send an update anyway to be safe
                            if (isPersonalMode) {
                                this.menu.processPersonalStorageAdd(this.minecraft.player, i, storageSlotAfter.copy());
                            } else {
                                this.menu.processCommunalStorageAdd(this.minecraft.player, i, storageSlotAfter.copy());
                            }
                            anySlotUpdated = true;
                        }
                    }
                }
                
                // If no specific slot updates were found, request a full refresh
                if (!anySlotUpdated) {
                    BlockPos townPos = this.menu.getTownBlockPos();
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
            
            // Let standard handling happen for other shift-click operations
            super.slotClicked(slot, slotId, mouseButton, type);
            return;
        }
        
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
                BlockPos townPos = this.menu.getTownBlockPos();
                if (townPos != null && this.minecraft != null && this.minecraft.player != null) {
                    if (isPersonalMode) {
                        // Request personal storage data refresh
                        ModMessages.sendToServer(new PersonalStorageRequestPacket(townPos, this.minecraft.player.getUUID()));
                    } else {
                        // Request communal storage data refresh
                        requestCommunalStorageData();
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
            
            // Phase 1: Track affected slots based on drag operation stage
            if (mouseButton == 0 || mouseButton == 4) {
                // Starting a new drag operation (either left or right) - clear the affected slots
                this.affectedDragSlots.clear();
                LOGGER.debug("Starting drag operation, mouseButton={}", mouseButton);
            } else if ((mouseButton == 1 || mouseButton == 5) && slot != null && slotId < STORAGE_INVENTORY_SIZE) {
                // Adding a slot to the drag operation - track it if it's in the storage area
                this.affectedDragSlots.add(slotId);
                LOGGER.debug("Adding slot {} to drag operation, dragSlots now contains {} slots", 
                    slotId, this.affectedDragSlots.size());
            }
            
            // Phase 2: Let Minecraft handle the standard drag behavior
            super.slotClicked(slot, slotId, mouseButton, type);
            
            // Phase 3: Process the end of a drag operation
            if (mouseButton == 2 || mouseButton == 6) {
                LOGGER.debug("Ending drag operation with {} affected slots", this.affectedDragSlots.size());
                
                // At the end of the drag, process all affected slots individually
                processAffectedDragSlots(new ArrayList<>(this.affectedDragSlots), isPersonalMode);
            }
            
            return;
        }
        
        // Check if it's a storage slot (in our 2x9 grid)
        if (slot != null && slotId < STORAGE_INVENTORY_SIZE) {
            // If player is adding an item to storage (left click with carried item)
            if (type == ClickType.PICKUP && mouseButton == 0 && !this.menu.getCarried().isEmpty()) {
                // Store information about the state before clicking
                ItemStack carriedBeforeAdd = this.menu.getCarried().copy();
                ItemStack slotBeforeAdd = slot.getItem().isEmpty() ? ItemStack.EMPTY : slot.getItem().copy();
                
                // Let the standard handling happen first
                super.slotClicked(slot, slotId, mouseButton, type);
                
                // Calculate what actually changed
                ItemStack carriedAfterAdd = this.menu.getCarried();
                ItemStack slotAfterAdd = slot.getItem();
                
                // Only send packet if items were actually added to storage
                if (slotAfterAdd.getCount() > slotBeforeAdd.getCount() || 
                    (slotBeforeAdd.isEmpty() && !slotAfterAdd.isEmpty())) {
                    
                    // Calculate how many items were actually added
                    int itemsAddedToSlot;
                    if (slotBeforeAdd.isEmpty()) {
                        // New stack was created
                        itemsAddedToSlot = slotAfterAdd.getCount();
                    } else {
                        // Existing stack was increased
                        itemsAddedToSlot = slotAfterAdd.getCount() - slotBeforeAdd.getCount();
                    }
                    
                    // If items were actually added, send the packet with the correct item count
                    if (itemsAddedToSlot > 0) {
                        ItemStack itemToAdd = slotAfterAdd.copy();
                        itemToAdd.setCount(itemsAddedToSlot);
                        
                        LOGGER.debug("Adding {} items to storage at slot {}", itemsAddedToSlot, slotId);
                        
                        // Process the storage add operation based on mode
                        if (isPersonalMode) {
                            this.menu.processPersonalStorageAdd(this.minecraft.player, slotId, itemToAdd);
                        } else {
                            this.menu.processCommunalStorageAdd(this.minecraft.player, slotId, itemToAdd);
                        }
                    }
                }
                // Handle the case where we swapped different items (not just adding more of the same type)
                else if (!ItemStack.isSameItemSameTags(slotBeforeAdd, slotAfterAdd) && 
                        !slotBeforeAdd.isEmpty() && !slotAfterAdd.isEmpty()) {
                    
                    LOGGER.debug("Swapping different items in slot {}: {} -> {}", 
                        slotId, slotBeforeAdd.getHoverName().getString(), slotAfterAdd.getHoverName().getString());
                    
                    // First, remove the original item from storage
                    if (isPersonalMode) {
                        this.menu.processPersonalStorageRemove(this.minecraft.player, slotId, slotBeforeAdd);
                    } else {
                        this.menu.processCommunalStorageRemove(this.minecraft.player, slotId, slotBeforeAdd);
                    }
                    
                    // Then, add the new item to storage
                    if (isPersonalMode) {
                        this.menu.processPersonalStorageAdd(this.minecraft.player, slotId, slotAfterAdd.copy());
                    } else {
                        this.menu.processCommunalStorageAdd(this.minecraft.player, slotId, slotAfterAdd.copy());
                    }
                }
                return;
            }
            // If player is right-clicking to split a stack in storage
            else if (type == ClickType.PICKUP && mouseButton == 1 && slot.hasItem() && this.menu.getCarried().isEmpty()) {
                // Store information about the state before clicking
                ItemStack slotBeforeSplit = slot.getItem().copy();
                int countBefore = slotBeforeSplit.getCount();
                
                // Let the standard handling happen first
                super.slotClicked(slot, slotId, mouseButton, type);
                
                // After splitting, get the remaining items in the slot
                ItemStack slotAfterSplit = slot.getItem();
                ItemStack carriedAfterSplit = this.menu.getCarried();
                
                // Calculate how many items were actually removed
                int itemsRemoved = carriedAfterSplit.getCount();
                
                LOGGER.debug("Right-click split: {} items remaining in slot, {} items in hand", 
                    slotAfterSplit.getCount(), carriedAfterSplit.getCount());
                
                // Remove the entire stack from storage first
                if (isPersonalMode) {
                    this.menu.processPersonalStorageRemove(this.minecraft.player, slotId, slotBeforeSplit);
                } else {
                    this.menu.processCommunalStorageRemove(this.minecraft.player, slotId, slotBeforeSplit);
                }
                
                // Then add back the remaining items if any
                if (!slotAfterSplit.isEmpty()) {
                    if (isPersonalMode) {
                        this.menu.processPersonalStorageAdd(this.minecraft.player, slotId, slotAfterSplit);
                    } else {
                        this.menu.processCommunalStorageAdd(this.minecraft.player, slotId, slotAfterSplit);
                    }
                }
                
                return;
            }
            // If player is removing an item from storage (left click on item in slot)
            else if (type == ClickType.PICKUP && mouseButton == 0 && !slot.getItem().isEmpty()) {
                // Get a copy of the item before it's removed
                ItemStack itemStackToRemove = slot.getItem().copy();
                
                // Let the standard handling happen first
                super.slotClicked(slot, slotId, mouseButton, type);
                
                // Then process the storage remove operation based on mode
                if (isPersonalMode) {
                    this.menu.processPersonalStorageRemove(this.minecraft.player, slotId, itemStackToRemove);
                } else {
                    this.menu.processCommunalStorageRemove(this.minecraft.player, slotId, itemStackToRemove);
                }
                return;
            }

            // Add a new case for right-click placing a single item from carried stack
            // If player is right-clicking with carried item to place just one item
            else if (type == ClickType.PICKUP && mouseButton == 1 && !this.menu.getCarried().isEmpty()) {
                // Store information about the state before clicking
                ItemStack carriedBeforePlace = this.menu.getCarried().copy();
                ItemStack slotBeforePlace = slot.getItem().isEmpty() ? ItemStack.EMPTY : slot.getItem().copy();
                
                // Let the standard handling happen first
                super.slotClicked(slot, slotId, mouseButton, type);
                
                // Calculate what actually changed
                ItemStack carriedAfterPlace = this.menu.getCarried();
                ItemStack slotAfterPlace = slot.getItem();
                
                // Only send packet if items were actually added to storage
                if (slotAfterPlace.getCount() > slotBeforePlace.getCount() || 
                    (slotBeforePlace.isEmpty() && !slotAfterPlace.isEmpty())) {
                    
                    // Calculate how many items were actually added (should be 1 for right-click)
                    int itemsAddedByRightClick;
                    if (slotBeforePlace.isEmpty()) {
                        // New stack was created
                        itemsAddedByRightClick = slotAfterPlace.getCount();
                    } else {
                        // Existing stack was increased
                        itemsAddedByRightClick = slotAfterPlace.getCount() - slotBeforePlace.getCount();
                    }
                    
                    // If items were actually added, send the packet with the correct item count
                    if (itemsAddedByRightClick > 0) {
                        ItemStack itemToAdd = slotAfterPlace.copy();
                        itemToAdd.setCount(itemsAddedByRightClick);
                        
                        LOGGER.debug("Right-click adding {} items to storage at slot {}", itemsAddedByRightClick, slotId);
                        
                        // Process the storage add operation based on mode
                        if (isPersonalMode) {
                            this.menu.processPersonalStorageAdd(this.minecraft.player, slotId, itemToAdd);
                        } else {
                            this.menu.processCommunalStorageAdd(this.minecraft.player, slotId, itemToAdd);
                        }
                    }
                }
                return;
            }
        }
        
        // For other types of slots or click types, use default behavior
        super.slotClicked(slot, slotId, mouseButton, type);
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
    
    /**
     * Update the storage display with communal items from the server
     * Called when the client receives a CommunalStorageResponsePacket
     * 
     * @param items Map of items and their counts from the town's communal storage
     */
    public void updateStorageItems(Map<Item, Integer> items) {
        // This is just a pass-through to the menu
        this.menu.updateStorageItems(items);
    }
    
    /**
     * Update the storage display with personal items from the server
     * Called when the client receives a PersonalStorageResponsePacket
     * 
     * @param items Map of items and their counts from the player's personal storage
     */
    public void updatePersonalStorageItems(Map<Item, Integer> items) {
        // This is just a pass-through to the menu
        this.menu.updatePersonalStorageItems(items);
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
        
        return InventoryRenderer.isMouseOverElement(mouseX, mouseY, x, y, 
                BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
    }
    
    private boolean isMouseOverToggleButton(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        return InventoryRenderer.isMouseOverElement(mouseX, mouseY, x, y, 
                TOGGLE_BUTTON_X, TOGGLE_BUTTON_Y, TOGGLE_BUTTON_WIDTH, TOGGLE_BUTTON_HEIGHT);
    }
    
    private void returnToMainUI() {
        // Close the current screen
        this.onClose();
        
        // Use the utility method to return to the main interface
        ScreenNavigationHelper.returnToTownInterface(this.minecraft, this.minecraft.player, this.menu.getTownBlockPos());
    }
    
    /**
     * Helper method to request communal storage data refresh from the server
     * Uses a special CommunalStoragePacket with slotId=-1 as a marker
     */
    private void requestCommunalStorageData() {
        BlockPos townPos = this.menu.getTownBlockPos();
        if (townPos != null && this.minecraft != null && this.minecraft.player != null) {
            LOGGER.debug("Requesting communal storage refresh from server");
            try {
                // Send the request packet with the special marker (-1)
                ModMessages.sendToServer(new CommunalStoragePacket(townPos, ItemStack.EMPTY, -1, true));
            } catch (Exception e) {
                LOGGER.error("Error sending communal storage request", e);
            }
        }
    }
    
    /**
     * Helper method to request personal storage data refresh from the server
     * Uses a PersonalStorageRequestPacket to request data for the current player
     */
    private void requestPersonalStorageData() {
        BlockPos townPos = this.menu.getTownBlockPos();
        if (townPos != null && this.minecraft != null && this.minecraft.player != null) {
            LOGGER.debug("Requesting personal storage refresh from server");
            try {
                // Send the request packet with the player UUID
                UUID playerId = this.minecraft.player.getUUID();
                ModMessages.sendToServer(new PersonalStorageRequestPacket(townPos, playerId));
            } catch (Exception e) {
                LOGGER.error("Error sending personal storage request", e);
            }
        }
    }
    
    /**
     * Process each slot affected by a drag operation, sending individual updates to the server.
     * After a drag operation ends, this method ensures all affected slots are properly synchronized
     * with the server by sending individual update packets.
     * 
     * @param affectedSlots List of slot indices affected by the drag operation
     * @param isPersonalMode Whether we're in personal storage mode
     */
    private void processAffectedDragSlots(List<Integer> affectedSlots, boolean isPersonalMode) {
        LOGGER.debug("Processing {} affected slots after drag operation", affectedSlots.size());
        
        if (affectedSlots.isEmpty()) {
            // If no slots were affected but we tracked a drag, request full refresh
            if (isPersonalMode) {
                requestPersonalStorageData();
            } else {
                requestCommunalStorageData();
            }
            return;
        }
        
        // For each affected slot, send individual updates to the server
        BlockPos townPos = this.menu.getTownBlockPos();
        boolean updateNeeded = false;
        
        for (int slotIndex : affectedSlots) {
            try {
                if (slotIndex >= 0 && slotIndex < this.menu.slots.size()) {
                    Slot slot = this.menu.slots.get(slotIndex);
                    ItemStack stack = slot.getItem();
                    
                    if (isPersonalMode) {
                        // Use the proper constructor: BlockPos, ItemStack, slotId, isAddOperation, playerId
                        BlockPos townPosition = this.menu.getTownBlockPos();
                        if (townPosition != null) {
                            ModMessages.sendToServer(new PersonalStoragePacket(
                                townPosition, stack, slotIndex, true, this.minecraft.player.getUUID()));
                        }
                    } else if (townPos != null) {
                        ModMessages.sendToServer(new CommunalStoragePacket(townPos, stack, slotIndex, true));
                    }
                    
                    updateNeeded = true;
                } else {
                    LOGGER.warn("Invalid slot index during drag processing: {}", slotIndex);
                }
            } catch (Exception e) {
                LOGGER.error("Error processing affected slot during drag", e);
            }
        }
        
        // If we didn't process any valid slots but had affected slots, request a full refresh
        if (!updateNeeded && !affectedSlots.isEmpty()) {
            if (isPersonalMode) {
                requestPersonalStorageData();
            } else {
                requestCommunalStorageData();
            }
        }
    }
} 