package com.yourdomain.businesscraft.ui.managers;

import com.yourdomain.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.packets.ui.PlayerExitUIPacket;
import com.yourdomain.businesscraft.ui.modal.factories.BCModalGridFactory;
import com.yourdomain.businesscraft.ui.modal.specialized.BCModalGridScreen;
import com.yourdomain.businesscraft.ui.components.containers.BCTabPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Manages visitor history modal creation and handling.
 * Extracted from TownInterfaceScreen to improve code organization.
 */
public class VisitorHistoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisitorHistoryManager.class);
    
    /**
     * Creates and shows a visitor history screen.
     * 
     * @param parentScreen The parent screen to return to
     * @param blockPos The position of the town block
     * @param tabPanel The tab panel to set active tab on close (optional)
     * @param onScreenClosed Optional callback when screen is closed
     */
    public static void showVisitorHistoryScreen(
            Screen parentScreen,
            BlockPos blockPos,
            BCTabPanel tabPanel,
            Consumer<BCModalGridScreen<VisitHistoryRecord>> onScreenClosed) {
        
        // Get the block entity and visit history
        List<VisitHistoryRecord> visitHistory = getVisitHistoryFromBlockEntity(blockPos);
        
        // Create town name lookup function
        Function<UUID, String> townNameLookup = createTownNameLookup(blockPos);
        
        // Create modal grid screen with the visitor history data
        BCModalGridScreen<VisitHistoryRecord> visitorScreen = BCModalGridFactory.createVisitorHistoryScreen(
            Component.literal("Town Visitor History"),
            parentScreen,
            visitHistory,
            screen -> {
                // When closing, ensure we're on the population tab
                if (tabPanel != null) {
                    tabPanel.setActiveTab("population");
                }
                if (onScreenClosed != null) {
                    onScreenClosed.accept(screen);
                }
            },
            townNameLookup
        );
        
        // Customize appearance
        visitorScreen.withBackButtonText("Back")
                     .withTitleScale(1.5f)
                     .withRowHeight(20);
        
        // Show the visitor history screen
        Minecraft.getInstance().setScreen(visitorScreen);
    }
    
    /**
     * Retrieves visit history from the town block entity.
     * 
     * @param blockPos The position of the town block
     * @return List of visit history records
     */
    private static List<VisitHistoryRecord> getVisitHistoryFromBlockEntity(BlockPos blockPos) {
        List<VisitHistoryRecord> visitHistory = new ArrayList<>();
        
        if (Minecraft.getInstance() != null && Minecraft.getInstance().level != null) {
            BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(blockPos);
            if (blockEntity instanceof TownBlockEntity townBlock) {
                // Request the town block entity to sync its town data with the server
                ModMessages.sendToServer(new PlayerExitUIPacket(blockPos));
                
                // Get the visit history
                visitHistory.addAll(townBlock.getVisitHistory());
            }
        }
        
        return visitHistory;
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
                // On the client side, we need to rely on the TownBlockEntity to get the town name
                if (Minecraft.getInstance() != null && Minecraft.getInstance().level != null) {
                    BlockEntity blockEntity = Minecraft.getInstance().level.getBlockEntity(blockPos);
                    if (blockEntity instanceof TownBlockEntity townBlock) {
                        // Use the getTownNameFromId method to look up the town name
                        String townName = townBlock.getTownNameFromId(townId);
                        
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