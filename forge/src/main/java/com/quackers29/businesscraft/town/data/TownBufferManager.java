package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the synchronization between town buffer storage and platform-specific inventory handlers for hopper automation.
 * Extracted from TownInterfaceEntity to improve code organization and maintainability.
 * 
 * FORGE VERSION: Platform-specific version that can accept TownInterfaceEntity directly.
 */
public class TownBufferManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBufferManager.class);
    
    // Buffer configuration
    private static final int BUFFER_SLOTS = 18; // 2x9 grid
    
    // References
    private final com.quackers29.businesscraft.block.entity.TownInterfaceEntity blockEntity;
    private final Level level;
    private UUID townId;
    
    // Buffer synchronization tracking
    private Map<Item, Integer> lastKnownTownBuffer = new HashMap<>();
    private boolean bufferNeedsSync = true;
    private boolean suppressBufferCallbacks = false; // Prevents infinite sync loops
    
    // Payment Buffer ItemHandler - 18 slots (2x9) for hopper extraction
    private final ItemStackHandler bufferHandler = new ItemStackHandler(BUFFER_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            // Only sync if not suppressed (prevents infinite loops during our own syncing)
            if (!suppressBufferCallbacks) {
                // Sync buffer changes back to town data and notify clients
                syncBufferToTownData();
            }
            blockEntity.setChanged();
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true; // Allow all items in buffer
        }
    };
    
    public TownBufferManager(com.quackers29.businesscraft.block.entity.TownInterfaceEntity blockEntity, Level level) {
        this.blockEntity = blockEntity;
        this.level = level;
    }
    
    
    public void setTownId(UUID townId) {
        this.townId = townId;
        this.bufferNeedsSync = true; // Force sync when town changes
    }
    
    public ItemStackHandler getBufferHandler() {
        return bufferHandler;
    }
    
    /**
     * Called when the block entity loads to perform initial synchronization
     */
    public void onLoad() {
        if (!level.isClientSide()) {
            bufferNeedsSync = true; // Force initial sync
            syncTownDataToBufferIfNeeded(); // Sync town buffer data to platform inventory
        }
    }
    
    /**
     * Called every few seconds to maintain synchronization
     */
    public void tick() {
        if (!level.isClientSide() && townId != null) {
            // Only sync town data to buffer if buffer is empty or if town storage changed significantly
            // This prevents overwriting items during hopper operations
            syncTownDataToBufferIfNeeded();
        }
    }
    
    /**
     * Called when items are added to town buffer storage externally (e.g., from claim system)
     * Forces a buffer sync to ensure platform inventory reflects the new items
     */
    public void onTownBufferChanged() {
        bufferNeedsSync = true;
        // Immediately sync if we're on server side
        if (level != null && !level.isClientSide()) {
            syncTownDataToBufferIfNeeded();
        }
    }
    
    /**
     * Called when platform inventory contents change (e.g., hopper extraction)
     * Syncs changes back to town data
     */
    public void onInventoryChanged() {
        // Only sync if not suppressed (prevents infinite loops during our own syncing)
        if (!suppressBufferCallbacks) {
            // Sync buffer changes back to town data and notify clients
            syncBufferToTownData();
        }
        // Mark block entity as changed for persistence
        blockEntity.setChanged();
    }
    
    /**
     * Called when platform inventory extraction occurs
     */
    public void onInventoryExtracted() {
        // If not suppressed, trigger sync after extraction
        if (!suppressBufferCallbacks) {
            // Ensure sync happens after extraction
            syncBufferToTownData();
        }
    }
    
    /**
     * Synchronizes town payment buffer data to the platform inventory for hopper access
     * Only syncs when needed to avoid conflicts with hopper operations
     */
    private void syncTownDataToBufferIfNeeded() {
        if (level == null || level.isClientSide() || townId == null) return;
        
        if (level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            if (town != null) {
                // Get the real payment board buffer storage
                TownPaymentBoard paymentBoard = town.getPaymentBoard();
                Map<Item, Integer> currentTownBuffer = paymentBoard.getBufferStorage();
                
                // Check if town buffer has changed significantly or if we need initial sync
                if (bufferNeedsSync || !currentTownBuffer.equals(lastKnownTownBuffer)) {
                    // Only sync if buffer is mostly empty or if this is initial sync
                    boolean bufferMostlyEmpty = isBufferMostlyEmpty();
                    
                    if (bufferNeedsSync || bufferMostlyEmpty) {
                        syncTownDataToBuffer(currentTownBuffer);
                        lastKnownTownBuffer = new HashMap<>(currentTownBuffer);
                        bufferNeedsSync = false;
                        
                        LOGGER.debug("Synced town buffer data to platform inventory for town {}", townId);
                    }
                }
            }
        }
    }
    
    /**
     * Checks if the buffer is mostly empty (less than 3 items total)
     * Platform-specific implementation should override this
     */
    protected boolean isBufferMostlyEmpty() {
        // Default implementation - platform should override
        return true;
    }
    
    /**
     * Synchronizes town payment buffer data to the platform inventory for hopper access
     * Now preserves exact slot positions using SlotBasedStorage
     */
    private void syncTownDataToBuffer(Map<Item, Integer> bufferStorage) {
        if (level == null || level.isClientSide() || townId == null) return;
        
        if (level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            if (town != null) {
                // Get the actual SlotBasedStorage from the town's payment board
                TownPaymentBoard paymentBoard = town.getPaymentBoard();
                SlotBasedStorage slotStorage = paymentBoard.getBufferStorageSlots();
                
                // Suppress callbacks while we're syncing to prevent infinite loops
                suppressBufferCallbacks = true;
                
                try {
                    // Platform-specific sync implementation
                    syncSlotsToInventory(slotStorage);
                    
                    LOGGER.debug("Synced {} slots from payment board buffer to platform inventory", slotStorage.getSlotCount());
                } finally {
                    // Always re-enable callbacks
                    suppressBufferCallbacks = false;
                }
            }
        }
    }
    
    /**
     * Platform-specific method to sync SlotBasedStorage to platform inventory
     * Forge implementation: Updates ItemStackHandler from town buffer storage
     */
    protected void syncSlotsToInventory(SlotBasedStorage slotStorage) {
        boolean anyChanges = false;
        
        // Sync each slot from SlotBasedStorage to ItemStackHandler
        for (int i = 0; i < Math.min(slotStorage.getSlotCount(), bufferHandler.getSlots()); i++) {
            ItemStack slotStack = slotStorage.getSlot(i);
            ItemStack handlerStack = bufferHandler.getStackInSlot(i);
            
            // Check if the stacks are different
            if (!ItemStack.matches(slotStack, handlerStack)) {
                // Update the ItemStackHandler with the current SlotBasedStorage contents
                bufferHandler.setStackInSlot(i, slotStack.copy());
                anyChanges = true;
                
                LOGGER.debug("Synced slot {} to inventory: {} -> {}", i,
                    handlerStack.isEmpty() ? "empty" : handlerStack.getCount() + "x" + handlerStack.getItem(),
                    slotStack.isEmpty() ? "empty" : slotStack.getCount() + "x" + slotStack.getItem());
            }
        }
        
        // CRITICAL: Notify clients of inventory changes if any slots were updated
        if (anyChanges) {
            blockEntity.setChanged();
            
            // Force block update to synchronize container to client
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
                net.minecraft.world.level.block.state.BlockState state = serverLevel.getBlockState(pos);
                serverLevel.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
                
                LOGGER.debug("Forced client sync for buffer inventory changes at position {}", pos);
            }
        }
    }
    
    /**
     * Synchronizes platform inventory changes back to town payment buffer data
     * Now preserves exact slot positions using SlotBasedStorage
     */
    private void syncBufferToTownData() {
        if (level == null || level.isClientSide() || townId == null) {
            return;
        }
        
        if (level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            if (town != null) {
                // Get the SlotBasedStorage and update it directly from platform inventory
                TownPaymentBoard paymentBoard = town.getPaymentBoard();
                SlotBasedStorage slotStorage = paymentBoard.getBufferStorageSlots();
                
                // Platform-specific sync implementation
                boolean bufferChanged = syncInventoryToSlots(slotStorage);
                
                // Update our tracking if buffer changed
                if (bufferChanged) {
                    // Update our tracking with the current buffer storage state
                    lastKnownTownBuffer = new HashMap<>(paymentBoard.getBufferStorage());
                    
                    LOGGER.debug("Synced {} slots from platform inventory back to payment board buffer", slotStorage.getSlotCount());
                    
                    // Notify clients of buffer storage changes for UI updates using new slot-based method
                    notifyClientsOfSlotBasedBufferChange(town);
                } else {
                    // Always notify clients, even if no changes detected, in case of sync issues
                    notifyClientsOfSlotBasedBufferChange(town);
                }
            }
        }
    }
    
    /**
     * Platform-specific method to sync platform inventory to SlotBasedStorage
     * Forge implementation: Updates town buffer storage from ItemStackHandler
     * @return true if any changes were made
     */
    protected boolean syncInventoryToSlots(SlotBasedStorage slotStorage) {
        boolean changed = false;
        
        // Sync each slot from ItemStackHandler to SlotBasedStorage
        for (int i = 0; i < Math.min(bufferHandler.getSlots(), slotStorage.getSlotCount()); i++) {
            ItemStack handlerStack = bufferHandler.getStackInSlot(i);
            ItemStack slotStack = slotStorage.getSlot(i);
            
            // Check if the stacks are different
            if (!ItemStack.matches(handlerStack, slotStack)) {
                // Update the SlotBasedStorage with the current ItemStackHandler contents
                slotStorage.setSlot(i, handlerStack.copy());
                changed = true;
                
                LOGGER.debug("Synced slot {}: {} -> {}", i, 
                    slotStack.isEmpty() ? "empty" : slotStack.getCount() + "x" + slotStack.getItem(),
                    handlerStack.isEmpty() ? "empty" : handlerStack.getCount() + "x" + handlerStack.getItem());
            }
        }
        
        return changed;
    }
    
    /**
     * Notifies all clients with the Payment Board UI open of slot-based buffer storage changes
     */
    private void notifyClientsOfSlotBasedBufferChange(Town town) {
        if (level instanceof ServerLevel sLevel) {
            // Send slot-based buffer update packet to all players with Payment Board UI open
            // This ensures real-time UI updates with exact slot preservation when hoppers extract items
            TownPaymentBoard paymentBoard = town.getPaymentBoard();
            ClientSyncHelper.notifyBufferSlotStorageChange(sLevel, townId, paymentBoard.getBufferStorageSlots());
            LOGGER.debug("Notified clients of slot-based buffer storage changes for town {}", townId);
        }
    }
    
    /**
     * Get the number of buffer slots
     */
    public int getBufferSlots() {
        return BUFFER_SLOTS;
    }
    
    /**
     * Check if callbacks are currently suppressed
     */
    public boolean areCallbacksSuppressed() {
        return suppressBufferCallbacks;
    }
}