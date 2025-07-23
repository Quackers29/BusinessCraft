package com.yourdomain.businesscraft.ui.modal.specialized;

import com.yourdomain.businesscraft.debug.DebugConfig;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.packets.ui.RequestTownMapDataPacket;
import com.yourdomain.businesscraft.network.packets.ui.ClientTownMapCache;
import com.yourdomain.businesscraft.network.packets.ui.TownMapDataResponsePacket;
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
    
    // Colors (themed to match existing UI)
    private static final int BACKGROUND_COLOR = 0xFF222222;
    private static final int BORDER_COLOR = 0xFFAAAAAA;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int MAP_BACKGROUND_COLOR = 0xFF1A1A1A;
    private static final int TOWN_MARKER_COLOR = 0xFF4CAF50;
    private static final int CURRENT_TOWN_COLOR = 0xFFFF9800;
    private static final int SELECTED_TOWN_COLOR = 0xFF2196F3;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int INFO_BACKGROUND = 0xFF333333; // Fully opaque now
    
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
     * Load town data from client cache or request from server
     */
    private void loadTownData() {
        try {
            ClientTownMapCache cache = ClientTownMapCache.getInstance();
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                "Loading town data - Cache has data: {}, Cache is stale: {}", 
                cache.hasData(), cache.isStale());
            
            // Check if we have cached data and it's not stale
            if (cache.hasData() && !cache.isStale()) {
                this.allTowns = cache.getAllTowns();
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                    "Town data loaded from cache: {} towns", this.allTowns.size());
            } else {
                // Request fresh data from server
                this.allTowns = new java.util.HashMap<>();
                ModMessages.sendToServer(new RequestTownMapDataPacket());
                
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                    "Requested fresh town data from server - cache had {} towns", cache.size());
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Sending RequestTownMapDataPacket to server");
            }
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
     * Check if a town marker is within the map bounds
     */
    private boolean isTownWithinMapBounds(TownMapDataResponsePacket.TownMapInfo town) {
        int screenX = worldToScreenX(town.position.getX());
        int screenY = worldToScreenZ(town.position.getZ());
        
        int mapLeft = mapCenterX - mapWidth / 2;
        int mapTop = mapCenterY - mapHeight / 2;
        
        return screenX >= mapLeft && screenX <= mapLeft + mapWidth &&
               screenY >= mapTop && screenY <= mapTop + mapHeight;
    }
    
    /**
     * Get the edge position for a town that's outside the map bounds
     */
    private int[] getTownEdgePosition(TownMapDataResponsePacket.TownMapInfo town) {
        int screenX = worldToScreenX(town.position.getX());
        int screenY = worldToScreenZ(town.position.getZ());
        
        int mapLeft = mapCenterX - mapWidth / 2;
        int mapTop = mapCenterY - mapHeight / 2;
        int mapRight = mapLeft + mapWidth;
        int mapBottom = mapTop + mapHeight;
        
        // Clamp to map edges
        int clampedX = Math.max(mapLeft, Math.min(mapRight, screenX));
        int clampedY = Math.max(mapTop, Math.min(mapBottom, screenY));
        
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
        // Draw map background
        int mapLeft = mapCenterX - mapWidth / 2;
        int mapTop = mapCenterY - mapHeight / 2;
        guiGraphics.fill(mapLeft, mapTop, mapLeft + mapWidth, mapTop + mapHeight, MAP_BACKGROUND_COLOR);
        
        // Draw map border
        guiGraphics.hLine(mapLeft, mapLeft + mapWidth - 1, mapTop, BORDER_COLOR);
        guiGraphics.hLine(mapLeft, mapLeft + mapWidth - 1, mapTop + mapHeight - 1, BORDER_COLOR);
        guiGraphics.vLine(mapLeft, mapTop, mapTop + mapHeight - 1, BORDER_COLOR);
        guiGraphics.vLine(mapLeft + mapWidth - 1, mapTop, mapTop + mapHeight - 1, BORDER_COLOR);
        
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
        int mapLeft = mapCenterX - mapWidth / 2;
        int mapTop = mapCenterY - mapHeight / 2;
        int gridColor = 0x40AAAAAA;
        
        // Calculate adaptive grid spacing based on zoom level
        int baseGridSpacing = getAdaptiveGridSpacing();
        
        // Calculate the world coordinates visible on screen
        double viewWidthInWorld = mapWidth / (zoomLevel * 0.1);
        double viewHeightInWorld = mapHeight / (zoomLevel * 0.1);
        
        // Calculate starting positions for grid lines
        int startX = ((int)(mapOffsetX - viewWidthInWorld / 2) / baseGridSpacing) * baseGridSpacing;
        int startZ = ((int)(mapOffsetZ - viewHeightInWorld / 2) / baseGridSpacing) * baseGridSpacing;
        
        // Draw vertical lines
        for (int worldX = startX; worldX <= mapOffsetX + viewWidthInWorld / 2; worldX += baseGridSpacing) {
            int screenX = worldToScreenX(worldX);
            if (screenX >= mapLeft && screenX <= mapLeft + mapWidth) {
                guiGraphics.vLine(screenX, mapTop, mapTop + mapHeight, gridColor);
            }
        }
        
        // Draw horizontal lines
        for (int worldZ = startZ; worldZ <= mapOffsetZ + viewHeightInWorld / 2; worldZ += baseGridSpacing) {
            int screenY = worldToScreenZ(worldZ);
            if (screenY >= mapTop && screenY <= mapTop + mapHeight) {
                guiGraphics.hLine(mapLeft, mapLeft + mapWidth, screenY, gridColor);
            }
        }
    }
    
    /**
     * Calculate adaptive grid spacing based on zoom level
     */
    private int getAdaptiveGridSpacing() {
        // Grid spacing adapts to zoom level for optimal visibility
        if (zoomLevel >= 20.0) {
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
        if (zoomLevel >= 15.0) {
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
        int mapLeft = mapCenterX - mapWidth / 2;
        int mapTop = mapCenterY - mapHeight / 2;
        int mapRight = mapLeft + mapWidth;
        int mapBottom = mapTop + mapHeight;
        
        // Calculate the world coordinates visible on screen
        double viewWidthInWorld = mapWidth / (zoomLevel * 0.1);
        double viewHeightInWorld = mapHeight / (zoomLevel * 0.1);
        
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
        // Calculate the world coordinates visible on screen
        double viewWidthInWorld = mapWidth / (zoomLevel * 0.1);
        double viewHeightInWorld = mapHeight / (zoomLevel * 0.1);
        
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
                int mapLeft = mapCenterX - mapWidth / 2;
                int mapTop = mapCenterY - mapHeight / 2;
                int mapRight = mapLeft + mapWidth;
                int mapBottom = mapTop + mapHeight;
                
                // Clamp marker position to stay within map bounds
                markerX = Math.max(mapLeft + markerSize/2, Math.min(mapRight - markerSize/2, screenX));
                markerY = Math.max(mapTop + markerSize/2, Math.min(mapBottom - markerSize/2, screenY));
            }
            
            // Normal town marker (with clamped position for current town)
            guiGraphics.fill(markerX - markerSize/2, markerY - markerSize/2, 
                            markerX + markerSize/2, markerY + markerSize/2, markerColor);
            
            // Draw town name above marker (for towns within bounds OR current town)
            if (withinBounds || isCurrentTown) {
                String townName = town.name;
                int textWidth = this.font.width(townName);
                
                // Calculate name position (use clamped marker position for current town)
                int nameX = (isCurrentTown ? markerX : screenX) - textWidth/2;
                int nameY = (isCurrentTown ? markerY : screenY) - markerSize/2 - 12;
                
                // Check if town name would overlap with info panel
                boolean nameOverlapsInfo = selectedTown != null && 
                    nameY >= panelTop + 50 && nameY <= panelTop + 110 && 
                    nameX + textWidth >= panelLeft && nameX <= panelLeft + 200;
                
                if (!nameOverlapsInfo) {
                    // For current town, also clamp the name position to stay within map bounds
                    if (isCurrentTown) {
                        int mapLeft = mapCenterX - mapWidth / 2;
                        int mapTop = mapCenterY - mapHeight / 2;
                        int mapRight = mapLeft + mapWidth;
                        int mapBottom = mapTop + mapHeight;
                        
                        // Clamp name position to stay within map bounds
                        nameX = Math.max(mapLeft, Math.min(mapRight - textWidth, nameX));
                        nameY = Math.max(mapTop, Math.min(mapBottom - 12, nameY));
                    }
                    
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
                int mapLeft = mapCenterX - mapWidth / 2;
                int mapTop = mapCenterY - mapHeight / 2;
                int mapRight = mapLeft + mapWidth;
                int mapBottom = mapTop + mapHeight;
                
                ringLeft = Math.max(mapLeft, ringLeft);
                ringRight = Math.min(mapRight, ringRight);
                ringTop = Math.max(mapTop, ringTop);
                ringBottom = Math.min(mapBottom, ringBottom);
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
     * Draw current position marker
     */
    private void drawCurrentPosition(GuiGraphics guiGraphics) {
        if (currentTownPos != null) {
            int screenX = worldToScreenX(currentTownPos.getX());
            int screenY = worldToScreenZ(currentTownPos.getZ());
            
            // Get map bounds
            int mapLeft = mapCenterX - mapWidth / 2;
            int mapTop = mapCenterY - mapHeight / 2;
            int mapRight = mapLeft + mapWidth;
            int mapBottom = mapTop + mapHeight;
            
            // ALWAYS ensure current town marker stays within map bounds
            screenX = Math.max(mapLeft + 4, Math.min(mapRight - 4, screenX));
            screenY = Math.max(mapTop + 4, Math.min(mapBottom - 4, screenY));
            
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
     * Draw information about selected town
     */
    private void drawTownInfo(GuiGraphics guiGraphics) {
        if (selectedTown == null || currentTownPos == null) return;
        
        // Calculate distance and get coordinates
        BlockPos selectedPos = selectedTown.position;
        double distance = Math.sqrt(currentTownPos.distSqr(selectedPos));
        
        // Draw info background
        int infoX = panelLeft + 10;
        int infoY = panelTop + 50;
        int infoWidth = 200;
        int infoHeight = 60;
        
        guiGraphics.fill(infoX, infoY, infoX + infoWidth, infoY + infoHeight, INFO_BACKGROUND);
        guiGraphics.hLine(infoX, infoX + infoWidth - 1, infoY, BORDER_COLOR);
        guiGraphics.hLine(infoX, infoX + infoWidth - 1, infoY + infoHeight - 1, BORDER_COLOR);
        guiGraphics.vLine(infoX, infoY, infoY + infoHeight - 1, BORDER_COLOR);
        guiGraphics.vLine(infoX + infoWidth - 1, infoY, infoY + infoHeight - 1, BORDER_COLOR);
        
        // Draw text with coordinates instead of direction
        guiGraphics.drawString(this.font, "Selected: " + selectedTown.name, infoX + 5, infoY + 8, TEXT_COLOR);
        guiGraphics.drawString(this.font, String.format("Distance: %.1f blocks", distance), infoX + 5, infoY + 20, TEXT_COLOR);
        guiGraphics.drawString(this.font, String.format("Position: X %d, Z %d", selectedPos.getX(), selectedPos.getZ()), infoX + 5, infoY + 32, TEXT_COLOR);
        guiGraphics.drawString(this.font, "Population: " + selectedTown.population, infoX + 5, infoY + 44, TEXT_COLOR);
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
            // Check if clicking on a town
            TownMapDataResponsePacket.TownMapInfo clickedTown = getTownAtPosition((int)mouseX, (int)mouseY);
            if (clickedTown != null) {
                selectedTown = clickedTown;
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Selected town: {}", clickedTown.name);
                return true;
            }
            
            // Start dragging if clicking on map area
            if (isInMapArea((int)mouseX, (int)mouseY)) {
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
            // At max zoom (30x), the map should show roughly 10 blocks wide
            double zoomFactor = delta > 0 ? 1.3 : (1.0 / 1.3);
            zoomLevel = Math.max(0.05, Math.min(30.0, zoomLevel * zoomFactor));
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Map zoom level: {}", zoomLevel);
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    /**
     * Check if coordinates are within the map area
     */
    private boolean isInMapArea(int x, int y) {
        int mapLeft = mapCenterX - mapWidth / 2;
        int mapTop = mapCenterY - mapHeight / 2;
        return x >= mapLeft && x <= mapLeft + mapWidth && y >= mapTop && y <= mapTop + mapHeight;
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
                        int mapLeft = mapCenterX - mapWidth / 2;
                        int mapTop = mapCenterY - mapHeight / 2;
                        int mapRight = mapLeft + mapWidth;
                        int mapBottom = mapTop + mapHeight;
                        int markerSize = getAdaptiveMarkerSize();
                        
                        townScreenX = Math.max(mapLeft + markerSize/2, Math.min(mapRight - markerSize/2, townScreenX));
                        townScreenY = Math.max(mapTop + markerSize/2, Math.min(mapBottom - markerSize/2, townScreenY));
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
     * Handle screen close
     */
    @Override
    public void onClose() {
        // Execute close callback BEFORE switching screens
        if (onCloseCallback != null) {
            onCloseCallback.accept(this);
        }
        
        // Return to parent screen
        this.minecraft.setScreen(parentScreen);
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "TownMapModal closed");
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
     * This would be called after the modal is created but before it's shown
     */
    public TownMapModal withTownData(Map<UUID, TownMapDataResponsePacket.TownMapInfo> townData) {
        this.allTowns = townData != null ? new java.util.HashMap<>(townData) : new java.util.HashMap<>();
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Town data set: {} towns", this.allTowns.size());
        return this;
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
}