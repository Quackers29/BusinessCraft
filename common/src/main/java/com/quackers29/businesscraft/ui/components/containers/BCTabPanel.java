package com.quackers29.businesscraft.ui.components.containers;

import com.quackers29.businesscraft.ui.components.basic.BCComponent;
import com.quackers29.businesscraft.ui.components.basic.BCPanel;
import com.quackers29.businesscraft.ui.components.basic.BCButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

/**
 * Enhanced tab panel implementation for BusinessCraft UI.
 * Uses direct rendering with matrix transformations for reliable display of tab content.
 */
public class BCTabPanel extends BCComponent {
    private final List<TabButton> tabButtons = new ArrayList<>();
    private final Map<String, BCPanel> contentPanels = new HashMap<>();
    private final int tabHeight;
    private String activeTabId = null;
    
    // Style properties with lighter default colors
    private int activeTabColor = 0xA0CCDDFF;      // Lighter blue for active tab
    private int inactiveTabColor = 0x60555555;    // Semi-transparent gray for inactive tabs
    private int contentBackgroundColor = 0x80222222; // Semi-transparent dark background for content
    private int contentBorderColor = 0xA0AAAAAA;  // Light gray border
    private int tabTextColor = 0xFFFFFFFF;        // White text for tab buttons
    private boolean showBorders = true;           // Whether to draw borders
    private int tabSpacing = 2;                   // Space between tabs
    
    /**
     * Creates a new tab panel with the specified dimensions
     */
    public BCTabPanel(int width, int height, int tabHeight) {
        super(width, height);
        this.tabHeight = tabHeight;
    }
    
    /**
     * Set the tab appearance
     * @param activeColor Background color for the active tab
     * @param inactiveColor Background color for inactive tabs
     * @param textColor Color for tab text
     */
    public BCTabPanel withTabStyle(int activeColor, int inactiveColor, int textColor) {
        this.activeTabColor = activeColor;
        this.inactiveTabColor = inactiveColor;
        this.tabTextColor = textColor;
        
        // Update existing tab buttons
        for (TabButton tab : tabButtons) {
            tab.button.withTextColor(textColor);
        }
        return this;
    }
    
    /**
     * Set the content area appearance
     * @param backgroundColor Background color for content area
     * @param borderColor Border color for content area
     */
    public BCTabPanel withContentStyle(int backgroundColor, int borderColor) {
        this.contentBackgroundColor = backgroundColor;
        this.contentBorderColor = borderColor;
        return this;
    }
    
    /**
     * Set whether to show borders
     */
    public BCTabPanel withTabBorder(boolean show, int borderColor) {
        this.showBorders = show;
        this.contentBorderColor = borderColor;
        return this;
    }
    
    /**
     * Add a tab to the panel
     * @param id Unique identifier for the tab
     * @param title Title displayed on the tab
     * @param contentPanel Panel containing the tab's content
     */
    public BCTabPanel addTab(String id, Component title, BCPanel contentPanel) {
        // Create a button for this tab
        int buttonWidth = 60; // Default width, will be recalculated
        BCButton button = new BCButton(title, b -> setActiveTab(id), buttonWidth, tabHeight);
        button.withTextColor(tabTextColor);
        button.withShadow(true);
        
        // Store tab info
        tabButtons.add(new TabButton(id, button));
        contentPanels.put(id, contentPanel);
        
        // Set first tab as active by default
        if (activeTabId == null) {
            activeTabId = id;
        }
        
        // Recalculate tab button widths
        recalculateTabWidths();
        
        return this;
    }
    
    /**
     * Recalculate tab button widths to fit the panel width
     */
    private void recalculateTabWidths() {
        if (tabButtons.isEmpty()) return;
        
        // Calculate width for each tab with spacing
        int totalTabSpace = width - (tabButtons.size() + 1) * tabSpacing;
        int buttonWidth = Math.min(120, totalTabSpace / tabButtons.size());
        
        // Resize all tab buttons
        for (TabButton tab : tabButtons) {
            tab.button.size(buttonWidth, tabHeight);
        }
    }
    
    /**
     * Switch to a different tab
     */
    public void setActiveTab(String tabId) {
        if (!contentPanels.containsKey(tabId) || tabId.equals(activeTabId)) {
            return;
        }
        
        // Update active tab
        String previousTabId = activeTabId;
        activeTabId = tabId;
        
        // Trigger tab change event
        triggerEvent("tabChange");
    }
    
    /**
     * Get the active tab ID
     */
    public String getActiveTabId() {
        return activeTabId;
    }
    
    /**
     * Get the active content panel
     */
    public BCPanel getActiveTabPanel() {
        return contentPanels.get(activeTabId);
    }
    
    /**
     * Get a tab panel by ID
     */
    public BCPanel getTabPanel(String tabId) {
        return contentPanels.get(tabId);
    }
    
    /**
     * Check if the panel has a tab with the given ID
     */
    public boolean hasTab(String tabId) {
        return contentPanels.containsKey(tabId);
    }
    
    /**
     * Get the number of tabs
     */
    public int getTabCount() {
        return tabButtons.size();
    }
    
    /**
     * Render the tab panel and its content
     */
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (tabButtons.isEmpty()) return;
        
        // 1. Draw the content area
        int contentX = x;
        int contentY = y + tabHeight;
        int contentWidth = width;
        int contentHeight = height - tabHeight;
        
