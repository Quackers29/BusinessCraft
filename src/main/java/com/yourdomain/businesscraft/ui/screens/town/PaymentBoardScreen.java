package com.yourdomain.businesscraft.ui.screens.town;

import com.yourdomain.businesscraft.menu.PaymentBoardMenu;
import com.yourdomain.businesscraft.ui.util.InventoryRenderer;
import com.yourdomain.businesscraft.ui.util.ScreenNavigationHelper;
import com.yourdomain.businesscraft.ui.builders.UIGridBuilder;
import com.yourdomain.businesscraft.town.data.RewardEntry;
import com.yourdomain.businesscraft.town.data.ClaimStatus;
import com.yourdomain.businesscraft.town.data.RewardSource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.core.BlockPos;
import com.yourdomain.businesscraft.debug.DebugConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.util.FormattedCharSequence;

import java.util.*;
import java.util.List;

public class PaymentBoardScreen extends AbstractContainerScreen<PaymentBoardMenu> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentBoardScreen.class);
    
    // Layout constants - reduced margins to prevent overlaps
    private static final int SECTION_PADDING = 8; // Reduced from 12
    private static final int INNER_PADDING = 6; // Reduced from 12
    private static final int ELEMENT_SPACING = 20;
    
    // Header section (back button + title)
    private static final int HEADER_HEIGHT = 35; // Reduced from 45
    private static final int BACK_BUTTON_X = SECTION_PADDING;
    private static final int BACK_BUTTON_Y = SECTION_PADDING;
    private static final int BACK_BUTTON_WIDTH = 28;
    private static final int BACK_BUTTON_HEIGHT = 20;
    
    // Payment board section - centered to screen width with slight right adjustment
    private static final int PAYMENT_BOARD_X = SECTION_PADDING + 12; // Move right for better centering
    private static final int PAYMENT_BOARD_Y = HEADER_HEIGHT + ELEMENT_SPACING;
    private static final int PAYMENT_BOARD_WIDTH = 300; // Restore full width for screen centering
    private static final int PAYMENT_BOARD_HEIGHT = 60; // Height for scrolling
    
    // Buffer storage section - centered in wider screen
    private static final int BUFFER_START_X = 90; // Centered for 340px screen width
    private static final int BUFFER_START_Y = 140; // Back to original position
    private static final int BUFFER_ROWS = 2;
    private static final int BUFFER_COLS = 9;
    
    // Player inventory section - centered in wider screen
    private static final int INV_START_X = 90; // Centered for 340px screen width
    private static final int INV_START_Y = 200; // Back to original position
    private static final int HOTBAR_START_Y = 270; // Back to original position
    
    // Buffer inventory size
    private static final int BUFFER_INVENTORY_SIZE = BUFFER_ROWS * BUFFER_COLS; // 18 slots
    
    // Payment board UI components
    private UIGridBuilder paymentBoardGrid;
    private List<RewardEntry> currentRewards = new ArrayList<>();
    
    // Client-side cached rewards data (synced from server)
    private List<RewardEntry> cachedRewards = new ArrayList<>();
    
    // Enhanced color scheme for better visual hierarchy
    private static final int SUCCESS_COLOR = 0xB0228B22; // Slightly more opaque green
    private static final int INFO_COLOR = 0xB0336699;    // Slightly more opaque blue
    private static final int EXPIRED_COLOR = 0xA0666666; // Gray for expired
    private static final int TEXT_COLOR = 0xFFFFFFFF;    // White text
    private static final int HEADER_COLOR = 0xFFFCB821;  // Gold for headers
    private static final int SECTION_BG_COLOR = 0x90000000; // Semi-transparent black for sections
    
    public PaymentBoardScreen(PaymentBoardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        
        // Expanded screen to accommodate wider payment board and taller layout
        this.imageWidth = 340; // Expanded from 176 to accommodate 300px payment board + margins
        this.imageHeight = HOTBAR_START_Y + InventoryRenderer.SLOT_SIZE + ELEMENT_SPACING + 8;
        
        // Title positioning in compact header
        this.titleLabelX = BACK_BUTTON_X + BACK_BUTTON_WIDTH + 6; // Reduced spacing
        this.titleLabelY = SECTION_PADDING + 4; // Adjusted for smaller header
        // Fix: Set inventory label to static position, not following slots (centered)
        this.inventoryLabelX = INV_START_X; // Static position, centered with inventory
        this.inventoryLabelY = INV_START_Y - 12; // Static position above inventory
        
        // Initialize payment board grid
        initializePaymentBoardGrid();
    }
    
    private void initializePaymentBoardGrid() {
        // The grid will be created on first data update - this preserves scroll state
        // No need to create it here since we'll create it in updatePaymentBoardData
        paymentBoardGrid = null;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Request reward data from server after screen is fully initialized
        requestPaymentBoardData();
        
        // Request buffer storage data from server
        this.menu.requestBufferStorageData();
    }
    
    /**
     * Request payment board data from the server (proper timing after init)
     */
    private void requestPaymentBoardData() {
        if (this.minecraft != null && this.minecraft.level != null && this.minecraft.level.isClientSide()) {
            BlockPos townBlockPos = this.menu.getTownBlockPos();
            if (townBlockPos != null) {
                try {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "PaymentBoardScreen.requestPaymentBoardData() - Requesting data from server for town block at {}", 
                        townBlockPos);
                    com.yourdomain.businesscraft.network.ModMessages.sendToServer(
                        new com.yourdomain.businesscraft.network.packets.storage.PaymentBoardRequestPacket(townBlockPos));
                } catch (Exception e) {
                    LOGGER.error("Error sending payment board data request", e);
                }
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "PaymentBoardScreen.requestPaymentBoardData() - No town block position available");
            }
        } else {
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "PaymentBoardScreen.requestPaymentBoardData() - Not on client side or minecraft not available");
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Update payment board data if needed
        updatePaymentBoardData();
        
        // Render the background
        this.renderBackground(guiGraphics);
        
        // Render the screen elements
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render payment board grid
        if (paymentBoardGrid != null) {
            int screenX = (this.width - this.imageWidth) / 2;
            int screenY = (this.height - this.imageHeight) / 2;
            
            // Translate grid coordinates to screen coordinates
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(screenX, screenY, 0);
            paymentBoardGrid.render(guiGraphics, mouseX - screenX, mouseY - screenY);
            guiGraphics.pose().popPose();
        }
        
        // Render custom multi-line tooltips for tourist arrivals
        renderCustomTooltips(guiGraphics, mouseX, mouseY);
        
        // Render tooltips for slots
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // Render tooltip for back button if mouse is over it
        if (isMouseOverBackButton(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Return to Town Interface"), mouseX, mouseY);
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Calculate position for centered interface
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Draw the main background with improved styling
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, InventoryRenderer.BACKGROUND_COLOR);
        InventoryRenderer.drawBorder(guiGraphics, x, y, this.imageWidth, this.imageHeight, InventoryRenderer.BORDER_COLOR, 2);
        guiGraphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, InventoryRenderer.BACKGROUND_COLOR);
        
        // Enhanced header section with visual separation
        renderHeaderSection(guiGraphics, x, y, mouseX, mouseY);
        
        // Enhanced payment board section
        renderPaymentBoardSection(guiGraphics, x, y);
        
        // Enhanced buffer storage section
        renderBufferStorageSection(guiGraphics, x, y);
        
        // Enhanced player inventory section
        renderPlayerInventorySection(guiGraphics, x, y);
    }
    
    private void renderHeaderSection(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        // Properly sized header background
        guiGraphics.fill(x + 4, y + 4, x + this.imageWidth - 4, y + HEADER_HEIGHT, SECTION_BG_COLOR);
        
        // Draw the back button
        boolean isBackButtonHovered = isMouseOverBackButton(mouseX, mouseY);
        InventoryRenderer.drawButton(guiGraphics, 
                x + BACK_BUTTON_X, y + BACK_BUTTON_Y, 
                BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, 
                "◀", this.font, isBackButtonHovered);
        
        // Draw screen title
        guiGraphics.drawString(this.font, "Payment Board", 
                x + this.titleLabelX, y + this.titleLabelY, HEADER_COLOR);
    }
    
    private void renderPaymentBoardSection(GuiGraphics guiGraphics, int x, int y) {
        // Payment board section background - centered and sized properly
        int padding = INNER_PADDING;
        int sectionX = x + PAYMENT_BOARD_X - padding;
        int sectionY = y + PAYMENT_BOARD_Y - padding - 10;
        int sectionWidth = PAYMENT_BOARD_WIDTH + (padding * 2);
        int sectionHeight = PAYMENT_BOARD_HEIGHT + (padding * 2) + 10;
        
        guiGraphics.fill(sectionX, sectionY, sectionX + sectionWidth, sectionY + sectionHeight, SECTION_BG_COLOR);
        InventoryRenderer.drawBorder(guiGraphics, sectionX, sectionY, sectionWidth, sectionHeight, 
                InventoryRenderer.INVENTORY_BORDER_COLOR, 2);
        
        // Payment board header centered with other sections
        guiGraphics.drawString(this.font, "Unclaimed Rewards", 
                x + PAYMENT_BOARD_X + 6, y + PAYMENT_BOARD_Y - 12, HEADER_COLOR);
    }
    
    private void renderBufferStorageSection(GuiGraphics guiGraphics, int x, int y) {
        // Buffer storage section background - ensure proper alignment
        int bufferWidth = BUFFER_COLS * InventoryRenderer.SLOT_SIZE;
        int bufferHeight = BUFFER_ROWS * InventoryRenderer.SLOT_SIZE;
        
        int padding = INNER_PADDING;
        int sectionX = x + BUFFER_START_X - padding;
        int sectionY = y + BUFFER_START_Y - padding - 10;
        int sectionWidth = bufferWidth + (padding * 2);
        int sectionHeight = bufferHeight + (padding * 2) + 10;
        
        // Ensure section doesn't exceed screen width
        if (sectionX + sectionWidth > x + this.imageWidth - 4) {
            sectionWidth = (x + this.imageWidth - 4) - sectionX;
        }
        
        guiGraphics.fill(sectionX, sectionY, sectionX + sectionWidth, sectionY + sectionHeight, SECTION_BG_COLOR);
        InventoryRenderer.drawBorder(guiGraphics, sectionX, sectionY, sectionWidth, sectionHeight, 
                InventoryRenderer.INVENTORY_BORDER_COLOR, 2);
        
        // Buffer storage label with more noticeable positioning
        guiGraphics.drawString(this.font, "Payment Buffer", 
                x + BUFFER_START_X + 6, y + BUFFER_START_Y - 12, HEADER_COLOR);
        
        // Draw buffer slot grid exactly matching slot positions
        for (int row = 0; row < BUFFER_ROWS; row++) {
            for (int col = 0; col < BUFFER_COLS; col++) {
                int slotX = x + BUFFER_START_X + (col * InventoryRenderer.SLOT_SIZE);
                int slotY = y + BUFFER_START_Y + (row * InventoryRenderer.SLOT_SIZE);
                
                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, InventoryRenderer.SLOT_BG_COLOR);
                InventoryRenderer.drawSlotBorder(guiGraphics, slotX - 1, slotY - 1, 18, 18, InventoryRenderer.SLOT_BORDER_COLOR);
            }
        }
    }
    
    private void renderPlayerInventorySection(GuiGraphics guiGraphics, int x, int y) {
        // Player inventory section background - ensure proper alignment
        int invWidth = 9 * InventoryRenderer.SLOT_SIZE;
        int invHeight = 3 * InventoryRenderer.SLOT_SIZE;
        int hotbarHeight = InventoryRenderer.SLOT_SIZE;
        int totalHeight = invHeight + (HOTBAR_START_Y - INV_START_Y - invHeight) + hotbarHeight;
        
        int padding = INNER_PADDING;
        int sectionX = x + INV_START_X - padding;
        int sectionY = y + INV_START_Y - padding - 10;
        int sectionWidth = invWidth + (padding * 2);
        int sectionHeight = totalHeight + (padding * 2) + 10;
        
        // Ensure section doesn't exceed screen width
        if (sectionX + sectionWidth > x + this.imageWidth - 4) {
            sectionWidth = (x + this.imageWidth - 4) - sectionX;
        }
        
        guiGraphics.fill(sectionX, sectionY, sectionX + sectionWidth, sectionY + sectionHeight, SECTION_BG_COLOR);
        InventoryRenderer.drawBorder(guiGraphics, sectionX, sectionY, sectionWidth, sectionHeight, 
                InventoryRenderer.INVENTORY_BORDER_COLOR, 2);
        
        // Inventory label with static positioning (fixed position, doesn't follow slots)
        guiGraphics.drawString(this.font, "Inventory", 
                x + this.inventoryLabelX, y + this.inventoryLabelY, HEADER_COLOR);
        
        // Player inventory with proper spacing
        InventoryRenderer.drawInventoryWithHotbar(guiGraphics, 
                x + INV_START_X, y + INV_START_Y, 
                9, 3, 1, HOTBAR_START_Y - INV_START_Y - (3 * InventoryRenderer.SLOT_SIZE));
    }
    
    private void updatePaymentBoardData() {
        // Get current rewards from the menu
        List<RewardEntry> rewards = this.menu.getUnclaimedRewards();
        
        // Only update if the rewards have changed
        if (!rewards.equals(currentRewards)) {
            currentRewards = new ArrayList<>(rewards);
            
            // Create grid only once (like StandardTabContent), then update data
            if (paymentBoardGrid == null) {
                createPaymentBoardGrid();
            } else {
                updatePaymentBoardGridData();
            }
        }
    }
    
    /**
     * Create the payment board grid only once (like StandardTabContent does)
     */
    private void createPaymentBoardGrid() {
        paymentBoardGrid = UIGridBuilder.create(
            PAYMENT_BOARD_X, PAYMENT_BOARD_Y + 5, // Move grid down 5px from section top
            PAYMENT_BOARD_WIDTH - 8, PAYMENT_BOARD_HEIGHT - 10, // More width since scrollbar is outside grid
            4) // 4 columns: Source, Rewards, Time, Claim
            .withRowHeight(16) // Slightly smaller rows to fit better
            .withSpacing(8, 2) // Restore spacing, reduce vertical spacing
            .withMargins(8, 4) // Reduce vertical margins
            .drawBackground(false) // Don't draw background, we handle it ourselves
            .drawBorder(false);
            
        // Set up scrolling if we have more than 3 rewards
        if (currentRewards.size() > 3) {
            paymentBoardGrid.withVerticalScroll(true, 3); // Show 3 rows, scroll for more
            paymentBoardGrid.updateTotalRows(currentRewards.size());
        }
        
        // Add initial data
        populateGridWithRewards();
    }
    
    /**
     * Update grid data while preserving scroll state (like StandardTabContent does)
     */
    private void updatePaymentBoardGridData() {
        // Clear existing elements but preserve grid structure and scroll state
        paymentBoardGrid.clearElements();
        
        // Update total rows if needed
        if (currentRewards.size() > 3) {
            paymentBoardGrid.updateTotalRows(currentRewards.size());
        }
        
        // Repopulate with new data
        populateGridWithRewards();
    }
    
    /**
     * Populate the grid with current reward data
     */
    private void populateGridWithRewards() {
        // Add rewards to grid (4 columns: Source, Rewards, Time, Claim)
        for (int i = 0; i < currentRewards.size(); i++) {
            RewardEntry reward = currentRewards.get(i);
            
            // Create enhanced tooltip for both columns
            String enhancedTooltip = createRewardTooltip(reward);
            
            if (reward.getSource() == RewardSource.TOURIST_ARRIVAL) {
                // Column 0: Tourist info "[quantity] x [origin town]" for tourist arrivals
                String touristInfo = createTouristInfoDisplay(reward);
                String truncatedTouristInfo = truncateTextStable(touristInfo, 12); // Limit to 12 chars
                // Don't add UIGrid tooltips for tourist arrivals - use custom tooltips only
                paymentBoardGrid.addLabel(i, 0, truncatedTouristInfo, TEXT_COLOR);
                
                // Column 1: Multi-item visual display for tourist arrivals
                addMultiItemDisplay(i, 1, reward, null); // No UIGrid tooltip - use custom tooltips only
            } else {
                // Column 0: Source icon (using first reward item as icon) for other rewards
                if (!reward.getRewards().isEmpty()) {
                    ItemStack firstItem = reward.getRewards().get(0);
                    if (enhancedTooltip != null) {
                        paymentBoardGrid.addItemStackWithTooltip(i, 0, firstItem, enhancedTooltip, null);
                    } else {
                        paymentBoardGrid.addItemStack(i, 0, firstItem, null);
                    }
                }
                
                // Column 1: Rewards summary (truncated to 12 chars max to prevent overlap) for other rewards
                String fullRewardsText = reward.getRewardsDisplay();
                String rewardsText = truncateTextStable(fullRewardsText, 12);
                
                if (enhancedTooltip != null) {
                    paymentBoardGrid.addLabelWithTooltip(i, 1, rewardsText, enhancedTooltip, TEXT_COLOR);
                } else if (fullRewardsText.length() > 12) {
                    paymentBoardGrid.addLabelWithTooltip(i, 1, rewardsText, fullRewardsText, TEXT_COLOR);
                } else {
                    paymentBoardGrid.addLabel(i, 1, rewardsText, TEXT_COLOR);
                }
            }
            
            // Column 2: Time in HH:mm:ss format with full date/time tooltip
            String timeText = reward.getTimeDisplay(); // HH:mm:ss format
            String fullDateTime = reward.getFullDateTimeDisplay(); // Full date/time for tooltip
            paymentBoardGrid.addLabelWithTooltip(i, 2, timeText, fullDateTime, TEXT_COLOR);
            
            // Column 3: Single Claim button (goes directly to buffer)
            if (reward.canBeClaimed("ALL") && reward.getStatus() == ClaimStatus.UNCLAIMED) {
                paymentBoardGrid.addButtonWithTooltip(i, 3, "Claim", 
                    "Claim to buffer storage", 
                    (v) -> claimReward(reward.getId(), true), // Always claim to buffer (true)
                    SUCCESS_COLOR);
            } else {
                // Show status instead of button
                String status = reward.getStatus() == ClaimStatus.CLAIMED ? "Claimed" :
                               reward.getStatus() == ClaimStatus.EXPIRED ? "Expired" : "N/A";
                paymentBoardGrid.addLabel(i, 3, status, EXPIRED_COLOR);
            }
        }
        
        // If no rewards, show a message
        if (currentRewards.isEmpty()) {
            paymentBoardGrid.addLabel(0, 1, "No unclaimed rewards", EXPIRED_COLOR);
        }
    }
    
    private String truncateText(String text, int maxCharacters) {
        if (text == null || text.length() <= maxCharacters) return text;
        
        // Use character-based truncation for now (simpler and more predictable)
        if (maxCharacters <= 3) return "...";
        return text.substring(0, maxCharacters - 3) + "...";
    }
    
    /**
     * Stable truncation that produces consistent results for the same input
     */
    private String truncateTextStable(String text, int maxCharacters) {
        if (text == null || text.isEmpty()) return "";
        if (text.length() <= maxCharacters) return text;
        
        // Ensure consistent truncation
        if (maxCharacters <= 3) return "...";
        
        // Try to break at word boundaries if possible for better readability
        String truncated = text.substring(0, maxCharacters - 3);
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > maxCharacters / 2) { // Only break at space if it's not too early
            truncated = truncated.substring(0, lastSpace);
        }
        
        return truncated + "...";
    }
    
    /**
     * Creates enhanced tooltips for rewards, especially tourist arrival rewards
     */
    private String createRewardTooltip(RewardEntry reward) {
        if (reward.getSource() == RewardSource.TOURIST_ARRIVAL) {
            // Return a marker string that we'll detect and replace with custom rendering
            return "TOURIST_ARRIVAL_TOOLTIP:" + reward.getId().toString();
        }
        
        // For non-tourist arrivals or if metadata is missing, return null
        return null;
    }
    
    /**
     * Creates tourist info display text "[quantity] x [origin town]"
     */
    private String createTouristInfoDisplay(RewardEntry reward) {
        String touristCountStr = reward.getMetadata().get("touristCount");
        String milestoneDistanceStr = reward.getMetadata().get("milestoneDistance");
        
        int touristCount = 1; // Default fallback
        if (touristCountStr != null && !touristCountStr.isEmpty()) {
            try {
                touristCount = Integer.parseInt(touristCountStr);
            } catch (NumberFormatException e) {
                // Fallback to calculating from emeralds if metadata is corrupted
                int totalEmeralds = 0;
                for (ItemStack stack : reward.getRewards()) {
                    if (stack.getItem() == net.minecraft.world.item.Items.EMERALD) {
                        totalEmeralds += stack.getCount();
                    }
                }
                touristCount = Math.max(1, totalEmeralds / 10); // Rough estimate: 10 emeralds per tourist
            }
        }
        
        // Get distance traveled from milestone distance metadata
        int metersTravel = 0;
        if (milestoneDistanceStr != null && !milestoneDistanceStr.isEmpty()) {
            try {
                metersTravel = Integer.parseInt(milestoneDistanceStr);
            } catch (NumberFormatException e) {
                metersTravel = 0; // Default fallback
            }
        }
        
        if (metersTravel > 0) {
            return touristCount + " x " + metersTravel + "m";
        } else {
            return touristCount + " x 0m";
        }
    }
    
    /**
     * Adds multi-item visual display (up to 4 overlapping items)
     */
    private void addMultiItemDisplay(int row, int col, RewardEntry reward, String tooltip) {
        List<ItemStack> items = reward.getRewards();
        
        if (!items.isEmpty()) {
            // For the multi-item display, prioritize showing variety over quantities
            List<ItemStack> uniqueItems = getUniqueItems(items, 4);
            
            if (uniqueItems.size() == 1) {
                // Single item - show normally (no UIGrid tooltip for tourist arrivals)
                ItemStack item = uniqueItems.get(0);
                paymentBoardGrid.addItemStack(row, col, item, null);
            } else {
                // Multiple items - use the new multi-item display component (no UIGrid tooltip for tourist arrivals)
                paymentBoardGrid.addMultiItemDisplay(row, col, uniqueItems, null, null);
            }
        }
    }
    
    /**
     * Gets unique items from a list, prioritizing emeralds first, then other items
     */
    private List<ItemStack> getUniqueItems(List<ItemStack> items, int maxItems) {
        List<ItemStack> unique = new ArrayList<>();
        Set<net.minecraft.world.item.Item> seenItems = new HashSet<>();
        
        // First, add emeralds if present
        for (ItemStack stack : items) {
            if (stack.getItem() == net.minecraft.world.item.Items.EMERALD && 
                !seenItems.contains(stack.getItem()) && unique.size() < maxItems) {
                unique.add(stack);
                seenItems.add(stack.getItem());
                break;
            }
        }
        
        // Then add other unique items
        for (ItemStack stack : items) {
            if (stack.getItem() != net.minecraft.world.item.Items.EMERALD && 
                !seenItems.contains(stack.getItem()) && unique.size() < maxItems) {
                unique.add(stack);
                seenItems.add(stack.getItem());
            }
        }
        
        return unique;
    }
    
    /**
     * Creates tooltip showing both enhanced info and item breakdown
     */
    private String createMultiItemTooltip(RewardEntry reward, String enhancedTooltip) {
        StringBuilder tooltip = new StringBuilder();
        
        // Add enhanced tooltip if available
        if (enhancedTooltip != null && !enhancedTooltip.trim().isEmpty()) {
            tooltip.append(enhancedTooltip).append("\n\nItems:\n");
        }
        
        // Add item breakdown
        String itemsText = reward.getRewardsDisplay();
        tooltip.append(itemsText);
        
        return tooltip.toString();
    }
    
    /**
     * Creates simplified MC-style multi-line tooltip components for tourist arrival rewards
     */
    private List<net.minecraft.network.chat.Component> createTouristArrivalTooltip(RewardEntry reward) {
        List<net.minecraft.network.chat.Component> tooltipLines = new ArrayList<>();
        
        String originTown = reward.getMetadata().get("originTown");
        String fareAmount = reward.getMetadata().get("fareAmount");
        String milestoneDistance = reward.getMetadata().get("milestoneDistance");
        
        // Line 1: "Origin: [TOWN] ([DISTANCE])"
        String originText = "Origin: " + (originTown != null ? originTown : "Unknown");
        if (milestoneDistance != null && !milestoneDistance.isEmpty()) {
            originText += " (" + milestoneDistance + "m)";
        }
        tooltipLines.add(net.minecraft.network.chat.Component.literal(originText)
            .withStyle(ChatFormatting.GRAY));
        
        // Line 2: "Fare: [Emeralds paid for travel]"
        if (fareAmount != null && !fareAmount.isEmpty()) {
            tooltipLines.add(net.minecraft.network.chat.Component.literal("Fare: " + fareAmount + " emeralds")
                .withStyle(ChatFormatting.GREEN));
        }
        
        // Line 3: "Milestone: [Rewards]" (milestone rewards only, no distance)
        List<String> milestoneRewards = new ArrayList<>();
        for (ItemStack stack : reward.getRewards()) {
            if (stack.getItem() != net.minecraft.world.item.Items.EMERALD) {
                String itemName = stack.getHoverName().getString();
                if (stack.getCount() > 1) {
                    milestoneRewards.add(stack.getCount() + "x" + itemName);
                } else {
                    milestoneRewards.add("1x" + itemName);
                }
            }
        }
        
        if (!milestoneRewards.isEmpty()) {
            String milestoneText = "Milestone: " + String.join(", ", milestoneRewards);
            tooltipLines.add(net.minecraft.network.chat.Component.literal(milestoneText)
                .withStyle(ChatFormatting.GOLD));
        }
        
        return tooltipLines;
    }
    
    /**
     * Renders custom multi-line tooltips for tourist arrival rewards
     */
    private void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (paymentBoardGrid == null || currentRewards.isEmpty()) {
            return;
        }
        
        // Check if mouse is over any tourist arrival reward
        for (int i = 0; i < currentRewards.size(); i++) {
            RewardEntry reward = currentRewards.get(i);
            if (reward.getSource() == RewardSource.TOURIST_ARRIVAL) {
                // Check if mouse is over this reward's row (columns 0 or 1)
                if (isMouseOverRewardRow(mouseX, mouseY, i)) {
                    // Render the multi-line tooltip
                    List<net.minecraft.network.chat.Component> tooltipLines = createTouristArrivalTooltip(reward);
                    // Convert Component list to FormattedCharSequence list for rendering
                    List<FormattedCharSequence> formattedLines = new ArrayList<>();
                    for (Component component : tooltipLines) {
                        formattedLines.add(component.getVisualOrderText());
                    }
                    guiGraphics.renderTooltip(this.font, formattedLines, mouseX, mouseY);
                    return; // Only show one tooltip at a time
                }
            }
        }
    }
    
    /**
     * Checks if the mouse is over a specific reward row (columns 0 or 1) using UIGridBuilder
     */
    private boolean isMouseOverRewardRow(int mouseX, int mouseY, int rowIndex) {
        if (paymentBoardGrid == null) {
            return false;
        }
        
        // Calculate grid-relative mouse position
        int screenX = (this.width - this.imageWidth) / 2;
        int screenY = (this.height - this.imageHeight) / 2;
        int gridMouseX = mouseX - screenX;
        int gridMouseY = mouseY - screenY;
        
        // Use UIGridBuilder's built-in row detection for columns 0 and 1 (tooltip area)
        return paymentBoardGrid.isMouseOverRow(gridMouseX, gridMouseY, rowIndex, 2);
    }
    
    private void claimReward(UUID rewardId, boolean toBuffer) {
        // Send claim request to server via menu
        this.menu.claimReward(rewardId, toBuffer);
        
        // Update the display
        updatePaymentBoardData();
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if the back button was clicked
        if (button == 0 && isMouseOverBackButton((int)mouseX, (int)mouseY)) {
            // Play a click sound
            net.minecraft.client.resources.sounds.SimpleSoundInstance sound = 
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F);
            this.minecraft.getSoundManager().play(sound);
            
            // Return to the main UI
            returnToMainUI();
            return true;
        }
        
        // Handle payment board grid clicks
        if (paymentBoardGrid != null) {
            int screenX = (this.width - this.imageWidth) / 2;
            int screenY = (this.height - this.imageHeight) / 2;
            
            boolean handled = paymentBoardGrid.mouseClicked(
                (int)mouseX - screenX, (int)mouseY - screenY, button);
            if (handled) return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Forward scroll events directly to the grid (like StandardTabContent does)
        if (paymentBoardGrid != null) {
            int screenX = (this.width - this.imageWidth) / 2;
            int screenY = (this.height - this.imageHeight) / 2;
            
            // Adjust mouse coordinates to grid-relative coordinates
            return paymentBoardGrid.mouseScrolled(mouseX - screenX, mouseY - screenY, delta);
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slotId < 0 || slot == null) {
            super.slotClicked(slot, slotId, mouseButton, type);
            return;
        }

        // Handle buffer storage slots (first 18 slots)
        if (slotId < BUFFER_INVENTORY_SIZE) {
            // Buffer storage slot interactions - same logic as original storage
            handleBufferSlotClick(slot, slotId, mouseButton, type);
            return;
        }
        
        // For player inventory slots, use default behavior
        super.slotClicked(slot, slotId, mouseButton, type);
    }
    
    private void handleBufferSlotClick(Slot slot, int slotId, int mouseButton, ClickType type) {
        // WITHDRAWAL-ONLY BUFFER: Users can only remove items, not add them
        // This maintains hopper automation input capability while preventing manual user additions
        
        ItemStack slotBefore = slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
        
        // Handle shift-clicking - ALLOW (removal only)
        if (type == ClickType.QUICK_MOVE) {
            if (!slotBefore.isEmpty()) {
                // Allow shift-click to remove items from buffer to player inventory
                super.slotClicked(slot, slotId, mouseButton, type);
                
                // Calculate what was removed and send to server
                ItemStack slotAfter = slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                int countBefore = slotBefore.getCount();
                int countAfter = slotAfter.isEmpty() ? 0 : slotAfter.getCount();
                int itemsTaken = countBefore - countAfter;
                
                if (itemsTaken > 0) {
                    ItemStack itemsToRemove = slotBefore.copy();
                    itemsToRemove.setCount(itemsTaken);
                    this.menu.processBufferStorageRemove(this.minecraft.player, slotId, itemsToRemove);
                }
            }
            return;
        }
        
        // Handle regular clicks
        if (type == ClickType.PICKUP && mouseButton == 0) {
            if (!this.menu.getCarried().isEmpty()) {
                // BLOCK: User trying to add item to buffer - prevent this
                // Do nothing, effectively blocking the action
                return;
            } else if (!slot.getItem().isEmpty()) {
                // ALLOW: User trying to remove item from buffer - allow this
                ItemStack itemToRemove = slot.getItem().copy();
                super.slotClicked(slot, slotId, mouseButton, type);
                this.menu.processBufferStorageRemove(this.minecraft.player, slotId, itemToRemove);
            }
        } else {
            // For other click types (right-click, etc.), only allow if removing items
            if (!slot.getItem().isEmpty() && this.menu.getCarried().isEmpty()) {
                // Allow removal operations
                super.slotClicked(slot, slotId, mouseButton, type);
            }
            // Block all other operations that could add items
        }
    }
    
    /**
     * Update the buffer storage display with items from the server
     */
    public void updateBufferStorageItems(Map<Item, Integer> items) {
        this.menu.updateBufferStorageItems(items);
    }
    
    private boolean isMouseOverBackButton(int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        return InventoryRenderer.isMouseOverElement(mouseX, mouseY, x, y, 
                BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
    }
    
    private void returnToMainUI() {
        this.onClose();
        ScreenNavigationHelper.returnToTownInterface(this.minecraft, this.minecraft.player, this.menu.getTownBlockPos());
    }
    
    /**
     * Update the cached reward data from the server
     * Called by PaymentBoardResponsePacket
     */
    public void updateRewardData(List<RewardEntry> rewards) {
        this.cachedRewards = new ArrayList<>(rewards);
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "PaymentBoardScreen.updateRewardData() - received {} rewards from server", rewards.size());
        
        // Trigger a UI update
        updatePaymentBoardData();
    }
}