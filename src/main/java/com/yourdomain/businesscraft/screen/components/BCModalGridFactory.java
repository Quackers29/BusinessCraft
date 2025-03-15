package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import com.yourdomain.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.Item;

/**
 * Factory class for creating common types of modal grid screens
 */
public class BCModalGridFactory {

    /**
     * Common color themes for modal grids
     */
    public static class Themes {
        // BusinessCraft default theme (Bluish)
        public static final int[] BC_DEFAULT = {
            0xFF000000,    // Background
            0xFFDDDDDD,    // Border
            0xFFFFFFFF,    // Title
            0xFFDDFFFF,    // Header
            0xFFFFFFFF     // Text
        };
        
        // Dark theme
        public static final int[] DARK = {
            0xFF101010,    // Background
            0xFF404040,    // Border
            0xFFCCCCCC,    // Title
            0xFF999999,    // Header
            0xFFCCCCCC     // Text
        };
        
        // Light theme
        public static final int[] LIGHT = {
            0xFFEEEEEE,    // Background
            0xFF999999,    // Border
            0xFF000000,    // Title
            0xFF333333,    // Header
            0xFF000000     // Text
        };
        
        // Success theme (Greenish)
        public static final int[] SUCCESS = {
            0xFF001000,    // Background
            0xFF006600,    // Border
            0xFFCCFFCC,    // Title
            0xFF99CC99,    // Header
            0xFFDDFFDD     // Text
        };
        
        // Danger theme (Reddish)
        public static final int[] DANGER = {
            0xFF100000,    // Background
            0xFF660000,    // Border
            0xFFFFCCCC,    // Title
            0xFFCC9999,    // Header
            0xFFFFDDDD     // Text
        };
    }
    
    /**
     * Create a visitor history screen that displays town visit history
     * 
     * @param title Screen title
     * @param parentScreen Parent screen to return to
     * @param visitHistory List of visit history records
     * @param onCloseCallback Optional callback when closing screen
     * @param townNameLookup Function to convert town UUID to name
     * @return Configured BCModalGridScreen
     */
    public static BCModalGridScreen<VisitHistoryRecord> createVisitorHistoryScreen(
            Component title,
            Screen parentScreen,
            List<VisitHistoryRecord> visitHistory,
            Consumer<BCModalGridScreen<VisitHistoryRecord>> onCloseCallback,
            Function<UUID, String> townNameLookup) {
        
        // Create the screen
        BCModalGridScreen<VisitHistoryRecord> screen = new BCModalGridScreen<>(
            title,
            parentScreen,
            onCloseCallback
        );
        
        // Configure with standard visitor history columns
        screen.addColumn("Time", record -> formatTime(record.getTimestamp()))
              .addColumn("Origin", record -> formatOrigin(record, townNameLookup))
              .addColumn("Count", record -> String.valueOf(record.getCount()))
              .withData(visitHistory)
              .withBackButtonText("Back")
              .withColors(
                  Themes.BC_DEFAULT[0],  // Background
                  Themes.BC_DEFAULT[1],  // Border
                  Themes.BC_DEFAULT[2],  // Title
                  Themes.BC_DEFAULT[3],  // Header
                  Themes.BC_DEFAULT[4]   // Text
              )
              .withAlternatingRowColors(0x30FFFFFF, 0x20FFFFFF);
        
        return screen;
    }
    
    /**
     * Overloaded version without town name lookup function (for backwards compatibility)
     */
    public static BCModalGridScreen<VisitHistoryRecord> createVisitorHistoryScreen(
            Component title,
            Screen parentScreen,
            List<VisitHistoryRecord> visitHistory,
            Consumer<BCModalGridScreen<VisitHistoryRecord>> onCloseCallback) {
        
        // Use a default town name lookup that just shows the UUID
        return createVisitorHistoryScreen(
            title, 
            parentScreen, 
            visitHistory, 
            onCloseCallback,
            uuid -> uuid != null ? "Town-" + uuid.toString().substring(0, 8) : "Unknown"
        );
    }
    
