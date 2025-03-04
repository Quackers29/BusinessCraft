package com.yourdomain.businesscraft.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.SetPlatformDestinationPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Random;

/**
 * Screen for selecting destination towns for a platform
 */
public class DestinationsScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation BACKGROUND = new ResourceLocation(BusinessCraft.MOD_ID, "textures/gui/destinations_background.png");
    
    // UI constants
    private static final int BACKGROUND_WIDTH = 250;
    private static final int BACKGROUND_HEIGHT = 200;
    private static final int ENTRY_HEIGHT = 24;
    private static final int MAX_VISIBLE_ENTRIES = 4;
    
    // Colors
    private static final int COLOR_TITLE = 0xFFEEEEFF;
    private static final int COLOR_SUBTITLE = 0xFFDDDDFF;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_ENABLED = 0xFF4CAF50; // Green
    private static final int COLOR_DISABLED = 0xFFE57373; // Light red
    private static final int COLOR_ENTRY_BG = 0x40404060;
    private static final int COLOR_ENTRY_BG_HOVER = 0x60505070;
    
    private final BlockPos blockPos;
    private final UUID platformId;
    private final String platformName;
    private final Map<UUID, String> townNames;
    private final Map<UUID, Boolean> enabledState;
    private final Map<UUID, Integer> townDistances = new HashMap<>(); // Store distances
    private final Map<UUID, String> townDirections = new HashMap<>(); // Store directions
    
    private int leftPos;
    private int topPos;
    private int scrollOffset = 0;
    private Button closeButton;
    private Button scrollUpButton;
    private Button scrollDownButton;
    private final List<DestinationEntry> entries = new ArrayList<>();
    private int hoveredEntry = -1;
    private boolean hasScrollButtons = false;
    
    public DestinationsScreen(BlockPos blockPos, UUID platformId, String platformName, 
                              Map<UUID, String> townNames, Map<UUID, Boolean> enabledState) {
        super(Component.translatable("screen.businesscraft.destinations"));
        this.blockPos = blockPos;
        this.platformId = platformId;
        this.platformName = platformName;
        this.townNames = townNames;
        this.enabledState = enabledState;
    }
    
    public DestinationsScreen(BlockPos blockPos, UUID platformId, String platformName, 
                          Map<UUID, String> townNames, Map<UUID, Boolean> enabledState,
                          Map<UUID, Integer> townDistances) {
        super(Component.translatable("screen.businesscraft.destinations"));
        this.blockPos = blockPos;
        this.platformId = platformId;
        this.platformName = platformName;
        this.townNames = townNames;
        this.enabledState = enabledState;
        this.townDistances.putAll(townDistances); // Copy the provided distances
    }
    
    public DestinationsScreen(BlockPos blockPos, UUID platformId, String platformName, 
                          Map<UUID, String> townNames, Map<UUID, Boolean> enabledState,
                          Map<UUID, Integer> townDistances, Map<UUID, String> townDirections) {
        super(Component.translatable("screen.businesscraft.destinations"));
        this.blockPos = blockPos;
        this.platformId = platformId;
        this.platformName = platformName;
        this.townNames = townNames;
        this.enabledState = enabledState;
        this.townDistances.putAll(townDistances); // Copy the provided distances
        this.townDirections.putAll(townDirections); // Copy the provided directions
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.leftPos = (this.width - BACKGROUND_WIDTH) / 2;
        this.topPos = (this.height - BACKGROUND_HEIGHT) / 2;
        
        int contentWidth = BACKGROUND_WIDTH - 30; // Slightly wider content area
        int rightPanelX = leftPos + contentWidth - 10; // Move buttons more to the left
        
        // Close button at the bottom
        closeButton = Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(leftPos + BACKGROUND_WIDTH - 80, topPos + BACKGROUND_HEIGHT - 30, 60, 20)
                .build();
        this.addRenderableWidget(closeButton);
        
        // Create entries from town data
        createEntries();
        
        // Add scroll buttons if needed
        hasScrollButtons = entries.size() > MAX_VISIBLE_ENTRIES;
        if (hasScrollButtons) {
            scrollUpButton = Button.builder(Component.translatable("businesscraft.scroll_up"), button -> scroll(-1))
                    .bounds(rightPanelX, topPos + 60, 40, 20) // Smaller width (40 instead of 50)
                    .build();
            this.addRenderableWidget(scrollUpButton);
            
            // Move down button up a bit - adjust the position
            int listBottomY = topPos + 60 + (MAX_VISIBLE_ENTRIES * ENTRY_HEIGHT) - 15; // Moved up by 11 pixels
            scrollDownButton = Button.builder(Component.translatable("businesscraft.scroll_down"), button -> scroll(1))
                    .bounds(rightPanelX, listBottomY, 40, 20) // Positioned at bottom of list but higher
                    .build();
            this.addRenderableWidget(scrollDownButton);
        }
        
        // Update the positions of all entries
        updateEntryPositions();
    }
    
    private void createEntries() {
        entries.clear();
        
        // Create a destination entry for each town - sorted by distance
        List<Map.Entry<UUID, String>> sortedTowns = new ArrayList<>(townNames.entrySet());
        sortedTowns.sort((a, b) -> {
            int distA = townDistances.getOrDefault(a.getKey(), 100);
            int distB = townDistances.getOrDefault(b.getKey(), 100);
            return Integer.compare(distA, distB);
        });
        
        int index = 0;
        int entryWidth = BACKGROUND_WIDTH - 70; // Slightly wider entries
        int entryX = leftPos + 20; // Move entries to the left
        
        for (Map.Entry<UUID, String> townEntry : sortedTowns) {
            UUID townId = townEntry.getKey();
            String townName = townEntry.getValue();
            boolean enabled = enabledState.getOrDefault(townId, false);
            int distance = townDistances.getOrDefault(townId, 100);
            String direction = townDirections.getOrDefault(townId, "");
            
            DestinationEntry entry = new DestinationEntry(
                townId, townName, enabled, distance, direction,
                entryX, topPos + 60 + (index * ENTRY_HEIGHT)
            );
            entries.add(entry);
            index++;
        }
    }
    
    private void updateEntryPositions() {
        // Update entry visibility and positions based on scroll offset
        int visibleIndex = 0;
        for (int i = 0; i < entries.size(); i++) {
            DestinationEntry entry = entries.get(i);
            
            if (i >= scrollOffset && i < scrollOffset + MAX_VISIBLE_ENTRIES) {
                entry.setVisible(true);
                entry.setY(topPos + 60 + (visibleIndex * ENTRY_HEIGHT));
                visibleIndex++;
            } else {
                entry.setVisible(false);
            }
        }
        
        // Update scroll button states
        if (hasScrollButtons) {
            scrollUpButton.active = scrollOffset > 0;
            scrollDownButton.active = scrollOffset < entries.size() - MAX_VISIBLE_ENTRIES;
        }
    }
    
    private void scroll(int direction) {
        int newOffset = scrollOffset + direction;
        if (newOffset >= 0 && newOffset <= Math.max(0, entries.size() - MAX_VISIBLE_ENTRIES)) {
            scrollOffset = newOffset;
            updateEntryPositions();
        }
    }
    
    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        renderBackgroundPanel(gui);
        
        // Render title and subtitle
        gui.drawString(font, Component.translatable("screen.businesscraft.destinations"), leftPos + 20, topPos + 20, COLOR_TITLE);
        gui.drawString(font, Component.translatable("businesscraft.platform_subtitle", platformName), 
                leftPos + 20, topPos + 36, COLOR_SUBTITLE);
        
        // Render information text
        if (entries.isEmpty()) {
            gui.drawString(font, Component.translatable("businesscraft.no_destinations"), 
                    leftPos + 20, topPos + 70, COLOR_TEXT);
        }
        
        // Find hovered entry
        hoveredEntry = -1;
        for (int i = 0; i < entries.size(); i++) {
            DestinationEntry entry = entries.get(i);
            if (entry.isVisible() && mouseX >= entry.getX() && mouseX < entry.getX() + BACKGROUND_WIDTH - 70 
                    && mouseY >= entry.getY() && mouseY < entry.getY() + ENTRY_HEIGHT) {
                hoveredEntry = i;
                break;
            }
        }
        
        // Render entries
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).render(gui, mouseX, mouseY, i == hoveredEntry);
        }
        
        super.render(gui, mouseX, mouseY, partialTick);
    }
    
    private void renderBackgroundPanel(GuiGraphics gui) {
        // Use a custom texture or just draw a filled rectangle
        gui.fill(this.leftPos, this.topPos, this.leftPos + BACKGROUND_WIDTH, 
                 this.topPos + BACKGROUND_HEIGHT, 0x80202040);
        
        // Draw border
        gui.hLine(this.leftPos, this.leftPos + BACKGROUND_WIDTH - 1, this.topPos, 0xFF6060A0);
        gui.hLine(this.leftPos, this.leftPos + BACKGROUND_WIDTH - 1, this.topPos + BACKGROUND_HEIGHT - 1, 0xFF6060A0);
        gui.vLine(this.leftPos, this.topPos, this.topPos + BACKGROUND_HEIGHT - 1, 0xFF6060A0);
        gui.vLine(this.leftPos + BACKGROUND_WIDTH - 1, this.topPos, this.topPos + BACKGROUND_HEIGHT - 1, 0xFF6060A0);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredEntry >= 0 && hoveredEntry < entries.size()) {
            DestinationEntry entry = entries.get(hoveredEntry);
            if (entry.isVisible()) {
                entry.toggleEnabled();
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // Return false to prevent the game from pausing
    }
    
    private class DestinationEntry {
        private final UUID townId;
        private final String townName;
        private boolean enabled;
        private int distance; // Distance in meters
        private String direction; // Direction (N, NE, E, etc.)
        private int x;
        private int y;
        private boolean visible = true;
        
        public DestinationEntry(UUID townId, String townName, boolean enabled, int distance, String direction, int x, int y) {
            this.townId = townId;
            this.townName = townName;
            this.enabled = enabled;
            this.distance = distance;
            this.direction = direction;
            this.x = x;
            this.y = y;
        }
        
        public void render(GuiGraphics gui, int mouseX, int mouseY, boolean isHovered) {
            if (!visible) return;
            
            // Draw background
            int bgColor = isHovered ? COLOR_ENTRY_BG_HOVER : COLOR_ENTRY_BG;
            int width = BACKGROUND_WIDTH - 70; // Slightly wider
            gui.fill(x, y, x + width, y + ENTRY_HEIGHT, bgColor);
            
            // Draw town name with distance and direction
            String displayName = townName + " (" + direction + " " + distance + "m)";
            gui.drawString(font, displayName, x + 10, y + 8, COLOR_TEXT);
            
            // Draw enabled/disabled status
            int statusColor = enabled ? COLOR_ENABLED : COLOR_DISABLED;
            String statusText = enabled ? 
                Component.translatable("businesscraft.enabled").getString() : 
                Component.translatable("businesscraft.disabled").getString();
            
            int statusX = x + width - font.width(statusText) - 10;
            gui.drawString(font, statusText, statusX, y + 8, statusColor);
        }
        
        public void toggleEnabled() {
            enabled = !enabled;
            
            // Send packet to server to update the destination
            ModMessages.sendToServer(new SetPlatformDestinationPacket(blockPos, platformId, townId, enabled));
        }
        
        public int getX() {
            return x;
        }
        
        public int getY() {
            return y;
        }
        
        public void setY(int y) {
            this.y = y;
        }
        
        public boolean isVisible() {
            return visible;
        }
        
        public void setVisible(boolean visible) {
            this.visible = visible;
        }
    }
} 