package com.quackers29.businesscraft.menu;

import com.quackers29.businesscraft.BusinessCraft;
import com.quackers29.businesscraft.init.ModMenuTypes;
import com.quackers29.businesscraft.town.data.RewardEntry;
import com.quackers29.businesscraft.town.data.TownPaymentBoard;
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
import com.quackers29.businesscraft.network.ModMessages;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
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
    
    // Track if buffer slots are connected to real buffer handler
    private boolean connectedToRealBufferHandler = false;
    
    // Client-side cached rewards (synced from server via packets)
    private List<RewardEntry> cachedRewards = new ArrayList<>();
    
    // Buffer grid positions - centered in wider screen layout
    private static final int BUFFER_START_X = 90; // Centered for 340px screen width
    private static final int BUFFER_START_Y = 140; // Back to original position
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
            // Try to connect to the actual TownBlockEntity's buffer handler on server side only
            if (!playerInventory.player.level().isClientSide()) {
                connectToTownBlockEntity(playerInventory.player.level());
            }
        }
    }
    
    // Constructor with BlockPos
    public PaymentBoardMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, new ItemStackHandler(BUFFER_SIZE));
        this.townBlockPos = pos;
        // Try to connect to the actual TownBlockEntity's buffer handler on server side only
        if (!playerInventory.player.level().isClientSide()) {
            connectToTownBlockEntity(playerInventory.player.level());
        }
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
                this.addSlot(new BufferSlot(this, this.bufferInventory, index, x, y));
            }
        }
        
        // Add slots for player inventory (3 rows of 9) - centered in wider screen
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 
                        90 + col * SLOT_SIZE, 200 + row * SLOT_SIZE)); // Back to y=200
            }
        }
        
        // Add slots for player hotbar (1 row of 9) - centered in wider screen
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 90 + col * SLOT_SIZE, 270)); // Back to y=270
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
                // Allow: Try to move to player inventory (withdrawal-only buffer)
                if (!this.moveItemStackTo(slotStack, BUFFER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } 
            // If shift-clicking from player inventory
            else {
                // BLOCK: Do not allow any movement from player inventory to buffer
                // This prevents unintended stacking into existing buffer stacks
                // Buffer should only be populated by reward claims, not user items
                return ItemStack.EMPTY;
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
    
    // Custom slot implementation for buffer storage - WITHDRAWAL-ONLY for users
    private class BufferSlot extends SlotItemHandler {
        private final PaymentBoardMenu menu;
        
        public BufferSlot(PaymentBoardMenu menu, IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
            this.menu = menu;
        }
        
        // BLOCK user placement of items - withdrawal-only buffer
        // This prevents users from manually adding items while maintaining hopper automation
        @Override
        public boolean mayPlace(ItemStack stack) {
            return false; // Users cannot place items in buffer slots
        }
        
        // Allow taking items from buffer (for withdrawal functionality)
        @Override
        public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
            return true; // Users can always take items from buffer
        }
        
        // Override onTake to sync with server-side buffer storage
        @Override
        public void onTake(net.minecraft.world.entity.player.Player player, ItemStack stack) {
            super.onTake(player, stack);
            
            // Only sync if slots are NOT connected to real buffer handler
            // If they are connected, the TownBufferManager.onContentsChanged will handle the sync
            if (!player.level().isClientSide() && menu.townBlockPos != null && !menu.isConnectedToRealBufferHandler()) {
                menu.processBufferStorageRemove(player, this.getSlotIndex(), stack);
            }
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
     * Check if buffer slots are connected to the real buffer handler
     * @return true if connected to real handler, false if using local handler
     */
    public boolean isConnectedToRealBufferHandler() {
        return connectedToRealBufferHandler;
    }
    
    /**
     * Connect to the TownBlockEntity's actual buffer handler to prevent ghost items
     * This method is only called on the server side
     */
    private void connectToTownBlockEntity(Level level) {
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "connectToTownBlockEntity called on SERVER - townBlockPos: {}", townBlockPos);
            
        if (townBlockPos != null && level != null) {
            BlockEntity blockEntity = level.getBlockEntity(townBlockPos);
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "BlockEntity at {}: {}", townBlockPos, blockEntity);
            
            if (blockEntity instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity townInterfaceEntity) {
                // Get the real buffer handler from the TownInterfaceEntity
                var bufferHandler = townInterfaceEntity.getBufferHandler();
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Got buffer handler: {}", bufferHandler != null);
                
                if (bufferHandler != null) {
                    // Replace our buffer slots with ones connected to the real handler
                    reconnectBufferSlots(bufferHandler);
                    connectedToRealBufferHandler = true;
                    DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Successfully reconnected buffer slots to real handler");
                } else {
                    LOGGER.warn("Buffer handler is null from TownInterfaceEntity");
                }
            } else {
                LOGGER.warn("BlockEntity is not a TownInterfaceEntity: {}", blockEntity);
            }
        } else {
            LOGGER.warn("Cannot connect - townBlockPos: {}, level: {}", townBlockPos, level != null);
        }
    }
    
    /**
     * Reconnect buffer slots to use the real buffer handler
     */
    private void reconnectBufferSlots(net.minecraftforge.items.ItemStackHandler realBufferHandler) {
        // Replace the buffer slots (slots 0-17) with ones connected to the real handler
        for (int i = 0; i < BUFFER_SIZE; i++) {
            if (i < this.slots.size()) {
                // Remove old slot and add new one connected to real handler
                this.slots.set(i, new BufferSlot(this, realBufferHandler, i, 
                    BUFFER_START_X + (i % BUFFER_COLS) * SLOT_SIZE,
                    BUFFER_START_Y + (i / BUFFER_COLS) * SLOT_SIZE));
            }
        }
    }

    /**
     * Gets a TownInterfaceMenu instance for accessing town data
     * 
     * @return A TownInterfaceMenu instance or null if unable to create one
     */
    public com.quackers29.businesscraft.menu.TownInterfaceMenu getTownInterfaceMenu() {
        if (townBlockPos != null) {
            // Get the current player's inventory and minecraft instance
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                Level level = player.level();
                if (level != null) {
                    BlockEntity blockEntity = level.getBlockEntity(townBlockPos);
                    if (blockEntity != null) {
                        // Create a menu with containerId 0 (temporary menu just for data access)
                        return new com.quackers29.businesscraft.menu.TownInterfaceMenu(0, player.getInventory(), townBlockPos);
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
        // Check if we're on the client side
        Player player = Minecraft.getInstance().player;
        if (player != null && player.level().isClientSide()) {
            // Client side: use cached rewards
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                "PaymentBoardMenu.getUnclaimedRewards() - CLIENT SIDE: returning {} cached rewards", 
                cachedRewards.size());
            return new ArrayList<>(cachedRewards);
        }
        
        // Server side: access real town data
        com.quackers29.businesscraft.menu.TownInterfaceMenu townMenu = getTownInterfaceMenu();
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "PaymentBoardMenu.getUnclaimedRewards() - SERVER SIDE: townMenu: {}", townMenu != null);
            
        if (townMenu != null) {
            // Access the town through the town data provider
            var townDataProvider = townMenu.getTownDataProvider();
            if (townDataProvider instanceof com.quackers29.businesscraft.town.Town town) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                    "PaymentBoardMenu.getUnclaimedRewards() - SERVER SIDE: town: {}, townName: {}", 
                    town != null, town != null ? town.getName() : "null");
                    
                List<RewardEntry> rewards = town.getPaymentBoard().getUnclaimedRewards();
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                    "PaymentBoardMenu.getUnclaimedRewards() - SERVER SIDE: found {} rewards in town {}", 
                    rewards.size(), town.getName());
                return rewards;
            }
        }
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "PaymentBoardMenu.getUnclaimedRewards() - returning empty list (no town data)");
        // Fallback: return empty list when no town data is available
        return new ArrayList<>();
    }
    
    /**
     * Update the cached rewards (called by network packet)
     */
    public void updateCachedRewards(List<RewardEntry> rewards) {
        this.cachedRewards = new ArrayList<>(rewards);
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "PaymentBoardMenu.updateCachedRewards() - cached {} rewards", rewards.size());
    }
    
    
    /**
     * Claim a reward by ID
     */
    public void claimReward(UUID rewardId, boolean toBuffer) {
        if (townBlockPos != null) {
            // Send reward claim packet to server
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                "Claiming reward {} to {}", rewardId, toBuffer ? "buffer" : "inventory");
            
            try {
                ModMessages.sendToServer(new com.quackers29.businesscraft.network.packets.storage.PaymentBoardClaimPacket(
                    townBlockPos, rewardId, toBuffer));
            } catch (Exception e) {
                LOGGER.error("Error sending claim request for reward {}", rewardId, e);
            }
        } else {
            LOGGER.warn("Cannot claim reward - no town block position available");
        }
    }
    
    /**
     * Process buffer storage action when an item is put into a buffer slot
     */
    public boolean processBufferStorageAdd(Player player, int slotId, ItemStack itemStack) {
        // Only process slots in the buffer inventory (0-17)
        if (slotId >= BUFFER_SIZE || itemStack.isEmpty() || townBlockPos == null) {
            return false;
        }
        
        // Send a packet to the server to add the item to buffer storage
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Adding {} items to buffer storage at slot {}", itemStack.getCount(), slotId);
        
        try {
            ModMessages.sendToServer(new com.quackers29.businesscraft.network.packets.storage.BufferStoragePacket(
                townBlockPos, itemStack, slotId, true)); // true = add operation
        } catch (Exception e) {
            LOGGER.error("Error sending buffer storage add request", e);
            return false;
        }
        
        return true;
    }
    
    /**
     * Process buffer storage removal when an item is taken from a buffer slot
     */
    public boolean processBufferStorageRemove(Player player, int slotId, ItemStack itemStack) {
        // Only process slots in the buffer inventory (0-17)
        if (slotId >= BUFFER_SIZE || itemStack.isEmpty() || townBlockPos == null) {
            return false;
        }
        
        // Server-side: directly update the Payment Board buffer storage
        if (!player.level().isClientSide()) {
            // Get the town directly from the TownBlockEntity
            if (townBlockPos != null) {
                net.minecraft.world.level.block.entity.BlockEntity blockEntity = player.level().getBlockEntity(townBlockPos);
                if (blockEntity instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity townInterfaceEntity) {
                    // Get town using the same pattern as TownInterfaceMenu
                    java.util.UUID townId = townInterfaceEntity.getTownId();
                    if (townId != null && player.level() instanceof net.minecraft.server.level.ServerLevel sLevel) {
                        com.quackers29.businesscraft.town.Town town = com.quackers29.businesscraft.town.TownManager.get(sLevel).getTown(townId);
                        if (town != null) {
                            // Remove item from Payment Board buffer storage
                            boolean success = town.getPaymentBoard().removeFromBuffer(itemStack.getItem(), itemStack.getCount());
                            
                            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                                "Directly removed {} {} from payment board buffer storage: {}", 
                                itemStack.getCount(), itemStack.getItem(), success);
                            
                            // Trigger buffer change notification to sync UI
                            townInterfaceEntity.onTownBufferChanged();
                            
                            return success;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Request buffer storage data from the server
     */
    public void requestBufferStorageData() {
        if (townBlockPos != null) {
            try {
                // Send a request packet with slotId -1 to get all buffer storage data
                ModMessages.sendToServer(new com.quackers29.businesscraft.network.packets.storage.BufferStoragePacket(
                    townBlockPos, ItemStack.EMPTY, -1, true)); // slotId -1 = data request
            } catch (Exception e) {
                LOGGER.error("Error sending buffer storage data request", e);
            }
        }
    }
    
    /**
     * Update the buffer inventory with items from the server (legacy method)
     * Converts Map<Item, Integer> to slot-based display for backward compatibility
     */
    public void updateBufferStorageItems(Map<Item, Integer> items) {
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Updating buffer storage inventory with {} items from server (legacy format)", items.size());
        
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
    
    /**
     * Update the buffer inventory with slot-based data from server (new method)
     * Preserves exact slot positions using SlotBasedStorage data
     */
    public void updateBufferStorageSlots(com.quackers29.businesscraft.town.data.SlotBasedStorage slotStorage) {
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Updating buffer storage inventory with slot-based data from server");
        
        // Copy each slot directly from SlotBasedStorage to local inventory
        int copySlots = Math.min(bufferInventory.getSlots(), slotStorage.getSlotCount());
        for (int i = 0; i < copySlots; i++) {
            ItemStack slotStack = slotStorage.getSlot(i);
            bufferInventory.setStackInSlot(i, slotStack);
        }
        
        // Clear any remaining slots if local inventory is larger
        for (int i = copySlots; i < bufferInventory.getSlots(); i++) {
            bufferInventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}