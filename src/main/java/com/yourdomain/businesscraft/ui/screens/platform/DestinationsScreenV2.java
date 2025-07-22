package com.yourdomain.businesscraft.ui.screens.platform;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.packets.platform.SetPlatformDestinationPacket;
import com.yourdomain.businesscraft.ui.builders.UIGridBuilder;
import com.yourdomain.businesscraft.ui.util.InventoryRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Redesigned destinations screen using BC UI framework patterns
 * Following Payment Board UI implementation style
 */
public class DestinationsScreenV2 extends Screen {
    // Data
    private final BlockPos blockPos;
    private final UUID platformId;
    private final String platformName;
    private final Map<UUID, String> townNames;
    private final Map<UUID, Boolean> enabledState;
    private final Map<UUID, Integer> townDistances;
    private final Map<UUID, String> townDirections;
    
    // Layout constants matching Payment Board style
    private static final int SECTION_PADDING = 8;
    private static final int INNER_PADDING = 6;
    private static final int ELEMENT_SPACING = 20;
    
    // Header section
    private static final int HEADER_HEIGHT = 35;
    private static final int BACK_BUTTON_X = SECTION_PADDING;
    private static final int BACK_BUTTON_Y = SECTION_PADDING;
    private static final int BACK_BUTTON_WIDTH = 28;
    private static final int BACK_BUTTON_HEIGHT = 20;
    
    // Destinations list section
    private static final int DESTINATIONS_LIST_X = SECTION_PADDING + 12;
    private static final int DESTINATIONS_LIST_Y = HEADER_HEIGHT + ELEMENT_SPACING;
    private static final int DESTINATIONS_LIST_WIDTH = 280;
    private static final int DESTINATIONS_LIST_HEIGHT = 120;
    
    // UI Components
    private UIGridBuilder destinationsGrid;
    private List<DestinationEntry> currentDestinations = new ArrayList<>();
    
    // Screen dimensions
    private int imageWidth = 320;
    private int imageHeight = 200;
    
    // Enhanced color scheme matching Payment Board
    private static final int SUCCESS_COLOR = 0xB0228B22; // Green for enabled
    private static final int DANGER_COLOR = 0xB0CC2222;  // Red for disabled
    private static final int TEXT_COLOR = 0xFFFFFFFF;    // White text
    private static final int HEADER_COLOR = 0xFFFCB821;  // Gold for headers
    private static final int SECTION_BG_COLOR = 0x90000000; // Semi-transparent black
    private static final int MUTED_COLOR = 0xFFA0A0A0;   // Gray for distance/direction
    
    // Destination entry data class
    private static class DestinationEntry {
        final UUID townId;
        final String townName;
        boolean enabled;
        final int distance;
        final String direction;
        
        DestinationEntry(UUID townId, String townName, boolean enabled, int distance, String direction) {
            this.townId = townId;
            this.townName = townName;
            this.enabled = enabled;
            this.distance = distance;
            this.direction = direction;
        }
    }
    
    public DestinationsScreenV2(BlockPos blockPos, UUID platformId, String platformName,
                               Map<UUID, String> townNames, Map<UUID, Boolean> enabledState,
                               Map<UUID, Integer> townDistances, Map<UUID, String> townDirections) {
        super(Component.translatable("screen.businesscraft.destinations"));
        this.blockPos = blockPos;
        this.platformId = platformId;
        this.platformName = platformName;
        this.townNames = townNames;
        this.enabledState = enabledState;
        this.townDistances = townDistances != null ? townDistances : new HashMap<>();
        this.townDirections = townDirections != null ? townDirections : new HashMap<>();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Prepare destination entries
        prepareDestinationEntries();
        
        // Initialize destinations grid
        initializeDestinationsGrid();
    }
    
