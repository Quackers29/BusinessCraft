package com.yourdomain.businesscraft.screen.tabs;

import com.yourdomain.businesscraft.screen.TownInterfaceScreen;
import com.yourdomain.businesscraft.screen.TownInterfaceTheme;
import com.yourdomain.businesscraft.screen.components.BCComponent;
import com.yourdomain.businesscraft.screen.components.BCFlowLayout;
import com.yourdomain.businesscraft.screen.components.BCLabel;
import com.yourdomain.businesscraft.screen.components.BCComponentFactory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Population tab implementation for the Town Interface.
 * Displays citizens and their information with scrolling support.
 */
public class PopulationTab extends BaseTownTab {
    private static final Logger LOGGER = LoggerFactory.getLogger(PopulationTab.class);
    private static final int TEXT_COLOR = TownInterfaceTheme.TEXT_COLOR;
    private static final int TEXT_HIGHLIGHT = TownInterfaceTheme.TEXT_HIGHLIGHT;
    // Use centralized theme constants
    private static final int BACKGROUND_COLOR = TownInterfaceTheme.BACKGROUND_COLOR;
    private static final int BORDER_COLOR = TownInterfaceTheme.BORDER_COLOR;
    
    // Sample citizen data (would be replaced with real data in production)
    private static final String[] names = {"John Smith", "Emma Johnson", "Alex Lee", "Sofia Garcia", 
                         "Michael Brown", "Lisa Wang", 
                         "Michael Brown", "Lisa Wang", 
                         "Michael Brown", "Lisa Wang"};
    private static final String[] jobs = {"Miner", "Farmer", "Builder", "Trader", 
                        "Blacksmith", "Scholar","Blacksmith", "Scholar","Blacksmith", "Scholar"};
    private static final int[] levels = {3, 2, 4, 1, 5, 2, 3, 4, 2, 5};
    
    private BCComponent citizenList;
    
    /**
     * Creates a new Population tab.
     * 
     * @param parentScreen The parent screen
     * @param width The width of the tab panel
     * @param height The height of the tab panel
     */
    public PopulationTab(TownInterfaceScreen parentScreen, int width, int height) {
        super(parentScreen, width, height);
        
        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
    }
    
