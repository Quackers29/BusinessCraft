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
    
    // Status colors (changed from pink/purple)
    private static final int COLOR_ENABLED = 0xFF4CAF50; // Green
    private static final int COLOR_DISABLED = 0xFFE57373; // Light red
    
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
        
        // Return button - with proper text
        returnButton = Button.builder(Component.translatable("businesscraft.back_button"), (button) -> screen.showMainTab())
            .bounds(leftPos + CONTENT_PADDING, topPos + CONTENT_PADDING, 60, 20)
            .tooltip(Tooltip.create(Component.translatable("businesscraft.return_to_settings")))
            .build();
        
        // Add platform button - moved to bottom of screen
        addPlatformButton = Button.builder(Component.translatable("businesscraft.add_button"), this::onAddPlatform)
            .bounds(leftPos + width - 80, topPos + height - 30, 70, 20)
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
        
        // Create entries with position information
        for (int i = 0; i < platforms.size(); i++) {
            boolean isLastPlatform = (i == platforms.size() - 1);
            platformEntries.add(new PlatformEntry(platforms.get(i), i + 1, isLastPlatform));
        }
        
        scrollOffset = 0;
        updatePlatformPositions();
    }
    
    private void onAddPlatform(Button button) {
        // Send the add platform packet to the server
        ModMessages.sendToServer(new AddPlatformPacket(screen.getBlockPos()));
        
        // Simply return to settings page - more reliable than the async approach
        screen.showMainTab();
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
        Component title = Component.translatable("businesscraft.manage_platforms");
        gui.drawString(
            Minecraft.getInstance().font,
            title,
            contentLeft + (contentWidth / 2) - (Minecraft.getInstance().font.width(title) / 2),
            contentTop + 10,
            COLOR_TITLE,
            true // with shadow
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
        private final int positionNumber;
        private final boolean isLastPlatform;
        private Button toggleButton;
        private Button setPathButton;
        private Button deleteButton;
        private boolean visible = true;
        
        public PlatformEntry(Platform platform, int positionNumber, boolean isLastPlatform) {
            this.platform = platform;
            this.positionNumber = positionNumber;
            this.isLastPlatform = isLastPlatform;
            
            int baseX = contentLeft + 5;
            int baseY = contentTop + 40;
            
            // Toggle button (on/off) - changed to standard button
            toggleButton = Button.builder(Component.translatable(platform.isEnabled() ? 
                    "businesscraft.enabled" : "businesscraft.disabled"), 
                    button -> onToggle())
                .bounds(baseX + 120, baseY, 60, 20)
                .tooltip(Tooltip.create(Component.translatable(platform.isEnabled() ? 
                    "businesscraft.enabled" : "businesscraft.disabled")))
                .build();
            
            // Set path button - improved styling
            setPathButton = Button.builder(Component.translatable("businesscraft.set"), this::onSetPath)
                .bounds(baseX + 185, baseY, 30, 20)
                .tooltip(Tooltip.create(Component.translatable("businesscraft.set_platform_path")))
                .build();
            
            // Delete button - improved styling - only for last platform
            if (isLastPlatform) {
                deleteButton = Button.builder(Component.translatable("businesscraft.del"), this::onDelete)
                    .bounds(baseX + 220, baseY, 30, 20)
                    .tooltip(Tooltip.create(Component.translatable("businesscraft.delete_platform")))
                    .build();
                
                // Register delete button
                screen.addPlatformButton(deleteButton);
            }
            
            // Register buttons with the screen
            screen.addPlatformButton(toggleButton);
            screen.addPlatformButton(setPathButton);
        }
        
        public void updatePosition(int x, int y) {
            toggleButton.setX(x + 120);
            toggleButton.setY(y);
            
            setPathButton.setX(x + 185);
            setPathButton.setY(y);
            
            if (isLastPlatform && deleteButton != null) {
                deleteButton.setX(x + 220);
                deleteButton.setY(y);
            }
        }
        
        public void render(GuiGraphics gui, int mouseX, int mouseY, boolean isHovered) {
            if (!visible) return;
            
            // Entry background with hover effect
            int bgColor = isHovered ? COLOR_ENTRY_BG_HOVER : COLOR_ENTRY_BG;
            
            // Draw rounded entry background
            int entryX = toggleButton.getX() - 120; // Match the toggle button offset
            int entryY = toggleButton.getY();      // Get y from button position
            int entryWidth = contentWidth - CONTENT_PADDING * 2;
            
            gui.fill(
                entryX, entryY, 
                entryX + entryWidth - 30, entryY + PLATFORM_ENTRY_HEIGHT - 5,
                bgColor
            );
            
            // Use position number instead of platform name
            Component nameComponent = Component.translatable("businesscraft.platform_number", positionNumber);
            int textColor = platform.isEnabled() ? COLOR_PLATFORM_NAME : 0xBBAAAACC;
            
            // Draw name with position offset from toggle button
            gui.drawString(
                Minecraft.getInstance().font,
                nameComponent,
                entryX + 30,
                entryY + 6,
                textColor
            );
            
            // Show platform status (complete or not)
            Component statusText = platform.isComplete() 
                ? Component.translatable("businesscraft.platform_complete") 
                : Component.translatable("businesscraft.platform_incomplete");
            
            int statusTextColor = platform.isComplete() ? 0xFF66BB6A : 0xFFFF7043;
            
            gui.drawString(
                Minecraft.getInstance().font,
                statusText,
                entryX + 30,
                entryY + 18,
                statusTextColor
            );
            
            // Draw status indicator (dot)
            int statusX = entryX + 12;
            int statusY = entryY + 10;
            int statusColor = platform.isEnabled() ? COLOR_ENABLED : COLOR_DISABLED;
            
            // Draw colored status dot
            gui.fill(statusX, statusY, statusX + 8, statusY + 8, statusColor);
            
            // Draw outline around the status indicator
            gui.fill(statusX, statusY, statusX + 8, statusY + 1, 0xFFFFFFFF); // Top
            gui.fill(statusX, statusY, statusX + 1, statusY + 8, 0xFFFFFFFF); // Left
            gui.fill(statusX + 7, statusY, statusX + 8, statusY + 8, 0xFFFFFFFF); // Right
            gui.fill(statusX, statusY + 7, statusX + 8, statusY + 8, 0xFFFFFFFF); // Bottom
            
            // Render buttons - the toggleButton will now show "Enabled" or "Disabled" text
            toggleButton.render(gui, mouseX, mouseY, 0);
            setPathButton.render(gui, mouseX, mouseY, 0);
            
            // Only render delete button for last platform
            if (isLastPlatform && deleteButton != null) {
                deleteButton.render(gui, mouseX, mouseY, 0);
            }
        }
        
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!visible) return false;
            
            // Only include delete button in check if it's the last platform
            if (isLastPlatform && deleteButton != null) {
                return toggleButton.mouseClicked(mouseX, mouseY, button) ||
                       setPathButton.mouseClicked(mouseX, mouseY, button) ||
                       deleteButton.mouseClicked(mouseX, mouseY, button);
            } else {
                return toggleButton.mouseClicked(mouseX, mouseY, button) ||
                       setPathButton.mouseClicked(mouseX, mouseY, button);
            }
        }
        
        private void onToggle() {
            // Update UI immediately for responsiveness
            platform.setEnabled(!platform.isEnabled());
            
            // Remove old button from screen
            screen.removePlatformButton(toggleButton);
            
            // Create new button with updated status
            toggleButton = Button.builder(Component.translatable(platform.isEnabled() ? 
                    "businesscraft.enabled" : "businesscraft.disabled"), 
                    button -> onToggle())
                .bounds(toggleButton.getX(), toggleButton.getY(), 60, 20)
                .tooltip(Tooltip.create(Component.translatable(platform.isEnabled() ? 
                    "businesscraft.enabled" : "businesscraft.disabled")))
                .build();
            
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
            screen.showPathEditor(platform.getId());
        }
        
        private void onDelete(Button button) {
            // Send delete packet to server
            ModMessages.sendToServer(new DeletePlatformPacket(
                screen.getBlockPos(), 
                platform.getId()
            ));
            
            // Simply return to settings page - more reliable than the async approach
            screen.showMainTab();
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
            if (isLastPlatform && deleteButton != null) deleteButton.visible = visible;
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
            
            if (isLastPlatform && deleteButton != null) {
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