    /**
     * Create a resource list screen that displays items and their quantities
     * 
     * @param title Screen title
     * @param parentScreen Parent screen to return to
     * @param resources Map of items and their quantities
     * @param onCloseCallback Optional callback when closing screen
     * @return Configured BCModalGridScreen
     */
    public static BCModalGridScreen<Map.Entry<Item, Integer>> createResourceListScreen(
            Component title,
            Screen parentScreen,
            Map<Item, Integer> resources,
            Consumer<BCModalGridScreen<Map.Entry<Item, Integer>>> onCloseCallback) {
        
        // Create the screen
        BCModalGridScreen<Map.Entry<Item, Integer>> screen = new BCModalGridScreen<>(
            title,
            parentScreen,
            onCloseCallback
        );
        
        // Convert map to list
        List<Map.Entry<Item, Integer>> resourceList = resources.entrySet().stream().toList();
        
        // Configure with resource columns
        screen.addColumn("Item", entry -> entry.getKey().getDescriptionId())
              .addColumn("Quantity", entry -> String.valueOf(entry.getValue()))
              .withData(resourceList)
              .withBackButtonText("Back to Town")
              .withColors(
                  Themes.BC_DEFAULT[0],  // Background
                  Themes.BC_DEFAULT[1],  // Border
                  Themes.BC_DEFAULT[2],  // Title
                  Themes.BC_DEFAULT[3],  // Header
                  Themes.BC_DEFAULT[4]   // Text
              )
              .withRowHeight(14);  // Smaller rows for resource lists
        
        return screen;
    }
    
    /**
     * Create a generic string list screen
     * 
     * @param title Screen title
     * @param parentScreen Parent screen to return to
     * @param items List of strings to display
     * @param onItemClickHandler Optional handler for item clicks
     * @param theme Color theme to use
     * @return Configured BCModalGridScreen
     */
    public static BCModalGridScreen<String> createStringListScreen(
            Component title,
            Screen parentScreen,
            List<String> items,
            Consumer<String> onItemClickHandler,
            int[] theme) {
        
        // Create the screen
        BCModalGridScreen<String> screen = new BCModalGridScreen<>(
            title,
            parentScreen,
            null
        );
        
        // Use provided theme or default
        int[] colorsToUse = theme != null ? theme : Themes.BC_DEFAULT;
        
        // Configure with a single column
        screen.addColumn("Item", str -> str)
              .withData(items)
              .withBackButtonText("Back")
              .withColors(
                  colorsToUse[0],  // Background
                  colorsToUse[1],  // Border
                  colorsToUse[2],  // Title
                  colorsToUse[3],  // Header
                  colorsToUse[4]   // Text
              )
              .withRowClickHandler(onItemClickHandler);
        
        return screen;
    }
    
    // Helper methods
    
    private static String formatTime(long timestamp) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("MM/dd HH:mm");
        return timeFormat.format(new Date(timestamp));
    }
    
    private static String formatOrigin(VisitHistoryRecord record, Function<UUID, String> townNameLookup) {
        // Get town UUID
        UUID townId = record.getOriginTownId();
        
        // Use the lookup function to get the actual town name
        String originName = townNameLookup.apply(townId);
        
        // Add direction if position available
        if (record.getOriginPos() != null) {
            BlockPos originPos = record.getOriginPos();
            
            if (originPos != null && !originPos.equals(BlockPos.ZERO)) {
                // Calculate direction based on the origin position
                int x = originPos.getX();
                int z = originPos.getZ();
                
                String direction = " at " + x + "," + z;
                originName += direction;
            }
        }
        
        return originName;
    }
    
    // Old method for backwards compatibility
    private static String formatOrigin(VisitHistoryRecord record) {
        return formatOrigin(record, uuid -> 
            uuid != null ? "Town-" + uuid.toString().substring(0, 8) : "Unknown"
        );
    }
} 