    @Override
    public void init(Consumer<Button> registerWidget) {
        // Add title
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("POPULATION", panel.getInnerWidth());
        titleLabel.withTextColor(TEXT_HIGHLIGHT).withShadow(true);
        panel.addChild(titleLabel);
        
        // Calculate dimensions for the citizens grid - ensure it fits within panel boundaries
        // Account for the panel's padding and the title height
        int titleHeight = 20; // Approximate height of the header
        int verticalSpacing = 10; // Space between title and grid
        
        // Calculate available space for the grid
        int availableWidth = panel.getInnerWidth();
        int availableHeight = panel.getInnerHeight() - titleHeight - verticalSpacing;
        
        // Create a direct implementation for the citizen list with scrolling
        citizenList = new BCComponent(availableWidth, availableHeight) {
            // Scrolling state
            private int scrollOffset = 0;
            private final int itemHeight = 16; // Reduced item height for more compact display
            private final int padding = 8; // Reduced padding for more content space
            private final int verticalPadding = 5; // Even smaller vertical padding
            private int maxVisible; // Will be calculated based on available height
            private boolean isDraggingScrollbar = false;
            private boolean isMiddleMouseScrolling = false; // Track middle mouse scrolling
            private double lastMouseY = 0; // Last mouse Y position for middle mouse scrolling
            private final int scrollbarWidth = 8;
            
            @Override
            protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
                // Calculate max visible items based on available height
                int contentHeight = this.height - (verticalPadding * 2);
                maxVisible = contentHeight / itemHeight;
                
                // Draw the background panel
                guiGraphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, BACKGROUND_COLOR);
                
                // Draw border
                guiGraphics.hLine(this.x, this.x + this.width - 1, this.y, BORDER_COLOR);
                guiGraphics.hLine(this.x, this.x + this.width - 1, this.y + this.height - 1, BORDER_COLOR);
                guiGraphics.vLine(this.x, this.y, this.y + this.height - 1, BORDER_COLOR);
                guiGraphics.vLine(this.x + this.width - 1, this.y, this.y + this.height - 1, BORDER_COLOR);
                
                // Calculate column widths (3 columns: name, job, level)
                int contentWidth = this.width - scrollbarWidth - (padding * 2);
                int nameWidth = (int)(contentWidth * 0.4);
                int jobWidth = (int)(contentWidth * 0.35);
                int levelWidth = (int)(contentWidth * 0.25);
                
                // Calculate max scroll offset
                int maxScrollOffset = Math.max(0, names.length - maxVisible);
                if (scrollOffset > maxScrollOffset) {
                    scrollOffset = maxScrollOffset;
                }
                
                // Draw the scrollbar if needed
                if (names.length > maxVisible) {
                    // Draw scrollbar track
                    int trackHeight = this.height - (verticalPadding * 2);
                    guiGraphics.fill(
                        this.x + this.width - scrollbarWidth - padding,
                        this.y + verticalPadding,
                        this.x + this.width - padding,
                        this.y + this.height - verticalPadding,
                        0x40FFFFFF // Light gray semi-transparent
                    );
                    
                    // Draw scrollbar thumb
                    float thumbRatio = (float)maxVisible / names.length;
                    int thumbHeight = Math.max(20, (int)(trackHeight * thumbRatio));
                    int thumbY = this.y + verticalPadding + (int)((trackHeight - thumbHeight) * ((float)scrollOffset / maxScrollOffset));
                    
                    // Highlight if mouse is over
                    boolean isOverScrollbar = mouseX >= this.x + this.width - scrollbarWidth - padding &&
                                            mouseX <= this.x + this.width - padding &&
                                            mouseY >= this.y + verticalPadding &&
                                            mouseY <= this.y + this.height - verticalPadding;
                    
                    guiGraphics.fill(
                        this.x + this.width - scrollbarWidth - padding,
                        thumbY,
                        this.x + this.width - padding,
                        thumbY + thumbHeight,
                        isOverScrollbar ? 0xFFCCDDFF : 0xA0CCDDFF // Light blue with variable opacity
                    );
                }
                
                // Draw only visible citizens
                int startY = this.y + verticalPadding;
                for (int i = 0; i < Math.min(maxVisible, names.length - scrollOffset); i++) {
                    int dataIndex = i + scrollOffset;
                    int rowY = startY + (i * itemHeight);
                    
                    if (rowY + itemHeight > this.y + this.height - verticalPadding) {
                        break;
                    }
                    
                    // Draw name (first column)
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        names[dataIndex],
                        this.x + padding,
                        rowY + 3, // Slight vertical centering
                        TEXT_HIGHLIGHT
                    );
                    
                    // Draw job (second column)
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        jobs[dataIndex],
                        this.x + padding + nameWidth,
                        rowY + 3, // Slight vertical centering
                        TEXT_COLOR
                    );
                    
