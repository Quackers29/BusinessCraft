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
    
    // Payment board section - smaller to prevent overlap
    private static final int PAYMENT_BOARD_X = SECTION_PADDING;
    private static final int PAYMENT_BOARD_Y = HEADER_HEIGHT + ELEMENT_SPACING;
    private static final int PAYMENT_BOARD_WIDTH = 160;
    private static final int PAYMENT_BOARD_HEIGHT = 45; // Further reduced to prevent overlap
    
    // Buffer storage section - positioned to not overlap with payment board
    private static final int BUFFER_START_X = 8;
    private static final int BUFFER_START_Y = 140; // Keep this to match menu slots
    private static final int BUFFER_ROWS = 2;
    private static final int BUFFER_COLS = 9;
    
    // Player inventory section - properly spaced from buffer
    private static final int INV_START_X = 8;
    private static final int INV_START_Y = 200; // Keep this to match menu slots
    private static final int HOTBAR_START_Y = 270; // Keep this to match menu slots
    
    // Buffer inventory size
    private static final int BUFFER_INVENTORY_SIZE = BUFFER_ROWS * BUFFER_COLS; // 18 slots
    
    // Payment board UI components
    private UIGridBuilder paymentBoardGrid;
    private List<RewardEntry> currentRewards = new ArrayList<>();
    
    // Enhanced color scheme for better visual hierarchy
    private static final int SUCCESS_COLOR = 0xB0228B22; // Slightly more opaque green
    private static final int INFO_COLOR = 0xB0336699;    // Slightly more opaque blue
    private static final int EXPIRED_COLOR = 0xA0666666; // Gray for expired
    private static final int TEXT_COLOR = 0xFFFFFFFF;    // White text
    private static final int HEADER_COLOR = 0xFFFCB821;  // Gold for headers
    private static final int SECTION_BG_COLOR = 0x90000000; // Semi-transparent black for sections
    
    public PaymentBoardScreen(PaymentBoardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        
        // More compact screen for reduced margins
        this.imageWidth = 176; // Reduced back to standard width
        this.imageHeight = HOTBAR_START_Y + InventoryRenderer.SLOT_SIZE + ELEMENT_SPACING + 8; // Reduced padding
        
        // Title positioning in compact header
        this.titleLabelX = BACK_BUTTON_X + BACK_BUTTON_WIDTH + 6; // Reduced spacing
        this.titleLabelY = SECTION_PADDING + 4; // Adjusted for smaller header
        // Fix: Set inventory label to static position, not following slots
        this.inventoryLabelX = 8; // Static position
        this.inventoryLabelY = INV_START_Y - 12; // Static position above inventory
        
        // Initialize payment board grid
        initializePaymentBoardGrid();
    }
    
    private void initializePaymentBoardGrid() {
        paymentBoardGrid = UIGridBuilder.create(
            PAYMENT_BOARD_X, PAYMENT_BOARD_Y, 
            PAYMENT_BOARD_WIDTH, PAYMENT_BOARD_HEIGHT, 
            5) // 5 columns: Source, Rewards, Time, Claim, Buffer
            .withRowHeight(16) // Reduced to fit better within designated area
            .withVerticalScroll(true, 2) // Reduced to 2 rows to fit in constrained height
            .withSpacing(4, 2) // Tighter spacing for constrained area
            .withMargins(4, 4) // Smaller margins to maximize usable area
            .drawBackground(true)
            .drawBorder(true);
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
        // Payment board section background - ensure it fits within screen bounds
        int padding = INNER_PADDING;
        int sectionX = x + PAYMENT_BOARD_X - padding;
        int sectionY = y + PAYMENT_BOARD_Y - padding - 10;
        int sectionWidth = PAYMENT_BOARD_WIDTH + (padding * 2);
        int sectionHeight = PAYMENT_BOARD_HEIGHT + (padding * 2) + 10;
        
        guiGraphics.fill(sectionX, sectionY, sectionX + sectionWidth, sectionY + sectionHeight, SECTION_BG_COLOR);
        InventoryRenderer.drawBorder(guiGraphics, sectionX, sectionY, sectionWidth, sectionHeight, 
                InventoryRenderer.INVENTORY_BORDER_COLOR, 2);
        
        // Payment board header with more noticeable positioning
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
            rebuildPaymentBoardGrid();
        }
    }
    
    private void rebuildPaymentBoardGrid() {
        // Clear existing grid - using corrected coordinates with same constraints as init
        paymentBoardGrid = UIGridBuilder.create(
            PAYMENT_BOARD_X, PAYMENT_BOARD_Y, 
            PAYMENT_BOARD_WIDTH, PAYMENT_BOARD_HEIGHT, 
            5) // 5 columns
            .withRowHeight(16) // Match init method
            .withVerticalScroll(true, 2) // Match init method
            .withSpacing(4, 2) // Match init method
            .withMargins(4, 4) // Match init method  
            .drawBackground(false) // Don't draw background, we handle it ourselves
            .drawBorder(false);
        
        // Add rewards to grid
        for (int i = 0; i < currentRewards.size(); i++) {
            RewardEntry reward = currentRewards.get(i);
            
            // Column 0: Source icon (using first reward item as icon)
            if (!reward.getRewards().isEmpty()) {
                ItemStack firstItem = reward.getRewards().get(0);
                paymentBoardGrid.addItemStack(i, 0, firstItem, null);
            }
            
            // Column 1: Rewards summary (truncated)
            String rewardsText = truncateText(reward.getRewardsDisplay(), 20);
            paymentBoardGrid.addLabel(i, 1, rewardsText, TEXT_COLOR);
            
            // Column 2: Time ago
            String timeText = reward.getTimeAgoDisplay();
            paymentBoardGrid.addLabel(i, 2, timeText, TEXT_COLOR);
            
            // Column 3: Claim button
            if (reward.canBeClaimed("ALL") && reward.getStatus() == ClaimStatus.UNCLAIMED) {
                paymentBoardGrid.addButtonWithTooltip(i, 3, "Claim", 
                    "Claim to inventory", 
                    (v) -> claimReward(reward.getId(), false), 
                    SUCCESS_COLOR);
            } else {
                // Show status instead of button
                String status = reward.getStatus() == ClaimStatus.CLAIMED ? "Claimed" :
                               reward.getStatus() == ClaimStatus.EXPIRED ? "Expired" : "N/A";
                paymentBoardGrid.addLabel(i, 3, status, EXPIRED_COLOR);
            }
            
            // Column 4: Buffer button  
            if (reward.canBeClaimed("ALL") && reward.getStatus() == ClaimStatus.UNCLAIMED) {
                paymentBoardGrid.addButtonWithTooltip(i, 4, "→Buf", 
                    "Claim to buffer storage", 
                    (v) -> claimReward(reward.getId(), true), 
                    INFO_COLOR);
            } else {
                paymentBoardGrid.addLabel(i, 4, "", TEXT_COLOR); // Empty cell
            }
        }
        
        // If no rewards, show a message
        if (currentRewards.isEmpty()) {
            paymentBoardGrid.addLabel(0, 1, "No unclaimed rewards", EXPIRED_COLOR);
        }
    }
    
    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
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
        // Handle payment board grid scrolling
        if (paymentBoardGrid != null) {
            int screenX = (this.width - this.imageWidth) / 2;
            int screenY = (this.height - this.imageHeight) / 2;
            
            boolean handled = paymentBoardGrid.mouseScrolled(
                mouseX - screenX, mouseY - screenY, delta);
            if (handled) return true;
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
        // Similar logic to the original storage screen but for buffer storage
        ItemStack slotBefore = slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
        
        // Handle shift-clicking
        if (type == ClickType.QUICK_MOVE) {
            if (!slotBefore.isEmpty()) {
                // Let standard handling happen
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
        if (type == ClickType.PICKUP && mouseButton == 0 && !this.menu.getCarried().isEmpty()) {
            // Adding item to buffer
            super.slotClicked(slot, slotId, mouseButton, type);
            
            ItemStack slotAfter = slot.getItem();
            if (slotAfter.getCount() > slotBefore.getCount() || 
                (slotBefore.isEmpty() && !slotAfter.isEmpty())) {
                
                int itemsAdded = slotBefore.isEmpty() ? slotAfter.getCount() : 
                               slotAfter.getCount() - slotBefore.getCount();
                
                if (itemsAdded > 0) {
                    ItemStack itemToAdd = slotAfter.copy();
                    itemToAdd.setCount(itemsAdded);
                    this.menu.processBufferStorageAdd(this.minecraft.player, slotId, itemToAdd);
                }
            }
        } else if (type == ClickType.PICKUP && mouseButton == 0 && !slot.getItem().isEmpty()) {
            // Removing item from buffer
            ItemStack itemToRemove = slot.getItem().copy();
            super.slotClicked(slot, slotId, mouseButton, type);
            this.menu.processBufferStorageRemove(this.minecraft.player, slotId, itemToRemove);
        } else {
            // Other click types
            super.slotClicked(slot, slotId, mouseButton, type);
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
}