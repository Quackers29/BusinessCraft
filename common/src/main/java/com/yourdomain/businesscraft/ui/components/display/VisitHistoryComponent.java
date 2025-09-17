package com.yourdomain.businesscraft.ui.components.display;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
 * Refactored to use BCScrollableListComponent base class
 */
public class VisitHistoryComponent extends BCScrollableListComponent<VisitHistoryComponent.VisitEntry> {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisitHistoryComponent.class);
    private static final int ITEM_HEIGHT = 20;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("MM/dd HH:mm");
    
    private final Supplier<List<VisitEntry>> historySupplier;

    public VisitHistoryComponent(Supplier<List<VisitEntry>> historySupplier, int width) {
        super(width, 
              ITEM_HEIGHT * 6 + 25, // Display area height (6 items plus title)
              ITEM_HEIGHT, 
              entry -> formatVisitText(entry));
        
        this.historySupplier = historySupplier;
        
        // Style customization
        withBorderColor(0x80FFFFFF);
        withBackgroundColor(0x80000000);
        withItemBackgroundColor(0x20FFFFFF);
        withHoveredItemBackgroundColor(0x40FFFFFF);
        withSelectedItemBackgroundColor(0x60FFFFFF);
        withItemSpacing(1);
        withCornerRadius(4);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Draw the title with a divider
        guiGraphics.drawString(Minecraft.getInstance().font, "Tourist Visits", x + 5, y + 5, 0xFFFFFF);
        guiGraphics.fill(x + 5, y + 18, x + width - 5, y + 19, 0x80FFFFFF); // Divider line
        
        // Update the history list
        updateHistoryList();
        
        // Render the scrollable content below the title
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 25, 0); // Move down past the title
        super.renderContent(guiGraphics, mouseX, mouseY);
        guiGraphics.pose().popPose();
    }
    
    private static String formatVisitText(VisitEntry entry) {
        StringBuilder sb = new StringBuilder();
        
        // Format: [Time] VisitorName (count: N)
        sb.append(TIME_FORMAT.format(new Date(entry.getTimestamp())));
        sb.append(" - ");
        sb.append(entry.getVisitorName());
        
        if (entry.getCount() > 1) {
            sb.append(" (x");
            sb.append(entry.getCount());
            sb.append(")");
        }
        
        return sb.toString();
    }
    
    private void updateHistoryList() {
        // Get the current history entries
        List<VisitEntry> entries = historySupplier.get();
        
        // Update our list
        setItems(new ArrayList<>(entries));
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Forward the drag event to the scrollable list implementation
        // Adjust mouse Y to account for the title area offset (25 pixels for the title)
        return super.mouseDragged(mouseX, mouseY - 25, button, dragX, dragY);
    }
    
    /**
     * Record to store visit information for display
     */
    public static class VisitEntry {
        private final String visitorName;
        private final long timestamp;
        private final int count;
        private final String direction;
        private final BlockPos position;
        
        public VisitEntry(String visitorName, long timestamp, int count, String direction, BlockPos position) {
            this.visitorName = visitorName;
            this.timestamp = timestamp;
            this.count = count;
            this.direction = direction;
            this.position = position;
        }
        
        public String getVisitorName() {
            return visitorName;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public int getCount() {
            return count;
        }
        
        public String getDirection() {
            return direction;
        }
        
        public BlockPos getPosition() {
            return position;
        }
    }
} 