package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.town.data.TownPaymentBoard;
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
 * Manages the synchronization between town buffer storage and ItemStackHandler for hopper automation.
 * Extracted from TownInterfaceEntity to improve code organization and maintainability.
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
            // Buffer is extraction-only for hoppers, no insertion allowed
            return false;
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Allow extraction for hopper automation
            ItemStack extracted = super.extractItem(slot, amount, simulate);
            
            // If not simulating and we actually extracted something, trigger sync
            if (!simulate && !extracted.isEmpty() && !suppressBufferCallbacks) {
                // Ensure sync happens after extraction
                syncBufferToTownData();
            }
            
            return extracted;
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // Block insertion - buffer is managed internally
            return stack;
        }
    };
    
    public TownBufferManager(TownInterfaceEntity blockEntity, Level level) {
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
            syncTownDataToBufferIfNeeded(); // Sync town buffer data to ItemStackHandler
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
     * Forces a buffer sync to ensure ItemStackHandler reflects the new items
     */
    public void onTownBufferChanged() {
        bufferNeedsSync = true;
        // Immediately sync if we're on server side
        if (level != null && !level.isClientSide()) {
            syncTownDataToBufferIfNeeded();
        }
    }
    
    /**
     * Synchronizes town payment buffer data to the ItemStackHandler for hopper access
     * Only syncs when needed to avoid conflicts with hopper operations
     */
    private void syncTownDataToBufferIfNeeded() {
        if (level == null || level.isClientSide() || townId == null) return;
        
        if (level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            if (town != null) {
                // Get the real payment board buffer storage
                TownPaymentBoard paymentBoard = (TownPaymentBoard) town.getPaymentBoard();
                Map<Item, Integer> currentTownBuffer = paymentBoard.getBufferStorage();
                
                // Check if town buffer has changed significantly or if we need initial sync
                if (bufferNeedsSync || !currentTownBuffer.equals(lastKnownTownBuffer)) {
                    // Only sync if buffer is mostly empty or if this is initial sync
                    boolean bufferMostlyEmpty = isBufferMostlyEmpty();
                    
                    if (bufferNeedsSync || bufferMostlyEmpty) {
                        syncTownDataToBuffer(currentTownBuffer);
                        lastKnownTownBuffer = new HashMap<>(currentTownBuffer);
                        bufferNeedsSync = false;
                        
                        LOGGER.debug("Synced town buffer data to ItemStackHandler for town {}", townId);
                    }
                }
            }
        }
    }
    
    /**
     * Checks if the buffer handler is mostly empty (less than 3 items total)
     */
    private boolean isBufferMostlyEmpty() {
        int totalItems = 0;
        for (int i = 0; i < bufferHandler.getSlots(); i++) {
            ItemStack stack = bufferHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                totalItems += stack.getCount();
            }
        }
        return totalItems < 3; // Consider mostly empty if less than 3 items
    }
    
    /**
     * Synchronizes town payment buffer data to the ItemStackHandler for hopper access
     * Now preserves exact slot positions using SlotBasedStorage
     */
    private void syncTownDataToBuffer(Map<Item, Integer> bufferStorage) {
        if (level == null || level.isClientSide() || townId == null) return;
        
        if (level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            if (town != null) {
                // Get the actual SlotBasedStorage from the town's payment board
                TownPaymentBoard paymentBoard = (TownPaymentBoard) town.getPaymentBoard();
                SlotBasedStorage slotStorage = paymentBoard.getBufferStorageSlots();
                
                // Suppress callbacks while we're syncing to prevent infinite loops
                suppressBufferCallbacks = true;
                
                try {
                    // Copy each slot directly from SlotBasedStorage to ItemStackHandler
                    for (int i = 0; i < bufferHandler.getSlots() && i < slotStorage.getSlotCount(); i++) {
                        ItemStack slotStack = slotStorage.getSlot(i);
                        bufferHandler.setStackInSlot(i, slotStack);
                    }
                    
                    // Clear any remaining slots if handler is larger
                    for (int i = slotStorage.getSlotCount(); i < bufferHandler.getSlots(); i++) {
                        bufferHandler.setStackInSlot(i, ItemStack.EMPTY);
                    }
                    
                    LOGGER.debug("Synced {} slots from payment board buffer to ItemStackHandler", slotStorage.getSlotCount());
                } finally {
                    // Always re-enable callbacks
                    suppressBufferCallbacks = false;
                }
            }
        }
    }
    
    /**
     * Synchronizes ItemStackHandler changes back to town payment buffer data
     * Now preserves exact slot positions using SlotBasedStorage
     */
    private void syncBufferToTownData() {
        if (level == null || level.isClientSide() || townId == null) {
            return;
        }
        
        if (level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            if (town != null) {
                // Get the SlotBasedStorage and update it directly from ItemStackHandler
                TownPaymentBoard paymentBoard = (TownPaymentBoard) town.getPaymentBoard();
                SlotBasedStorage slotStorage = paymentBoard.getBufferStorageSlots();
                
                boolean bufferChanged = false;
                
                // Copy each slot from ItemStackHandler to SlotBasedStorage
                for (int i = 0; i < bufferHandler.getSlots() && i < slotStorage.getSlotCount(); i++) {
                    ItemStack handlerStack = bufferHandler.getStackInSlot(i);
                    ItemStack storageStack = slotStorage.getSlot(i);
                    
                    // Check if slot contents changed
                    if (!ItemStack.isSameItemSameTags(handlerStack, storageStack) || 
                        handlerStack.getCount() != storageStack.getCount()) {
                        
                        slotStorage.setSlot(i, handlerStack);
                        bufferChanged = true;
                    }
                }
                
                // Update our tracking if buffer changed
                if (bufferChanged) {
                    // Update our tracking with the current buffer storage state
                    lastKnownTownBuffer = new HashMap<>(paymentBoard.getBufferStorage());
                    
                    LOGGER.debug("Synced {} slots from ItemStackHandler back to payment board buffer", slotStorage.getSlotCount());
                    
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
     * Notifies all clients with the Payment Board UI open of buffer storage changes (legacy method)
     */
    private void notifyClientsOfBufferChange(Town town) {
        if (level instanceof ServerLevel sLevel) {
            // Send legacy buffer update packet to all players with Payment Board UI open
            // This ensures real-time UI updates when hoppers extract items
            TownPaymentBoard paymentBoard = (TownPaymentBoard) town.getPaymentBoard();
            ClientSyncHelper.notifyBufferStorageChange(sLevel, townId, paymentBoard.getBufferStorage());
            LOGGER.debug("Notified clients of buffer storage changes for town {}", townId);
        }
    }
    
    /**
     * Notifies all clients with the Payment Board UI open of slot-based buffer storage changes
     */
    private void notifyClientsOfSlotBasedBufferChange(Town town) {
        if (level instanceof ServerLevel sLevel) {
            // Send slot-based buffer update packet to all players with Payment Board UI open
            // This ensures real-time UI updates with exact slot preservation when hoppers extract items
            TownPaymentBoard paymentBoard = (TownPaymentBoard) town.getPaymentBoard();
            ClientSyncHelper.notifyBufferSlotStorageChange(sLevel, townId, paymentBoard.getBufferStorageSlots());
            LOGGER.debug("Notified clients of slot-based buffer storage changes for town {}", townId);
        }
    }
}