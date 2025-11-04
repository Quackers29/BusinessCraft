package com.quackers29.businesscraft.client;

import com.quackers29.businesscraft.api.EventCallbacks;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
// BusinessCraft moved to platform-specific module
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.*;

/**
 * Renders debug information about towns on the screen
 * Can be toggled with F4
 */
public class TownDebugOverlay {
    private static boolean visible = false;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int HEADER_COLOR = 0xFFFFAA00;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int WARNING_COLOR = 0xFFFF5555;
    private static final int REFRESH_INTERVAL_TICKS = 20; // 1 second at 20 ticks/sec
    private static final int SCROLL_AMOUNT = 15; // Pixels to scroll per wheel notch
    
    // Cached town data from server
    private static List<TownDebugData> townData = new ArrayList<>();
    private static long lastRefreshTime = 0;
    private static long lastUpdateTick = 0;
    private static int scrollOffset = 0; // Tracks vertical scroll position
    
    public static void toggleVisibility() {
        visible = !visible;
        if (visible) {
            // Request data from server when overlay is shown
            refreshData();
            scrollOffset = 0; // Reset scroll position when showing
        }
    }
    
    /**
     * Requests fresh data from the server
     */
    public static void refreshData() {
        TownDebugNetwork.requestTownData();
        lastRefreshTime = System.currentTimeMillis();
    }
    
