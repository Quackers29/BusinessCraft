package com.yourdomain.businesscraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
// BusinessCraft moved to platform-specific module
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Renders debug information about towns on the screen
 * Can be toggled with F4
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "businesscraft", value = Dist.CLIENT)
public class TownDebugOverlay implements IGuiOverlay {
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
        lastUpdateTick = Minecraft.getInstance().level != null ? 
            Minecraft.getInstance().level.getGameTime() : 0;
    }
    
    public static boolean isVisible() {
        return visible;
    }
    
    /**
     * Handles mouse scroll events when the overlay is visible
     */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (visible) {
            // Adjust scroll offset based on mouse wheel direction
            scrollOffset -= (int)(event.getScrollDelta() * SCROLL_AMOUNT);
            // Ensure we don't scroll past the top
            if (scrollOffset < 0) {
                scrollOffset = 0;
            }
            // We handled this scroll event
            event.setCanceled(true);
        }
    }
    
    /**
     * Tick handler for periodic updates
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && visible) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                // Check if it's time for a refresh
                long currentTick = mc.level.getGameTime();
                if (currentTick - lastUpdateTick > REFRESH_INTERVAL_TICKS) {
                    refreshData();
                }
            }
        }
    }
    
    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!visible || Minecraft.getInstance().options.hideGui) {
            return;
        }
        
        Font font = Minecraft.getInstance().font;
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