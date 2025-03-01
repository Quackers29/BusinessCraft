package com.yourdomain.businesscraft.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.AddPlatformPacket;
import com.yourdomain.businesscraft.network.DeletePlatformPacket;
import com.yourdomain.businesscraft.network.SetPlatformEnabledPacket;
import com.yourdomain.businesscraft.network.SetPlatformPathPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tab for managing multiple tourist platforms
 */
public class PlatformsTab extends Tab {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation BUTTON_TEXTURES = new ResourceLocation(BusinessCraft.MOD_ID, "textures/gui/town_block_buttons.png");
    private static final ResourceLocation TOGGLE_TEXTURES = new ResourceLocation(BusinessCraft.MOD_ID, "textures/gui/toggle_buttons.png");
    
    // UI constants
    private static final int PLATFORM_ENTRY_HEIGHT = 30;
    private static final int MAX_VISIBLE_PLATFORMS = 5;
    private static final int HEADER_HEIGHT = 30;
    private static final int CONTENT_PADDING = 10;
    
    // Colors
    private static final int COLOR_HEADER_BG = 0x80202040;
    private static final int COLOR_CONTENT_BG = 0x80303050;
    private static final int COLOR_ENTRY_BG = 0x40404060;
    private static final int COLOR_ENTRY_BG_HOVER = 0x60505070;
    private static final int COLOR_TITLE = 0xFFEEEEFF;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_PLATFORM_NAME = 0xFFDDDDFF;
    
    private final TownBlockScreen screen;
    private final List<PlatformEntry> platformEntries = new ArrayList<>();
    
    private Button addPlatformButton;
    private Button returnButton;
    private int scrollOffset = 0;
    private Button scrollUpButton;
    private Button scrollDownButton;
    private int hoveredEntry = -1;
    
    public PlatformsTab(TownBlockScreen screen) {
        super(Component.translatable("businesscraft.platforms_tab"));
        this.screen = screen;
    }
    
    @Override
    public void init(int leftPos, int topPos, int width, int height) {
        super.init(leftPos, topPos, width, height);
        
        platformEntries.clear();
        
        // Return button - styled better
        returnButton = Button.builder(Component.literal("←"), (button) -> screen.showMainTab())
            .bounds(leftPos + CONTENT_PADDING, topPos + CONTENT_PADDING, 20, 20)
            .tooltip(Tooltip.create(Component.translatable("businesscraft.return_to_settings")))
            .build();
        
        // Add platform button - moved to bottom of screen
        addPlatformButton = Button.builder(Component.literal("+"), this::onAddPlatform)
            .bounds(leftPos + width - 40, topPos + height - 30, 30, 20)
            .tooltip(Tooltip.create(Component.translatable("businesscraft.add_platform")))
            .build();
        
        // Scroll buttons - restyled
        scrollUpButton = Button.builder(Component.literal("▲"), b -> scroll(-1))
            .bounds(leftPos + width - 25, topPos + HEADER_HEIGHT + CONTENT_PADDING, 15, 15)
            .build();
            
        scrollDownButton = Button.builder(Component.literal("▼"), b -> scroll(1))
            .bounds(leftPos + width - 25, topPos + height - 45, 15, 15)
            .build();
        
        // Register the buttons with the screen
        screen.addPlatformButton(returnButton);
        screen.addPlatformButton(addPlatformButton);
        screen.addPlatformButton(scrollUpButton);
        screen.addPlatformButton(scrollDownButton);
        
        refreshPlatforms();
    }
    
    private void scroll(int direction) {
        int newOffset = scrollOffset + direction;
        int maxOffset = Math.max(0, platformEntries.size() - MAX_VISIBLE_PLATFORMS);
        
        if (newOffset >= 0 && newOffset <= maxOffset) {
            scrollOffset = newOffset;
            updatePlatformPositions();
        }
    }
    
    private void updatePlatformPositions() {
        int startIndex = scrollOffset;
        int endIndex = Math.min(scrollOffset + MAX_VISIBLE_PLATFORMS, platformEntries.size());
        
        for (int i = 0; i < platformEntries.size(); i++) {
            PlatformEntry entry = platformEntries.get(i);
            if (i >= startIndex && i < endIndex) {
                int relativeIndex = i - startIndex;
                entry.setVisible(true);
                entry.updatePosition(
                    contentLeft + CONTENT_PADDING, 
                    contentTop + HEADER_HEIGHT + CONTENT_PADDING + (relativeIndex * PLATFORM_ENTRY_HEIGHT)
                );
            } else {
                entry.setVisible(false);
            }
        }
    }
    
