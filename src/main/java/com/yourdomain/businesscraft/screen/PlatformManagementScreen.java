package com.yourdomain.businesscraft.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.network.AddPlatformPacket;
import com.yourdomain.businesscraft.network.DeletePlatformPacket;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.SetPlatformEnabledPacket;
import com.yourdomain.businesscraft.network.OpenDestinationsUIPacket;
import com.yourdomain.businesscraft.network.SetPlatformPathCreationModePacket;
import com.yourdomain.businesscraft.network.ResetPlatformPathPacket;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.screen.components.*;
import com.yourdomain.businesscraft.client.PlatformPathKeyHandler;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.screen.util.ScreenNavigationHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.UUID;

/**
 * Screen for managing platforms in BusinessCraft towns
 */
public class PlatformManagementScreen extends Screen {
    // Reference to the block position being managed
    private final BlockPos townBlockPos;
    
    // Platform data
    private List<Platform> platforms;
    
    // Screen dimensions
    private int leftPos, topPos;
    private int screenWidth = 320;
    private int screenHeight = 240;
    
    // UI constants
    private static final int PLATFORM_ENTRY_HEIGHT = 30;
    private static final int MAX_VISIBLE_PLATFORMS = 5;
    private static final int HEADER_HEIGHT = 30;
    private static final int CONTENT_PADDING = 10;
    
    // Colors (matched to our BCTheme where possible)
    private static final int PRIMARY_COLOR = 0xA0335599;       // Semi-transparent blue
    private static final int BACKGROUND_COLOR = 0x80222222;    // Semi-transparent dark gray
    private static final int BORDER_COLOR = 0xA0AAAAAA;        // Light gray
    private static final int COLOR_HEADER_BG = 0x80202040;     // Header background
    private static final int COLOR_CONTENT_BG = 0x80303050;    // Content background
    private static final int COLOR_ENTRY_BG = 0x40404060;      // Platform entry background
    private static final int COLOR_ENTRY_BG_HOVER = 0x60505070; // Platform entry hover
    private static final int COLOR_TITLE = 0xFFEEEEFF;         // Title text
    private static final int COLOR_TEXT = 0xFFFFFFFF;          // Regular text
    private static final int COLOR_PLATFORM_NAME = 0xFFDDDDFF; // Platform name text
    private static final int COLOR_ENABLED = 0xFF4CAF50;       // Green for enabled
    private static final int COLOR_DISABLED = 0xFFE57373;      // Red for disabled
    
    // UI components
    private Button backButton;
    private Button addPlatformButton;
    
    // Platform entries for rendering
    private List<PlatformEntry> platformEntries = new ArrayList<>();
    
    // Scrolling state
    private int scrollOffset = 0;
    private int hoveredEntry = -1;

    /**
     * Create a new platform management screen
     * 
     * @param townBlockPos The position of the town block this screen manages
     * @param platforms The list of platforms to display and manage
     */
    public PlatformManagementScreen(BlockPos townBlockPos, List<Platform> platforms) {
        super(Component.translatable("businesscraft.manage_platforms"));
        this.townBlockPos = townBlockPos;
        this.platforms = platforms;
    }