    private void prepareDestinationEntries() {
        currentDestinations.clear();
        
        // Create sorted list of destinations by distance
        List<Map.Entry<UUID, String>> sortedTowns = new ArrayList<>(townNames.entrySet());
        sortedTowns.sort((a, b) -> {
            int distA = townDistances.getOrDefault(a.getKey(), 999);
            int distB = townDistances.getOrDefault(b.getKey(), 999);
            return Integer.compare(distA, distB);
        });
        
        // Convert to destination entries
        for (Map.Entry<UUID, String> townEntry : sortedTowns) {
            UUID townId = townEntry.getKey();
            String townName = townEntry.getValue();
            boolean enabled = enabledState.getOrDefault(townId, false);
            int distance = townDistances.getOrDefault(townId, 0);
            String direction = townDirections.getOrDefault(townId, "");
            
            currentDestinations.add(new DestinationEntry(townId, townName, enabled, distance, direction));
        }
    }
    
    private void initializeDestinationsGrid() {
        // Create grid similar to Payment Board pattern
        destinationsGrid = UIGridBuilder.create(
            DESTINATIONS_LIST_X, DESTINATIONS_LIST_Y + 5,
            DESTINATIONS_LIST_WIDTH - 8, DESTINATIONS_LIST_HEIGHT - 10,
            4) // 4 columns: Town Name, Direction, Distance, Toggle
            .withRowHeight(18)
            .withSpacing(6, 3)
            .withMargins(8, 4)
            .drawBackground(false)
            .drawBorder(false);
        
        // Set up scrolling if we have more than 5 destinations
        if (currentDestinations.size() > 5) {
            destinationsGrid.withVerticalScroll(true, 5);
            destinationsGrid.updateTotalRows(currentDestinations.size());
        }
        
        // Populate with data
        populateGridWithDestinations();
    }
    
    private void populateGridWithDestinations() {
        for (int i = 0; i < currentDestinations.size(); i++) {
            DestinationEntry destination = currentDestinations.get(i);
            
            // Column 0: Town name (with tooltip showing full info)
            String townDisplayName = destination.townName;
            if (townDisplayName.length() > 12) {
                townDisplayName = townDisplayName.substring(0, 9) + "...";
            }
            String fullTooltip = destination.townName + "\n" + 
                destination.direction + " " + destination.distance + "m\n" +
                "Click to " + (destination.enabled ? "disable" : "enable");
            
            destinationsGrid.addLabelWithTooltip(i, 0, townDisplayName, fullTooltip, TEXT_COLOR);
            
            // Column 1: Direction
            destinationsGrid.addLabel(i, 1, destination.direction, MUTED_COLOR);
            
            // Column 2: Distance
            String distanceText = destination.distance + "m";
            destinationsGrid.addLabel(i, 2, distanceText, MUTED_COLOR);
            
            // Column 3: Toggle button
            String toggleText = destination.enabled ? "ON" : "OFF";
            int toggleColor = destination.enabled ? SUCCESS_COLOR : DANGER_COLOR;
            String toggleTooltip = destination.enabled ? 
                "Click to disable this destination" : "Click to enable this destination";
                
            destinationsGrid.addButtonWithTooltip(i, 3, toggleText, toggleTooltip,
                (v) -> toggleDestination(destination), toggleColor);
        }
        
        // If no destinations, show message
        if (currentDestinations.isEmpty()) {
            destinationsGrid.addLabel(0, 1, "No destinations available", MUTED_COLOR);
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render background
        this.renderBackground(guiGraphics);
        
        // Calculate screen position
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        
        // Render background sections
        renderBackground(guiGraphics, x, y);
        
        // Render header
        renderHeader(guiGraphics, x, y, mouseX, mouseY);
        
        // Render destinations list section
        renderDestinationsListSection(guiGraphics, x, y);
        
        // Render destinations grid
        if (destinationsGrid != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            destinationsGrid.render(guiGraphics, mouseX - x, mouseY - y);
            guiGraphics.pose().popPose();
        }
        
        // Render back button tooltip
        if (isMouseOverBackButton(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Return to Platform Management"), mouseX, mouseY);
        }
    }
    
    private void renderBackground(GuiGraphics guiGraphics, int x, int y) {
        // Main background with border (Payment Board style)
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, InventoryRenderer.BACKGROUND_COLOR);
        InventoryRenderer.drawBorder(guiGraphics, x, y, imageWidth, imageHeight, InventoryRenderer.BORDER_COLOR, 2);
        guiGraphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, InventoryRenderer.BACKGROUND_COLOR);
    }
    