                    // Draw level (third column)
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        "Level " + levels[dataIndex],
                        this.x + padding + nameWidth + jobWidth,
                        rowY + 3, // Slight vertical centering
                        TEXT_COLOR
                    );
                }
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (isMouseOver((int)mouseX, (int)mouseY)) {
                    // Calculate max visible items based on available height
                    int contentHeight = this.height - (verticalPadding * 2);
                    maxVisible = contentHeight / itemHeight;
                    
                    // Middle mouse button for scrolling
                    if (button == 2) { // Middle mouse button
                        isMiddleMouseScrolling = true;
                        lastMouseY = mouseY;
                        return true;
                    }
                    
                    // Left mouse button
                    if (button == 0) {
                        // Check if clicking on scrollbar
                        if (names.length > maxVisible && 
                            mouseX >= this.x + this.width - scrollbarWidth - padding &&
                            mouseX <= this.x + this.width - padding &&
                            mouseY >= this.y + verticalPadding &&
                            mouseY <= this.y + this.height - verticalPadding) {
                            
                            isDraggingScrollbar = true;
                            
                            // Calculate new scroll position
                            int trackHeight = this.height - (verticalPadding * 2);
                            float relativeY = (float)(mouseY - (this.y + verticalPadding)) / trackHeight;
                            int maxScrollOffset = Math.max(0, names.length - maxVisible);
                            scrollOffset = (int)(relativeY * maxScrollOffset);
                            
                            // Clamp scroll offset
                            if (scrollOffset < 0) {
                                scrollOffset = 0;
                            } else if (scrollOffset > maxScrollOffset) {
                                scrollOffset = maxScrollOffset;
                            }
                            
                            return true;
                        }
                        
                        // Check if clicking on a citizen
                        int clickedItem = -1;
                        int startY = this.y + verticalPadding;
                        for (int i = 0; i < Math.min(maxVisible, names.length - scrollOffset); i++) {
                            int dataIndex = i + scrollOffset;
                            int rowY = startY + (i * itemHeight);
                            
                            if (mouseY >= rowY && mouseY < rowY + itemHeight) {
                                clickedItem = dataIndex;
                                break;
                            }
                        }
                        
                        if (clickedItem != -1 && clickedItem < names.length) {
                            // Handle citizen click
                            parentScreen.playButtonClickSound();
                            parentScreen.sendChatMessage("Selected citizen: " + names[clickedItem] + " (" + jobs[clickedItem] + ")");
                            return true;
                        }
                    }
                }
                return false;
            }
            
            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                // Left mouse button dragging (scrollbar)
                if (isDraggingScrollbar && button == 0) {
                    // Calculate max visible items based on available height
                    int contentHeight = this.height - (verticalPadding * 2);
                    maxVisible = contentHeight / itemHeight;
                    
                    // Calculate new scroll position based on drag
                    int trackHeight = this.height - (verticalPadding * 2);
                    float relativeY = (float)(mouseY - (this.y + verticalPadding)) / trackHeight;
                    int maxScrollOffset = Math.max(0, names.length - maxVisible);
                    scrollOffset = (int)(relativeY * maxScrollOffset);
                    
                    // Clamp scroll offset
                    if (scrollOffset < 0) {
                        scrollOffset = 0;
                    } else if (scrollOffset > maxScrollOffset) {
                        scrollOffset = maxScrollOffset;
                    }
                    
                    return true;
                }
                
                // Middle mouse button dragging (direct scrolling)
                if (isMiddleMouseScrolling && button == 2) {
                    // Calculate max visible items based on available height
                    int contentHeight = this.height - (verticalPadding * 2);
                    maxVisible = contentHeight / itemHeight;
                    
                    // Calculate scroll amount based on mouse movement
                    double deltaY = mouseY - lastMouseY;
                    lastMouseY = mouseY;
                    
                    // Convert mouse movement to scroll amount (scale factor)
                    // Positive deltaY means dragging down, which should move content up (scroll down)
                    int scrollAmount = (int)(deltaY * 0.5);
                    
                    // Apply scrolling
                    int maxScrollOffset = Math.max(0, names.length - maxVisible);
                    scrollOffset += scrollAmount;
                    
                    // Clamp scroll offset
                    if (scrollOffset < 0) {
                        scrollOffset = 0;
                    } else if (scrollOffset > maxScrollOffset) {
                        scrollOffset = maxScrollOffset;
                    }
                    
                    // Middle mouse scrolling active
                    return true;
                }
                
                return false;
            }
            
            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                if (isDraggingScrollbar && button == 0) {
                    isDraggingScrollbar = false;
                    return true;
                }
                
                if (isMiddleMouseScrolling && button == 2) {
                    isMiddleMouseScrolling = false;
                    return true;
                }
                
                return false;
            }
            
            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
                // Calculate max visible items and offset
                int contentHeight = this.height - (verticalPadding * 2);
                maxVisible = contentHeight / itemHeight;
                int maxScrollOffset = Math.max(0, names.length - maxVisible);
                
                // Apply scrolling directly based on delta sign (delta > 0 means scroll up)
                scrollOffset -= (int)Math.signum(delta);
                
                // Clamp scroll position
                if (scrollOffset < 0) {
                    scrollOffset = 0;
                }
                if (scrollOffset > maxScrollOffset) {
                    scrollOffset = maxScrollOffset;
                }
                
                return true;
            }
        };
        
        // Add the citizen list to the panel
        panel.addChild(citizenList);
    }
    
    @Override
    public void update() {
        // This tab doesn't need periodic updates unless citizen data changes
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Always forward scroll events to citizenList
        if (citizenList != null) {
            // Log for debugging
            LOGGER.debug("PopulationTab forwarding scroll: {}", delta);
            
            // Try with original coordinates first
            if (citizenList.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
            
            // If that didn't work, try with slight position adjustments
            for (int i = 0; i < 9; i++) {
                double adjustedX = mouseX + (i % 3 - 1) * 5; // Try offsets of -5, 0, +5
                double adjustedY = mouseY + (i / 3 - 1) * 5;
                if (citizenList.mouseScrolled(adjustedX, adjustedY, delta)) {
                    LOGGER.debug("PopulationTab scroll succeeded with adjustment {}", i);
                    return true;
                }
            }
        }
        return false;
    }
} 