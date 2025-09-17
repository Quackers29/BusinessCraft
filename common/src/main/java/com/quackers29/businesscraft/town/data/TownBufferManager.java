package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
// import net.minecraftforge.items.ItemStackHandler;
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
    private final Object bufferHandler = createBufferHandler();
    
    public TownBufferManager(TownInterfaceEntity blockEntity, Level level) {
        this.blockEntity = blockEntity;
        this.level = level;
    }

    /**
     * Create the buffer handler with custom behavior for town buffer synchronization
     */
    private Object createBufferHandler() {
        return PlatformAccess.getItemHandlers().createCustomItemStackHandler(BUFFER_SLOTS, () -> {
            // Only sync if not suppressed (prevents infinite loops during our own syncing)
            if (!suppressBufferCallbacks) {
                // Sync buffer changes back to town data and notify clients
                syncBufferToTownData();
            }
            blockEntity.setChanged();
        });
    }

    public void setTownId(UUID townId) {
        this.townId = townId;
        this.bufferNeedsSync = true; // Force sync when town changes
    }
    
    public Object getBufferHandler() {
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
                Map<Item, Integer> currentTownBuffer = town.getPaymentBoard().getBufferStorage();
                
                // Check if town buffer has changed significantly or if we need initial sync
                if (bufferNeedsSync || !currentTownBuffer.equals(lastKnownTownBuffer)) {
                    // Only sync if buffer is mostly empty or if this is initial sync
                    boolean bufferMostlyEmpty = isBufferMostlyEmpty();
                    
                    if (bufferNeedsSync || bufferMostlyEmpty) {
                        syncTownDataToBuffer(currentTownBuffer);
                        lastKnownTownBuffer = new HashMap<>(currentTownBuffer);
                        bufferNeedsSync = false;
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
        for (int i = 0; i < PlatformAccess.getItemHandlers().getSlots(bufferHandler); i++) {
            ItemStack stack = PlatformAccess.getItemHandlers().getStackInSlot(bufferHandler, i);
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
                // Get the actual SlotBasedStorage from the town
                SlotBasedStorage slotStorage = town.getPaymentBoard().getBufferStorageSlots();
                
                // Suppress callbacks while we're syncing to prevent infinite loops
                suppressBufferCallbacks = true;
                
                try {
                    // Copy each slot directly from SlotBasedStorage to ItemStackHandler
                    for (int i = 0; i < PlatformAccess.getItemHandlers().getSlots(bufferHandler) && i < slotStorage.getSlotCount(); i++) {
                        ItemStack slotStack = slotStorage.getSlot(i);
                        PlatformAccess.getItemHandlers().setStackInSlot(bufferHandler, i, slotStack);
                    }
                    
                    // Clear any remaining slots if handler is larger
                    for (int i = slotStorage.getSlotCount(); i < PlatformAccess.getItemHandlers().getSlots(bufferHandler); i++) {
                        PlatformAccess.getItemHandlers().setStackInSlot(bufferHandler, i, ItemStack.EMPTY);
                    }
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
                SlotBasedStorage slotStorage = town.getPaymentBoard().getBufferStorageSlots();
                boolean bufferChanged = false;
                
                // Copy each slot from ItemStackHandler to SlotBasedStorage
                for (int i = 0; i < PlatformAccess.getItemHandlers().getSlots(bufferHandler) && i < slotStorage.getSlotCount(); i++) {
                    ItemStack handlerStack = PlatformAccess.getItemHandlers().getStackInSlot(bufferHandler, i);
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
                    lastKnownTownBuffer = new HashMap<>(town.getPaymentBoard().getBufferStorage());
                    
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
            ClientSyncHelper.notifyBufferStorageChange(sLevel, townId, town.getPaymentBoard().getBufferStorage());
        }
    }
    
    /**
     * Notifies all clients with the Payment Board UI open of slot-based buffer storage changes
     */
    private void notifyClientsOfSlotBasedBufferChange(Town town) {
        if (level instanceof ServerLevel sLevel) {
            // Send slot-based buffer update packet to all players with Payment Board UI open
            // This ensures real-time UI updates with exact slot preservation when hoppers extract items
            ClientSyncHelper.notifyBufferSlotStorageChange(sLevel, townId, town.getPaymentBoard().getBufferStorageSlots());
        }
    }
}
