package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
 * UNIFIED ARCHITECTURE: Platform-agnostic core with minimal platform abstractions for inventory handling.
 */
public class TownBufferManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBufferManager.class);
    
    // Buffer configuration
    private static final int BUFFER_SLOTS = 18; // 2x9 grid
    
    // References
    private final TownInterfaceEntity blockEntity;
    private final Level level;
    private UUID townId;
    
    // Buffer synchronization tracking
    private Map<Item, Integer> lastKnownTownBuffer = new HashMap<>();
    private boolean bufferNeedsSync = true;
    private boolean suppressBufferCallbacks = false; // Prevents infinite sync loops
    
    public TownBufferManager(TownInterfaceEntity blockEntity, Level level) {
        this.blockEntity = blockEntity;
        this.level = level;
    }
    
    public void setTownId(UUID townId) {
        this.townId = townId;
        this.bufferNeedsSync = true; // Force sync when town changes
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
     * Should be implemented by platform-specific subclasses
     */
    protected void syncSlotsToInventory(SlotBasedStorage slotStorage) {
        // Default no-op - platform implementations should override
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
     * Should be implemented by platform-specific subclasses
     * @return true if any changes were made
     */
    protected boolean syncInventoryToSlots(SlotBasedStorage slotStorage) {
        // Default no-op - platform implementations should override
        return false;
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