    @Override
    protected void init() {
        super.init();
        
        // Calculate screen position (centered)
        this.leftPos = (this.width - screenWidth) / 2;
        this.topPos = (this.height - screenHeight) / 2;
        
        // Create the back button
        backButton = Button.builder(Component.translatable("businesscraft.back_button"), this::onBackButtonClick)
            .bounds(leftPos + CONTENT_PADDING, topPos + screenHeight - 30, 80, 20)
            .tooltip(Tooltip.create(Component.translatable("businesscraft.return_to_settings")))
            .build();
        
        // Create the add platform button
        addPlatformButton = Button.builder(Component.translatable("businesscraft.add_platform"), this::onAddPlatform)
            .bounds(leftPos + screenWidth - 100, topPos + screenHeight - 30, 80, 20)
            .tooltip(Tooltip.create(Component.translatable("businesscraft.add_platform")))
            .build();
        
        // Add buttons to the screen
        this.addRenderableWidget(backButton);
        this.addRenderableWidget(addPlatformButton);
        
        // Initialize platform entries
        refreshPlatforms();
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw the dimmed background
        this.renderBackground(graphics);
        
        // Draw the main window background
        renderWindowBackground(graphics);
        
        // Draw header section
        renderHeader(graphics);
        
        // Render platform entries
        renderPlatformEntries(graphics, mouseX, mouseY);
        
        // Render buttons
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    /**
     * Renders the window background with border
     */
    private void renderWindowBackground(GuiGraphics graphics) {
        // Main background
        graphics.fill(leftPos, topPos, leftPos + screenWidth, topPos + screenHeight, BACKGROUND_COLOR);
        
        // Border
        graphics.hLine(leftPos, leftPos + screenWidth - 1, topPos, BORDER_COLOR);
        graphics.hLine(leftPos, leftPos + screenWidth - 1, topPos + screenHeight - 1, BORDER_COLOR);
        graphics.vLine(leftPos, topPos, topPos + screenHeight - 1, BORDER_COLOR);
        graphics.vLine(leftPos + screenWidth - 1, topPos, topPos + screenHeight - 1, BORDER_COLOR);
        
        // Content area background
        graphics.fill(
            leftPos + CONTENT_PADDING, 
            topPos + HEADER_HEIGHT + CONTENT_PADDING,
            leftPos + screenWidth - CONTENT_PADDING,
            topPos + screenHeight - 40,
            COLOR_CONTENT_BG
        );
    }
    
    /**
     * Renders the header with title
     */
    private void renderHeader(GuiGraphics graphics) {
        // Header background
        graphics.fill(
            leftPos, 
            topPos,
            leftPos + screenWidth,
            topPos + HEADER_HEIGHT,
            COLOR_HEADER_BG
        );
        
        // Title text
        graphics.drawCenteredString(
            this.font,
            this.title,
            leftPos + screenWidth / 2,
            topPos + 10,
            COLOR_TITLE
        );
    }
    
    /**
     * Renders platform entries
     */
    private void renderPlatformEntries(GuiGraphics graphics, int mouseX, int mouseY) {
        // Update hovered entry detection
        hoveredEntry = -1;
        
        // Only show a limited number of entries based on scroll position
        int maxEntries = Math.min(platformEntries.size(), MAX_VISIBLE_PLATFORMS);
        int endIndex = Math.min(scrollOffset + maxEntries, platformEntries.size());
        
        for (int i = scrollOffset; i < endIndex; i++) {
            PlatformEntry entry = platformEntries.get(i);
            int entryY = topPos + HEADER_HEIGHT + CONTENT_PADDING + ((i - scrollOffset) * PLATFORM_ENTRY_HEIGHT);
            
            // Check if mouse is hovering over this entry
            if (mouseX >= leftPos + CONTENT_PADDING && 
                mouseX <= leftPos + screenWidth - CONTENT_PADDING - 30 &&
                mouseY >= entryY && 
                mouseY <= entryY + PLATFORM_ENTRY_HEIGHT - 5) {
                hoveredEntry = i;
            }
            
            // Render entry background
            boolean isHovered = (i == hoveredEntry);
            graphics.fill(
                leftPos + CONTENT_PADDING, 
                entryY,
                leftPos + screenWidth - CONTENT_PADDING,
                entryY + PLATFORM_ENTRY_HEIGHT - 5,
                isHovered ? COLOR_ENTRY_BG_HOVER : COLOR_ENTRY_BG
            );
            
            // Render platform info
            Platform platform = entry.platform;
            
            // Status indicator (green/red)
            int indicatorColor = platform.isEnabled() ? COLOR_ENABLED : COLOR_DISABLED;
            graphics.fill(
                leftPos + CONTENT_PADDING + 5, 
                entryY + 5, 
                leftPos + CONTENT_PADDING + 9, 
                entryY + PLATFORM_ENTRY_HEIGHT - 10, 
                indicatorColor
            );
            
            // Platform name and ID
            graphics.drawString(
                this.font,
                platform.getName() + " #" + (i + 1),
                leftPos + CONTENT_PADDING + 15,
                entryY + 7,
                COLOR_PLATFORM_NAME
            );
            
            // Render the buttons for this entry
            entry.renderButtons(graphics, mouseX, mouseY, 0);
        }
        
        // Draw "No platforms" message if list is empty
        if (platformEntries.isEmpty()) {
            graphics.drawCenteredString(
                this.font,
                Component.translatable("businesscraft.no_platforms"),
                leftPos + screenWidth / 2,
                topPos + 80,
                COLOR_TEXT
            );
        }
    }
    
    /**
     * Refreshes platform entries based on current data
     */
    private void refreshPlatforms() {
        platformEntries.clear();
        
        // Create entries for each platform
        for (int i = 0; i < platforms.size(); i++) {
            boolean isLastPlatform = (i == platforms.size() - 1);
            platformEntries.add(new PlatformEntry(platforms.get(i), i + 1, isLastPlatform));
        }
        
        // Reset scroll offset
        scrollOffset = 0;
    }
    
    /**
     * Handle back button click
     */
    private void onBackButtonClick(Button button) {
        // Close this screen and return to TownInterfaceScreen using the helper
        this.minecraft.setScreen(null);
        ScreenNavigationHelper.returnToTownInterface(this.minecraft, this.minecraft.player, townBlockPos);
    }
    
    /**
     * Handle add platform button click
     */
    private void onAddPlatform(Button button) {
        // Send packet to add a new platform
        ModMessages.sendToServer(new AddPlatformPacket(townBlockPos));
        
        // Return to parent screen for now - in future could stay on screen and refresh
        this.minecraft.setScreen(null);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Handle mouse wheel scrolling
        if (delta > 0 && scrollOffset > 0) {
            // Scroll up
            scrollOffset--;
            return true;
        } else if (delta < 0 && scrollOffset < platformEntries.size() - MAX_VISIBLE_PLATFORMS) {
            // Scroll down
            scrollOffset++;
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    /**
     * Class representing a platform entry in the UI
     */
    private class PlatformEntry {
        private final Platform platform;
        private final int positionNumber;
        private final boolean isLastPlatform;
        
        // Buttons for this entry
        private Button toggleButton;
        private Button destButton;
        private Button setPathButton;
        private Button deleteButton;
        
        public PlatformEntry(Platform platform, int positionNumber, boolean isLastPlatform) {
            this.platform = platform;
            this.positionNumber = positionNumber;
            this.isLastPlatform = isLastPlatform;
            
            // Create buttons based on entry index
            int index = positionNumber - 1;
            int rowY = topPos + HEADER_HEIGHT + CONTENT_PADDING + ((index - scrollOffset) * PLATFORM_ENTRY_HEIGHT);
            
            // Toggle button (On/Off)
            toggleButton = Button.builder(
                    Component.literal(platform.isEnabled() ? "On" : "Off"), 
                    button -> onToggle())
                .bounds(leftPos + screenWidth - 130, rowY + 3, 30, 20)
                .tooltip(Tooltip.create(Component.translatable(
                    platform.isEnabled() ? "businesscraft.enabled" : "businesscraft.disabled")))
                .build();
            
            // Destinations button
            String destTooltip = platform.hasNoEnabledDestinations() ? 
                "businesscraft.dest_any" : "businesscraft.dest_selected";
            destButton = Button.builder(
                    Component.translatable("businesscraft.dest"), 
                    button -> onDestinations())
                .bounds(leftPos + screenWidth - 170, rowY + 3, 35, 20)
                .tooltip(Tooltip.create(Component.translatable(destTooltip)))
                .build();
            
            // Set Path button
            setPathButton = Button.builder(
                    Component.translatable(platform.isComplete() ? "businesscraft.reset" : "businesscraft.set"), 
                    button -> onSetPath())
                .bounds(leftPos + screenWidth - 95, rowY + 3, 35, 20)
                .tooltip(Tooltip.create(Component.translatable(platform.isComplete() ? 
                    "businesscraft.reset_platform_path" : "businesscraft.set_platform_path")))
                .build();
            
            // Delete button (only for last platform)
            if (isLastPlatform) {
                deleteButton = Button.builder(
                        Component.translatable("businesscraft.del"), 
                        button -> onDelete())
                    .bounds(leftPos + screenWidth - 55, rowY + 3, 35, 20)
                    .tooltip(Tooltip.create(Component.translatable("businesscraft.delete_platform")))
                    .build();
                
                PlatformManagementScreen.this.addRenderableWidget(deleteButton);
            }
            
            // Add buttons to screen
            PlatformManagementScreen.this.addRenderableWidget(toggleButton);
            PlatformManagementScreen.this.addRenderableWidget(destButton);
            PlatformManagementScreen.this.addRenderableWidget(setPathButton);
        }
        
        /**
         * Update button positions based on scrolling
         */
        public void updateButtonPositions() {
            int index = positionNumber - 1;
            int visibleIndex = index - scrollOffset;
            
            // Only update if this entry is visible
            if (visibleIndex >= 0 && visibleIndex < MAX_VISIBLE_PLATFORMS) {
                int rowY = topPos + HEADER_HEIGHT + CONTENT_PADDING + (visibleIndex * PLATFORM_ENTRY_HEIGHT);
                
                toggleButton.setY(rowY + 3);
                destButton.setY(rowY + 3);
                setPathButton.setY(rowY + 3);
                
                if (deleteButton != null) {
                    deleteButton.setY(rowY + 3);
                }
                
                // Make buttons visible
                toggleButton.visible = true;
                destButton.visible = true;
                setPathButton.visible = true;
                if (deleteButton != null) {
                    deleteButton.visible = true;
                }
            } else {
                // Hide buttons if not visible
                toggleButton.visible = false;
                destButton.visible = false;
                setPathButton.visible = false;
                if (deleteButton != null) {
                    deleteButton.visible = false;
                }
            }
        }
        
        /**
         * Render the buttons for this entry
         */
        public void renderButtons(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            // Update button positions first
            updateButtonPositions();
        }
        
        /**
         * Handle platform toggle
         */
        private void onToggle() {
            // Send packet to toggle platform state
            boolean newState = !platform.isEnabled();
            ModMessages.sendToServer(new SetPlatformEnabledPacket(
                townBlockPos,
                platform.getId(),
                newState
            ));
            
            // Update local state for immediate feedback
            platform.setEnabled(newState);
            
            // Update button appearance
            toggleButton.setMessage(Component.literal(newState ? "On" : "Off"));
        }
        
        /**
         * Handle destinations button click
         */
        private void onDestinations() {
            // Send packet to open destinations UI
            ModMessages.sendToServer(new OpenDestinationsUIPacket(
                townBlockPos,
                platform.getId()
            ));
        }
        
        /**
         * Handle set path button click
         */
        private void onSetPath() {
            // If the platform is already complete, the button acts as a reset
            if (platform.isComplete()) {
                // Create a copy of the platform's current state
                UUID platformId = platform.getId();
                
                // Send packet to reset the platform path
                ModMessages.sendToServer(new ResetPlatformPathPacket(
                    townBlockPos,
                    platformId
                ));
                
                // Clear the start and end positions locally - this resets the path
                platform.setStartPos(null);
                platform.setEndPos(null);
                
                // Update button text immediately for faster feedback
                setPathButton.setMessage(Component.translatable("businesscraft.set"));
                setPathButton.setTooltip(Tooltip.create(Component.translatable("businesscraft.set_platform_path")));
                
                // Display message to the player
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    player.displayClientMessage(
                        Component.translatable("businesscraft.platform_path_reset"),
                        false
                    );
                }
                
                return;
            }
            
            // Send packet to enter platform path creation mode
            ModMessages.sendToServer(new SetPlatformPathCreationModePacket(
                townBlockPos,
                platform.getId(),
                true
            ));
            
            // Register with the key handler to detect ESC key
            PlatformPathKeyHandler.setActivePlatform(
                townBlockPos,
                platform.getId()
            );
            
            // Display instructions to the player
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable("businesscraft.platform_path_instructions"),
                    false
                );
            }
            
            // Close the UI to allow world interaction
            minecraft.setScreen(null);
        }
        
        /**
         * Handle delete button click
         */
        private void onDelete() {
            // Only allow deleting if this is the last platform
            if (isLastPlatform) {
                // Send delete platform packet
                ModMessages.sendToServer(new DeletePlatformPacket(
                    townBlockPos,
                    platform.getId()
                ));
                
                // Return to parent screen
                minecraft.setScreen(null);
            }
        }
    }

    /**
     * Open the platform management screen with the latest data from the TownBlockEntity
     * 
     * @param blockPos The position of the town block
     */
    public static void open(BlockPos blockPos) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        
        if (level != null) {
            BlockEntity be = level.getBlockEntity(blockPos);
            
            if (be instanceof TownBlockEntity townBlock) {
                // Get the latest platform data
                List<Platform> platforms = townBlock.getPlatforms();
                
                // Create and show the platform management screen
                PlatformManagementScreen screen = new PlatformManagementScreen(blockPos, platforms);
                minecraft.setScreen(screen);
            }
        }
    }
} 