    private void renderHeader(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        // Header background
        guiGraphics.fill(x + 4, y + 4, x + imageWidth - 4, y + HEADER_HEIGHT, SECTION_BG_COLOR);
        
        // Back button
        boolean isBackButtonHovered = isMouseOverBackButton(mouseX, mouseY);
        InventoryRenderer.drawButton(guiGraphics, 
            x + BACK_BUTTON_X, y + BACK_BUTTON_Y,
            BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT,
            "◀", this.font, isBackButtonHovered);
        
        // Title and subtitle
        guiGraphics.drawString(this.font, "Platform Destinations", 
            x + BACK_BUTTON_X + BACK_BUTTON_WIDTH + 6, y + SECTION_PADDING + 4, HEADER_COLOR);
    }
    
    private void renderDestinationsListSection(GuiGraphics guiGraphics, int x, int y) {
        // Destinations list section background
        int padding = INNER_PADDING;
        int sectionX = x + DESTINATIONS_LIST_X - padding;
        int sectionY = y + DESTINATIONS_LIST_Y - padding - 10;
        int sectionWidth = DESTINATIONS_LIST_WIDTH + (padding * 2);
        int sectionHeight = DESTINATIONS_LIST_HEIGHT + (padding * 2) + 10;
        
        guiGraphics.fill(sectionX, sectionY, sectionX + sectionWidth, sectionY + sectionHeight, SECTION_BG_COLOR);
        InventoryRenderer.drawBorder(guiGraphics, sectionX, sectionY, sectionWidth, sectionHeight,
            InventoryRenderer.INVENTORY_BORDER_COLOR, 2);
        
        // Section header
        String headerText = platformName != null ? "Platform: " + platformName : "Platform Destinations";
        if (headerText.length() > 25) {
            headerText = headerText.substring(0, 22) + "...";
        }
        guiGraphics.drawString(this.font, headerText,
            x + DESTINATIONS_LIST_X + 6, y + DESTINATIONS_LIST_Y - 12, HEADER_COLOR);
        
        // Sub-header with enabled count
        int enabledCount = (int) currentDestinations.stream().mapToInt(d -> d.enabled ? 1 : 0).sum();
        String subHeader = enabledCount == 0 ? "Any destination allowed" : 
            enabledCount + "/" + currentDestinations.size() + " destinations enabled";
        guiGraphics.drawString(this.font, subHeader,
            x + DESTINATIONS_LIST_X + 6, y + DESTINATIONS_LIST_Y - 2, MUTED_COLOR);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Calculate screen position
            int x = (this.width - imageWidth) / 2;
            int y = (this.height - imageHeight) / 2;
            
            // Check back button
            if (isMouseOverBackButton((int)mouseX, (int)mouseY)) {
                onBackButtonClick();
                return true;
            }
            
            // Handle destinations grid clicks
            if (destinationsGrid != null) {
                boolean handled = destinationsGrid.mouseClicked(
                    (int)mouseX - x, (int)mouseY - y, button);
                if (handled) return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Forward scroll events to the grid
        if (destinationsGrid != null) {
            int x = (this.width - imageWidth) / 2;
            int y = (this.height - imageHeight) / 2;
            
            return destinationsGrid.mouseScrolled(mouseX - x, mouseY - y, delta);
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    private boolean isMouseOverBackButton(int mouseX, int mouseY) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        
        return InventoryRenderer.isMouseOverElement(mouseX, mouseY, x, y,
            BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
    }
    
    private void toggleDestination(DestinationEntry destination) {
        // Update local state
        destination.enabled = !destination.enabled;
        
        // Send packet to server
        ModMessages.sendToServer(new SetPlatformDestinationPacket(
            blockPos, platformId, destination.townId, destination.enabled));
        
        // Update the grid to reflect changes
        destinationsGrid.clearElements();
        populateGridWithDestinations();
    }
    
    private void onBackButtonClick() {
        // Play click sound
        net.minecraft.client.resources.sounds.SimpleSoundInstance sound = 
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F);
        this.minecraft.getSoundManager().play(sound);
        
        // Close this screen - it will return to platform management
        this.minecraft.setScreen(null);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }
}