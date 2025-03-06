package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Tab panel component for BusinessCraft UI system.
 * Provides a tabbed interface with content panels.
 */
public class BCTabPanel extends BCPanel {
    private final List<TabInfo> tabs = new ArrayList<>();
    private final Map<String, BCPanel> tabPanels = new HashMap<>();
    private final int tabHeight;
    private String activeTabId = null;
    private int tabButtonWidth;
    
    /**
     * Create a new tab panel with the specified dimensions
     */
    public BCTabPanel(int width, int height, int tabHeight) {
        super(width, height);
        this.tabHeight = tabHeight;
    }
    
    /**
     * Add a tab to the panel
     */
    public BCTabPanel addTab(String id, Component title, BCPanel contentPanel) {
        // Create tab button
        BCButton tabButton = new BCButton(title, b -> setActiveTab(id), tabButtonWidth, tabHeight);
        
        // Add tab info
        tabs.add(new TabInfo(id, title, tabButton));
        tabPanels.put(id, contentPanel);
        
        // Set first tab as active by default
        if (activeTabId == null) {
            activeTabId = id;
        }
        
        return this;
    }
    
    /**
     * Set the active tab
     */
    public void setActiveTab(String tabId) {
        if (!tabPanels.containsKey(tabId)) {
            return;
        }
        
        // Hide all tab content first
        for (String key : tabPanels.keySet()) {
            tabPanels.get(key).setVisible(false);
        }
        
        // Show new tab content
        activeTabId = tabId;
        BCPanel contentPanel = tabPanels.get(activeTabId);
        contentPanel.setVisible(true);
        
        // Ensure the content panel is positioned correctly
        contentPanel.position(0, tabHeight);
    }
    
    /**
     * Get the active tab ID
     */
    public String getActiveTabId() {
        return activeTabId;
    }
    
    /**
     * Get the active tab panel
     */
    public BCPanel getActiveTabPanel() {
        return tabPanels.get(activeTabId);
    }
    
    /**
     * Render the tab panel and its active content
     */
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (tabs.isEmpty()) return;
        
        // Calculate tab width to evenly distribute across the panel width
        int totalTabWidth = width - 4;
        int tabWidth = totalTabWidth / tabs.size();
        this.tabButtonWidth = tabWidth;
        
        // Render content area background FIRST (lowest layer)
        int contentY = y + tabHeight;
        int contentHeight = height - tabHeight;
        
        // Draw a semi-transparent background for the content area
        guiGraphics.fill(x, contentY, x + width, contentY + contentHeight, 0x60000000);
        
        // Render border if needed
        if (borderColor != -1) {
            // Top and bottom
            guiGraphics.hLine(x, x + width - 1, contentY, borderColor);
            guiGraphics.hLine(x, x + width - 1, contentY + contentHeight - 1, borderColor);
            // Left and right
            guiGraphics.vLine(x, contentY, contentY + contentHeight - 1, borderColor);
            guiGraphics.vLine(x + width - 1, contentY, contentY + contentHeight - 1, borderColor);
        }
        
        // Then render active tab content BEFORE tab buttons
        if (activeTabId != null) {
            BCPanel contentPanel = tabPanels.get(activeTabId);
            if (contentPanel != null && contentPanel.isVisible()) {
                // Make sure the panel renders at the correct position
                contentPanel.position(0, tabHeight);
                // Calculate the actual position based on the tab panel's position
                int contentX = x;
                contentY = y + tabHeight;
                // Render the content panel
                contentPanel.render(guiGraphics, contentX, contentY, mouseX, mouseY);
            }
        }
        
        // Render tab buttons LAST (top layer)
        int tabX = x + 2;
        for (TabInfo tab : tabs) {
            // Set button width
            tab.button.size(tabWidth, tabHeight);
            
            // Highlight active tab with a different color
            if (tab.id.equals(activeTabId)) {
                guiGraphics.fill(tabX, y, tabX + tabButtonWidth, y + tabHeight, 0x80FFFFFF);
            }
            
            // Render tab button
            tab.button.render(guiGraphics, tabX, y, mouseX, mouseY);
            tabX += tabButtonWidth;
        }
    }
    
    @Override
    public void init(Consumer<Button> register) {
        // Register tab buttons
        for (TabInfo tab : tabs) {
            tab.button.init(register);
        }
        
        // Initialize all tab panels
        for (BCPanel panel : tabPanels.values()) {
            panel.init(register);
            
            // Hide all panels except the active one
            panel.setVisible(panel == tabPanels.get(activeTabId));
        }
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        
        // When showing the panel, only show the active tab content
        if (visible && activeTabId != null) {
            for (Map.Entry<String, BCPanel> entry : tabPanels.entrySet()) {
                entry.getValue().setVisible(entry.getKey().equals(activeTabId));
            }
        }
    }
    
    /**
     * Information about a tab
     */
    private static class TabInfo {
        final String id;
        final Component title;
        final BCButton button;
        
        TabInfo(String id, Component title, BCButton button) {
            this.id = id;
            this.title = title;
            this.button = button;
        }
    }
} 