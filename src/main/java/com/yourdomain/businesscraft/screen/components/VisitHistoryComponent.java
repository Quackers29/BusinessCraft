package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

/**
 * Component that displays a scrollable list of tourist visits to the town
 */
public class VisitHistoryComponent implements UIComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisitHistoryComponent.class);
    private static final int ITEM_HEIGHT = 20; // Reduced height for single line
    private static final int MAX_VISIBLE_ITEMS = 6; // Increased to 6
    private static final int SCROLL_AMOUNT = 1;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("MM/dd HH:mm");
    
    private final Supplier<List<VisitEntry>> historySupplier;
    private final int width;
    private boolean visible = true;
    private int scrollOffset = 0;
    private List<VisitEntry> historyEntries = new ArrayList<>();
    private Button scrollUpButton;
    private Button scrollDownButton;
    private int renderX;
    private int renderY;
    private int x, y;

    public VisitHistoryComponent(Supplier<List<VisitEntry>> historySupplier, int width) {
        this.historySupplier = historySupplier;
        this.width = width;
    }

    @Override
    public void init(Consumer<Button> register) {
        scrollUpButton = new Button.Builder(Component.literal("Up"), button -> scrollUp())
            .pos(0, 0) // Position will be set in render
            .size(28, 20)
            .build();
        
        scrollDownButton = new Button.Builder(Component.literal("Down"), button -> scrollDown())
            .pos(0, 0) // Position will be set in render
            .size(28, 20)
            .build();
        
        register.accept(scrollUpButton);
        register.accept(scrollDownButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        if (!visible) return;
        
        this.x = x;
        this.y = y;
        
        // Store render position for hit testing
        this.renderX = x;
        this.renderY = y;
        
        // Update the list of history entries
        updateHistoryList();
        
        // Update button positions
        scrollUpButton.setX(x + width - 32);
        scrollUpButton.setY(y);
        scrollDownButton.setX(x + width - 32);
        scrollDownButton.setY(y + getHeight() - 20);
        
        // Enable/disable scroll buttons based on scroll position
        scrollUpButton.active = scrollOffset > 0;
        scrollDownButton.active = scrollOffset < Math.max(0, historyEntries.size() - MAX_VISIBLE_ITEMS);
        
        // Draw background for the list
        int listHeight = getHeight();
        guiGraphics.fill(x, y, x + width, y + listHeight, 0x80000000); // Semi-transparent background
        
        // Draw the title with a divider
        guiGraphics.drawString(Minecraft.getInstance().font, "Tourist Visits", x + 5, y + 5, 0xFFFFFF);
        guiGraphics.fill(x + 5, y + 18, x + width - 5, y + 19, 0x80FFFFFF); // Divider line
        
        // Check if we have any history
        if (historyEntries.isEmpty()) {
            // Draw a message when no history is available
            guiGraphics.drawString(Minecraft.getInstance().font, 
                "No tourists have visited yet", x + 20, y + 40, 0xAAAAAA);
            return;
        }
        
        // Draw the history list
        int yOffset = y + 24; // Start below the title and divider
        int count = 0;
        for (int i = scrollOffset; i < historyEntries.size() && count < MAX_VISIBLE_ITEMS; i++) {
            VisitEntry entry = historyEntries.get(i);
            
            // Draw row background with alternating colors
            int rowColor = count % 2 == 0 ? 0x30FFFFFF : 0x20FFFFFF;
            guiGraphics.fill(x + 5, yOffset, x + width - 37, yOffset + ITEM_HEIGHT, rowColor);
            
            // Format all information in a single line
            String timeText = formatTime(entry.getTimestamp()) + ": ";
            String townText = entry.getTownName() + " x" + entry.getCount();
            String directionText = getDirectionText(entry);
            if (!directionText.isEmpty()) {
                directionText = " (" + directionText + ")";
            }
            
            // Draw the combined information on a single line
            guiGraphics.drawString(Minecraft.getInstance().font, 
                timeText + townText + directionText, 
                x + 8, yOffset + 6, 0xFFFFFF);
            
            yOffset += ITEM_HEIGHT;
            count++;
        }
    }
    
    private String getDirectionText(VisitEntry entry) {
        if (entry.getOriginPos() == null || entry.getOriginPos().equals(BlockPos.ZERO)) {
            return "";
        }
        
        // Get player position as reference
        BlockPos playerPos = Minecraft.getInstance().player.blockPosition();
        BlockPos originPos = entry.getOriginPos();
        
        // Calculate direction
        String direction = "";
        if (originPos.getZ() < playerPos.getZ()) direction += "N";
        else if (originPos.getZ() > playerPos.getZ()) direction += "S";
        
        if (originPos.getX() > playerPos.getX()) direction += "E";
        else if (originPos.getX() < playerPos.getX()) direction += "W";
        
        // Calculate distance
        int distance = (int) Math.sqrt(playerPos.distSqr(originPos));
        
        return direction + " " + distance + "m";
    }
    
    private String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    @Override
    public void tick() {
        // Update the list each tick to ensure it's current
        updateHistoryList();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return 20 + (MAX_VISIBLE_ITEMS * ITEM_HEIGHT);
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (scrollUpButton != null) scrollUpButton.visible = visible;
        if (scrollDownButton != null) scrollDownButton.visible = visible;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }
    
    @Override
    public int getX() {
        return x;
    }
    
    @Override
    public int getY() {
        return y;
    }
    
    private void scrollUp() {
        scrollOffset = Math.max(0, scrollOffset - SCROLL_AMOUNT);
    }
    
    private void scrollDown() {
        scrollOffset = Math.min(
            Math.max(0, historyEntries.size() - MAX_VISIBLE_ITEMS), 
            scrollOffset + SCROLL_AMOUNT
        );
    }
    
    private void updateHistoryList() {
        // Get the current history entries
        List<VisitEntry> entries = historySupplier.get();
        
        // Update our list
        historyEntries = new ArrayList<>(entries);
    }
    
    // Add mouse wheel scrolling support
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (isMouseOver(mouseX, mouseY)) {
            if (scrollAmount > 0) {
                scrollUp();
                return true;
            } else if (scrollAmount < 0) {
                scrollDown();
                return true;
            }
        }
        return false;
    }
    
    // Check if mouse is over the component
    private boolean isMouseOver(double mouseX, double mouseY) {
        // Use stored render position for more accurate hit testing
        return mouseX >= renderX && mouseX <= renderX + width && 
               mouseY >= renderY && mouseY <= renderY + getHeight();
    }
    
    /**
     * Record to store visit information for display
     */
    public static class VisitEntry {
        private final long timestamp;
        private final String townName;
        private final int count;
        private final BlockPos originPos;
        
        public VisitEntry(long timestamp, String townName, int count, BlockPos originPos) {
            this.timestamp = timestamp;
            this.townName = townName;
            this.count = count;
            this.originPos = originPos;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public String getTownName() {
            return townName;
        }
        
        public int getCount() {
            return count;
        }
        
        public BlockPos getOriginPos() {
            return originPos;
        }
    }
} 