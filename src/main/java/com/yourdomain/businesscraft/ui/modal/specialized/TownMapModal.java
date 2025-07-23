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
    private static final int INFO_BACKGROUND = 0xE0333333;
    
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
                    "Requested fresh town data from server");
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
     * Check if a town is within the current view
     */
    private boolean isTownVisible(TownMapDataResponsePacket.TownMapInfo town) {
        int screenX = worldToScreenX(town.position.getX());
        int screenY = worldToScreenZ(town.position.getZ());
        
        return screenX >= panelLeft + 20 && screenX <= panelLeft + panelWidth - 20 &&
               screenY >= panelTop + 70 && screenY <= panelTop + panelHeight - 50;
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
     * Draw grid lines for reference
     */
    private void drawGridLines(GuiGraphics guiGraphics) {
        int mapLeft = mapCenterX - mapWidth / 2;
        int mapTop = mapCenterY - mapHeight / 2;
        int gridColor = 0x40AAAAAA;
        
        // Draw vertical lines every 50 world coordinates
        for (int worldX = (int)(mapOffsetX - 500); worldX < mapOffsetX + 500; worldX += 100) {
            int screenX = worldToScreenX(worldX);
            if (screenX >= mapLeft && screenX <= mapLeft + mapWidth) {
                guiGraphics.vLine(screenX, mapTop, mapTop + mapHeight, gridColor);
            }
        }
        
        // Draw horizontal lines every 50 world coordinates
        for (int worldZ = (int)(mapOffsetZ - 500); worldZ < mapOffsetZ + 500; worldZ += 100) {
            int screenY = worldToScreenZ(worldZ);
            if (screenY >= mapTop && screenY <= mapTop + mapHeight) {
                guiGraphics.hLine(mapLeft, mapLeft + mapWidth, screenY, gridColor);
            }
        }
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
        int screenX = worldToScreenX(pos.getX());
        int screenY = worldToScreenZ(pos.getZ());
        
        // Determine marker color
        int markerColor = TOWN_MARKER_COLOR;
        if (town == selectedTown) {
            markerColor = SELECTED_TOWN_COLOR;
        } else if (currentTownPos != null && pos.equals(currentTownPos)) {
            markerColor = CURRENT_TOWN_COLOR;
        }
        
        // Draw marker (small filled rectangle)
        int markerSize = 6;
        guiGraphics.fill(screenX - markerSize/2, screenY - markerSize/2, 
                        screenX + markerSize/2, screenY + markerSize/2, markerColor);
        
        // Draw town name above marker
        String townName = town.name;
        int textWidth = this.font.width(townName);
        guiGraphics.drawString(this.font, townName, 
                              screenX - textWidth/2, screenY - markerSize/2 - 12, TEXT_COLOR);
        
        // Highlight if mouse is over
        if (mouseX >= screenX - markerSize && mouseX <= screenX + markerSize &&
            mouseY >= screenY - markerSize && mouseY <= screenY + markerSize) {
            // Draw highlight ring
            guiGraphics.hLine(screenX - markerSize - 1, screenX + markerSize + 1, screenY - markerSize - 1, 0xFFFFFFFF);
            guiGraphics.hLine(screenX - markerSize - 1, screenX + markerSize + 1, screenY + markerSize + 1, 0xFFFFFFFF);
            guiGraphics.vLine(screenX - markerSize - 1, screenY - markerSize - 1, screenY + markerSize + 1, 0xFFFFFFFF);
            guiGraphics.vLine(screenX + markerSize + 1, screenY - markerSize - 1, screenY + markerSize + 1, 0xFFFFFFFF);
        }
    }
    
    /**
     * Draw current position marker
     */
    private void drawCurrentPosition(GuiGraphics guiGraphics) {
        if (currentTownPos != null) {
            int screenX = worldToScreenX(currentTownPos.getX());
            int screenY = worldToScreenZ(currentTownPos.getZ());
            
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
        
        // Calculate distance and direction
        BlockPos selectedPos = selectedTown.position;
        double distance = Math.sqrt(currentTownPos.distSqr(selectedPos));
        String direction = getDirection(currentTownPos, selectedPos);
        
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
        
        // Draw text
        guiGraphics.drawString(this.font, "Selected: " + selectedTown.name, infoX + 5, infoY + 8, TEXT_COLOR);
        guiGraphics.drawString(this.font, String.format("Distance: %.1f blocks", distance), infoX + 5, infoY + 20, TEXT_COLOR);
        guiGraphics.drawString(this.font, "Direction: " + direction, infoX + 5, infoY + 32, TEXT_COLOR);
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
            // Zoom in/out
            double zoomFactor = delta > 0 ? 1.2 : 0.8;
            zoomLevel = Math.max(0.1, Math.min(5.0, zoomLevel * zoomFactor));
            
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
                int townScreenX = worldToScreenX(pos.getX());
                int townScreenY = worldToScreenZ(pos.getZ());
                
                int markerSize = 6;
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