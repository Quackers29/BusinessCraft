package com.quackers29.businesscraft.ui.modal.specialized;

import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.packets.ui.RequestTownMapDataPacket;
import com.quackers29.businesscraft.network.packets.ui.RequestTownPlatformDataPacket;
import com.quackers29.businesscraft.network.packets.ui.ClientTownMapCache;
import com.quackers29.businesscraft.network.packets.ui.TownMapDataResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket;
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
 * A modal screen that displays a map view of all towns in the world.
 * Allows users to see town locations, distances, and directions.
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
    
    // Town data
    private Map<UUID, TownMapDataResponsePacket.TownMapInfo> allTowns;
    private TownMapDataResponsePacket.TownMapInfo selectedTown = null;
    private Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> selectedTownPlatforms = null;
    private TownPlatformDataResponsePacket.PlatformInfo selectedPlatform = null;
    private TownPlatformDataResponsePacket.TownInfo selectedTownInfo = null; // Live town info including boundary
    
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
     * Constructor for the town map modal
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
            PlatformAccess.getNetworkMessages().sendToServer(new RequestTownMapDataPacket());
            
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
     * Check if a town is within the current view (always true now for edge indicators)
     */
    private boolean isTownVisible(TownMapDataResponsePacket.TownMapInfo town) {
        // Always return true so we can handle edge indicators for all towns
        return true;
    }
    
    /**
     * Clamp coordinates to map bounds
     */
    private int[] clampToMapBounds(int x, int y, int margin) {
        int[] bounds = getMapBounds();
        return new int[]{
            Math.max(bounds[0] + margin, Math.min(bounds[2] - margin, x)),
            Math.max(bounds[1] + margin, Math.min(bounds[3] - margin, y))
        };
    }
    
    /**
     * Check if a town marker is within the map bounds
     */
    private boolean isTownWithinMapBounds(TownMapDataResponsePacket.TownMapInfo town) {
        int screenX = worldToScreenX(town.position.getX());
        int screenY = worldToScreenZ(town.position.getZ());
        int[] bounds = getMapBounds();
        
        return screenX >= bounds[0] && screenX <= bounds[2] &&
               screenY >= bounds[1] && screenY <= bounds[3];
    }
    
    /**
     * Get the edge position for a town that's outside the map bounds
     */
    private int[] getTownEdgePosition(TownMapDataResponsePacket.TownMapInfo town) {
        int screenX = worldToScreenX(town.position.getX());
        int screenY = worldToScreenZ(town.position.getZ());
        int[] bounds = getMapBounds();
        
        // Clamp to map edges
        int clampedX = Math.max(bounds[0], Math.min(bounds[2], screenX));
        int clampedY = Math.max(bounds[1], Math.min(bounds[3], screenY));
        
        return new int[]{clampedX, clampedY};
    }
    
    /**
     * Render the screen
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
        
        // Draw platforms and paths for selected town
        drawSelectedTownPlatforms(guiGraphics);
        
        // Draw current position marker
        drawCurrentPosition(guiGraphics);
        
        // Draw coordinate markers
        drawCoordinateMarkers(guiGraphics);
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
     * Calculate adaptive marker size based on zoom level
     */
    private int getAdaptiveMarkerSize() {
        // Marker size adapts to zoom level for better visibility
        if (zoomLevel >= 400.0) {
            return 24; // Very large markers for extreme zoom (5x5 block view)
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
     * Draw coordinate markers around the map edges
     */
    private void drawCoordinateMarkers(GuiGraphics guiGraphics) {
        int[] bounds = getMapBounds();
        int mapLeft = bounds[0], mapTop = bounds[1], mapRight = bounds[2], mapBottom = bounds[3];
        
        double viewWidthInWorld = mapWidth / (zoomLevel * COORDINATE_CONVERSION);
        double viewHeightInWorld = mapHeight / (zoomLevel * COORDINATE_CONVERSION);
        
        // Calculate coordinate spacing based on zoom level
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
                // Check if this coordinate would overlap with the town info panel
                boolean wouldOverlapInfo = selectedTown != null && 
                    screenY >= panelTop + 50 && screenY <= panelTop + 110 && 
                    mapLeft - 50 <= panelLeft + 200; // Info panel extends to 200px
                
                if (!wouldOverlapInfo) {
                    // Draw tick mark
                    guiGraphics.hLine(mapLeft - 5, mapLeft, screenY, BORDER_COLOR);
                    
                    // Draw coordinate label
                    String zLabel = String.valueOf(worldZ);
                    int labelWidth = this.font.width(zLabel);
                    guiGraphics.drawString(this.font, zLabel, 
                                          mapLeft - labelWidth - 7, screenY - 4, TEXT_COLOR);
                }
            }
        }
        
        // Draw axis labels
        guiGraphics.drawString(this.font, "X", mapRight + 5, mapBottom + 7, 0xFFCCCCCC);
        
        // Only draw Z axis label if it won't overlap with town info panel
        boolean zAxisOverlapsInfo = selectedTown != null && 
            mapTop - 10 >= panelTop + 50 && mapTop - 10 <= panelTop + 110 && 
            mapLeft - 15 <= panelLeft + 200; // Info panel extends to 200px
        
        if (!zAxisOverlapsInfo) {
            guiGraphics.drawString(this.font, "Z", mapLeft - 15, mapTop - 10, 0xFFCCCCCC);
        }
    }
    
    /**
     * Calculate coordinate marker spacing based on zoom level
     * Ensures maximum ~7 markers per axis (increased from 5 by 2)
     */
    private int getCoordinateSpacing() {
        double viewWidthInWorld = mapWidth / (zoomLevel * COORDINATE_CONVERSION);
        double viewHeightInWorld = mapHeight / (zoomLevel * COORDINATE_CONVERSION);
        
        // Find the maximum view dimension
        double maxViewDimension = Math.max(viewWidthInWorld, viewHeightInWorld);
        
        // Calculate spacing to have approximately 7 markers maximum (increased from 5)
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
     * Draw all towns on the map
     */
    private void drawTowns(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (allTowns == null) return;
        
        for (TownMapDataResponsePacket.TownMapInfo town : allTowns.values()) {
            if (isTownVisible(town)) {
                drawTown(guiGraphics, town, mouseX, mouseY);
            }
        }
    }
    
    /**
     * Draw a single town marker
     */
    private void drawTown(GuiGraphics guiGraphics, TownMapDataResponsePacket.TownMapInfo town, int mouseX, int mouseY) {
        BlockPos pos = town.position;
        boolean isCurrentTown = currentTownPos != null && pos.equals(currentTownPos);
        boolean withinBounds = isTownWithinMapBounds(town);
        
        int screenX, screenY;
        // Current town should NEVER be clamped to edges - always show at actual position
        if (withinBounds || isCurrentTown) {
            // Town is within map bounds OR is current town - use normal position
            screenX = worldToScreenX(pos.getX());
            screenY = worldToScreenZ(pos.getZ());
        } else {
            // Town is outside bounds and NOT current town - clamp to edge
            int[] edgePos = getTownEdgePosition(town);
            screenX = edgePos[0];
            screenY = edgePos[1];
        }
        
        // Determine marker color
        int markerColor = TOWN_MARKER_COLOR;
        if (town == selectedTown) {
            markerColor = SELECTED_TOWN_COLOR;
        } else if (isCurrentTown) {
            markerColor = CURRENT_TOWN_COLOR;
        }
        
        // Draw marker with size that scales with zoom level
        int markerSize = getAdaptiveMarkerSize();
        
        // Initialize marker position (will be clamped for current town if needed)
        int markerX = screenX;
        int markerY = screenY;
        
        // For edge indicators (non-current towns outside bounds), make them dimmer and triangular
        if (!withinBounds && !isCurrentTown) {
            markerColor = (markerColor & 0x00FFFFFF) | 0x80000000; // Make semi-transparent
            // Draw triangular edge indicator pointing towards the town
            drawEdgeIndicator(guiGraphics, screenX, screenY, town, markerColor, markerSize);
        } else {
            // For current town, clamp marker position to stay within map bounds
            
            if (isCurrentTown) {
                int[] clamped = clampToMapBounds(screenX, screenY, markerSize/2);
                markerX = clamped[0];
                markerY = clamped[1];
            }
            
            // Normal town marker (with clamped position for current town)
            guiGraphics.fill(markerX - markerSize/2, markerY - markerSize/2, 
                            markerX + markerSize/2, markerY + markerSize/2, markerColor);
            
            // Draw town name above marker (only for towns within bounds)
            if (withinBounds) {
                String townName = town.name;
                int textWidth = this.font.width(townName);
                
                // Calculate name position
                int nameX = screenX - textWidth/2;
                int nameY = screenY - markerSize/2 - 12;
                
                // Check if town name would overlap with info panel
                boolean nameOverlapsInfo = selectedTown != null && 
                    nameY >= panelTop + 50 && nameY <= panelTop + 110 && 
                    nameX + textWidth >= panelLeft && nameX <= panelLeft + 200;
                
                if (!nameOverlapsInfo) {
                    guiGraphics.drawString(this.font, townName, nameX, nameY, TEXT_COLOR);
                }
            }
        }
        
        // Highlight if mouse is over (use clamped marker position for current town)
        int checkX = isCurrentTown ? markerX : screenX;
        int checkY = isCurrentTown ? markerY : screenY;
        
        if (mouseX >= checkX - markerSize && mouseX <= checkX + markerSize &&
            mouseY >= checkY - markerSize && mouseY <= checkY + markerSize) {
            // Calculate highlight ring bounds
            int ringLeft = checkX - markerSize - 1;
            int ringRight = checkX + markerSize + 1;
            int ringTop = checkY - markerSize - 1;
            int ringBottom = checkY + markerSize + 1;
            
            // For current town, clamp highlight ring to stay within map bounds
            if (isCurrentTown) {
                int[] clampedTopLeft = clampToMapBounds(ringLeft, ringTop, 0);
                int[] clampedBottomRight = clampToMapBounds(ringRight, ringBottom, 0);
                ringLeft = clampedTopLeft[0];
                ringTop = clampedTopLeft[1];
                ringRight = clampedBottomRight[0];
                ringBottom = clampedBottomRight[1];
            }
            
            // Draw highlight ring
            guiGraphics.hLine(ringLeft, ringRight, ringTop, 0xFFFFFFFF);
            guiGraphics.hLine(ringLeft, ringRight, ringBottom, 0xFFFFFFFF);
            guiGraphics.vLine(ringLeft, ringTop, ringBottom, 0xFFFFFFFF);
            guiGraphics.vLine(ringRight, ringTop, ringBottom, 0xFFFFFFFF);
        }
    }
    
    /**
     * Draw edge indicator for towns outside the map bounds
     */
    private void drawEdgeIndicator(GuiGraphics guiGraphics, int edgeX, int edgeY, 
                                  TownMapDataResponsePacket.TownMapInfo town, int color, int size) {
        // Get the actual town position
        int actualX = worldToScreenX(town.position.getX());
        int actualY = worldToScreenZ(town.position.getZ());
        
        // Calculate direction vector from edge to actual position
        int dx = actualX - edgeX;
        int dy = actualY - edgeY;
        
        // Normalize and draw a small triangle pointing in that direction
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length > 0) {
            // Draw a small diamond/arrow shape
            guiGraphics.fill(edgeX - size/3, edgeY - size/3, edgeX + size/3, edgeY + size/3, color);
            
            // Add a small directional indicator
            int arrowSize = size / 4;
            if (Math.abs(dx) > Math.abs(dy)) {
                // Horizontal arrow
                int arrowDir = dx > 0 ? 1 : -1;
                guiGraphics.fill(edgeX + arrowDir * arrowSize, edgeY - arrowSize/2, 
                               edgeX + arrowDir * arrowSize + arrowDir * arrowSize/2, edgeY + arrowSize/2, color);
            } else {
                // Vertical arrow
                int arrowDir = dy > 0 ? 1 : -1;
                guiGraphics.fill(edgeX - arrowSize/2, edgeY + arrowDir * arrowSize, 
                               edgeX + arrowSize/2, edgeY + arrowDir * arrowSize + arrowDir * arrowSize/2, color);
            }
        }
    }
    
    /**
     * Draw platforms and paths for the selected town
     */
    private void drawSelectedTownPlatforms(GuiGraphics guiGraphics) {
        if (selectedTown == null || selectedTownPlatforms == null || selectedTownPlatforms.isEmpty()) {
            return;
        }
        
        int[] bounds = getMapBounds();
        int mapLeft = bounds[0], mapTop = bounds[1], mapRight = bounds[2], mapBottom = bounds[3];
        
        // Draw platforms and their paths
        for (TownPlatformDataResponsePacket.PlatformInfo platform : selectedTownPlatforms.values()) {
            // Convert world coordinates to screen coordinates
            int startScreenX = worldToScreenX(platform.startPos.getX());
            int startScreenY = worldToScreenZ(platform.startPos.getZ());
            int endScreenX = worldToScreenX(platform.endPos.getX());
            int endScreenY = worldToScreenZ(platform.endPos.getZ());
            
            // Only draw if at least part of the path is visible
            if (isLineVisible(startScreenX, startScreenY, endScreenX, endScreenY, mapLeft, mapTop, mapRight, mapBottom)) {
                // Choose color based on platform status and selection
                boolean isSelected = selectedPlatform != null && selectedPlatform.id.equals(platform.id);
                int pathColor, platformColor;
                
                if (isSelected) {
                    // Bright colors for selected platform
                    pathColor = 0xFFFFFF00; // Bright yellow for selected
                    platformColor = 0xFFFFAA00; // Orange for selected markers
                } else {
                    // Normal colors based on enabled status
                    pathColor = platform.enabled ? 0xFF00FF00 : 0xFFFF0000; // Green if enabled, red if disabled
                    platformColor = platform.enabled ? 0xFF00AA00 : 0xFFAA0000; // Darker green/red for markers
                }
                
                // Draw the path line with clipping
                drawClippedLine(guiGraphics, startScreenX, startScreenY, endScreenX, endScreenY, pathColor, 
                              mapLeft, mapTop, mapRight, mapBottom);
                
                // Draw platform markers (small squares) - reduced by 50%
                int markerSize = Math.max(1, (int)(2 * Math.min(zoomLevel / 10.0, 2.0))); // Size scales with zoom, 50% smaller
                
                // Start platform marker
                if (isPointInBounds(startScreenX, startScreenY, mapLeft, mapTop, mapRight, mapBottom)) {
                    guiGraphics.fill(startScreenX - markerSize, startScreenY - markerSize, 
                                   startScreenX + markerSize, startScreenY + markerSize, platformColor);
                    
                    // Add highlight ring for selected platform
                    if (isSelected) {
                        int ringSize = markerSize + 2;
                        guiGraphics.hLine(startScreenX - ringSize, startScreenX + ringSize, startScreenY - ringSize, 0xFFFFFFFF);
                        guiGraphics.hLine(startScreenX - ringSize, startScreenX + ringSize, startScreenY + ringSize, 0xFFFFFFFF);
                        guiGraphics.vLine(startScreenX - ringSize, startScreenY - ringSize, startScreenY + ringSize, 0xFFFFFFFF);
                        guiGraphics.vLine(startScreenX + ringSize, startScreenY - ringSize, startScreenY + ringSize, 0xFFFFFFFF);
                    }
                }
                
                // End platform marker
                if (isPointInBounds(endScreenX, endScreenY, mapLeft, mapTop, mapRight, mapBottom)) {
                    guiGraphics.fill(endScreenX - markerSize, endScreenY - markerSize, 
                                   endScreenX + markerSize, endScreenY + markerSize, platformColor);
                    
                    // Add highlight ring for selected platform
                    if (isSelected) {
                        int ringSize = markerSize + 2;
                        guiGraphics.hLine(endScreenX - ringSize, endScreenX + ringSize, endScreenY - ringSize, 0xFFFFFFFF);
                        guiGraphics.hLine(endScreenX - ringSize, endScreenX + ringSize, endScreenY + ringSize, 0xFFFFFFFF);
                        guiGraphics.vLine(endScreenX - ringSize, endScreenY - ringSize, endScreenY + ringSize, 0xFFFFFFFF);
                        guiGraphics.vLine(endScreenX + ringSize, endScreenY - ringSize, endScreenY + ringSize, 0xFFFFFFFF);
                    }
                }
                
                // Draw platform name at start position if zoom is high enough
                if (zoomLevel >= 5.0 && isPointInBounds(startScreenX, startScreenY, mapLeft, mapTop, mapRight, mapBottom)) {
                    String platformName = platform.name;
                    int textWidth = this.font.width(platformName);
                    int nameX = startScreenX - textWidth / 2;
                    int nameY = startScreenY - markerSize - 12;
                    
                    // Ensure name doesn't go outside map bounds
                    nameX = Math.max(mapLeft, Math.min(mapRight - textWidth, nameX));
                    nameY = Math.max(mapTop, Math.min(mapBottom - 10, nameY));
                    
                    // Check if platform name would overlap with town info panel
                    boolean nameOverlapsInfo = selectedTown != null && 
                        nameY >= panelTop + 50 && nameY <= panelTop + 110 && 
                        nameX + textWidth >= panelLeft + 10 && nameX <= panelLeft + 210;
                    
                    if (!nameOverlapsInfo) {
                        // Draw platform name with background for visibility
                        guiGraphics.fill(nameX - 2, nameY - 1, nameX + textWidth + 2, nameY + 9, 0x80000000);
                        guiGraphics.drawString(this.font, platformName, nameX, nameY, 0xFFFFFFFF);
                    }
                }
            }
        }
        
        // Draw boundary circle for selected town (after platform rendering)
        drawSelectedTownBoundary(guiGraphics);
    }
    
    /**
     * Draw boundary circle for the selected town
     */
    private void drawSelectedTownBoundary(GuiGraphics guiGraphics) {
        if (selectedTown == null || selectedTownInfo == null) {
            return; // Need selected town and live town info (boundary only shows after clicking)
        }
        
        int[] bounds = getMapBounds();
        int mapLeft = bounds[0], mapTop = bounds[1], mapRight = bounds[2], mapBottom = bounds[3];
        
        // Use live server-calculated boundary radius from platform packet
        int boundaryRadius = selectedTownInfo.boundaryRadius;
        if (boundaryRadius <= 0) {
            return; // No boundary to draw
        }
        
        // Convert town center to screen coordinates
        BlockPos townPos = selectedTown.position;
        int centerScreenX = worldToScreenX(townPos.getX());
        int centerScreenY = worldToScreenZ(townPos.getZ());
        
        // Convert boundary radius from world blocks to screen pixels
        int radiusInPixels = (int)(boundaryRadius * zoomLevel * 0.1);
        
        // Use green color matching 3D boundary visualization
        int boundaryColor = 0xFF00FF00; // Bright green like 3D boundaries
        
        // Draw the circular boundary using line segments (64 segments for smooth circle)
        int segments = 64;
        double angleStep = 2 * Math.PI / segments;
        
        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            
            // Calculate start and end points of this segment
            int x1 = centerScreenX + (int)(Math.cos(angle1) * radiusInPixels);
            int y1 = centerScreenY + (int)(Math.sin(angle1) * radiusInPixels);
            int x2 = centerScreenX + (int)(Math.cos(angle2) * radiusInPixels);
            int y2 = centerScreenY + (int)(Math.sin(angle2) * radiusInPixels);
            
            // Draw the line segment with clipping to map bounds
            drawClippedLine(guiGraphics, x1, y1, x2, y2, boundaryColor, 
                          mapLeft, mapTop, mapRight, mapBottom);
        }
    }
    
    /**
     * Draw a line between two points with clipping to specified bounds
     */
    private void drawClippedLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color,
                                int clipLeft, int clipTop, int clipRight, int clipBottom) {
        // Use Cohen-Sutherland line clipping algorithm
        int[] clipped = clipLine(x1, y1, x2, y2, clipLeft, clipTop, clipRight, clipBottom);
        if (clipped == null) return; // Line is completely outside bounds
        
        // Draw the clipped line
        drawLine(guiGraphics, clipped[0], clipped[1], clipped[2], clipped[3], color);
    }
    
    /**
     * Draw a line between two points (assuming points are within bounds)
     */
    private void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        // Simple line drawing using filled rectangles
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        
        if (steps == 0) return;
        
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
    }
    
    /**
     * Cohen-Sutherland line clipping algorithm
     * Returns clipped line coordinates [x1, y1, x2, y2] or null if completely outside
     */
    private int[] clipLine(int x1, int y1, int x2, int y2, int clipLeft, int clipTop, int clipRight, int clipBottom) {
        // Compute outcodes for both endpoints
        int outcode1 = computeOutcode(x1, y1, clipLeft, clipTop, clipRight, clipBottom);
        int outcode2 = computeOutcode(x2, y2, clipLeft, clipTop, clipRight, clipBottom);
        
        while (true) {
            if ((outcode1 | outcode2) == 0) {
                // Both points inside - line is completely visible
                return new int[]{x1, y1, x2, y2};
            } else if ((outcode1 & outcode2) != 0) {
                // Both points outside same boundary - line is completely invisible
                return null;
            } else {
                // Line crosses boundary - clip it
                int outcodeOut = (outcode1 != 0) ? outcode1 : outcode2;
                int x, y;
                
                if ((outcodeOut & 8) != 0) { // Top
                    x = x1 + (x2 - x1) * (clipTop - y1) / (y2 - y1);
                    y = clipTop;
                } else if ((outcodeOut & 4) != 0) { // Bottom
                    x = x1 + (x2 - x1) * (clipBottom - y1) / (y2 - y1);
                    y = clipBottom;
                } else if ((outcodeOut & 2) != 0) { // Right
                    y = y1 + (y2 - y1) * (clipRight - x1) / (x2 - x1);
                    x = clipRight;
                } else { // Left
                    y = y1 + (y2 - y1) * (clipLeft - x1) / (x2 - x1);
                    x = clipLeft;
                }
                
                if (outcodeOut == outcode1) {
                    x1 = x;
                    y1 = y;
                    outcode1 = computeOutcode(x1, y1, clipLeft, clipTop, clipRight, clipBottom);
                } else {
                    x2 = x;
                    y2 = y;
                    outcode2 = computeOutcode(x2, y2, clipLeft, clipTop, clipRight, clipBottom);
                }
            }
        }
    }
    
    /**
     * Compute outcode for Cohen-Sutherland clipping
     */
    private int computeOutcode(int x, int y, int clipLeft, int clipTop, int clipRight, int clipBottom) {
        int code = 0;
        if (x < clipLeft) code |= 1; // Left
        if (x > clipRight) code |= 2; // Right
        if (y < clipTop) code |= 8; // Top
        if (y > clipBottom) code |= 4; // Bottom
        return code;
    }
    
    /**
     * Check if a line is visible within the given bounds
     */
    private boolean isLineVisible(int x1, int y1, int x2, int y2, int left, int top, int right, int bottom) {
        // Simple bounds check - if either endpoint is visible or line crosses bounds
        return isPointInBounds(x1, y1, left, top, right, bottom) || 
               isPointInBounds(x2, y2, left, top, right, bottom) ||
               (x1 < left && x2 > right) || (x1 > right && x2 < left) ||
               (y1 < top && y2 > bottom) || (y1 > bottom && y2 < top);
    }
    
    /**
     * Check if a point is within the given bounds
     */
    private boolean isPointInBounds(int x, int y, int left, int top, int right, int bottom) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }
    
    /**
     * Draw current position marker
     */
    private void drawCurrentPosition(GuiGraphics guiGraphics) {
        if (currentTownPos != null) {
            int screenX = worldToScreenX(currentTownPos.getX());
            int screenY = worldToScreenZ(currentTownPos.getZ());
            // ALWAYS ensure current town marker stays within map bounds
            int[] clamped = clampToMapBounds(screenX, screenY, 4);
            screenX = clamped[0];
            screenY = clamped[1];
            
            // Draw a distinct marker for current position
            int size = 4;
            guiGraphics.fill(screenX - size, screenY - size, screenX + size, screenY + size, CURRENT_TOWN_COLOR);
            
            // Draw border
            guiGraphics.hLine(screenX - size - 1, screenX + size, screenY - size - 1, TEXT_COLOR);
            guiGraphics.hLine(screenX - size - 1, screenX + size, screenY + size, TEXT_COLOR);
            guiGraphics.vLine(screenX - size - 1, screenY - size - 1, screenY + size, TEXT_COLOR);
            guiGraphics.vLine(screenX + size, screenY - size - 1, screenY + size, TEXT_COLOR);
        }
    }
    
    /**
     * Draw information about selected town or platform
     */
    private void drawTownInfo(GuiGraphics guiGraphics) {
        if (selectedTown == null || currentTownPos == null) return;
        
        // Draw info background
        int infoX = panelLeft + 10;
        int infoY = panelTop + 50;
        int infoWidth = 200;
        int infoHeight = 60; // Same height for both town and platform info
        
        guiGraphics.fill(infoX, infoY, infoX + infoWidth, infoY + infoHeight, INFO_BACKGROUND);
        guiGraphics.hLine(infoX, infoX + infoWidth - 1, infoY, BORDER_COLOR);
        guiGraphics.hLine(infoX, infoX + infoWidth - 1, infoY + infoHeight - 1, BORDER_COLOR);
        guiGraphics.vLine(infoX, infoY, infoY + infoHeight - 1, BORDER_COLOR);
        guiGraphics.vLine(infoX + infoWidth - 1, infoY, infoY + infoHeight - 1, BORDER_COLOR);
        
        if (selectedPlatform != null) {
            // Draw platform information
            String statusText = selectedPlatform.enabled ? "(Enabled)" : "(Disabled)";
            guiGraphics.drawString(this.font, "Selected: " + selectedPlatform.name + " " + statusText, infoX + 5, infoY + 8, TEXT_COLOR);
            guiGraphics.drawString(this.font, "Town: " + selectedTown.name, infoX + 5, infoY + 20, TEXT_COLOR);
            guiGraphics.drawString(this.font, String.format("Start Pos: %d, %d, %d", 
                selectedPlatform.startPos.getX(), selectedPlatform.startPos.getY(), selectedPlatform.startPos.getZ()), 
                infoX + 5, infoY + 32, TEXT_COLOR);
            guiGraphics.drawString(this.font, String.format("End Pos: %d, %d, %d", 
                selectedPlatform.endPos.getX(), selectedPlatform.endPos.getY(), selectedPlatform.endPos.getZ()), 
                infoX + 5, infoY + 44, TEXT_COLOR);
        } else {
            // Draw town information
            BlockPos selectedPos = selectedTown.position;
            double distance = Math.sqrt(currentTownPos.distSqr(selectedPos));
            
            guiGraphics.drawString(this.font, "Selected: " + selectedTown.name, infoX + 5, infoY + 8, TEXT_COLOR);
            guiGraphics.drawString(this.font, String.format("Distance: %.1f blocks", distance), infoX + 5, infoY + 20, TEXT_COLOR);
            guiGraphics.drawString(this.font, String.format("Position: X %d, Z %d", selectedPos.getX(), selectedPos.getZ()), infoX + 5, infoY + 32, TEXT_COLOR);
            guiGraphics.drawString(this.font, "Population: " + selectedTown.population, infoX + 5, infoY + 44, TEXT_COLOR);
        }
    }
    
    /**
     * Get direction string from current position to target
     */
    private String getDirection(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? "East" : "West";
        } else {
            return dz > 0 ? "South" : "North";
        }
    }
    
    /**
     * Draw controls help text
     */
    private void drawControlsHelp(GuiGraphics guiGraphics) {
        String[] helpText = {
            "Drag to pan • Scroll to zoom • Click town for info"
        };
        
        int helpX = panelLeft + panelWidth / 2;
        int helpY = panelTop + panelHeight - 50;
        
        for (int i = 0; i < helpText.length; i++) {
            int textWidth = this.font.width(helpText[i]);
            guiGraphics.drawString(this.font, helpText[i], 
                                  helpX - textWidth/2, helpY + i * 12, 0xFFAAAAAA);
        }
    }
    
    /**
     * Handle mouse clicks
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            // Check if clicking on a platform first (if we have a selected town with platforms)
            if (selectedTown != null && selectedTownPlatforms != null) {
                TownPlatformDataResponsePacket.PlatformInfo clickedPlatform = getPlatformAtPosition((int)mouseX, (int)mouseY);
                if (clickedPlatform != null) {
                    selectedPlatform = clickedPlatform;
                    DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Selected platform: {}", clickedPlatform.name);
                    return true;
                }
            }
            
            // Check if clicking on a town
            TownMapDataResponsePacket.TownMapInfo clickedTown = getTownAtPosition((int)mouseX, (int)mouseY);
            if (clickedTown != null) {
                // If selecting a different town, clear previous platform data and selection
                if (selectedTown == null || !selectedTown.id.equals(clickedTown.id)) {
                    selectedTownPlatforms = null;
                    selectedPlatform = null;
                    selectedTownInfo = null; // Clear live town info
                }
                
                selectedTown = clickedTown;
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Selected town: {}", clickedTown.name);
                
                // Request platform data for the selected town
                requestTownPlatformData(clickedTown.id);
                return true;
            }
            
            // Check if clicking outside map area to deselect
            if (!isInMapArea((int)mouseX, (int)mouseY)) {
                if (selectedPlatform != null || selectedTown != null) {
                    selectedPlatform = null;
                    selectedTown = null;
                    selectedTownPlatforms = null;
                    selectedTownInfo = null; // Clear live town info
                    DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Deselected town and platform");
                    return true;
                }
            }
            
            // Start dragging if clicking on map area (and nothing was selected)
            if (isInMapArea((int)mouseX, (int)mouseY)) {
                // Clear platform selection when clicking empty space on map
                if (selectedPlatform != null) {
                    selectedPlatform = null;
                    DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Deselected platform");
                    return true;
                }
                
                isDragging = true;
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Handle mouse dragging for panning
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && button == 0) {
            // Pan the map
            double deltaX = (mouseX - lastMouseX) / (zoomLevel * 0.1);
            double deltaY = (mouseY - lastMouseY) / (zoomLevel * 0.1);
            
            mapOffsetX -= deltaX;
            mapOffsetZ -= deltaY;
            
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            
            return true;
        }
        
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    /**
     * Handle mouse release
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging && button == 0) {
            isDragging = false;
            return true;
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    /**
     * Handle mouse scrolling for zoom
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isInMapArea((int)mouseX, (int)mouseY)) {
            // Zoom in/out with better scaling
            // At max zoom (600x), the map should show 5x5 blocks
            double zoomFactor = delta > 0 ? 1.3 : (1.0 / 1.3);
            zoomLevel = Math.max(0.05, Math.min(600.0, zoomLevel * zoomFactor));
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Map zoom level: {}", zoomLevel);
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    /**
     * Get map bounds as [left, top, right, bottom]
     */
    private int[] getMapBounds() {
        int mapLeft = mapCenterX - mapWidth / 2;
        int mapTop = mapCenterY - mapHeight / 2;
        return new int[]{mapLeft, mapTop, mapLeft + mapWidth, mapTop + mapHeight};
    }
    
    /**
     * Check if coordinates are within the map area
     */
    private boolean isInMapArea(int x, int y) {
        int[] bounds = getMapBounds();
        return x >= bounds[0] && x <= bounds[2] && y >= bounds[1] && y <= bounds[3];
    }
    
    /**
     * Get town at the given screen coordinates
     */
    private TownMapDataResponsePacket.TownMapInfo getTownAtPosition(int screenX, int screenY) {
        if (allTowns == null) return null;
        
        for (TownMapDataResponsePacket.TownMapInfo town : allTowns.values()) {
            if (isTownVisible(town)) {
                BlockPos pos = town.position;
                boolean isCurrentTown = currentTownPos != null && pos.equals(currentTownPos);
                boolean withinBounds = isTownWithinMapBounds(town);
                
                int townScreenX, townScreenY;
                if (withinBounds || isCurrentTown) {
                    townScreenX = worldToScreenX(pos.getX());
                    townScreenY = worldToScreenZ(pos.getZ());
                    
                    // For current town, use clamped position for click detection
                    if (isCurrentTown) {
                        int markerSize = getAdaptiveMarkerSize();
                        int[] clamped = clampToMapBounds(townScreenX, townScreenY, markerSize/2);
                        townScreenX = clamped[0];
                        townScreenY = clamped[1];
                    }
                } else {
                    int[] edgePos = getTownEdgePosition(town);
                    townScreenX = edgePos[0];
                    townScreenY = edgePos[1];
                }
                
                int markerSize = getAdaptiveMarkerSize();
                if (screenX >= townScreenX - markerSize && screenX <= townScreenX + markerSize &&
                    screenY >= townScreenY - markerSize && screenY <= townScreenY + markerSize) {
                    return town;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get platform at the given screen coordinates
     */
    private TownPlatformDataResponsePacket.PlatformInfo getPlatformAtPosition(int screenX, int screenY) {
        if (selectedTownPlatforms == null || selectedTownPlatforms.isEmpty()) return null;
        
        int[] bounds = getMapBounds();
        int mapLeft = bounds[0], mapTop = bounds[1], mapRight = bounds[2], mapBottom = bounds[3];
        
        for (TownPlatformDataResponsePacket.PlatformInfo platform : selectedTownPlatforms.values()) {
            // Convert world coordinates to screen coordinates
            int startScreenX = worldToScreenX(platform.startPos.getX());
            int startScreenY = worldToScreenZ(platform.startPos.getZ());
            int endScreenX = worldToScreenX(platform.endPos.getX());
            int endScreenY = worldToScreenZ(platform.endPos.getZ());
            
            // Only check platforms that are at least partially visible
            if (isLineVisible(startScreenX, startScreenY, endScreenX, endScreenY, mapLeft, mapTop, mapRight, mapBottom)) {
                int markerSize = Math.max(1, (int)(2 * Math.min(zoomLevel / 10.0, 2.0))); // Same as drawing logic
                
                // Check start marker
                if (isPointInBounds(startScreenX, startScreenY, mapLeft, mapTop, mapRight, mapBottom)) {
                    if (screenX >= startScreenX - markerSize - 2 && screenX <= startScreenX + markerSize + 2 &&
                        screenY >= startScreenY - markerSize - 2 && screenY <= startScreenY + markerSize + 2) {
                        return platform;
                    }
                }
                
                // Check end marker
                if (isPointInBounds(endScreenX, endScreenY, mapLeft, mapTop, mapRight, mapBottom)) {
                    if (screenX >= endScreenX - markerSize - 2 && screenX <= endScreenX + markerSize + 2 &&
                        screenY >= endScreenY - markerSize - 2 && screenY <= endScreenY + markerSize + 2) {
                        return platform;
                    }
                }
                
                // Check line (with some tolerance)
                if (isClickOnLine(screenX, screenY, startScreenX, startScreenY, endScreenX, endScreenY, 3)) {
                    return platform;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if a click is on a line within tolerance
     */
    private boolean isClickOnLine(int clickX, int clickY, int x1, int y1, int x2, int y2, int tolerance) {
        // Calculate distance from point to line
        double lineLength = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
        if (lineLength == 0) return false; // Degenerate line
        
        double distance = Math.abs((y2 - y1) * clickX - (x2 - x1) * clickY + x2 * y1 - y2 * x1) / lineLength;
        
        // Also check if the click is within the line segment bounds
        double dotProduct = ((clickX - x1) * (x2 - x1) + (clickY - y1) * (y2 - y1)) / (lineLength * lineLength);
        boolean withinSegment = dotProduct >= 0 && dotProduct <= 1;
        
        return distance <= tolerance && withinSegment;
    }
    
    /**
     * Handle screen close
     */
    @Override
    public void onClose() {
        // Clear map cache to ensure fresh data on next open
        ClientTownMapCache.getInstance().clear();
        
        // Execute close callback BEFORE switching screens
        if (onCloseCallback != null) {
            onCloseCallback.accept(this);
        }
        
        // Return to parent screen
        this.minecraft.setScreen(parentScreen);
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "TownMapModal closed and cache cleared");
    }
    
    /**
     * Prevent game from pausing when screen is open
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    /**
     * Set town data (for external initialization)
     */
    public void setTownData(Map<UUID, TownMapDataResponsePacket.TownMapInfo> townData) {
        this.allTowns = townData != null ? new java.util.HashMap<>(townData) : new java.util.HashMap<>();
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Town data set: {} towns", this.allTowns.size());
    }
    
    /**
     * Refresh town data from cache (called when new data arrives from server)
     */
    public void refreshFromCache() {
        ClientTownMapCache cache = ClientTownMapCache.getInstance();
        if (cache.hasData()) {
            this.allTowns = cache.getAllTowns();
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Town data refreshed from cache: {} towns", this.allTowns.size());
        }
    }
    
    /**
     * Request platform data for a specific town
     */
    private void requestTownPlatformData(UUID townId) {
        try {
            // Always request fresh data from server for live updates
            // This ensures population and other town data is always current
            PlatformAccess.getNetworkMessages().sendToServer(new RequestTownPlatformDataPacket(townId));
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "Requesting fresh platform and town data for town {}", townId);
                
        } catch (Exception e) {
            LOGGER.error("Failed to request platform data for town {}", townId, e);
        }
    }
    
    /**
     * Handle platform data response from server
     */
    public void refreshPlatformData(UUID townId, Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> platforms) {
        if (selectedTown != null && selectedTown.id.equals(townId)) {
            selectedTownPlatforms = new HashMap<>(platforms);
            
            // Check if the currently selected platform still exists in the new data
            if (selectedPlatform != null) {
                boolean platformStillExists = false;
                for (TownPlatformDataResponsePacket.PlatformInfo platform : platforms.values()) {
                    if (platform.id.equals(selectedPlatform.id)) {
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
        if (selectedTown != null && selectedTown.id.equals(townId)) {
            // Store the live town info including boundary radius
            selectedTownInfo = townInfo;
            
            // Update the selected town with fresh data (no boundary in map data anymore)
            selectedTown = new TownMapDataResponsePacket.TownMapInfo(
                selectedTown.id,
                townInfo.name,
                selectedTown.position,
                townInfo.population,
                townInfo.touristCount
            );
            
            // Also update the main town cache with fresh data
            if (allTowns != null) {
                allTowns.put(townId, selectedTown);
            }
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                "Refreshed town data for selected town {}: pop={}, tourists={}, boundary={}", 
                townId, townInfo.population, townInfo.touristCount, townInfo.boundaryRadius);
        } else {
            // If this town data is for a different town than selected, clear platform selection
            if (selectedPlatform != null) {
                selectedPlatform = null;
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                    "Cleared platform selection due to town data for different town");
            }
        }
    }
}
