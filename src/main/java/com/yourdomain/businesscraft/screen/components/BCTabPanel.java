package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * A tab panel component for organizing content into tabs.
 */
public class BCTabPanel implements UIComponent {
    private int width;
    private int height;
    private final int tabHeight;
    private final List<TabButton> tabButtons = new ArrayList<>();
    private final Map<String, BCPanel> tabContents = new HashMap<>();
    private String activeTabId = null;
    private int x;
    private int y;
    private boolean visible = true;
    private BiFunction<String, String, Boolean> tabChangeListener = null;
    
    /**
     * Creates a new tab panel with the specified size
     */
    public BCTabPanel(int width, int height, int tabHeight) {
        this.width = width;
        this.height = height;
        this.tabHeight = tabHeight;
    }
    
    /**
     * Add a tab to the panel
     */
    public void addTab(String id, Component title, BCPanel content) {
        // Create a new tab button
        TabButton tabButton = new TabButton(
                0, 0, // Will be positioned later
                getTabWidth(title.getString()),
                tabHeight,
                title,
                button -> {
                    if (activeTabId != null && tabChangeListener != null) {
                        // If we have a listener, let it decide whether to change tabs immediately
                        if (!tabChangeListener.apply(activeTabId, id)) {
                            return; // Listener will handle tab change later
                        }
                    }
                    setActiveTab(id);
                }
        );
        
        tabButtons.add(tabButton);
        tabContents.put(id, content);
        
        // If this is the first tab, make it active
        if (activeTabId == null) {
            activeTabId = id;
        }
        
        // Position all tab buttons
        positionTabs();
    }
    
    /**
     * Set a listener to be called when tabs change
     * The listener should return true if the tab change should proceed immediately,
     * or false if it will handle the tab change itself (e.g., for animations)
     */
    public void setTabChangeListener(BiFunction<String, String, Boolean> listener) {
        this.tabChangeListener = listener;
    }
    
    /**
     * Set the active tab with animation
     */
    public void setActiveTab(String id) {
        if (activeTabId == null || !activeTabId.equals(id)) {
            // Change selected state of tab buttons
            for (TabButton tab : tabButtons) {
                tab.setSelected(tab.getTabId().equals(id));
            }
            
            // Set the active tab ID
            activeTabId = id;
        }
    }
    
    /**
     * Set the active tab without triggering animation
     * Used internally for animation completion
     */
    public void setActiveTabWithoutAnimation(String id) {
        // Change selected state of tab buttons
        for (TabButton tab : tabButtons) {
            tab.setSelected(tab.getTabId().equals(id));
        }
        
        // Set the active tab ID
        activeTabId = id;
    }
    
    /**
     * Get the active tab ID
     */
    public String getActiveTabId() {
        return activeTabId;
    }
    
    /**
     * Returns the width of the tab panel
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Returns the height of the tab panel
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Calculates the width for a tab with the given title
     */
    private int getTabWidth(String title) {
        return Math.max(80, title.length() * 7); // Simple calculation, adjust as needed
    }
    
    /**
     * Position all tab buttons
     */
    private void positionTabs() {
        int tabX = 0;
        
        for (TabButton tab : tabButtons) {
            tab.setRelativePosition(tabX, 0);
            tabX += tab.getWidth();
        }
    }
    
    // UIComponent implementation
    
    public void position(int x, int y) {
        this.x = x;
        this.y = y;
        
        // Update tab contents position
        for (BCPanel tabContent : tabContents.values()) {
            tabContent.position(x, y + tabHeight);
        }
    }
    
    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
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
    
    @Override
    public void init(Consumer<Button> widgetAdder) {
        if (!visible) return;
        
        // Add all tab buttons to the screen
        for (TabButton tab : tabButtons) {
            tab.setPosition(x + tab.getRelativeX(), y + tab.getRelativeY());
            widgetAdder.accept(tab);
        }
        
        // Initialize the active tab content
        if (activeTabId != null) {
            BCPanel activeContent = tabContents.get(activeTabId);
            if (activeContent != null) {
                activeContent.init(widgetAdder);
            }
        }
    }
    
    @Override
    public void tick() {
        // Tick the active tab content
        if (activeTabId != null) {
            BCPanel activeContent = tabContents.get(activeTabId);
            if (activeContent != null) {
                activeContent.tick();
            }
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int renderX, int renderY, int mouseX, int mouseY) {
        if (!visible) return;
        
        // Render tab buttons
        renderTabsOnly(guiGraphics, renderX, renderY, mouseX, mouseY);
        
        // Render active tab content
        if (activeTabId != null) {
            BCPanel activeContent = tabContents.get(activeTabId);
            if (activeContent != null) {
                activeContent.render(guiGraphics, renderX, renderY + tabHeight, mouseX, mouseY);
            }
        }
    }
    
    /**
     * Render only the tab buttons, not the content
     * Used for animations
     */
    public void renderTabsOnly(GuiGraphics guiGraphics, int renderX, int renderY, int mouseX, int mouseY) {
        if (!visible) return;
        
        // Render tab buttons
        for (TabButton tab : tabButtons) {
            // Manually render tab buttons since they're already added to the screen
            tab.renderWidget(guiGraphics, mouseX, mouseY, 0);
        }
        
        // Render tab background
        guiGraphics.fill(
                renderX,
                renderY + tabHeight,
                renderX + width,
                renderY + height,
                0xFFDDDDDD
        );
    }
    
    /**
     * A button representing a tab
     */
    private static class TabButton extends Button {
        private final String tabId;
        private boolean selected = false;
        private int relativeX;
        private int relativeY;
        private final int width;
        private final int height;
        private final Component message;
        
        public TabButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(Button.builder(message, onPress)
                    .pos(x, y)
                    .size(width, height));
                    
            this.tabId = message.getString();
            this.relativeX = x;
            this.relativeY = y;
            this.width = width;
            this.height = height;
            this.message = message;
        }
        
        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // Get the background color based on selection state
            int bgColor = selected ? 0xFFDDDDDD : 0xFF888888;
            
            // Render tab background
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
            
            // Render tab border
            guiGraphics.hLine(getX(), getX() + width - 1, getY(), 0xFF000000);
            guiGraphics.vLine(getX(), getY(), getY() + height - 1, 0xFF000000);
            guiGraphics.vLine(getX() + width - 1, getY(), getY() + height - 1, 0xFF000000);
            
            // Only draw the bottom border if not selected
            if (!selected) {
                guiGraphics.hLine(getX(), getX() + width - 1, getY() + height - 1, 0xFF000000);
            }
            
            // Render tab text
            int textColor = selected ? 0xFF000000 : 0xFFFFFFFF;
            Font font = Minecraft.getInstance().font;
            int textWidth = font.width(message);
            guiGraphics.drawString(font, message, getX() + (width - textWidth) / 2, getY() + (height - 8) / 2, textColor);
        }
        
        public void setRelativePosition(int x, int y) {
            this.relativeX = x;
            this.relativeY = y;
        }
        
        public int getRelativeX() {
            return relativeX;
        }
        
        public int getRelativeY() {
            return relativeY;
        }
        
        public int getWidth() {
            return width;
        }
        
        public int getHeight() {
            return height;
        }
        
        public String getTabId() {
            return tabId;
        }
        
        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
} 