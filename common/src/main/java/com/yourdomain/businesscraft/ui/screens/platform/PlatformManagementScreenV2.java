package com.yourdomain.businesscraft.ui.screens.platform;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.network.packets.platform.AddPlatformPacket;
import com.yourdomain.businesscraft.network.packets.platform.DeletePlatformPacket;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.packets.platform.SetPlatformEnabledPacket;
import com.yourdomain.businesscraft.network.packets.ui.OpenDestinationsUIPacket;
import com.yourdomain.businesscraft.network.packets.platform.SetPlatformPathCreationModePacket;
import com.yourdomain.businesscraft.network.packets.platform.ResetPlatformPathPacket;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.client.PlatformPathKeyHandler;
import com.yourdomain.businesscraft.block.entity.TownInterfaceEntity;
import com.yourdomain.businesscraft.ui.util.ScreenNavigationHelper;
import com.yourdomain.businesscraft.ui.builders.UIGridBuilder;
import com.yourdomain.businesscraft.ui.util.InventoryRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Redesigned platform management screen using BC UI framework patterns
 * Following Payment Board UI implementation style and Settings tab patterns
 */
public class PlatformManagementScreenV2 extends Screen {
    // Reference to the block position being managed
    private final BlockPos townBlockPos;
    
    // Platform data
    private List<Platform> platforms;
    private List<Platform> currentPlatforms = new ArrayList<>();
    
    // Layout constants matching Payment Board style
    private static final int SECTION_PADDING = 8;
    private static final int INNER_PADDING = 6;
    private static final int ELEMENT_SPACING = 20;
    
    // Header section (back button + title + add button)
    private static final int HEADER_HEIGHT = 35;
    private static final int BACK_BUTTON_X = SECTION_PADDING;
    private static final int BACK_BUTTON_Y = SECTION_PADDING;
    private static final int BACK_BUTTON_WIDTH = 28;
    private static final int BACK_BUTTON_HEIGHT = 20;
    
    // Platform list section
    private static final int PLATFORM_LIST_X = SECTION_PADDING + 12;
    private static final int PLATFORM_LIST_Y = HEADER_HEIGHT + ELEMENT_SPACING;
    private static final int PLATFORM_LIST_WIDTH = 300;
    private static final int PLATFORM_LIST_HEIGHT = 120; // Height for scrolling
    
    // UI Components
    private UIGridBuilder platformListGrid;
    
    // Screen dimensions
    private int imageWidth = 340;
    private int imageHeight = 200;
    
    // Enhanced color scheme matching Payment Board
    private static final int SUCCESS_COLOR = 0xB0228B22; // Green for enabled
    private static final int INFO_COLOR = 0xB0336699;    // Blue for actions
    private static final int DANGER_COLOR = 0xB0CC2222;  // Red for delete
    private static final int DISABLED_COLOR = 0xA0666666; // Gray for disabled
    private static final int TEXT_COLOR = 0xFFFFFFFF;    // White text
    private static final int HEADER_COLOR = 0xFFFCB821;  // Gold for headers
    private static final int SECTION_BG_COLOR = 0x90000000; // Semi-transparent black
    
    public PlatformManagementScreenV2(BlockPos townBlockPos, List<Platform> platforms) {
        super(Component.translatable("businesscraft.manage_platforms"));
        this.townBlockPos = townBlockPos;
        this.platforms = platforms != null ? new ArrayList<>(platforms) : new ArrayList<>();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Initialize platform list grid
        initializePlatformListGrid();
        
        // Update data
        updatePlatformListData();
    }
    
    private void initializePlatformListGrid() {
        // Create grid similar to Payment Board pattern
        platformListGrid = UIGridBuilder.create(
            PLATFORM_LIST_X, PLATFORM_LIST_Y + 5,
            PLATFORM_LIST_WIDTH - 8, PLATFORM_LIST_HEIGHT - 10,
            5) // 5 columns: Status, Name, Toggle, Destinations, Actions
            .withRowHeight(18)
            .withSpacing(8, 3) // Back to normal spacing
            .withMargins(4, 2) // Reduced vertical margin from 4 to 2 to fit more rows
            .drawBackground(false)
            .drawBorder(false);
        
        // Enable auto-scrolling - let UIGrid calculate visible rows from available height
        platformListGrid.withVerticalScrollAuto(true);
        platformListGrid.updateTotalRows(platforms.size()); // Use actual platform count, not forced minimum
        
        // Populate with initial data
        populateGridWithPlatforms();
    }
    