    public static void setTownData(List<TownDebugData> data) {
        townData = data;
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object levelObj = clientHelper.getClientLevel();
            if (levelObj instanceof net.minecraft.world.level.Level level) {
                lastUpdateTick = level.getGameTime();
            } else {
                lastUpdateTick = 0;
            }
        } else {
            lastUpdateTick = 0;
        }
    }
    
    public static boolean isVisible() {
        return visible;
    }
    
    /**
     * Initialize event callbacks and register overlay. Should be called during mod initialization.
     */
    public static void initialize() {
        PlatformAccess.getEvents().registerMouseScrollCallback(TownDebugOverlay::onMouseScroll);
        PlatformAccess.getEvents().registerClientTickCallback(TownDebugOverlay::onClientTick);
        
        // Register overlay with RenderHelper
        RenderHelper renderHelper = PlatformAccess.getRender();
        if (renderHelper != null) {
            renderHelper.registerOverlay("town_debug", TownDebugOverlay::renderOverlay);
        }
    }
    
    /**
     * Render overlay (platform-agnostic version)
     */
    private static void renderOverlay(GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) return;
        
        Object minecraftObj = clientHelper.getMinecraft();
        if (!(minecraftObj instanceof net.minecraft.client.Minecraft minecraft)) return;
        
        if (!visible || minecraft.options.hideGui) {
            return;
        }
        
        Object fontObj = clientHelper.getFont();
        if (!(fontObj instanceof Font font)) return;
        
        int lineHeight = font.lineHeight + 1;
        int y = 5 - scrollOffset; // Apply scroll offset to starting position
        int leftMargin = 5;
        
        // Title section - always at the top regardless of scroll
        guiGraphics.fill(2, 2, screenWidth - 2, 2 + lineHeight + 3, BACKGROUND_COLOR);
        
        guiGraphics.drawString(font, 
            Component.literal("BusinessCraft Town Debug Overlay")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), 
            leftMargin, 5, HEADER_COLOR, false);
        y += lineHeight + 8; // Fixed header height
        
        if (townData.isEmpty()) {
            if (y + scrollOffset >= 0 && y <= screenHeight) { // Only render if in view
                guiGraphics.fill(2, y, screenWidth - 2, y + lineHeight + 3, BACKGROUND_COLOR);
                guiGraphics.drawString(font, 
                    Component.literal("No town data available. Server out of range or not running?").withStyle(ChatFormatting.RED), 
                    leftMargin, y, WARNING_COLOR, false);
            }
            return;
        }
        
        // For each town, display its data
        for (TownDebugData town : townData) {
            // Only render towns/sections that would be visible based on scroll position
            if (y + lineHeight < 0) {
                // This town header is scrolled off the top, just update y and skip rendering
                y += lineHeight + 2; // Town header height
                y += (lineHeight * 5); // 5 data lines
                y += 5; // Space between towns
                continue;
            }
            
            // If we've scrolled past the bottom of the screen, stop rendering
            if (y > screenHeight) {
                break;
            }
            
            // Town header
            guiGraphics.fill(2, y, screenWidth - 2, y + lineHeight + 3, BACKGROUND_COLOR);
            guiGraphics.drawString(font, 
                Component.literal("Town: " + town.name + " (" + town.id + ")").withStyle(ChatFormatting.YELLOW), 
                leftMargin, y, HEADER_COLOR, false);
            y += lineHeight + 2;
            
            // Town data
            String[] dataLines = {
                "Position: " + town.position,
                "Population: " + town.population + " | Bread: " + town.breadCount,
                "Tourist Spawning: " + (town.touristSpawningEnabled ? "Enabled" : "Disabled") + 
                    " | Can Spawn: " + (town.canSpawnTourists ? "Yes" : "No"),
                "Path: " + (town.pathStart != null ? town.pathStart : "None") + " → " + 
                    (town.pathEnd != null ? town.pathEnd : "None"),
                "Search Radius: " + town.searchRadius + " | Total Visitors: " + town.totalVisitors
            };
            
            for (String line : dataLines) {
                // Only render if this line would be visible
                if (y >= 0 && y <= screenHeight) {
                    guiGraphics.fill(2, y, screenWidth - 2, y + lineHeight, BACKGROUND_COLOR);
                    guiGraphics.drawString(font, line, leftMargin + 10, y, TEXT_COLOR, false);
                }
                y += lineHeight;
            }
            
            y += 5; // Add some space between towns
        }
        
        // Show scroll indicator if we're not at the top
        if (scrollOffset > 0) {
            guiGraphics.fill(screenWidth - 20, 2 + lineHeight + 3, screenWidth - 5, 2 + lineHeight + 10, 0xAAFFFFFF);
        }
    }
    
    /**
     * Handles mouse scroll events when the overlay is visible
     */
    private static boolean onMouseScroll(double scrollDelta) {
        if (visible) {
            // Adjust scroll offset based on mouse wheel direction
            scrollOffset -= (int)(scrollDelta * SCROLL_AMOUNT);
            // Ensure we don't scroll past the top
            if (scrollOffset < 0) {
                scrollOffset = 0;
            }
            // We handled this scroll event
            return true; // Cancel event
        }
        return false;
    }
    
    /**
     * Tick handler for periodic updates
     */
    private static void onClientTick() {
        if (visible) {
            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper != null) {
                Object levelObj = clientHelper.getClientLevel();
                Object playerObj = clientHelper.getClientPlayer();
                if (levelObj instanceof net.minecraft.world.level.Level level && playerObj != null) {
                    // Check if it's time for a refresh
                    long currentTick = level.getGameTime();
                    if (currentTick - lastUpdateTick > REFRESH_INTERVAL_TICKS) {
                        refreshData();
                    }
                }
            }
        }
    }
    
    /**
     * Data class to hold town information for rendering
     */
    public static class TownDebugData {
        public String id;
        public String name;
        public String position;
        public int population;
        public int breadCount;
        public boolean touristSpawningEnabled;
        public boolean canSpawnTourists;
        public String pathStart;
        public String pathEnd;
        public int searchRadius;
        public int totalVisitors;
        
        public TownDebugData(String id, String name, String position, int population, int breadCount,
                         boolean touristSpawningEnabled, boolean canSpawnTourists, 
                         String pathStart, String pathEnd, int searchRadius, int totalVisitors) {
            this.id = id;
            this.name = name;
            this.position = position;
            this.population = population;
            this.breadCount = breadCount;
            this.touristSpawningEnabled = touristSpawningEnabled;
            this.canSpawnTourists = canSpawnTourists;
            this.pathStart = pathStart;
            this.pathEnd = pathEnd;
            this.searchRadius = searchRadius;
            this.totalVisitors = totalVisitors;
        }
    }
} 
