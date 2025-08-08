package com.quackers29.businesscraft.ui.modal.specialized;

import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.network.packets.ui.RequestTownMapDataPacket;
// TODO: Migrate RequestTownPlatformDataPacket to common module
// import com.quackers29.businesscraft.network.packets.ui.RequestTownPlatformDataPacket;
// TODO: Migrate ClientTownMapCache to common module
// import com.quackers29.businesscraft.network.packets.ui.ClientTownMapCache;
import com.quackers29.businesscraft.network.packets.ui.TownMapDataResponsePacket;
// TODO: Migrate TownPlatformDataResponsePacket to common module
// import com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket;
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
 * 
 * Enhanced MultiLoader Template: Uses migrated packets from common module.
 * Packets available: RequestTownMapDataPacket, TownMapDataResponsePacket
 */
public class TownMapModal extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownMapModal.class);
    
    private final Screen parentScreen;
    private final BlockPos townPos;
    private final Consumer<TownMapModal> onClose;
    private Map<String, String> townMapData;
    private int currentZoomLevel = 1;
    private boolean isDataLoaded = false;
    
    public TownMapModal(Screen parentScreen, BlockPos townPos, Consumer<TownMapModal> onClose) {
        super(Component.literal("Town Map View"));
        this.parentScreen = parentScreen;
        this.townPos = townPos;
        this.onClose = onClose;
        this.townMapData = new HashMap<>();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Add close button
        this.addRenderableWidget(Button.builder(Component.literal("Close"), button -> {
            this.onClose();
        }).bounds(this.width - 80, 10, 70, 20).build());
        
        // Add zoom buttons
        this.addRenderableWidget(Button.builder(Component.literal("Zoom In"), button -> {
            if (currentZoomLevel < 5) {
                currentZoomLevel++;
                requestMapData();
            }
        }).bounds(10, this.height - 60, 60, 20).build());
        
        this.addRenderableWidget(Button.builder(Component.literal("Zoom Out"), button -> {
            if (currentZoomLevel > 1) {
                currentZoomLevel--;
                requestMapData();
            }
        }).bounds(80, this.height - 60, 60, 20).build());
        
        // Request initial map data
        requestMapData();
    }
    
    private void requestMapData() {
        LOGGER.debug("Requesting town map data for position: {} with zoom level: {}", townPos, currentZoomLevel);
        
        // Send packet to request map data
        RequestTownMapDataPacket packet = new RequestTownMapDataPacket(
            townPos.getX(), townPos.getY(), townPos.getZ(), currentZoomLevel, true);
        ModMessages.sendToServer(packet);
        
        isDataLoaded = false;
    }
    
    public void updateMapData(String mapData, int zoomLevel) {
        LOGGER.debug("Received map data for zoom level: {}", zoomLevel);
        // Simple implementation - store the data
        this.townMapData.put("zoom_" + zoomLevel, mapData);
        if (zoomLevel == this.currentZoomLevel) {
            this.isDataLoaded = true;
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render background
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
        
        // Render title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        
        // Render map area
        int mapX = 50;
        int mapY = 50;
        int mapWidth = this.width - 100;
        int mapHeight = this.height - 130;
        
        // Map background
        guiGraphics.fill(mapX, mapY, mapX + mapWidth, mapY + mapHeight, 0xFF333333);
        
        // Render map content
        if (isDataLoaded) {
            String currentMapData = townMapData.get("zoom_" + currentZoomLevel);
            if (currentMapData != null && !currentMapData.equals("{}")) {
                // Render actual map data (simplified implementation)
                guiGraphics.drawCenteredString(this.font, "Map Data Loaded (Zoom: " + currentZoomLevel + ")", 
                    mapX + mapWidth / 2, mapY + mapHeight / 2, 0xFFFFFF);
            } else {
                guiGraphics.drawCenteredString(this.font, "No map data available", 
                    mapX + mapWidth / 2, mapY + mapHeight / 2, 0xFF8888);
            }
        } else {
            guiGraphics.drawCenteredString(this.font, "Loading map data...", 
                mapX + mapWidth / 2, mapY + mapHeight / 2, 0xFFFF88);
        }
        
        // Render zoom level indicator
        guiGraphics.drawString(this.font, "Zoom: " + currentZoomLevel, 10, this.height - 40, 0xFFFFFF);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
        if (onClose != null) {
            onClose.accept(this);
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}