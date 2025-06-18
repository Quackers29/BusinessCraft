package com.yourdomain.businesscraft.menu;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.init.ModMenuTypes;
import com.yourdomain.businesscraft.town.data.RewardEntry;
import com.yourdomain.businesscraft.town.data.TownPaymentBoard;
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
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.debug.DebugConfig;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.menu.TownBlockMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.UUID;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class PaymentBoardMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentBoardMenu.class);
    
    // Constants for the number of slots
    private static final int BUFFER_ROWS = 2;
    private static final int BUFFER_COLS = 9;
    private static final int BUFFER_SIZE = BUFFER_ROWS * BUFFER_COLS; // 18 slots for buffer storage
    
    // ItemHandlers for the buffer inventory
    private final ItemStackHandler bufferInventory;
    
    // Store the position of the town block
    private BlockPos townBlockPos;
    
    // Buffer grid positions - dramatically increased spacing for much better UI layout
    private static final int BUFFER_START_X = 8;
    private static final int BUFFER_START_Y = 140; // Moved way down from 110 to create massive space after payment board
    private static final int SLOT_SIZE = 18;
    
    // Constructor for client-side creation
    public PaymentBoardMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new ItemStackHandler(BUFFER_SIZE));
    }
    
    // Constructor for server-side creation
    public PaymentBoardMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new ItemStackHandler(BUFFER_SIZE));
        // Read the BlockPos from the extra data if available
        if (extraData != null && extraData.readableBytes() > 0) {
            this.townBlockPos = extraData.readBlockPos();
        }
    }
    
    // Constructor with BlockPos
    public PaymentBoardMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, new ItemStackHandler(BUFFER_SIZE));
        this.townBlockPos = pos;
    }
    
    // Main constructor
    public PaymentBoardMenu(int containerId, Inventory playerInventory, IItemHandler bufferInventory) {
        super(ModMenuTypes.PAYMENT_BOARD_MENU.get(), containerId);
        
        // Create the buffer inventory if it doesn't exist
        if (bufferInventory instanceof ItemStackHandler) {
            this.bufferInventory = (ItemStackHandler) bufferInventory;
        } else {
            this.bufferInventory = new ItemStackHandler(BUFFER_SIZE);
        }
        
        // Add slots for the buffer inventory (2 rows of 9) - dramatically expanded positions
        for (int row = 0; row < BUFFER_ROWS; row++) {
            for (int col = 0; col < BUFFER_COLS; col++) {
                int index = col + row * BUFFER_COLS;
                int x = BUFFER_START_X + col * SLOT_SIZE;
                int y = BUFFER_START_Y + row * SLOT_SIZE;
                this.addSlot(new BufferSlot(this.bufferInventory, index, x, y));
            }
        }
        
        // Add slots for player inventory (3 rows of 9) - moved way down for massive spacing
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 
                        8 + col * SLOT_SIZE, 200 + row * SLOT_SIZE)); // Moved way down from 158
            }
        }
        
        // Add slots for player hotbar (1 row of 9) - moved way down for massive spacing
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * SLOT_SIZE, 270)); // Moved way down from 220
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
            
            // If shift-clicking from the buffer inventory
            if (index < BUFFER_SIZE) {
                // Try to move to player inventory
                if (!this.moveItemStackTo(slotStack, BUFFER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } 
            // If shift-clicking from player inventory
            else {
                // Try to move to buffer inventory
                if (!this.moveItemStackTo(slotStack, 0, BUFFER_SIZE, false)) {
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
    
    // Custom slot implementation for buffer storage
    private static class BufferSlot extends SlotItemHandler {
        public BufferSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }
        
        // Allow any item to be placed in buffer slots
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
     * Get unclaimed rewards from the town payment board
     */
    public List<RewardEntry> getUnclaimedRewards() {
        TownBlockMenu townMenu = getTownBlockMenu();
        if (townMenu != null) {
            // Access the town's payment board through the town menu
            // This will need to be implemented when we integrate with Town.java
            // For now, return empty list
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }
    
    /**
     * Claim a reward by ID
     */
    public void claimReward(UUID rewardId, boolean toBuffer) {
        if (townBlockPos != null) {
            // Send reward claim packet to server
            // This will need to be implemented with proper networking
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                "Claiming reward {} to {}", rewardId, toBuffer ? "buffer" : "inventory");
        }
    }
    
    /**
     * Process buffer storage action when an item is put into a buffer slot
     */
    public boolean processBufferStorageAdd(Player player, int slotId, ItemStack itemStack) {
        // Only process slots in the buffer inventory (0-17)
        if (slotId >= BUFFER_SIZE || itemStack.isEmpty()) {
            return false;
        }
        
        // Send a packet to the server to add the item to buffer storage
        // For now, just log the action
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Adding {} items to buffer storage at slot {}", itemStack.getCount(), slotId);
        
        return true;
    }
    
    /**
     * Process buffer storage removal when an item is taken from a buffer slot
     */
    public boolean processBufferStorageRemove(Player player, int slotId, ItemStack itemStack) {
        // Only process slots in the buffer inventory (0-17)
        if (slotId >= BUFFER_SIZE || itemStack.isEmpty()) {
            return false;
        }
        
        // Send a packet to the server to remove the item from buffer storage
        // For now, just log the action
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Removing {} items from buffer storage at slot {}", itemStack.getCount(), slotId);
        
        return true;
    }
    
    /**
     * Update the buffer inventory with items from the server
     */
    public void updateBufferStorageItems(Map<Item, Integer> items) {
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Updating buffer storage inventory with {} items from server", items.size());
        
        // Clear current inventory
        for (int i = 0; i < bufferInventory.getSlots(); i++) {
            bufferInventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        
        // Fill slots with items from the buffer storage
        int slotIndex = 0;
        for (Map.Entry<Item, Integer> entry : items.entrySet()) {
            Item item = entry.getKey();
            int count = entry.getValue();
            
            // Skip empty items or zero counts
            if (item == null || count <= 0) {
                continue;
            }
            
            // Calculate how many stacks we need
            int maxStackSize = item.getMaxStackSize();
            int fullStacks = count / maxStackSize;
            int remainder = count % maxStackSize;
            
            // Add full stacks
            for (int i = 0; i < fullStacks && slotIndex < BUFFER_SIZE; i++) {
                ItemStack stack = new ItemStack(item, maxStackSize);
                bufferInventory.setStackInSlot(slotIndex++, stack);
            }
            
            // Add the remainder as a partial stack (if any)
            if (remainder > 0 && slotIndex < BUFFER_SIZE) {
                ItemStack stack = new ItemStack(item, remainder);
                bufferInventory.setStackInSlot(slotIndex++, stack);
            }
            
            // Break if we've filled all slots
            if (slotIndex >= BUFFER_SIZE) {
                LOGGER.warn("Buffer inventory full, some items not displayed");
                break;
            }
        }
    }
}