    public void refreshPlatforms() {
        platformEntries.clear();
        List<Platform> platforms = screen.getPlatforms();
        
        LOGGER.debug("Refreshing platforms tab with {} platforms", platforms.size());
        
        for (Platform platform : platforms) {
            LOGGER.debug("Adding platform '{}' (enabled: {}, id: {})", 
                platform.getName(), platform.isEnabled(), platform.getId());
            platformEntries.add(new PlatformEntry(platform));
        }
        
        scrollOffset = 0;
        updatePlatformPositions();
    }
    
    private void onAddPlatform(Button button) {
        LOGGER.debug("Adding new platform");
        ModMessages.sendToServer(new AddPlatformPacket(screen.getBlockPos()));
    }
    
    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        if (!isVisible()) return;
        
        // Draw stylish background panel
        renderBackgroundPanel(gui);
        
        // Draw header section
        renderHeader(gui);
        
        // Determine visible entries based on scroll
        int maxEntries = Math.min(platformEntries.size(), MAX_VISIBLE_PLATFORMS);
        int endIndex = Math.min(scrollOffset + maxEntries, platformEntries.size());
        
        // Update hovered entry detection
        hoveredEntry = -1;
        for (int i = scrollOffset; i < endIndex; i++) {
            PlatformEntry entry = platformEntries.get(i);
            int entryY = contentTop + HEADER_HEIGHT + CONTENT_PADDING + (i - scrollOffset) * PLATFORM_ENTRY_HEIGHT;
            
            // Check if mouse is hovering over this entry
            if (mouseX >= contentLeft + CONTENT_PADDING && 
                mouseX <= contentLeft + contentWidth - CONTENT_PADDING - 30 &&
                mouseY >= entryY && 
                mouseY <= entryY + PLATFORM_ENTRY_HEIGHT - 5) {
                hoveredEntry = i;
            }
            
            // Position the entry
            entry.updatePosition(contentLeft + CONTENT_PADDING, entryY);
            
            // Render the entry with highlight if hovered
            boolean isHovered = (i == hoveredEntry);
            entry.render(gui, mouseX, mouseY, isHovered);
        }
        
        // Draw scroll indicator if needed
        if (platformEntries.size() > MAX_VISIBLE_PLATFORMS) {
            renderScrollIndicator(gui);
        }
        
        // Render buttons
        returnButton.render(gui, mouseX, mouseY, partialTick);
        addPlatformButton.render(gui, mouseX, mouseY, partialTick);
        
