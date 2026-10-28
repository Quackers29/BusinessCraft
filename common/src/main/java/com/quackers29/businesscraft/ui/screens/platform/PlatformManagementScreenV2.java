package com.quackers29.businesscraft.ui.screens.platform;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.ui.util.ScreenNavigationHelper;
import com.quackers29.businesscraft.ui.builders.UIGridBuilder;
import com.quackers29.businesscraft.ui.util.InventoryRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Redesigned platform management screen using BC UI framework patterns
 * Following Payment Board UI implementation style and Settings tab patterns
 */
public class PlatformManagementScreenV2 extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformManagementScreenV2.class);
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
    private static final int PLATFORM_LIST_HEIGHT = 120;

    // UI Components
    private UIGridBuilder platformListGrid;

    // Screen dimensions
    private int imageWidth = 340;
    private int imageHeight = 200;

    // Enhanced color scheme matching Payment Board
    private static final int SUCCESS_COLOR = 0xFF8FDB9A; // Light green for enabled
    private static final int INFO_COLOR = 0xB0336699; // Blue for actions
    private static final int DANGER_COLOR = 0xB0CC2222; // Red for delete
    private static final int DISABLED_COLOR = 0xFFF0A3A3; // Light red for disabled
    private static final int TEXT_COLOR = 0xFFFFFFFF; // White text
    private static final int HEADER_COLOR = 0xFFFCB821; // Gold for headers
    private static final int SECTION_BG_COLOR = 0x90000000; // Semi-transparent black

    public PlatformManagementScreenV2(BlockPos townBlockPos, List<Platform> platforms) {
        super(Component.translatable("businesscraft.manage_platforms"));
        this.townBlockPos = townBlockPos;
        this.platforms = platforms != null ? new ArrayList<>(platforms) : new ArrayList<>();
    }

    @Override
    protected void init() {
        super.init();
        this.imageWidth = Math.min(340, Math.max(220, this.width - 8));
        this.imageHeight = Math.min(200, Math.max(120, this.height - 8));

        // Initialize platform list grid
        initializePlatformListGrid();

        // Update data
        updatePlatformListData();

        // Request fresh platform data from server to ensure we're in sync
        // This fixes the issue where platforms aren't visible until a change is made
        requestPlatformData();
    }

    private void requestPlatformData() {
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object levelObj = clientHelper.getClientLevel();
            if (levelObj instanceof Level level) {
                BlockEntity be = level.getBlockEntity(townBlockPos);

                if (be instanceof TownInterfaceEntity townInterface) {
                    UUID townId = townInterface.getTownId();
                    if (townId != null) {
                        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS,
                                "Requesting fresh platform data for town {} (block at {})", townId, townBlockPos);
                        PlatformAccess.getNetworkMessages().sendToServer(
                                new com.quackers29.businesscraft.network.packets.ui.RequestTownPlatformDataPacket(
                                        townId));
                    } else {
                        LOGGER.warn("Cannot request platform data: TownInterfaceEntity at {} has no town ID",
                                townBlockPos);
                    }
                }
            }
        }
    }

    private void initializePlatformListGrid() {
        // Create grid similar to Payment Board pattern
        int listWidth = Math.min(PLATFORM_LIST_WIDTH, Math.max(200, imageWidth - PLATFORM_LIST_X - 16));
        int listHeight = Math.min(PLATFORM_LIST_HEIGHT, Math.max(60, imageHeight - PLATFORM_LIST_Y - 16));
        platformListGrid = UIGridBuilder.create(
                PLATFORM_LIST_X, PLATFORM_LIST_Y + 5,
                listWidth - 8, listHeight - 10,
                5) // Status, Name, State, Path, Destinations. The block position is on the name tooltip.
                .withColumnWeights(new float[] {0.08f, 0.26f, 0.22f, 0.22f, 0.22f})
                .withRowHeight(18)
                .withSpacing(4, 3) // Back to normal spacing
                .withMargins(4, 2) // Reduced vertical margin from 4 to 2 to fit more rows
                .drawBackground(false)
                .drawBorder(false);

        // Enable auto-scrolling - let UIGrid calculate visible rows from available
        // height
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

            // Column 0: Status indicator - green only if enabled AND has valid path
            boolean canSpawn = platform.isEnabled() && platform.isComplete();
            platformListGrid.addStatusIndicator(i, 0, canSpawn, SUCCESS_COLOR, DISABLED_COLOR, 13);

            // Column 1: Platform name. The name column is wide enough for the full word.
            String nameText = platform.getName() == null || platform.getName().isBlank()
                    ? "Platform " + (i + 1)
                    : platform.getName();
            net.minecraft.core.BlockPos at = platform.getInterfacePos();
            String where = at == null ? "no block position" : "at " + at.getX() + ", " + at.getY() + ", " + at.getZ();
            platformListGrid.addLabelWithTooltip(i, 1, nameText,
                    "Open this platform's Platform Interface to change it (" + where + ")", TEXT_COLOR);

            String state = platform.isEnabled() ? "Enabled" : "Disabled";
            platformListGrid.addLabel(i, 2, state, platform.isEnabled() ? SUCCESS_COLOR : DISABLED_COLOR);

            platformListGrid.addLabel(i, 3, platform.isComplete() ? "Path set" : "No path", TEXT_COLOR);

            int destCount = platform.getEnabledDestinations().size();
            String dest = switch (platform.getDestinationMode()) {
                case ANY -> "Any";
                case AUTO -> "Auto";
                case SPECIFIC -> destCount == 0 ? "Specific (none)"
                        : (destCount == 1 ? "1 town" : destCount + " towns");
            };
            platformListGrid.addLabel(i, 4, dest, TEXT_COLOR);
        }

        if (platforms.isEmpty()) {
            platformListGrid.updateTotalRows(1);
            platformListGrid.addLabel(0, 1, "No platforms yet", DISABLED_COLOR);
            platformListGrid.addLabelWithTooltip(0, 2, "Place a block",
                    "Place a Platform Interface inside the town boundary", TEXT_COLOR);
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
            guiGraphics.renderTooltip(this.font, Component.literal("Return to Town Centre"), mouseX, mouseY);
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
        guiGraphics.drawString(this.font, "Platforms",
                x + BACK_BUTTON_X + BACK_BUTTON_WIDTH + 6, y + SECTION_PADDING + 4, HEADER_COLOR);
    }

    private void renderPlatformListSection(GuiGraphics guiGraphics, int x, int y) {
        // Platform list section background
        int padding = INNER_PADDING;
        int sectionX = x + PLATFORM_LIST_X - padding;
        int sectionY = y + PLATFORM_LIST_Y - padding - 10;
        int sectionWidth = imageWidth - PLATFORM_LIST_X;
        int sectionHeight = imageHeight - PLATFORM_LIST_Y - 8;

        guiGraphics.fill(sectionX, sectionY, sectionX + sectionWidth, sectionY + sectionHeight, SECTION_BG_COLOR);
        InventoryRenderer.drawBorder(guiGraphics, sectionX, sectionY, sectionWidth, sectionHeight,
                InventoryRenderer.INVENTORY_BORDER_COLOR, 2);

        // Section header
        String headerText = platforms.isEmpty()
                ? "No platforms yet"
                : "Platforms (" + platforms.size() + ")  ·  read only";
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
            if (isMouseOverBackButton((int) mouseX, (int) mouseY)) {
                onBackButtonClick();
                return true;
            }

            // Handle platform grid clicks
            if (platformListGrid != null) {
                boolean handled = platformListGrid.mouseClicked(
                        (int) mouseX - x, (int) mouseY - y, button);
                if (handled)
                    return true;
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

    /**
     * Immediately refresh platform data from server (like PaymentBoardScreen
     * pattern)
     */
    private void refreshPlatformDataFromServer() {
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object levelObj = clientHelper.getClientLevel();
            if (levelObj instanceof Level level) {
                BlockEntity be = level.getBlockEntity(townBlockPos);

                if (be instanceof TownInterfaceEntity townInterface) {
                    // Get fresh platform data from server
                    List<Platform> freshPlatforms = townInterface.getPlatforms();

                    // Only update if platforms have actually changed
                    if (!freshPlatforms.equals(this.platforms)) {
                        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS,
                                "Platform data changed, updating UI (old: {}, new: {})",
                                this.platforms.size(), freshPlatforms.size());

                        this.platforms = new ArrayList<>(freshPlatforms);
                        // currentPlatforms will be updated in updatePlatformListData

                        // Force grid update
                        if (platformListGrid != null) {
                            updatePlatformListData();
                        } else {
                            initializePlatformListGrid();
                        }
                    }
                }
            }
        }
    }

    private void onBackButtonClick() {
        // Play click sound
        net.minecraft.client.resources.sounds.SimpleSoundInstance sound = net.minecraft.client.resources.sounds.SimpleSoundInstance
                .forUI(
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

    public boolean showcaseInvokeButton(String elementId) {
        if (platformListGrid == null) {
            return false;
        }
        return platformListGrid.invokeButtonById(elementId);
    }

    public boolean showcaseHasPlatforms() {
        return platforms != null && !platforms.isEmpty();
    }

    /**
     * Static factory method to open the platform management screen
     */
    public static void open(BlockPos blockPos) {
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object levelObj = clientHelper.getClientLevel();
            Object minecraftObj = clientHelper.getMinecraft();
            if (levelObj instanceof Level level && minecraftObj instanceof net.minecraft.client.Minecraft minecraft) {
                BlockEntity be = level.getBlockEntity(blockPos);

                if (be instanceof TownInterfaceEntity townInterface) {
                    List<Platform> platforms = townInterface.getPlatforms();
                    PlatformManagementScreenV2 screen = new PlatformManagementScreenV2(blockPos, platforms);
                    minecraft.setScreen(screen);
                }
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
        try {
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS,
                    "PlatformManagementScreenV2.refreshPlatformData called with force={}", force);

            // Don't refresh if we just did a toggle action (prevents conflicts) unless
            // forced
            if (!force) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastToggleTime < TOGGLE_PROTECTION_MS) {
                    DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS,
                            "Skipping refresh due to toggle protection ({}ms < {}ms)",
                            currentTime - lastToggleTime, TOGGLE_PROTECTION_MS);
                    return; // Skip refresh to avoid overriding local toggle state
                }
            }

            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper != null) {
                Object levelObj = clientHelper.getClientLevel();
                if (levelObj instanceof Level level) {
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

            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS,
                    "PlatformManagementScreenV2.refreshPlatformData completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error in PlatformManagementScreenV2.refreshPlatformData", e);
            // Don't crash the client, but log the error
        }
    }
}
