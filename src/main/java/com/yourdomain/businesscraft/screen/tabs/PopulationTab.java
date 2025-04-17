package com.yourdomain.businesscraft.screen.tabs;

import com.yourdomain.businesscraft.screen.components.BCComponent;
import com.yourdomain.businesscraft.screen.components.BCComponentFactory;
import com.yourdomain.businesscraft.screen.components.BCFlowLayout;
import com.yourdomain.businesscraft.screen.components.BCLabel;
import com.yourdomain.businesscraft.screen.components.BCPanel;
import com.yourdomain.businesscraft.screen.TownInterfaceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Handles the creation and management of the Population tab in the Town Interface Screen.
 */
public class PopulationTab {
    private final TownInterfaceScreen screen;
    private final BCPanel panel;
    private final int textHighlight;
    private final int textColor;
    private final int backgroundColor;
    private final int borderColor;

    public PopulationTab(TownInterfaceScreen screen, BCPanel panel, int textHighlight, int textColor, int backgroundColor, int borderColor) {
        this.screen = screen;
        this.panel = panel;
        this.textHighlight = textHighlight;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
    }

    public void create() {
        // Add title
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("POPULATION", panel.getInnerWidth());
        titleLabel.withTextColor(textHighlight).withShadow(true);
        panel.addChild(titleLabel);

        // Sample citizen data (expanded)
        String[] names = {"John Smith", "Emma Johnson", "Alex Lee", "Sofia Garcia", 
                         "Michael Brown", "Lisa Wang", 
                         "Michael Brown", "Lisa Wang", 
                         "Michael Brown", "Lisa Wang"};
        String[] jobs = {"Miner", "Farmer", "Builder", "Trader", 
                        "Blacksmith", "Scholar", "Blacksmith", "Scholar", "Blacksmith", "Scholar"};
        int[] levels = {3, 2, 4, 1, 5, 2, 3, 4, 2, 5};

        // Calculate dimensions for the citizens grid - ensure it fits within panel boundaries
        // Account for the panel's padding and the title height
        int titleHeight = 20; // Approximate height of the header
        int verticalSpacing = 10; // Space between title and grid

        // Calculate available space for the grid
        int availableWidth = panel.getInnerWidth();
        int availableHeight = panel.getInnerHeight() - titleHeight - verticalSpacing;

        // Create a direct implementation for the citizen list with scrolling
        BCComponent citizenList = new BCComponent(availableWidth, availableHeight) {
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
                guiGraphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor);

                // Draw border
                guiGraphics.hLine(this.x, this.x + this.width - 1, this.y, borderColor);
                guiGraphics.hLine(this.x, this.x + this.width - 1, this.y + this.height - 1, borderColor);
                guiGraphics.vLine(this.x, this.y, this.y + this.height - 1, borderColor);
                guiGraphics.vLine(this.x + this.width - 1, this.y, this.y + this.height - 1, borderColor);

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
                        textHighlight
                    );

                    // Draw job (second column)
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        jobs[dataIndex],
                        this.x + padding + nameWidth,
                        rowY + 3, // Slight vertical centering
                        textColor
                    );

                    // Draw level (third column)
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        "Level " + levels[dataIndex],
                        this.x + padding + nameWidth + jobWidth,
                        rowY + 3, // Slight vertical centering
                        textColor
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
                            playButtonClickSound();
                            sendChatMessage("Selected citizen: " + names[clickedItem] + " (" + jobs[clickedItem] + ")");
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double dragDeltaY) {
                // Left mouse button dragging (scrollbar)
                if (isDraggingScrollbar && button == 0) {
                    // Calculate new scrollbar position based on drag
                    double dragDistanceY = dragDeltaY;
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

                return super.mouseDragged(mouseX, mouseY, button, deltaX, dragDeltaY);
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

            private boolean isMouseOver(int mouseX, int mouseY) {
                return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
            }

            private void playButtonClickSound() {
                Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F)
                );
            }

            private void sendChatMessage(String message) {
                net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), false);
                }
            }
        };

        // Add the citizen list to the panel
        panel.addChild(citizenList);
    }

    public BCPanel getPanel() {
        return panel;
    }
} 