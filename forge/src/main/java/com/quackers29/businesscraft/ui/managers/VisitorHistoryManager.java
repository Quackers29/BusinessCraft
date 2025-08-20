package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.network.packets.ui.PlayerExitUIPacket;
import com.quackers29.businesscraft.ui.modal.factories.BCModalGridFactory;
import com.quackers29.businesscraft.ui.modal.specialized.BCModalGridScreen;
import com.quackers29.businesscraft.ui.components.containers.BCTabPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Manages visitor history modal creation and handling.
 * Extracted from TownInterfaceScreen to improve code organization.
 * Refactored to use BaseModalManager for common functionality.
 */
public class VisitorHistoryManager extends BaseModalManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisitorHistoryManager.class);
    
    /**
     * Creates and shows a visitor history screen.
     * 
     * @param parentScreen The parent screen to return to
     * @param blockPos The position of the town block
     * @param targetTab The tab to return to when modal closes (optional, defaults to "population")
     * @param onScreenClosed Optional callback when screen is closed
     */
    public static void showVisitorHistoryScreen(
            Screen parentScreen,
            BlockPos blockPos,
            String targetTab,
            Consumer<BCModalGridScreen<VisitHistoryRecord>> onScreenClosed) {
        
        // Get the block entity and visit history
        List<VisitHistoryRecord> visitHistory = getVisitHistoryFromBlockEntity(blockPos);
        
        // Create town name lookup function
        Function<UUID, String> townNameLookup = createTownNameLookup(blockPos);
        
        // Validate inputs and prepare parent screen
        validateParentScreen(parentScreen, "parentScreen");
        prepareParentScreen(parentScreen);
        
        // Create modal grid screen with the visitor history data using standardized callback
        BCModalGridScreen<VisitHistoryRecord> visitorScreen = BCModalGridFactory.createVisitorHistoryScreen(
            Component.literal("Town Visitor History"),
            parentScreen,
            visitHistory,
            createStandardCallback(parentScreen, onScreenClosed),
            townNameLookup
        );
        
        // Customize appearance
        visitorScreen.withBackButtonText("Back")
                     .withTitleScale(1.5f)
                     .withRowHeight(20);
        
        // Show the visitor history screen
        displayModal(visitorScreen);
    }
    
    /**
     * Retrieves visit history from the town block entity.
     * 
     * @param blockPos The position of the town block
     * @return List of visit history records
     */
    private static List<VisitHistoryRecord> getVisitHistoryFromBlockEntity(BlockPos blockPos) {
        List<VisitHistoryRecord> visitHistory = new ArrayList<>();
        
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
            "VISIT HISTORY DEBUG - VisitorHistoryManager.getVisitHistoryFromBlockEntity() called for pos {}", blockPos);
        
        if (Minecraft.getInstance() != null && Minecraft.getInstance().level != null) {
            BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(blockPos);
            DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
                "VISIT HISTORY DEBUG - BlockEntity found: {}", blockEntity != null ? blockEntity.getClass().getSimpleName() : "null");
                
            if (blockEntity instanceof TownInterfaceEntity townInterface) {
                // Request the town block entity to sync its town data with the server
                ModMessages.sendToServer(new PlayerExitUIPacket(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                
                // Brief delay to allow server sync to complete
                try {
                    Thread.sleep(100); // 100ms delay for sync
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Get the visit history
                List<ITownDataProvider.VisitHistoryRecord> rawHistory = townInterface.getVisitHistory();
                DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
                    "VISIT HISTORY DEBUG - TownInterface returned {} raw records", rawHistory.size());
                    
                for (ITownDataProvider.VisitHistoryRecord record : rawHistory) {
                    DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
                        "VISIT HISTORY DEBUG - Converting record: {} tourists from {} at timestamp {}", 
                        record.getCount(), record.getOriginTownId(), record.getTimestamp());
                        
                    // Create Position object for the constructor  
                    ITownDataProvider.Position pos = new ITownDataProvider.Position() {
                        @Override
                        public int getX() { return record.getOriginPos().getX(); }
                        @Override
                        public int getY() { return record.getOriginPos().getY(); }
                        @Override
                        public int getZ() { return record.getOriginPos().getZ(); }
                    };
                    
                    visitHistory.add(new VisitHistoryRecord(
                        record.getTimestamp(),
                        record.getOriginTownId(),
                        record.getCount(),
                        pos
                    ));
                }
                
                DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
                    "VISIT HISTORY DEBUG - Final converted visitHistory size: {}", visitHistory.size());
            }
        }
        
        // Smart deduplication: merge records from same town with same count within short time window
        // This handles cases where migration creates records with BlockPos.ZERO and new records have coordinates
        List<VisitHistoryRecord> deduplicated = new ArrayList<>();
        Map<String, VisitHistoryRecord> visitMap = new HashMap<>();
        
        for (VisitHistoryRecord record : visitHistory) {
            // Create key based on town ID, count, and 1-minute time window to catch near-simultaneous duplicates
            long timeWindow = record.getTimestamp() / (60 * 1000); // 1-minute windows  
            String key = record.getOriginTownId() + "_" + record.getCount() + "_" + timeWindow;
            
            VisitHistoryRecord existing = visitMap.get(key);
            if (existing == null) {
                visitMap.put(key, record);
            } else {
                // Choose the record with better coordinate data (non-zero coordinates preferred)
                VisitHistoryRecord better = chooseBetterRecord(existing, record);
                visitMap.put(key, better);
                
                DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
                    "VISIT HISTORY DEBUG - Merged duplicate records from town {} with count {}", 
                    record.getOriginTownId(), record.getCount());
            }
        }
        
        deduplicated.addAll(visitMap.values());
        
        // Sort by timestamp descending (newest first) to match expected behavior
        deduplicated.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
            "VISIT HISTORY DEBUG - Deduplicated {} raw records into {} unique visits", 
            visitHistory.size(), deduplicated.size());
        
        return deduplicated;
    }
    
    /**
     * Chooses the better of two visit history records, preferring the one with actual coordinates.
     * 
     * @param record1 First record
     * @param record2 Second record  
     * @return The record with better data (coordinates preferred over BlockPos.ZERO)
     */
    private static VisitHistoryRecord chooseBetterRecord(VisitHistoryRecord record1, VisitHistoryRecord record2) {
        // Use the later timestamp
        long laterTimestamp = Math.max(record1.getTimestamp(), record2.getTimestamp());
        
        // Check if either record has meaningful coordinates (not 0,0,0)
        boolean record1HasCoords = hasValidCoordinates(record1);
        boolean record2HasCoords = hasValidCoordinates(record2);
        
        // Prefer the record with valid coordinates, or use the later one if both/neither have coords
        VisitHistoryRecord chosen;
        if (record1HasCoords && !record2HasCoords) {
            chosen = record1;
        } else if (record2HasCoords && !record1HasCoords) {
            chosen = record2;
        } else {
            // Both have coordinates or both lack coordinates, use the later timestamp
            chosen = record1.getTimestamp() >= record2.getTimestamp() ? record1 : record2;
        }
        
        // Create a new record with the later timestamp but the chosen record's other data
        return new VisitHistoryRecord(
            laterTimestamp,
            chosen.getOriginTownId(), 
            chosen.getCount(),
            chosen.getOriginPos()
        );
    }
    
    /**
     * Checks if a visit history record has valid (non-zero) coordinates.
     * 
     * @param record The visit history record to check
     * @return true if the record has coordinates other than (0,0,0)
     */
    private static boolean hasValidCoordinates(VisitHistoryRecord record) {
        if (record.getOriginPos() == null) return false;
        
        int x = record.getOriginPos().getX();
        int y = record.getOriginPos().getY(); 
        int z = record.getOriginPos().getZ();
        
        // Consider (0,0,0) as invalid coordinates (likely from migration default)
        return !(x == 0 && y == 0 && z == 0);
    }
    
    /**
     * Creates a town name lookup function for resolving town UUIDs to names.
     * 
     * @param blockPos The position of the town block
     * @return Function that maps town UUIDs to names
     */
    private static Function<UUID, String> createTownNameLookup(BlockPos blockPos) {
        return townId -> {
            if (townId == null) return "Unknown";
            
            try {
                // On the client side, we need to rely on the TownInterfaceEntity to get the town name
                if (Minecraft.getInstance() != null && Minecraft.getInstance().level != null) {
                    BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(blockPos);
                    if (blockEntity instanceof TownInterfaceEntity townInterface) {
                        // Use the getTownNameFromId method to look up the town name
                        String townName = townInterface.getTownNameFromId(townId);
                        
                        // If we got a name, return it
                        if (townName != null) {
                            return townName;
                        }
                    }
                }
            } catch (Exception e) {
                // Log the error but don't crash
                LOGGER.error("Error looking up town name for UUID {}: {}", townId, e.getMessage());
            }
            
            // Fallback if town not found
            return "Town-" + townId.toString().substring(0, 8);
        };
    }
} 