    private void updatePlatformListData() {
        // Only update if platforms have changed
        if (!platforms.equals(currentPlatforms)) {
            currentPlatforms = new ArrayList<>(platforms);
            
            if (platformListGrid == null) {
                initializePlatformListGrid();
            } else {
                updatePlatformListGridData();
            }
        }
    }
    
    private void updatePlatformListGridData() {
        // Clear and repopulate while preserving scroll state
        platformListGrid.clearElements();
        
        // Always update total rows to handle dynamic changes
        platformListGrid.updateTotalRows(platforms.size());
        
        populateGridWithPlatforms();
    }
    
    private void populateGridWithPlatforms() {
        for (int i = 0; i < platforms.size(); i++) {
            Platform platform = platforms.get(i);
            boolean isLastPlatform = (i == platforms.size() - 1);
            
            // Column 0: Status indicator - green only if enabled AND has valid path
            boolean canSpawn = platform.isEnabled() && platform.isComplete();
            platformListGrid.addStatusIndicator(i, 0, canSpawn, SUCCESS_COLOR, DISABLED_COLOR, 13);
            
            // Column 1: Platform name and number (shortened for better fit)
            String nameText = "Plat #" + (i + 1);
            if (platform.isComplete()) {
                nameText += " ✓";
            }
            platformListGrid.addLabelWithTooltip(i, 1, nameText, 
                platform.isComplete() ? "Platform path is configured" : "Platform path not set", 
                TEXT_COLOR);
            
            // Column 2: Toggle button
            String toggleText = platform.isEnabled() ? "ON" : "OFF";
            int toggleColor = platform.isEnabled() ? SUCCESS_COLOR : DISABLED_COLOR;
            final int platformIndex = i; // Capture index for closure
            platformListGrid.addButtonWithTooltip(i, 2, toggleText,
                platform.isEnabled() ? "Click to disable platform" : "Click to enable platform",
                (v) -> togglePlatformByIndex(platformIndex), toggleColor);
            
            // Column 3: Destinations button
            String destTooltip = platform.hasNoEnabledDestinations() ? 
                "No destinations selected - allows any destination" : "Specific destinations selected";
            platformListGrid.addButtonWithTooltip(i, 3, "Dest",
                destTooltip,
                (v) -> openDestinationsByIndex(platformIndex), INFO_COLOR);
            
            // Column 4: Actions (Set Path / Reset Path)
            if (platform.isComplete()) {
                // Show Reset button
                platformListGrid.addButtonWithTooltip(i, 4, "Reset",
                    "Reset platform path",
                    (v) -> resetPlatformPathByIndex(platformIndex), DANGER_COLOR);
            } else {
                // Show Set Path button
                platformListGrid.addButtonWithTooltip(i, 4, "Set Path",
                    "Set platform path by clicking two blocks",
                    (v) -> setPlatformPathByIndex(platformIndex), INFO_COLOR);
            }
        }
        
        // If no platforms, show message (should rarely happen due to minimum limit of 1)
        if (platforms.isEmpty()) {
            platformListGrid.addLabel(0, 1, "No platforms configured", DISABLED_COLOR);
            platformListGrid.addLabel(0, 2, "Use 'Add Platform' button above", TEXT_COLOR);
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Update platform list data if needed (like PaymentBoardScreen)
        refreshPlatformDataFromServer();
        
        // Render background
        this.renderBackground(guiGraphics);
        
        // Calculate screen position
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        
        // Render background sections
        renderBackground(guiGraphics, x, y);
        
        // Render header
        renderHeader(guiGraphics, x, y, mouseX, mouseY);
        
        // Render platform list section
        renderPlatformListSection(guiGraphics, x, y);
        
        // Render platform grid
        if (platformListGrid != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            platformListGrid.render(guiGraphics, mouseX - x, mouseY - y);
            guiGraphics.pose().popPose();
        }
        
        // Render back button tooltip
        if (isMouseOverBackButton(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Return to Town Interface"), mouseX, mouseY);
        }
        
        // Render header button tooltips
        if (isMouseOverAddButton(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Add a new platform"), mouseX, mouseY);
        } else if (platforms.size() > 1 && isMouseOverDeleteLastButton(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Delete the last platform"), mouseX, mouseY);
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
        
        // Title
        guiGraphics.drawString(this.font, "Platform Management", 
            x + BACK_BUTTON_X + BACK_BUTTON_WIDTH + 6, y + SECTION_PADDING + 4, HEADER_COLOR);
        
        // Header buttons (right side)
        // Always show Add Platform button
        boolean isAddButtonHovered = isMouseOverAddButton(mouseX, mouseY);
        InventoryRenderer.drawButton(guiGraphics,
            x + imageWidth - 80, y + BACK_BUTTON_Y,
            70, BACK_BUTTON_HEIGHT,
            "Add Platform", this.font, isAddButtonHovered);
        
        // Delete Last button (only if we have more than 1 platform - minimum limit of 1)
        if (platforms.size() > 1) {
            boolean isDeleteLastHovered = isMouseOverDeleteLastButton(mouseX, mouseY);
            InventoryRenderer.drawButton(guiGraphics,
                x + imageWidth - 160, y + BACK_BUTTON_Y,
                70, BACK_BUTTON_HEIGHT,
                "Delete Last", this.font, isDeleteLastHovered);
        }
    }
    
    private void renderPlatformListSection(GuiGraphics guiGraphics, int x, int y) {
        // Platform list section background
        int padding = INNER_PADDING;
        int sectionX = x + PLATFORM_LIST_X - padding;
        int sectionY = y + PLATFORM_LIST_Y - padding - 10;
        int sectionWidth = PLATFORM_LIST_WIDTH + (padding * 2);
        int sectionHeight = PLATFORM_LIST_HEIGHT + (padding * 2) + 10;
        
        guiGraphics.fill(sectionX, sectionY, sectionX + sectionWidth, sectionY + sectionHeight, SECTION_BG_COLOR);
        InventoryRenderer.drawBorder(guiGraphics, sectionX, sectionY, sectionWidth, sectionHeight,
            InventoryRenderer.INVENTORY_BORDER_COLOR, 2);
        
        // Section header
        String headerText = platforms.isEmpty() ? "No Platforms" : "Platforms (" + platforms.size() + ")";
        guiGraphics.drawString(this.font, headerText,
            x + PLATFORM_LIST_X + 6, y + PLATFORM_LIST_Y - 12, HEADER_COLOR);
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
            
            // Check delete last button (only when more than 1 platform - minimum limit of 1)
            if (platforms.size() > 1 && isMouseOverDeleteLastButton((int)mouseX, (int)mouseY)) {
                deleteLastPlatform();
                return true;
            }
            
            // Check add button (always available)
            if (isMouseOverAddButton((int)mouseX, (int)mouseY)) {
                addPlatform();
                return true;
            }
            
            // Handle platform grid clicks
            if (platformListGrid != null) {
                boolean handled = platformListGrid.mouseClicked(
                    (int)mouseX - x, (int)mouseY - y, button);
                if (handled) return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Forward scroll events to the grid
        if (platformListGrid != null) {
            int x = (this.width - imageWidth) / 2;
            int y = (this.height - imageHeight) / 2;
            
            return platformListGrid.mouseScrolled(mouseX - x, mouseY - y, delta);
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    // Button interaction methods
    private boolean isMouseOverBackButton(int mouseX, int mouseY) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        
        return InventoryRenderer.isMouseOverElement(mouseX, mouseY, x, y,
            BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
    }
    
    private boolean isMouseOverAddButton(int mouseX, int mouseY) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        
        return InventoryRenderer.isMouseOverElement(mouseX, mouseY, x, y,
            imageWidth - 80, BACK_BUTTON_Y, 70, BACK_BUTTON_HEIGHT);
    }
    
    private boolean isMouseOverDeleteLastButton(int mouseX, int mouseY) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        
        return InventoryRenderer.isMouseOverElement(mouseX, mouseY, x, y,
            imageWidth - 160, BACK_BUTTON_Y, 70, BACK_BUTTON_HEIGHT);
    }
    
    // Platform action methods - using indices to avoid stale object references
    private void togglePlatformByIndex(int index) {
        if (index < 0 || index >= platforms.size()) return;
        
        Platform platform = platforms.get(index);
        boolean newState = !platform.isEnabled();
        
        // Record toggle time to prevent server refresh conflicts
        lastToggleTime = System.currentTimeMillis();
        
        ModMessages.sendToServer(new SetPlatformEnabledPacket(
            townBlockPos, platform.getId(), newState));
        
        // Update local state immediately for instant feedback
        platform.setEnabled(newState);
        
        // Update the UI immediately (server refresh will be blocked for 500ms)
        if (platformListGrid != null) {
            platformListGrid.clearElements();
            populateGridWithPlatforms();
        }
    }
    
    private void openDestinationsByIndex(int index) {
        if (index < 0 || index >= platforms.size()) return;
        
        Platform platform = platforms.get(index);
        ModMessages.sendToServer(new OpenDestinationsUIPacket(
            townBlockPos, platform.getId()));
    }
    
    private void setPlatformPathByIndex(int index) {
        if (index < 0 || index >= platforms.size()) return;
        
        Platform platform = platforms.get(index);
        // Send packet to enter platform path creation mode
        ModMessages.sendToServer(new SetPlatformPathCreationModePacket(
            townBlockPos, platform.getId(), true));
        
        // Register with key handler
        PlatformPathKeyHandler.setActivePlatform(townBlockPos, platform.getId());
        
        // Show instructions
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                Component.translatable("businesscraft.platform_path_instructions"), false);
        }
        
        // Close UI
        minecraft.setScreen(null);
    }
    
    private void resetPlatformPathByIndex(int index) {
        if (index < 0 || index >= platforms.size()) return;
        
        Platform platform = platforms.get(index);
        ModMessages.sendToServer(new ResetPlatformPathPacket(
            townBlockPos, platform.getId()));
        
        // Update local state
        platform.setStartPos(null);
        platform.setEndPos(null);
        
        // Show feedback
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(
                Component.translatable("businesscraft.platform_path_reset"), false);
        }
        
        // Force the grid to refresh immediately by clearing and repopulating
        if (platformListGrid != null) {
            platformListGrid.clearElements();
            populateGridWithPlatforms();
        }
    }
    
    private void addPlatform() {
        ModMessages.sendToServer(new AddPlatformPacket(townBlockPos));
        
        // Immediately refresh the UI (like PaymentBoardScreen does)
        refreshPlatformDataFromServer();
    }
    
    private void deleteLastPlatform() {
        // Enforce minimum platform limit of 1
        if (platforms.size() <= 1) return;
        
        // Get the last platform
        Platform lastPlatform = platforms.get(platforms.size() - 1);
        
        // Send delete platform packet
        ModMessages.sendToServer(new DeletePlatformPacket(
            townBlockPos,
            lastPlatform.getId()
        ));
        
        // Immediately refresh the UI (like PaymentBoardScreen does)  
        refreshPlatformDataFromServer();
    }
    
    /**
     * Immediately refresh platform data from server (like PaymentBoardScreen pattern)
     */
    private void refreshPlatformDataFromServer() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        
        if (level != null) {
            BlockEntity be = level.getBlockEntity(townBlockPos);
            
            if (be instanceof TownInterfaceEntity townInterface) {
                // Get fresh platform data from server
                List<Platform> freshPlatforms = townInterface.getPlatforms();
                
                // Always update platforms and force grid refresh (simplified logic)
                this.platforms = new ArrayList<>(freshPlatforms);
                currentPlatforms.clear(); // Force refresh by clearing cache
                
                // Force grid update
                if (platformListGrid != null) {
                    updatePlatformListGridData();
                } else {
                    initializePlatformListGrid();
                }
            }
        }
    }
    
    private void onBackButtonClick() {
        // Play click sound
        net.minecraft.client.resources.sounds.SimpleSoundInstance sound = 
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F);
        this.minecraft.getSoundManager().play(sound);
        
        // Return to town interface
        this.minecraft.setScreen(null);
        ScreenNavigationHelper.returnToTownInterface(this.minecraft, this.minecraft.player, townBlockPos);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game like Payment Board and other BC screens
    }
    
    /**
     * Static factory method to open the platform management screen
     */
    public static void open(BlockPos blockPos) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        
        if (level != null) {
            BlockEntity be = level.getBlockEntity(blockPos);
            
            if (be instanceof TownInterfaceEntity townInterface) {
                List<Platform> platforms = townInterface.getPlatforms();
                PlatformManagementScreenV2 screen = new PlatformManagementScreenV2(blockPos, platforms);
                minecraft.setScreen(screen);
            }
        }
    }
    
    // Track recent toggle actions to prevent server refresh conflicts
    private long lastToggleTime = 0;
    private static final long TOGGLE_PROTECTION_MS = 500; // 500ms protection window
    
    /**
     * Refresh platform data without reopening the screen (for packet updates)
     */
    public void refreshPlatformData() {
        refreshPlatformData(false);
    }
    
    /**
     * Refresh platform data with optional force flag
     */
    public void refreshPlatformData(boolean force) {
        // Don't refresh if we just did a toggle action (prevents conflicts) unless forced
        if (!force) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastToggleTime < TOGGLE_PROTECTION_MS) {
                return; // Skip refresh to avoid overriding local toggle state
            }
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        
        if (level != null) {
            BlockEntity be = level.getBlockEntity(townBlockPos);
            
            if (be instanceof TownInterfaceEntity townInterface) {
                // Update our platform list with fresh data from the server
                this.platforms = new ArrayList<>(townInterface.getPlatforms());
                
                // Force UI update by clearing currentPlatforms to ensure refresh
                currentPlatforms.clear();
                updatePlatformListData();
            }
        }
    }
}