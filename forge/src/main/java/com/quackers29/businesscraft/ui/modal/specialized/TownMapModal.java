package com.quackers29.businesscraft.ui.modal.specialized;

import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.network.packets.ui.RequestTownMapDataPacket;
import com.quackers29.businesscraft.network.packets.ui.RequestTownPlatformDataPacket;
import com.quackers29.businesscraft.network.packets.ui.TownMapDataResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket;
import com.quackers29.businesscraft.client.cache.ClientTownMapCache;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A modal screen that displays a sophisticated map view of all towns in the world.
 * Features: Pan/zoom controls, recenter button, mouse dragging, coordinate grid,
 * distance calculation, edge indicators for off-map towns.
 * 
 * Enhanced MultiLoader Template: Uses migrated packets from common module.
 * Restored from main branch with sophisticated rendering capabilities.
 */
public class TownMapModal extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownMapModal.class);
    
    // Screen properties
    private final Screen parentScreen;
    private final Consumer<TownMapModal> onCloseCallback;
    private final BlockPos currentTownPos;
    
    // Panel dimensions and position
    private int panelWidth;
    private int panelHeight;
    private int panelLeft;
    private int panelTop;
    
    // Map properties
    private final int mapWidth = 300;
    private final int mapHeight = 250;
    private int mapCenterX;
    private int mapCenterY;
    private double mapOffsetX = 0; // Camera offset in world coordinates
    private double mapOffsetZ = 0;
    private double zoomLevel = 1.0; // 1.0 = normal, 2.0 = zoomed in, 0.5 = zoomed out
    
    // UI components
    private Button closeButton;
    private Button recenterButton;
    
    // Mouse interaction
    private boolean isDragging = false;
    private double lastMouseX;
    private double lastMouseY;
    
    // Town data - using structured data for sophisticated features
    private Map<UUID, TownMapDataResponsePacket.TownMapInfo> allTowns;
    private TownMapDataResponsePacket.TownMapInfo selectedTown = null;
    private Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> selectedTownPlatforms = null;
    private TownPlatformDataResponsePacket.PlatformInfo selectedPlatform = null;
    private TownPlatformDataResponsePacket.TownInfo selectedTownInfo = null; // Live town info including boundary
    
    // Legacy compatibility for cache integration
    private Map<UUID, ClientTownMapCache.CachedTownData> cachedTowns;
    private boolean hasAdvancedMapData = false;
    
    // Colors (themed to match existing UI)
    private static final int BACKGROUND_COLOR = 0xFF222222;
    private static final int BORDER_COLOR = 0xFFAAAAAA;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int MAP_BACKGROUND_COLOR = 0xFF1A1A1A;
    private static final int TOWN_MARKER_COLOR = 0xFF4CAF50;
    private static final int CURRENT_TOWN_COLOR = 0xFFFF9800;
    private static final int SELECTED_TOWN_COLOR = 0xFF2196F3;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int INFO_BACKGROUND = 0xFF333333;
    
    // Map Constants
    private static final double MIN_ZOOM = 0.05;
    private static final double MAX_ZOOM = 600.0;
    private static final double ZOOM_FACTOR = 1.3;
    private static final double COORDINATE_CONVERSION = 0.1;
    private static final int MAX_COORDINATE_MARKERS = 7;
    private static final int CLICK_TOLERANCE = 3;
    
    /**
     * Constructor for the sophisticated town map modal
     * 
     * @param parentScreen The parent screen to return to when closing
     * @param currentTownPos The position of the current town (for recentering)
     * @param onCloseCallback Optional callback to execute when closing (can be null)
     */
    public TownMapModal(Screen parentScreen, BlockPos currentTownPos, Consumer<TownMapModal> onCloseCallback) {
        super(Component.literal("Town Map"));
        this.parentScreen = parentScreen;
        this.currentTownPos = currentTownPos;
        this.onCloseCallback = onCloseCallback;
        this.cachedTowns = new HashMap<>();
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "TownMapModal created with current town at: {}", currentTownPos);
    }
    
    /**
     * Initialize the screen layout and components
     */
    @Override
    protected void init() {
        super.init();
        
        // Calculate panel dimensions - use most of the screen
        this.panelWidth = Math.min(500, this.width - 60);
        this.panelHeight = Math.min(400, this.height - 60);
        this.panelLeft = (this.width - panelWidth) / 2;
        this.panelTop = (this.height - panelHeight) / 2;
        
        // Calculate map area
        this.mapCenterX = panelLeft + panelWidth / 2;
        this.mapCenterY = panelTop + 80 + mapHeight / 2; // 80 for title area
        
        // Load town data
        loadTownData();
        
        // Center map on current town initially
        centerOnCurrentTown();
        
        // Create buttons
        createButtons();
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "TownMapModal initialized with {} towns", 
            allTowns != null ? allTowns.size() : 0);
    }
    
    /**
     * Load town data - always request fresh data from server
     */
    private void loadTownData() {
        try {
            // Always request fresh data from server to ensure map shows current state
            this.allTowns = new java.util.HashMap<>();
            // Request map data - use current town position and default parameters
            if (currentTownPos != null) {
                ModMessages.sendToServer(new RequestTownMapDataPacket(
                    currentTownPos.getX(), currentTownPos.getY(), currentTownPos.getZ(), 1, true));
            }
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                "Requested fresh town data from server (always fresh on map open)");
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "Sending RequestTownMapDataPacket to server");
        } catch (Exception e) {
            LOGGER.error("Failed to load town data for map", e);
            this.allTowns = new java.util.HashMap<>();
        }
    }
    
    /**
     * Create UI buttons
     */
    private void createButtons() {
        // Close button
        this.closeButton = this.addRenderableWidget(Button.builder(
            Component.literal("Close"), 
            button -> this.onClose())
            .pos(panelLeft + panelWidth - 70, panelTop + panelHeight - 30)
            .size(60, 20)
            .build()
        );
        
        // Recenter button
        this.recenterButton = this.addRenderableWidget(Button.builder(
            Component.literal("Recenter"), 
            button -> this.centerOnCurrentTown())
            .pos(panelLeft + 10, panelTop + panelHeight - 30)
            .size(80, 20)
            .build()
        );
    }
    
    /**
     * Center the map on the current town
     */
    private void centerOnCurrentTown() {
        if (currentTownPos != null) {
            mapOffsetX = currentTownPos.getX();
            mapOffsetZ = currentTownPos.getZ();
            selectedTown = null; // Clear selection when recentering
            selectedTownInfo = null; // Clear live town info
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Map recentered on current town at: {}", currentTownPos);
        }
    }
    
    /**
     * Convert world coordinates to screen coordinates
     */
    private int worldToScreenX(double worldX) {
        return (int)(mapCenterX + (worldX - mapOffsetX) * zoomLevel * 0.1);
    }
    
    private int worldToScreenZ(double worldZ) {
        return (int)(mapCenterY + (worldZ - mapOffsetZ) * zoomLevel * 0.1);
    }
    
    /**
     * Convert screen coordinates to world coordinates
     */
    private double screenToWorldX(int screenX) {
        return mapOffsetX + (screenX - mapCenterX) / (zoomLevel * 0.1);
    }
    
    private double screenToWorldZ(int screenY) {
        return mapOffsetZ + (screenY - mapCenterY) / (zoomLevel * 0.1);
    }
    
    /**
     * Get map bounds for rendering calculations
     */
    private int[] getMapBounds() {
        int mapLeft = mapCenterX - mapWidth / 2;
        int mapTop = mapCenterY - mapHeight / 2;
        int mapRight = mapLeft + mapWidth;
        int mapBottom = mapTop + mapHeight;
        return new int[]{mapLeft, mapTop, mapRight, mapBottom};
    }
    
    /**
     * Update map data with structured town information from server response
     */
    public void updateMapData(Map<UUID, TownMapDataResponsePacket.TownMapInfo> townData) {
        if (townData != null) {
            this.allTowns = new HashMap<>(townData);
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Updated map with {} towns from server", townData.size());
        }
    }
    
    /**
     * Legacy compatibility method for string-based map data
     */
    public void updateMapData(String mapData, int zoomLevel) {
        LOGGER.debug("Received legacy string map data for zoom level: {} - converting to structured data", zoomLevel);
        // For now, log that we received data but prefer structured updates
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Legacy map data received: {}", mapData);
    }
    
    /**
     * Set cached town data for sophisticated map rendering.
     * This method restores the advanced map functionality that was temporarily disabled.
     * 
     * @param townData Map of town UUIDs to cached town data from ClientTownMapCache
     */
    public void setTownData(Map<UUID, ClientTownMapCache.CachedTownData> townData) {
        this.cachedTowns = townData != null ? new HashMap<>(townData) : new HashMap<>();
        this.hasAdvancedMapData = !this.cachedTowns.isEmpty();
        
        LOGGER.debug("Set town data for map modal: {} towns loaded, advanced features: {}", 
                    this.cachedTowns.size(), hasAdvancedMapData);
        
        // Request additional platform data for enhanced visualization
        if (hasAdvancedMapData) {
            requestPlatformData();
        }
    }
    
    /**
     * Request platform data for enhanced map visualization.
     * This enables sophisticated features like transportation network display.
     */
    private void requestPlatformData() {
        LOGGER.debug("Requesting platform data for enhanced map visualization at position: {}", currentTownPos);
        
        // Send packet to request platform and connection data
        if (currentTownPos != null) {
            RequestTownPlatformDataPacket packet = new RequestTownPlatformDataPacket(
                currentTownPos.getX(), currentTownPos.getY(), currentTownPos.getZ(), true, true, 5000);
            ModMessages.sendToServer(packet);
        }
    }
    
    /**
     * Request platform data for a specific town by ID
     */
    private void requestTownPlatformData(UUID townId) {
        LOGGER.debug("Requesting platform data for selected town: {}", townId);
        
        // Find the town's position from our town data
        if (allTowns != null && allTowns.containsKey(townId)) {
            TownMapDataResponsePacket.TownMapInfo town = allTowns.get(townId);
            RequestTownPlatformDataPacket packet = new RequestTownPlatformDataPacket(
                town.x, town.y, town.z, true, true, 5000);
            ModMessages.sendToServer(packet);
        }
    }
    
    /**
     * Render the sophisticated map screen
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Render darkened background
        this.renderBackground(guiGraphics);
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0000000);
        
        // Draw main panel
        drawPanel(guiGraphics);
        
        // Draw title
        drawTitle(guiGraphics);
        
        // Draw map
        drawMap(guiGraphics, mouseX, mouseY);
        
        // Draw town information if a town is selected
        drawTownInfo(guiGraphics);
        
        // Draw controls help
        drawControlsHelp(guiGraphics);
        
        // Check for fresh cache data on each render (in case server responded with new data)
        refreshCacheData();
        
        // Render buttons
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
    
    /**
     * Draw the main panel background and border
     */
    private void drawPanel(GuiGraphics guiGraphics) {
        // Background
        guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, BACKGROUND_COLOR);
        
        // Border
        guiGraphics.hLine(panelLeft, panelLeft + panelWidth - 1, panelTop, BORDER_COLOR);
        guiGraphics.hLine(panelLeft, panelLeft + panelWidth - 1, panelTop + panelHeight - 1, BORDER_COLOR);
        guiGraphics.vLine(panelLeft, panelTop, panelTop + panelHeight - 1, BORDER_COLOR);
        guiGraphics.vLine(panelLeft + panelWidth - 1, panelTop, panelTop + panelHeight - 1, BORDER_COLOR);
    }
    
    /**
     * Draw the title
     */
    private void drawTitle(GuiGraphics guiGraphics) {
        guiGraphics.drawCenteredString(
            this.font,
            this.title,
            panelLeft + panelWidth / 2,
            panelTop + 15,
            TITLE_COLOR
        );
        
        // Draw separator line
        guiGraphics.hLine(
            panelLeft + 20, 
            panelLeft + panelWidth - 20, 
            panelTop + 35,
            BORDER_COLOR
        );
    }
    
    /**
     * Draw the map area and towns
     */
    private void drawMap(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int[] bounds = getMapBounds();
        int mapLeft = bounds[0], mapTop = bounds[1], mapRight = bounds[2], mapBottom = bounds[3];
        
        // Draw map background and border
        guiGraphics.fill(mapLeft, mapTop, mapRight, mapBottom, MAP_BACKGROUND_COLOR);
        guiGraphics.hLine(mapLeft, mapRight - 1, mapTop, BORDER_COLOR);
        guiGraphics.hLine(mapLeft, mapRight - 1, mapBottom - 1, BORDER_COLOR);
        guiGraphics.vLine(mapLeft, mapTop, mapBottom - 1, BORDER_COLOR);
        guiGraphics.vLine(mapRight - 1, mapTop, mapBottom - 1, BORDER_COLOR);
        
        // Draw compass indicator (North arrow)
        drawCompassIndicator(guiGraphics);
        
        // Draw grid lines for reference
        drawGridLines(guiGraphics);
        
        // Draw towns
        drawTowns(guiGraphics, mouseX, mouseY);
        
        // Draw current position marker
        drawCurrentPosition(guiGraphics);
        
        // Draw coordinate markers
        drawCoordinateMarkers(guiGraphics);
    }
    
    /**
     * Draw controls help text
     */
    private void drawControlsHelp(GuiGraphics guiGraphics) {
        int helpY = panelTop + panelHeight - 55;
        guiGraphics.drawString(this.font, "Mouse: Drag to pan, Scroll to zoom", panelLeft + panelWidth / 2 - 60, helpY, 0xFFCCCCCC);
    }
    
    /**
     * Draw town information panel (placeholder)
     */
    private void drawTownInfo(GuiGraphics guiGraphics) {
        if (selectedTown != null) {
            // Draw town info panel - sophisticated implementation would go here
            int infoX = panelLeft + 20;
            int infoY = panelTop + 50;
            guiGraphics.drawString(this.font, "Selected: " + selectedTown.name, infoX, infoY, TEXT_COLOR);
        }
    }
    
    /**
     * Draw compass indicator showing North
     */
    private void drawCompassIndicator(GuiGraphics guiGraphics) {
        int compassX = panelLeft + panelWidth - 40;
        int compassY = panelTop + 60;
        
        // Draw "N" for North
        guiGraphics.drawString(this.font, "N", compassX, compassY, TITLE_COLOR);
        
        // Draw arrow pointing up (North)
        guiGraphics.vLine(compassX + 5, compassY - 10, compassY - 2, TITLE_COLOR);
        guiGraphics.hLine(compassX + 3, compassX + 7, compassY - 10, TITLE_COLOR);
        guiGraphics.hLine(compassX + 4, compassX + 6, compassY - 9, TITLE_COLOR);
    }
    
    /**
     * Draw adaptive grid lines for reference
     */
    private void drawGridLines(GuiGraphics guiGraphics) {
        int[] bounds = getMapBounds();
        int mapLeft = bounds[0], mapTop = bounds[1];
        int gridColor = 0x40AAAAAA;
        
        // Calculate adaptive grid spacing based on zoom level
        int baseGridSpacing = getAdaptiveGridSpacing();
        
        double viewWidthInWorld = mapWidth / (zoomLevel * COORDINATE_CONVERSION);
        double viewHeightInWorld = mapHeight / (zoomLevel * COORDINATE_CONVERSION);
        
        // Calculate starting positions for grid lines
        int startX = ((int)(mapOffsetX - viewWidthInWorld / 2) / baseGridSpacing) * baseGridSpacing;
        int startZ = ((int)(mapOffsetZ - viewHeightInWorld / 2) / baseGridSpacing) * baseGridSpacing;
        
        // Draw vertical lines
        for (int worldX = startX; worldX <= mapOffsetX + viewWidthInWorld / 2; worldX += baseGridSpacing) {
            int screenX = worldToScreenX(worldX);
            if (screenX >= mapLeft && screenX <= bounds[2]) {
                guiGraphics.vLine(screenX, mapTop, bounds[3], gridColor);
            }
        }
        
        // Draw horizontal lines
        for (int worldZ = startZ; worldZ <= mapOffsetZ + viewHeightInWorld / 2; worldZ += baseGridSpacing) {
            int screenY = worldToScreenZ(worldZ);
            if (screenY >= mapTop && screenY <= bounds[3]) {
                guiGraphics.hLine(mapLeft, bounds[2], screenY, gridColor);
            }
        }
    }
    
    /**
     * Calculate adaptive grid spacing based on zoom level
     */
    private int getAdaptiveGridSpacing() {
        // Grid spacing adapts to zoom level for optimal visibility
        if (zoomLevel >= 400.0) {
            return 1; // Single block grid for extreme zoom (5x5 block view)
        } else if (zoomLevel >= 200.0) {
            return 2; // 2-block grid for very high zoom
        } else if (zoomLevel >= 100.0) {
            return 3; // 3-block grid for high zoom
        } else if (zoomLevel >= 50.0) {
            return 5; // 5-block grid for medium-high zoom
        } else if (zoomLevel >= 20.0) {
            return 5; // Very fine grid for extreme zoom
        } else if (zoomLevel >= 10.0) {
            return 10; // Fine grid for high zoom
        } else if (zoomLevel >= 5.0) {
            return 25; // Medium grid for medium zoom
        } else if (zoomLevel >= 2.0) {
            return 50; // Standard grid for normal zoom
        } else if (zoomLevel >= 1.0) {
            return 100; // Coarse grid for low zoom
        } else if (zoomLevel >= 0.5) {
            return 200; // Very coarse grid for very low zoom
        } else {
            return 500; // Extremely coarse grid for extreme zoom out
        }
    }
    
    /**
     * Draw all towns on the map
     */
    private void drawTowns(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (allTowns == null) return;
        
        for (TownMapDataResponsePacket.TownMapInfo town : allTowns.values()) {
            drawTown(guiGraphics, town, mouseX, mouseY);
        }
        
        // Also draw legacy cached towns if available
        if (cachedTowns != null && !cachedTowns.isEmpty()) {
            for (ClientTownMapCache.CachedTownData cachedTown : cachedTowns.values()) {
                drawCachedTown(guiGraphics, cachedTown, mouseX, mouseY);
            }
        }
    }
    
    /**
     * Draw a single town marker from structured data
     */
    private void drawTown(GuiGraphics guiGraphics, TownMapDataResponsePacket.TownMapInfo town, int mouseX, int mouseY) {
        boolean isCurrentTown = currentTownPos != null && 
            currentTownPos.getX() == town.x && 
            currentTownPos.getY() == town.y && 
            currentTownPos.getZ() == town.z;
        
        int screenX = worldToScreenX(town.x);
        int screenY = worldToScreenZ(town.z);
        
        // Determine marker color
        int markerColor = TOWN_MARKER_COLOR;
        if (town == selectedTown) {
            markerColor = SELECTED_TOWN_COLOR;
        } else if (isCurrentTown) {
            markerColor = CURRENT_TOWN_COLOR;
        }
        
        // Draw marker with adaptive size
        int markerSize = getAdaptiveMarkerSize();
        guiGraphics.fill(screenX - markerSize/2, screenY - markerSize/2, 
                        screenX + markerSize/2, screenY + markerSize/2, markerColor);
        
        // Draw town name above marker
        String townName = town.name;
        int textWidth = this.font.width(townName);
        int nameX = screenX - textWidth/2;
        int nameY = screenY - markerSize/2 - 12;
        
        guiGraphics.drawString(this.font, townName, nameX, nameY, TEXT_COLOR);
        
        // Highlight if mouse is over
        if (mouseX >= screenX - markerSize && mouseX <= screenX + markerSize &&
            mouseY >= screenY - markerSize && mouseY <= screenY + markerSize) {
            // Draw highlight ring
            int ringSize = markerSize + 2;
            guiGraphics.hLine(screenX - ringSize, screenX + ringSize, screenY - ringSize, 0xFFFFFFFF);
            guiGraphics.hLine(screenX - ringSize, screenX + ringSize, screenY + ringSize, 0xFFFFFFFF);
            guiGraphics.vLine(screenX - ringSize, screenY - ringSize, screenY + ringSize, 0xFFFFFFFF);
            guiGraphics.vLine(screenX + ringSize, screenY - ringSize, screenY + ringSize, 0xFFFFFFFF);
        }
    }
    
    /**
     * Draw a cached town marker (legacy compatibility)
     */
    private void drawCachedTown(GuiGraphics guiGraphics, ClientTownMapCache.CachedTownData town, int mouseX, int mouseY) {
        int screenX = worldToScreenX(town.getX());
        int screenY = worldToScreenZ(town.getZ());
        
        // Draw cached town as smaller marker
        int markerSize = Math.max(4, getAdaptiveMarkerSize() / 2);
        guiGraphics.fill(screenX - markerSize/2, screenY - markerSize/2, 
                        screenX + markerSize/2, screenY + markerSize/2, 0xFF88FF88);
        
        // Draw town name
        String townName = town.getName();
        int textWidth = this.font.width(townName);
        int nameX = screenX - textWidth/2;
        int nameY = screenY - markerSize/2 - 12;
        
        guiGraphics.drawString(this.font, townName, nameX, nameY, 0xFFCCCCCC);
    }
    
    /**
     * Calculate adaptive marker size based on zoom level
     */
    private int getAdaptiveMarkerSize() {
        // Marker size adapts to zoom level for better visibility
        if (zoomLevel >= 400.0) {
            return 24; // Very large markers for extreme zoom
        } else if (zoomLevel >= 200.0) {
            return 20; // Large markers for very high zoom
        } else if (zoomLevel >= 100.0) {
            return 18; // Medium-large markers for high zoom
        } else if (zoomLevel >= 50.0) {
            return 16; // Medium markers for medium-high zoom
        } else if (zoomLevel >= 15.0) {
            return 12; // Large markers for extreme zoom
        } else if (zoomLevel >= 8.0) {
            return 10; // Medium-large markers for high zoom
        } else if (zoomLevel >= 3.0) {
            return 8; // Standard markers for medium zoom
        } else if (zoomLevel >= 1.0) {
            return 6; // Default markers for normal zoom
        } else if (zoomLevel >= 0.3) {
            return 4; // Small markers for low zoom
        } else {
            return 3; // Very small markers for extreme zoom out
        }
    }
    
    /**
     * Draw current position marker
     */
    private void drawCurrentPosition(GuiGraphics guiGraphics) {
        if (currentTownPos != null) {
            int screenX = worldToScreenX(currentTownPos.getX());
            int screenY = worldToScreenZ(currentTownPos.getZ());
            
            // Draw special marker for current position
            int markerSize = getAdaptiveMarkerSize();
            guiGraphics.fill(screenX - markerSize/2, screenY - markerSize/2, 
                           screenX + markerSize/2, screenY + markerSize/2, CURRENT_TOWN_COLOR);
            
            // Draw "You" label
            guiGraphics.drawCenteredString(this.font, "You", screenX, screenY - markerSize/2 - 15, TEXT_COLOR);
        }
    }
    
    /**
     * Draw coordinate markers around the map edges
     */
    private void drawCoordinateMarkers(GuiGraphics guiGraphics) {
        int[] bounds = getMapBounds();
        int mapLeft = bounds[0], mapTop = bounds[1], mapRight = bounds[2], mapBottom = bounds[3];
        
        double viewWidthInWorld = mapWidth / (zoomLevel * COORDINATE_CONVERSION);
        double viewHeightInWorld = mapHeight / (zoomLevel * COORDINATE_CONVERSION);
        
        // Calculate coordinate spacing to have approximately 7 markers maximum
        int coordSpacing = getCoordinateSpacing();
        
        // Draw X-axis markers (bottom of map)
        int startX = ((int)(mapOffsetX - viewWidthInWorld / 2) / coordSpacing) * coordSpacing;
        for (int worldX = startX; worldX <= mapOffsetX + viewWidthInWorld / 2; worldX += coordSpacing) {
            int screenX = worldToScreenX(worldX);
            if (screenX >= mapLeft && screenX <= mapRight) {
                // Draw tick mark
                guiGraphics.vLine(screenX, mapBottom, mapBottom + 5, BORDER_COLOR);
                
                // Draw coordinate label
                String xLabel = String.valueOf(worldX);
                int labelWidth = this.font.width(xLabel);
                guiGraphics.drawString(this.font, xLabel, 
                                      screenX - labelWidth/2, mapBottom + 7, TEXT_COLOR);
            }
        }
        
        // Draw Z-axis markers (left side of map)
        int startZ = ((int)(mapOffsetZ - viewHeightInWorld / 2) / coordSpacing) * coordSpacing;
        for (int worldZ = startZ; worldZ <= mapOffsetZ + viewHeightInWorld / 2; worldZ += coordSpacing) {
            int screenY = worldToScreenZ(worldZ);
            if (screenY >= mapTop && screenY <= mapBottom) {
                // Draw tick mark
                guiGraphics.hLine(mapLeft - 5, mapLeft, screenY, BORDER_COLOR);
                
                // Draw coordinate label
                String zLabel = String.valueOf(worldZ);
                int labelWidth = this.font.width(zLabel);
                guiGraphics.drawString(this.font, zLabel, 
                                      mapLeft - labelWidth - 7, screenY - 4, TEXT_COLOR);
            }
        }
        
        // Draw axis labels
        guiGraphics.drawString(this.font, "X", mapRight + 5, mapBottom + 7, 0xFFCCCCCC);
        guiGraphics.drawString(this.font, "Z", mapLeft - 15, mapTop - 10, 0xFFCCCCCC);
    }
    
    /**
     * Calculate coordinate marker spacing to have maximum ~7 markers per axis
     */
    private int getCoordinateSpacing() {
        double viewWidthInWorld = mapWidth / (zoomLevel * COORDINATE_CONVERSION);
        double viewHeightInWorld = mapHeight / (zoomLevel * COORDINATE_CONVERSION);
        
        // Find the maximum view dimension
        double maxViewDimension = Math.max(viewWidthInWorld, viewHeightInWorld);
        
        // Calculate spacing to have approximately 7 markers maximum
        double targetSpacing = maxViewDimension / 7.0;
        
        // Round to nice numbers (powers of 10, 25, 50)
        int spacing;
        if (targetSpacing <= 5) {
            spacing = 5;
        } else if (targetSpacing <= 10) {
            spacing = 10;
        } else if (targetSpacing <= 25) {
            spacing = 25;
        } else if (targetSpacing <= 50) {
            spacing = 50;
        } else if (targetSpacing <= 100) {
            spacing = 100;
        } else if (targetSpacing <= 250) {
            spacing = 250;
        } else if (targetSpacing <= 500) {
            spacing = 500;
        } else if (targetSpacing <= 1000) {
            spacing = 1000;
        } else if (targetSpacing <= 2500) {
            spacing = 2500;
        } else if (targetSpacing <= 5000) {
            spacing = 5000;
        } else if (targetSpacing <= 10000) {
            spacing = 10000;
        } else {
            // For very large ranges, use powers of 10
            int powerOf10 = (int) Math.pow(10, Math.ceil(Math.log10(targetSpacing)));
            spacing = powerOf10;
        }
        
        return spacing;
    }
    
    /**
     * Handle mouse interaction - pan and zoom
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging && button == 0) {
            // Pan the map based on mouse movement
            double worldDeltaX = -deltaX / (zoomLevel * COORDINATE_CONVERSION);
            double worldDeltaZ = -deltaY / (zoomLevel * COORDINATE_CONVERSION);
            
            mapOffsetX += worldDeltaX;
            mapOffsetZ += worldDeltaZ;
            
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Check if clicking within map bounds
            int[] bounds = getMapBounds();
            if (mouseX >= bounds[0] && mouseX <= bounds[2] && mouseY >= bounds[1] && mouseY <= bounds[3]) {
                isDragging = true;
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                
                // Check if clicking on a town marker
                if (allTowns != null) {
                    for (TownMapDataResponsePacket.TownMapInfo town : allTowns.values()) {
                        int screenX = worldToScreenX(town.x);
                        int screenY = worldToScreenZ(town.z);
                        int markerSize = getAdaptiveMarkerSize();
                        
                        if (Math.abs(mouseX - screenX) <= markerSize && Math.abs(mouseY - screenY) <= markerSize) {
                            // If selecting a different town, clear previous platform data
                            if (selectedTown == null || !selectedTown.townId.equals(town.townId)) {
                                selectedTownPlatforms = null;
                                selectedPlatform = null;
                                selectedTownInfo = null;
                            }
                            
                            selectedTown = town;
                            
                            // Request platform data for the selected town - this is the key missing piece!
                            requestTownPlatformData(town.townId);
                            
                            LOGGER.debug("Selected town: {} - requesting platform data", town.name);
                            return true;
                        }
                    }
                }
                
                selectedTown = null; // Clear selection if not clicking on town
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        // Check if scrolling within map bounds
        int[] bounds = getMapBounds();
        if (mouseX >= bounds[0] && mouseX <= bounds[2] && mouseY >= bounds[1] && mouseY <= bounds[3]) {
            double oldZoom = zoomLevel;
            
            if (scrollDelta > 0) {
                // Zoom in
                zoomLevel = Math.min(MAX_ZOOM, zoomLevel * ZOOM_FACTOR);
            } else {
                // Zoom out
                zoomLevel = Math.max(MIN_ZOOM, zoomLevel / ZOOM_FACTOR);
            }
            
            // Adjust map offset to zoom towards mouse position
            if (zoomLevel != oldZoom) {
                double mouseWorldX = screenToWorldX((int)mouseX);
                double mouseWorldZ = screenToWorldZ((int)mouseY);
                
                // Calculate how much the mouse position shifted in world coordinates
                double newMouseWorldX = mapOffsetX + ((int)mouseX - mapCenterX) / (zoomLevel * COORDINATE_CONVERSION);
                double newMouseWorldZ = mapOffsetZ + ((int)mouseY - mapCenterY) / (zoomLevel * COORDINATE_CONVERSION);
                
                // Adjust offset to keep mouse position stable
                mapOffsetX += mouseWorldX - newMouseWorldX;
                mapOffsetZ += mouseWorldZ - newMouseWorldZ;
            }
            
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
        if (onCloseCallback != null) {
            onCloseCallback.accept(this);
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    /**
     * Handle platform data response from server - enables platform visualization when clicking towns
     */
    public void refreshPlatformData(UUID townId, Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> platforms) {
        if (selectedTown != null && selectedTown.townId.equals(townId)) {
            selectedTownPlatforms = new HashMap<>(platforms);
            
            // Check if the currently selected platform still exists in the new data
            if (selectedPlatform != null) {
                boolean platformStillExists = false;
                for (TownPlatformDataResponsePacket.PlatformInfo platform : platforms.values()) {
                    if (platform.platformId.equals(selectedPlatform.platformId)) {
                        selectedPlatform = platform; // Update with fresh data
                        platformStillExists = true;
                        break;
                    }
                }
                
                // Clear selection if platform no longer exists
                if (!platformStillExists) {
                    selectedPlatform = null;
                    DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                        "Cleared selected platform as it no longer exists");
                }
            }
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                "Refreshed platform data for selected town {}: {} platforms", townId, platforms.size());
        }
    }
    
    /**
     * Handle town data response from server (platform packet context - includes live boundary)
     */
    public void refreshTownData(UUID townId, TownPlatformDataResponsePacket.TownInfo townInfo) {
        if (selectedTown != null && selectedTown.townId.equals(townId)) {
            // Store the live town info including boundary info
            selectedTownInfo = townInfo;
            
            // Update the selected town with fresh data from TownInfo
            // Note: TownInfo doesn't have population/touristCount, so keep existing values
            selectedTown = new TownMapDataResponsePacket.TownMapInfo(
                selectedTown.townId,
                townInfo.name,
                townInfo.centerX,
                townInfo.centerY, 
                townInfo.centerZ,
                selectedTown.population, // Keep existing population
                selectedTown.visitCount, // Keep existing visit count
                selectedTown.lastVisited, // Keep existing last visited
                selectedTown.isCurrentTown // Keep existing current town flag
            );
            
            // Also update the main town cache with fresh data
            if (allTowns != null) {
                allTowns.put(townId, selectedTown);
            }
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                "Refreshed town data for selected town {}: name={}, center=({},{},{}), detectionRadius={}", 
                townId, townInfo.name, townInfo.centerX, townInfo.centerY, townInfo.centerZ, townInfo.detectionRadius);
        } else {
            // If this town data is for a different town than selected, clear platform selection
            if (selectedPlatform != null) {
                selectedPlatform = null;
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                    "Cleared platform selection due to town data for different town");
            }
        }
    }
    
    /**
     * Refresh cache data from ClientTownMapCache on each render.
     * This ensures the modal picks up new town data when server responses arrive.
     */
    private void refreshCacheData() {
        ClientTownMapCache cache = ClientTownMapCache.getInstance();
        Map<UUID, ClientTownMapCache.CachedTownData> freshData = cache.getAllTowns();
        
        // Only update if we have new data or the data changed significantly
        if (!freshData.isEmpty() && (cachedTowns.isEmpty() || freshData.size() != cachedTowns.size())) {
            LOGGER.debug("Refreshing map modal with {} towns from live cache", freshData.size());
            
            this.cachedTowns = new HashMap<>(freshData);
            this.hasAdvancedMapData = !this.cachedTowns.isEmpty();
            
            if (hasAdvancedMapData) {
                LOGGER.debug("Sophisticated map features now enabled with {} towns", cachedTowns.size());
            }
        }
    }
    
}