        // Draw the content area background
        guiGraphics.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, contentBackgroundColor);
        
        // Draw content area border if enabled
        if (showBorders) {
            drawBorder(guiGraphics, contentX, contentY, contentWidth, contentHeight, contentBorderColor, 1);
        }
        
        // 2. Draw the tabs at the top
        renderTabs(guiGraphics, mouseX, mouseY);
        
        // 3. Draw the content if there is an active tab
        if (activeTabId != null && contentPanels.containsKey(activeTabId)) {
            BCPanel contentPanel = contentPanels.get(activeTabId);
            
            // Create render state - use pushPose to isolate this rendering
            guiGraphics.pose().pushPose();
            
            // Translate to the content area
            guiGraphics.pose().translate(contentX, contentY, 0);
            
            // Render the content panel with adjusted mouse coordinates
            contentPanel.render(guiGraphics, 0, 0, mouseX - contentX, mouseY - contentY);
            
            // Restore the rendering state
            guiGraphics.pose().popPose();
        }
    }
    
    /**
     * Draw tabs at the top of the panel
     */
    private void renderTabs(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int tabX = x + tabSpacing;
        
        for (TabButton tab : tabButtons) {
            // Get button width
            int tabWidth = tab.button.getWidth();
            
            // Determine if this is the active tab
            boolean isActiveTab = tab.id.equals(activeTabId);
            int bgColor = isActiveTab ? activeTabColor : inactiveTabColor;
            
            // Draw tab background with slight rounding at top corners
            if (isActiveTab) {
                // Draw active tab slightly taller to connect with content
                guiGraphics.fill(tabX, y, tabX + tabWidth, y + tabHeight + 1, bgColor);
                
                // Draw a lighter highlight at the top of the active tab
                guiGraphics.fill(tabX, y, tabX + tabWidth, y + 1, 0xFFFFFFFF);
            } else {
                // Draw inactive tab with standard height and darker
                guiGraphics.fill(tabX, y + 1, tabX + tabWidth, y + tabHeight, bgColor);
            }
            
            // Draw tab border if enabled
            if (showBorders && !isActiveTab) {
                drawBorder(guiGraphics, tabX, y + 1, tabWidth, tabHeight - 1, contentBorderColor, 1);
            }
            
            // Render the tab button
            tab.button.render(guiGraphics, tabX, y + (isActiveTab ? 0 : 1), mouseX, mouseY);
            
            tabX += tabWidth + tabSpacing;
        }
    }
    
    /**
     * Helper method to draw a border around a rectangle
     */
    private void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color, int thickness) {
        // Top border
        guiGraphics.fill(x, y, x + width, y + thickness, color);
        // Bottom border
        guiGraphics.fill(x, y + height - thickness, x + width, y + height, color);
        // Left border
        guiGraphics.fill(x, y + thickness, x + thickness, y + height - thickness, color);
        // Right border
        guiGraphics.fill(x + width - thickness, y + thickness, x + width, y + height - thickness, color);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check for tab button clicks
        if (mouseY >= y && mouseY < y + tabHeight) {
            int tabX = x + tabSpacing;
            for (TabButton tab : tabButtons) {
                int tabWidth = tab.button.getWidth();
                if (mouseX >= tabX && mouseX < tabX + tabWidth) {
                    // Tab button clicked - update active tab and notify
                    setActiveTab(tab.id);
                    tab.button.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
                tabX += tabWidth + tabSpacing;
            }
        }
        
        // Check for content panel clicks
        if (activeTabId != null && mouseY >= y + tabHeight && mouseY < y + height) {
            BCPanel contentPanel = contentPanels.get(activeTabId);
            if (contentPanel != null) {
                // Adjust mouse coordinates to the content panel's space
                double contentX = mouseX - x;
                double contentY = mouseY - (y + tabHeight);
                return contentPanel.mouseClicked(contentX, contentY, button);
            }
        }
        
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Forward to content panel if dragging in the content area
        if (activeTabId != null && mouseY >= y + tabHeight && mouseY < y + height) {
            BCPanel contentPanel = contentPanels.get(activeTabId);
            if (contentPanel != null) {
                // Adjust mouse coordinates to the content panel's space
                double contentX = mouseX - x;
                double contentY = mouseY - (y + tabHeight);
                return contentPanel.mouseDragged(contentX, contentY, button, dragX, dragY);
            }
        }
        
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Forward to content panel if releasing in the content area
        if (activeTabId != null && mouseY >= y + tabHeight && mouseY < y + height) {
            BCPanel contentPanel = contentPanels.get(activeTabId);
            if (contentPanel != null) {
                // Adjust mouse coordinates to the content panel's space
                double contentX = mouseX - x;
                double contentY = mouseY - (y + tabHeight);
                return contentPanel.mouseReleased(contentX, contentY, button);
            }
        }
        
        return false;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Forward to content panel if scrolling in the content area
        if (activeTabId != null && mouseY >= y + tabHeight && mouseY < y + height) {
            BCPanel contentPanel = contentPanels.get(activeTabId);
            if (contentPanel != null) {
                // Adjust mouse coordinates to the content panel's space
                double contentX = mouseX - x;
                double contentY = mouseY - (y + tabHeight);
                return contentPanel.mouseScrolled(contentX, contentY, delta);
            }
        }
        
        return false;
    }
    
    @Override
    public void init(Consumer<Button> register) {
        // Register all tab buttons
        for (TabButton tab : tabButtons) {
            tab.button.init(register);
        }
        
        // Initialize all content panels with proper size
        for (BCPanel panel : contentPanels.values()) {
            // Size each panel to fit the content area
            panel.size(width, height - tabHeight);
            panel.init(register);
        }
    }
    
    @Override
    public void tick() {
        // Tick the active content panel
        if (activeTabId != null) {
            BCPanel panel = contentPanels.get(activeTabId);
            if (panel != null) {
                panel.tick();
            }
        }
    }
    
    /**
     * Helper class to associate tab ID with button
     */
    private static class TabButton {
        final String id;
        final BCButton button;
        
        TabButton(String id, BCButton button) {
            this.id = id;
            this.button = button;
        }
    }
} 
