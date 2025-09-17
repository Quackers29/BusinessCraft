package com.yourdomain.businesscraft.ui.managers;

import com.yourdomain.businesscraft.ui.templates.TownInterfaceTheme;
import com.yourdomain.businesscraft.ui.modal.core.BCModalScreen;
import com.yourdomain.businesscraft.ui.modal.core.BCPopupScreen;
import com.yourdomain.businesscraft.ui.components.containers.BCTabPanel;
import com.yourdomain.businesscraft.ui.components.basic.BCPanel;
import com.yourdomain.businesscraft.ui.components.basic.UIComponent;
import com.yourdomain.businesscraft.ui.components.basic.BCComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Manages the rendering pipeline for the Town Interface Screen.
 * Extracted from TownInterfaceScreen to improve code organization.
 */
public class TownScreenRenderManager {
    
    // Rendering interfaces for delegation
    public interface CacheUpdateProvider {
        TownDataCacheManager getCacheManager();
        int getUpdateCounter();
        void setUpdateCounter(int counter);
        int getRefreshInterval();
        int getCurrentSearchRadius();
        void setCurrentSearchRadius(int radius);
        int getSearchRadiusFromMenu();
    }
    
    public interface ScreenLayoutProvider {
        int getLeftPos();
        int getTopPos();
        int getImageWidth();
        int getImageHeight();
        Font getFont();
        Component getTitle();
        void renderBackground(GuiGraphics graphics);
        void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY);
    }
    
    public interface ComponentProvider {
        BCTabPanel getTabPanel();
        BottomButtonManager getButtonManager();
        BCModalScreen getActiveModal();
        BCPopupScreen getActivePopup();
    }
    
    private final CacheUpdateProvider cacheProvider;
    private final ScreenLayoutProvider layoutProvider;
    private final ComponentProvider componentProvider;
    
    // Theme constants
    private static final int BORDER_COLOR = TownInterfaceTheme.BORDER_COLOR;
    private static final int TEXT_COLOR = TownInterfaceTheme.TEXT_COLOR;
    
    /**
     * Creates a new render manager.
     * 
     * @param cacheProvider Provider for cache update operations
     * @param layoutProvider Provider for screen layout information
     * @param componentProvider Provider for UI components
     */
    public TownScreenRenderManager(
            CacheUpdateProvider cacheProvider,
            ScreenLayoutProvider layoutProvider,
            ComponentProvider componentProvider) {
        this.cacheProvider = cacheProvider;
        this.layoutProvider = layoutProvider;
        this.componentProvider = componentProvider;
    }
    
    /**
     * Handles the complete rendering pipeline.
     * 
     * @param graphics Graphics context
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param partialTicks Partial ticks for animation
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Phase 1: Update cache and refresh components
        updateCacheAndComponents();
        
        // Phase 2: Render background and borders
        renderBackground(graphics);
        
        // Phase 3: Render main components
        renderMainComponents(graphics, mouseX, mouseY);
        
        // Phase 4: Render overlays (title, tooltips, modals, popups)
        renderOverlays(graphics, mouseX, mouseY);
    }
    
    /**
     * Phase 1: Updates cache and refreshes components periodically.
     */
    private void updateCacheAndComponents() {
        int updateCounter = cacheProvider.getUpdateCounter();
        updateCounter++;
        
        if (updateCounter >= cacheProvider.getRefreshInterval()) {
            updateCounter = 0;
            
            // Refresh cache if available
            TownDataCacheManager cacheManager = cacheProvider.getCacheManager();
            if (cacheManager != null) {
                cacheManager.invalidateCache();
                cacheManager.refreshCachedValues();
                // DON'T override search radius during refresh - it may have been manually changed
                // cacheProvider.setCurrentSearchRadius(cacheManager.getLocalCachedSearchRadius());
            } else {
                // DON'T override search radius during refresh - it may have been manually changed
                // cacheProvider.setCurrentSearchRadius(cacheProvider.getSearchRadiusFromMenu());
            }
            
            // Update the active tab if exists
            refreshActiveTab();
        }
        
        cacheProvider.setUpdateCounter(updateCounter);
    }
    
    /**
     * Refreshes the active tab components.
     * With the main town name sync fix, this periodic refresh is much less critical.
     */
    private void refreshActiveTab() {
        // Since we fixed the root cause of town name sync issues, we no longer need
        // aggressive tab refreshing. The overview tab will get updated properly
        // through the normal data synchronization mechanisms.
        
        // This method is kept for any future edge cases but does minimal work now
        BCTabPanel tabPanel = componentProvider.getTabPanel();
        if (tabPanel != null) {
            String activeTabId = tabPanel.getActiveTabId();
            // Only refresh if it's the overview tab, and only occasionally
            if ("overview".equals(activeTabId)) {
                // The tab will refresh properly when data changes through normal sync
                // No need for aggressive refreshing
            }
        }
    }
    
    /**
     * Phase 2: Renders background and window borders.
     * 
     * @param graphics Graphics context
     */
    private void renderBackground(GuiGraphics graphics) {
        // Draw the dimmed background
        layoutProvider.renderBackground(graphics);
        
        // Draw a semi-transparent background for the entire window
        int leftPos = layoutProvider.getLeftPos();
        int topPos = layoutProvider.getTopPos();
        int width = layoutProvider.getImageWidth();
        int height = layoutProvider.getImageHeight();
        
        graphics.fill(leftPos, topPos, leftPos + width, topPos + height, 0x80222222);
        
        // Draw border
        graphics.hLine(leftPos, leftPos + width - 1, topPos, BORDER_COLOR);
        graphics.hLine(leftPos, leftPos + width - 1, topPos + height - 1, BORDER_COLOR);
        graphics.vLine(leftPos, topPos, topPos + height - 1, BORDER_COLOR);
        graphics.vLine(leftPos + width - 1, topPos, topPos + height - 1, BORDER_COLOR);
    }
    
    /**
     * Phase 3: Renders main UI components (tabs and buttons).
     * 
     * @param graphics Graphics context
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     */
    private void renderMainComponents(GuiGraphics graphics, int mouseX, int mouseY) {
        BCTabPanel tabPanel = componentProvider.getTabPanel();
        BottomButtonManager buttonManager = componentProvider.getButtonManager();
        
        // Render the tab panel
        if (tabPanel != null) {
            tabPanel.render(graphics, tabPanel.getX(), tabPanel.getY(), mouseX, mouseY);
        }
        
        // Update and render the bottom buttons based on active tab
        if (buttonManager != null && tabPanel != null) {
            int leftPos = layoutProvider.getLeftPos();
            int topPos = layoutProvider.getTopPos();
            int width = layoutProvider.getImageWidth();
            int height = layoutProvider.getImageHeight();
            
            buttonManager.updateScreenPosition(leftPos, topPos, width, height);
            buttonManager.updateBottomButtons(tabPanel.getActiveTabId());
            buttonManager.render(graphics, mouseX, mouseY);
        }
    }
    
    /**
     * Phase 4: Renders overlays including title, tooltips, modals, and popups.
     * 
     * @param graphics Graphics context
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     */
    private void renderOverlays(GuiGraphics graphics, int mouseX, int mouseY) {
        // Render the screen title
        renderTitle(graphics);
        
        // Draw tooltips with proper Z-ordering
        renderTooltips(graphics, mouseX, mouseY);
        
        // Render modals and popups (must be last for proper layering)
        renderModalsAndPopups(graphics, mouseX, mouseY);
    }
    
    /**
     * Renders the screen title.
     * 
     * @param graphics Graphics context
     */
    private void renderTitle(GuiGraphics graphics) {
        Font font = layoutProvider.getFont();
        Component title = layoutProvider.getTitle();
        int leftPos = layoutProvider.getLeftPos();
        int topPos = layoutProvider.getTopPos();
        int width = layoutProvider.getImageWidth();
        
        graphics.drawCenteredString(font, title, leftPos + width / 2, topPos - 12, TEXT_COLOR);
    }
    
    /**
     * Renders tooltips with proper Z-ordering.
     * 
     * @param graphics Graphics context
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     */
    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw any tooltips
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);  // Move tooltips to front layer
        layoutProvider.renderTooltip(graphics, mouseX, mouseY);
        graphics.pose().popPose();
    }
    
    /**
     * Renders modals and popups with proper layering.
     * 
     * @param graphics Graphics context
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     */
    private void renderModalsAndPopups(GuiGraphics graphics, int mouseX, int mouseY) {
        // IMPORTANT: Render modals and popups AFTER everything else
        
        // If we have an active popup, render it on top of everything with matrix transformation for Z-ordering
        BCPopupScreen activePopup = componentProvider.getActivePopup();
        if (activePopup != null && activePopup.isVisible()) {
            // The popup handles its own matrix transformations internally
            activePopup.render(graphics, 0, 0, mouseX, mouseY);
        }
        
        // If we have an active modal, render it last to ensure it's on top of everything
        BCModalScreen activeModal = componentProvider.getActiveModal();
        if (activeModal != null) {
            // The modal now handles its own matrix transformations internally
            activeModal.render(graphics, 0, 0, mouseX, mouseY);
        }
    }
} 