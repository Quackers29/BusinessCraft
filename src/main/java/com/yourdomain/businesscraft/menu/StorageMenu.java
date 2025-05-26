package com.yourdomain.businesscraft.menu;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.init.ModMenuTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.CommunalStoragePacket;
import com.yourdomain.businesscraft.network.PersonalStoragePacket;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.menu.TownBlockMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.UUID;
import java.util.Collections;

public class StorageMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageMenu.class);
    
    // Constants for the number of slots
    private static final int STORAGE_ROWS = 2;
    private static final int STORAGE_COLS = 9;
    private static final int INVENTORY_SIZE = STORAGE_ROWS * STORAGE_COLS; // 18 slots for storage
    
    // ItemHandlers for the storage inventory
    private final ItemStackHandler storageInventory;
    
    // Store the position of the town block
    private BlockPos townBlockPos;
    
    // Storage grid positions
    private static final int STORAGE_START_X = 8;
    private static final int STORAGE_START_Y = 28;
    private static final int SLOT_SIZE = 18;
    
    // Track current storage mode
    private boolean isPersonalStorageMode = false;
    
    // Constructor for client-side creation
    public StorageMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(INVENTORY_SIZE));
    }
    
    // Constructor for server-side creation
    public StorageMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new ItemStackHandler(INVENTORY_SIZE));
        // Read the BlockPos from the extra data if available
        if (extraData != null && extraData.readableBytes() > 0) {
            this.townBlockPos = extraData.readBlockPos();
        }
    }
    
    // Constructor with BlockPos
    public StorageMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, new ItemStackHandler(INVENTORY_SIZE));
        this.townBlockPos = pos;
    }
    
    // Main constructor
    public StorageMenu(int containerId, Inventory playerInventory, IItemHandler storageInventory) {
        super(ModMenuTypes.STORAGE_MENU.get(), containerId);
        
        // Create the storage inventory if it doesn't exist
        if (storageInventory instanceof ItemStackHandler) {
            this.storageInventory = (ItemStackHandler) storageInventory;
        } else {
            this.storageInventory = new ItemStackHandler(INVENTORY_SIZE);
        }
        
        // Add slots for the storage inventory (2 rows of 9)
        for (int row = 0; row < STORAGE_ROWS; row++) {
            for (int col = 0; col < STORAGE_COLS; col++) {
                int index = col + row * STORAGE_COLS;
                int x = STORAGE_START_X + col * SLOT_SIZE;
                int y = STORAGE_START_Y + row * SLOT_SIZE;
                this.addSlot(new StorageSlot(this.storageInventory, index, x, y));
            }
        }
        
        // Add slots for player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 
                        8 + col * SLOT_SIZE, 84 + row * SLOT_SIZE));
            }
        }
        
        // Add slots for player hotbar (1 row of 9)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * SLOT_SIZE, 142));
        }
    }
    
    // Check if the player can interact with the menu
    @Override
    public boolean stillValid(Player player) {
        return true; // Allow interaction regardless of position
    }
    
    // Handle shift-clicking items between slots
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            
            // If shift-clicking from the storage inventory
            if (index < INVENTORY_SIZE) {
                // Try to move to player inventory
                if (!this.moveItemStackTo(slotStack, INVENTORY_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } 
            // If shift-clicking from player inventory
            else {
                // Try to move to storage inventory
                if (!this.moveItemStackTo(slotStack, 0, INVENTORY_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            slot.onTake(player, slotStack);
        }
        
        return itemstack;
    }
    
    // Custom slot implementation for storage
    private static class StorageSlot extends SlotItemHandler {
        public StorageSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }
        
        // Allow any item to be placed in storage slots
        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }
    }
    
    /**
     * Get the position of the town block
     * @return The BlockPos of the town block
     */
    public BlockPos getTownBlockPos() {
        return this.townBlockPos;
    }
    
    /**
     * Gets a TownBlockMenu instance for accessing town data
     * 
     * @return A TownBlockMenu instance or null if unable to create one
     */
    public TownBlockMenu getTownBlockMenu() {
        if (townBlockPos != null) {
            // Get the current player's inventory and minecraft instance
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                Level level = player.level();
                if (level != null) {
                    BlockEntity blockEntity = level.getBlockEntity(townBlockPos);
                    if (blockEntity != null) {
                        // Create a menu with containerId 0 (temporary menu just for data access)
                        return new TownBlockMenu(0, player.getInventory(), blockEntity);
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Toggle between personal and communal storage modes
     * 
     * @return The new storage mode (true = personal, false = communal)
     */
    public boolean toggleStorageMode() {
        isPersonalStorageMode = !isPersonalStorageMode;
        LOGGER.debug("Toggled storage mode to: {}", isPersonalStorageMode ? "Personal" : "Communal");
        return isPersonalStorageMode;
    }
    
    /**
     * Get the current storage mode
     * 
     * @return The current storage mode (true = personal, false = communal)
     */
    public boolean isPersonalStorageMode() {
        return isPersonalStorageMode;
    }
    
    /**
     * Process communal storage action when an item is put into a storage slot
     * This sends a packet to the server to add the item to the town's communal storage
     * 
     * @param player The player performing the action
     * @param slotId The slot that was interacted with
     * @param itemStack The specific item stack to add
     * @return true if the action was processed, false otherwise
     */
    public boolean processCommunalStorageAdd(Player player, int slotId, ItemStack itemStack) {
        // Only process slots in the storage inventory (0-17)
        if (slotId >= INVENTORY_SIZE || itemStack.isEmpty()) {
            return false;
        }
        
        // Send a packet to the server to add the item to communal storage
        // We're copying the stack to ensure we don't modify it before the server responds
        ModMessages.sendToServer(new CommunalStoragePacket(townBlockPos, itemStack.copy(), slotId, true));
        
        // Storage slot will be updated when server responds
        return true;
    }
    
    /**
     * Process communal storage removal when an item is taken from a storage slot
     * This sends a packet to the server to remove the item from the town's communal storage
     * 
     * @param player The player performing the action
     * @param slotId The slot that was interacted with
     * @param itemStack The item that was taken
     * @return true if the action was processed, false otherwise
     */
    public boolean processCommunalStorageRemove(Player player, int slotId, ItemStack itemStack) {
        // Only process slots in the storage inventory (0-17)
        if (slotId >= INVENTORY_SIZE || itemStack.isEmpty()) {
            return false;
        }
        
        // Send a packet to the server to remove the item from communal storage
        ModMessages.sendToServer(new CommunalStoragePacket(townBlockPos, itemStack.copy(), slotId, false));
        
        return true;
    }
    
    /**
     * Process personal storage action when an item is put into a storage slot
     * This sends a packet to the server to add the item to the player's personal storage
     * 
     * @param player The player performing the action
     * @param slotId The slot that was interacted with
     * @param itemStack The specific item stack to add
     * @return true if the action was processed, false otherwise
     */
    public boolean processPersonalStorageAdd(Player player, int slotId, ItemStack itemStack) {
        // Only process slots in the storage inventory (0-17)
        if (slotId >= INVENTORY_SIZE || itemStack.isEmpty()) {
            return false;
        }
        
        // Send a packet to the server to add the item to personal storage
        // We're copying the stack to ensure we don't modify it before the server responds
        ModMessages.sendToServer(new PersonalStoragePacket(townBlockPos, itemStack.copy(), slotId, true, player.getUUID()));
        
        // Storage slot will be updated when server responds
        return true;
    }
    
    /**
     * Process personal storage removal when an item is taken from a storage slot
     * This sends a packet to the server to remove the item from the player's personal storage
     * 
     * @param player The player performing the action
     * @param slotId The slot that was interacted with
     * @param itemStack The item that was taken
     * @return true if the action was processed, false otherwise
     */
    public boolean processPersonalStorageRemove(Player player, int slotId, ItemStack itemStack) {
        // Only process slots in the storage inventory (0-17)
        if (slotId >= INVENTORY_SIZE || itemStack.isEmpty()) {
            return false;
        }
        
        // Send a packet to the server to remove the item from personal storage
        ModMessages.sendToServer(new PersonalStoragePacket(townBlockPos, itemStack.copy(), slotId, false, player.getUUID()));
        
        return true;
    }
    
    /**
     * Update the storage inventory with the communal storage items from the server
     * This is called when the client receives a CommunalStorageResponsePacket
     * 
     * @param items Map of items and their counts from the town's communal storage
     */
    public void updateStorageItems(Map<Item, Integer> items) {
        // Only update if we're in communal storage mode
        if (isPersonalStorageMode) {
            LOGGER.info("MENU: Ignoring communal storage update because we're in personal mode");
            return;
        }
        
        LOGGER.info("MENU: Updating communal storage inventory with {} items from server", items.size());
        
        // Log each item being added to communal storage
        items.forEach((storageItem, count) -> {
            LOGGER.info("MENU: Communal storage item: {} x{}", storageItem.getDescription().getString(), count);
        });
        
        updateInventoryWithItems(items);
    }
    
    /**
     * Update the storage inventory with the personal storage items from the server
     * This is called when the client receives a PersonalStorageResponsePacket
     * 
     * @param items Map of items and their counts from the player's personal storage
     */
    public void updatePersonalStorageItems(Map<Item, Integer> items) {
        // Only update if we're in personal storage mode
        if (!isPersonalStorageMode) {
            LOGGER.info("MENU: Ignoring personal storage update because we're in communal mode");
            return;
        }
        
        LOGGER.info("MENU: Updating personal storage inventory with {} items from server", items.size());
        
        // Log each item being added to personal storage
        items.forEach((storageItem, count) -> {
            LOGGER.info("MENU: Personal storage item: {} x{}", storageItem.getDescription().getString(), count);
        });
        
        updateInventoryWithItems(items);
    }
    
    /**
     * Helper method to update the inventory with the provided items
     */
    private void updateInventoryWithItems(Map<Item, Integer> items) {
        LOGGER.info("MENU: Starting updateInventoryWithItems with {} items", items.size());
        
        // Log the current inventory state before clearing
        LOGGER.info("MENU: Current inventory state before clearing:");
        for (int i = 0; i < storageInventory.getSlots(); i++) {
            ItemStack stack = storageInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                LOGGER.info("MENU:   Slot {}: {} x{}", i, stack.getHoverName().getString(), stack.getCount());
            }
        }
        
        // Clear current inventory
        LOGGER.info("MENU: Clearing all {} inventory slots", storageInventory.getSlots());
        for (int i = 0; i < storageInventory.getSlots(); i++) {
            storageInventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        
        // Fill slots with items from the storage
        int slotIndex = 0;
        LOGGER.info("MENU: Filling slots with items from storage map");
        for (Map.Entry<Item, Integer> entry : items.entrySet()) {
            Item item = entry.getKey();
            int count = entry.getValue();
            
            // Skip empty items or zero counts
            if (item == null || count <= 0) {
                LOGGER.info("MENU:   Skipping null or empty item (count: {})", count);
                continue;
            }
            
            // Calculate how many stacks we need
            int maxStackSize = item.getMaxStackSize();
            int fullStacks = count / maxStackSize;
            int remainder = count % maxStackSize;
            
            LOGGER.info("MENU:   Processing item {} - count: {}, maxStackSize: {}, fullStacks: {}, remainder: {}", 
                item.getDescription().getString(), count, maxStackSize, fullStacks, remainder);
            
            // Add full stacks
            for (int i = 0; i < fullStacks && slotIndex < INVENTORY_SIZE; i++) {
                ItemStack stack = new ItemStack(item, maxStackSize);
                LOGGER.info("MENU:     Adding full stack to slot {}: {} x{}", 
                    slotIndex, stack.getHoverName().getString(), stack.getCount());
                storageInventory.setStackInSlot(slotIndex++, stack);
            }
            
            // Add the remainder as a partial stack (if any)
            if (remainder > 0 && slotIndex < INVENTORY_SIZE) {
                ItemStack stack = new ItemStack(item, remainder);
                LOGGER.info("MENU:     Adding remainder to slot {}: {} x{}", 
                    slotIndex, stack.getHoverName().getString(), stack.getCount());
                storageInventory.setStackInSlot(slotIndex++, stack);
            } else if (remainder > 0) {
                LOGGER.warn("MENU:     Cannot add remainder (slot {} >= inventory size {})", 
                    slotIndex, INVENTORY_SIZE);
            }
            
            // Break if we've filled all slots
            if (slotIndex >= INVENTORY_SIZE) {
                LOGGER.warn("Storage inventory full, some items not displayed");
                break;
            }
        }
        
        // Log the final inventory state
        LOGGER.info("MENU: Final inventory state after updates:");
        int filledSlots = 0;
        for (int i = 0; i < storageInventory.getSlots(); i++) {
            ItemStack stack = storageInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                LOGGER.info("MENU:   Slot {}: {} x{}", i, stack.getHoverName().getString(), stack.getCount());
                filledSlots++;
            }
        }
        LOGGER.info("MENU: Update complete - {} out of {} slots filled", filledSlots, storageInventory.getSlots());
        
        // Notify clients of inventory changes (handled by container system)
    }
    
    /**
     * Debug method to log the current state of all storage slots
     */
    public void debugLogStorageState(String context) {
        LOGGER.info("STORAGE-DEBUG [{}]: Current storage state:", context);
        int filledSlots = 0;
        for (int i = 0; i < storageInventory.getSlots(); i++) {
            ItemStack stack = storageInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                LOGGER.info("STORAGE-DEBUG [{}]:   Slot {}: {} x{}", 
                    context, i, stack.getHoverName().getString(), stack.getCount());
                filledSlots++;
            }
        }
        LOGGER.info("STORAGE-DEBUG [{}]: {} out of {} slots filled. Storage mode: {}", 
            context, filledSlots, storageInventory.getSlots(), 
            isPersonalStorageMode ? "PERSONAL" : "COMMUNAL");
    }
} 