        // Only show scroll buttons if needed
        if (platformEntries.size() > MAX_VISIBLE_PLATFORMS) {
            scrollUpButton.active = scrollOffset > 0;
            scrollDownButton.active = scrollOffset < platformEntries.size() - MAX_VISIBLE_PLATFORMS;
            
            scrollUpButton.render(gui, mouseX, mouseY, partialTick);
            scrollDownButton.render(gui, mouseX, mouseY, partialTick);
        }
    }
    
    /**
     * Renders the main background panel with modern styling
     */
    private void renderBackgroundPanel(GuiGraphics gui) {
        // Main content area with gradient
        int gradientTop = 0x99101030;
        int gradientBottom = 0x99202045;
        gui.fillGradient(
            contentLeft - CONTENT_PADDING, contentTop - CONTENT_PADDING,
            contentLeft + contentWidth + CONTENT_PADDING, contentTop + contentHeight + CONTENT_PADDING,
            gradientTop, gradientBottom
        );
        
        // Add subtle border
        int borderColor = 0x60AAAAAA;
        gui.fill(contentLeft - CONTENT_PADDING, contentTop - CONTENT_PADDING, 
                contentLeft + contentWidth + CONTENT_PADDING, contentTop - CONTENT_PADDING + 1, borderColor);
        gui.fill(contentLeft - CONTENT_PADDING, contentTop - CONTENT_PADDING, 
                contentLeft - CONTENT_PADDING + 1, contentTop + contentHeight + CONTENT_PADDING, borderColor);
        gui.fill(contentLeft + contentWidth + CONTENT_PADDING - 1, contentTop - CONTENT_PADDING, 
                contentLeft + contentWidth + CONTENT_PADDING, contentTop + contentHeight + CONTENT_PADDING, borderColor);
        gui.fill(contentLeft - CONTENT_PADDING, contentTop + contentHeight + CONTENT_PADDING - 1, 
                contentLeft + contentWidth + CONTENT_PADDING, contentTop + contentHeight + CONTENT_PADDING, borderColor);
    }
    
    /**
     * Renders the header section with title and styling
     */
    private void renderHeader(GuiGraphics gui) {
        // Header background
        gui.fill(
            contentLeft, 
            contentTop,
            contentLeft + contentWidth,
            contentTop + HEADER_HEIGHT,
            COLOR_HEADER_BG
        );
        
        // Title text with shadow
        Component title = Component.translatable("businesscraft.platforms_management");
        gui.drawString(
            Minecraft.getInstance().font,
            title,
            contentLeft + 35,
            contentTop + 16,
            COLOR_TITLE,
            true  // with shadow
        );
    }
    
    /**
     * Renders a scroll indicator
     */
    private void renderScrollIndicator(GuiGraphics gui) {
        // Track
        int trackHeight = contentHeight - HEADER_HEIGHT - CONTENT_PADDING * 2 - 50;
        int trackX = contentLeft + contentWidth - 20;
        int trackY = contentTop + HEADER_HEIGHT + CONTENT_PADDING + 20;
        
        // Track background
        gui.fill(trackX, trackY, trackX + 3, trackY + trackHeight, 0x40000000);
        
        // Thumb
        if (platformEntries.size() > 0) {
            float ratio = (float) MAX_VISIBLE_PLATFORMS / platformEntries.size();
            int thumbHeight = Math.max(20, (int)(trackHeight * ratio));
            
            float thumbPosition = 0;
            if (platformEntries.size() > MAX_VISIBLE_PLATFORMS) {
                thumbPosition = (float)scrollOffset / (platformEntries.size() - MAX_VISIBLE_PLATFORMS);
            }
            
            int thumbY = trackY + (int)((trackHeight - thumbHeight) * thumbPosition);
            gui.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xCCAAAAAA);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (returnButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        if (addPlatformButton.visible && addPlatformButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        if (platformEntries.size() > MAX_VISIBLE_PLATFORMS) {
            if (scrollUpButton.mouseClicked(mouseX, mouseY, button) || 
                scrollDownButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        for (PlatformEntry entry : platformEntries) {
            if (entry.isVisible() && entry.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Represents a single platform entry in the list
     */
    private class PlatformEntry {
        private final Platform platform;
        private Button toggleButton;
        private Button setPathButton;
        private Button deleteButton;
        private boolean visible = true;
        
        public PlatformEntry(Platform platform) {
            this.platform = platform;
            
            int baseX = contentLeft + 5;
            int baseY = contentTop + 40;
            
            // Toggle button (on/off) - restyled
            toggleButton = new ImageButton(
                baseX + 140, baseY, 20, 20, 
                0, platform.isEnabled() ? 0 : 20, 20, 
                TOGGLE_TEXTURES, 64, 64, 
                button -> onToggle()
            );
            toggleButton.setTooltip(Tooltip.create(
                Component.translatable(platform.isEnabled() ? 
                    "businesscraft.enabled" : "businesscraft.disabled")
            ));
            
            // Set path button - improved styling
            setPathButton = Button.builder(Component.translatable("businesscraft.set"), this::onSetPath)
                .bounds(baseX + 170, baseY, 30, 20)
                .tooltip(Tooltip.create(Component.translatable("businesscraft.set_platform_path")))
                .build();
            
            // Delete button - improved styling
            deleteButton = Button.builder(Component.translatable("businesscraft.del"), this::onDelete)
                .bounds(baseX + 210, baseY, 30, 20)
                .tooltip(Tooltip.create(Component.translatable("businesscraft.delete_platform")))
                .build();
            
            // Register buttons with the screen
            screen.addPlatformButton(toggleButton);
            screen.addPlatformButton(setPathButton);
            screen.addPlatformButton(deleteButton);
        }
        
        public void updatePosition(int x, int y) {
            toggleButton.setX(x + 140);
            toggleButton.setY(y);
            
            setPathButton.setX(x + 170);
            setPathButton.setY(y);
            
            deleteButton.setX(x + 210);
            deleteButton.setY(y);
        }
        
        public void render(GuiGraphics gui, int mouseX, int mouseY, boolean isHovered) {
            if (!visible) return;
            
            // Entry background with hover effect
            int bgColor = isHovered ? COLOR_ENTRY_BG_HOVER : COLOR_ENTRY_BG;
            
            // Draw rounded entry background
            int entryX = toggleButton.getX() - 140; // Calculate x based on button position
            int entryY = toggleButton.getY();      // Get y from button position
            int entryWidth = contentWidth - CONTENT_PADDING * 2;
            
            gui.fill(
                entryX, entryY, 
                entryX + entryWidth - 30, entryY + PLATFORM_ENTRY_HEIGHT - 5,
                bgColor
            );
            
            // Platform name with enabled/disabled state indication
            int textColor = platform.isEnabled() ? COLOR_PLATFORM_NAME : 0xBBAAAACC;
            Component nameComponent = Component.literal(platform.getName());
            
            // Draw name with position offset from toggle button
            gui.drawString(
                Minecraft.getInstance().font,
                nameComponent,
                entryX + 30,
                entryY + 6,
                textColor
            );
            
            // Show platform status (complete or not)
            String statusText = platform.isComplete() ? "Complete" : "Incomplete";
            int statusTextColor = platform.isComplete() ? 0xBB99FF99 : 0xBBFF9999;
            
            gui.drawString(
                Minecraft.getInstance().font,
                statusText,
                entryX + 30,
                entryY + 17,
                statusTextColor
            );
            
            // Status indicator (enabled/disabled)
            int statusSize = 8;
            int statusX = entryX + 12;
            int statusY = entryY + 10;
            int statusColor = platform.isEnabled() ? 0xFF44FF44 : 0xFFFF4444;
            
            // Draw colored status dot
            gui.fill(statusX, statusY, statusX + statusSize, statusY + statusSize, statusColor);
            
            // Render buttons
            toggleButton.render(gui, mouseX, mouseY, 0);
            deleteButton.render(gui, mouseX, mouseY, 0);
            setPathButton.render(gui, mouseX, mouseY, 0);
        }
        
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!visible) return false;
            
            return toggleButton.mouseClicked(mouseX, mouseY, button) ||
                   setPathButton.mouseClicked(mouseX, mouseY, button) ||
                   deleteButton.mouseClicked(mouseX, mouseY, button);
        }
        
        private void onToggle() {
            LOGGER.debug("Toggling platform {} ({})", platform.getName(), platform.getId());
            
            // Update UI immediately for responsiveness
            platform.setEnabled(!platform.isEnabled());
            toggleButton.setTooltip(Tooltip.create(
                Component.translatable(platform.isEnabled() ? 
                    "businesscraft.enabled" : "businesscraft.disabled")
            ));
            
            // Remove old button from screen
            screen.removePlatformButton(toggleButton);
            
            // Update button texture (using a different approach since setTexture isn't available)
            toggleButton = new ImageButton(
                toggleButton.getX(), toggleButton.getY(), 20, 20,
                0, platform.isEnabled() ? 0 : 20, 20,
                TOGGLE_TEXTURES, 64, 64,
                button -> onToggle()
            );
            toggleButton.setTooltip(Tooltip.create(
                Component.translatable(platform.isEnabled() ? 
                    "businesscraft.enabled" : "businesscraft.disabled")
            ));
            
            // Add new button to screen
            screen.addPlatformButton(toggleButton);
            
            // Send to server
            ModMessages.sendToServer(new SetPlatformEnabledPacket(
                screen.getBlockPos(), 
                platform.getId(), 
                platform.isEnabled()
            ));
        }
        
        private void onSetPath(Button button) {
            LOGGER.debug("Setting path for platform {} ({})", platform.getName(), platform.getId());
            screen.showPathEditor(platform.getId());
        }
        
        private void onDelete(Button button) {
            LOGGER.debug("Deleting platform {} ({})", platform.getName(), platform.getId());
            ModMessages.sendToServer(new DeletePlatformPacket(
                screen.getBlockPos(), 
                platform.getId()
            ));
        }
        
        public boolean isVisible() {
            return visible;
        }
        
        public void setVisible(boolean visible) {
            if (this.visible == visible) return;
            
            this.visible = visible;
            
            // Only update visibility without removing from widget list
            if (toggleButton != null) toggleButton.visible = visible;
            if (setPathButton != null) setPathButton.visible = visible;
            if (deleteButton != null) deleteButton.visible = visible;
        }
        
        /**
         * Clean up all buttons associated with this entry
         */
        public void cleanup() {
            if (toggleButton != null) {
                screen.removePlatformButton(toggleButton);
            }
            
            if (setPathButton != null) {
                screen.removePlatformButton(setPathButton);
            }
            
            if (deleteButton != null) {
                screen.removePlatformButton(deleteButton);
            }
        }
    }
    
    /**
     * Clean up all buttons associated with this tab
     */
    public void cleanupButtons() {
        // Remove all buttons from the screen when tab is hidden
        if (returnButton != null) {
            screen.removePlatformButton(returnButton);
        }
        
        if (addPlatformButton != null) {
            screen.removePlatformButton(addPlatformButton);
        }
        
        if (scrollUpButton != null) {
            screen.removePlatformButton(scrollUpButton);
        }
        
        if (scrollDownButton != null) {
            screen.removePlatformButton(scrollDownButton);
        }
        
        // Clean up all platform entry buttons
        for (PlatformEntry entry : platformEntries) {
            entry.cleanup();
        }
    }
    
    /**
     * Sets the visibility of this tab and handles button cleanup
     */
    @Override
    public void setVisible(boolean visible) {
        // If visibility is not changing, do nothing
        if (this.isVisible() == visible) return;
        
        // Set visibility
        super.setVisible(visible);
        
        // When hiding, clean up all buttons
        if (!visible) {
            cleanupButtons();
        }
    }
} 