package com.quackers29.businesscraft.ui.tabs;

import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.templates.TownInterfaceTheme;
import com.quackers29.businesscraft.ui.components.basic.BCPanel;
import com.quackers29.businesscraft.ui.layout.BCFlowLayout;
import com.quackers29.businesscraft.ui.components.basic.BCLabel;
import com.quackers29.businesscraft.ui.builders.BCComponentFactory;
import com.quackers29.businesscraft.ui.components.containers.StandardTabContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import java.util.function.Consumer;

/**
 * Base class for town interface tab implementations.
 * Provides common functionality and structure for all tabs.
 */
public abstract class BaseTownTab {
    protected final TownInterfaceScreen parentScreen;
    protected final BCPanel panel;
    protected final int tabWidth;
    protected final int tabHeight;
    
    /**
     * Creates a new tab with the specified parent screen and dimensions.
     * 
     * @param parentScreen The parent screen containing this tab
     * @param width The width of the tab panel
     * @param height The height of the tab panel
     */
    public BaseTownTab(TownInterfaceScreen parentScreen, int width, int height) {
        this.parentScreen = parentScreen;
        this.tabWidth = width;
        this.tabHeight = height;
        
        // Create the panel with appropriate dimensions and common styling
        this.panel = new BCPanel(width, height);
        this.panel.withPadding(10)
                 .withBackgroundColor(0x00000000) // Transparent background
                 .withCornerRadius(3);
    }
    
    /**
     * Creates a standard header label for the tab.
     * 
     * @param title The title text
     * @return Configured header label
     */
    protected BCLabel createHeaderLabel(String title) {
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel(title, panel.getInnerWidth());
        titleLabel.withTextColor(TownInterfaceTheme.TEXT_HIGHLIGHT).withShadow(true);
        return titleLabel;
    }
    
    /**
     * Calculates available dimensions for content after accounting for header.
     * 
     * @return Array with [width, height] of available content area
     */
    protected int[] getContentDimensions() {
        int titleHeight = 20; // Approximate height of the header
        int verticalSpacing = 10; // Space between title and content
        
        int availableWidth = panel.getInnerWidth();
        int availableHeight = panel.getInnerHeight() - titleHeight - verticalSpacing;
        
        return new int[]{availableWidth, availableHeight};
    }
    
    /**
     * Creates a standardized content component for this tab.
     * 
     * @param contentType The type of content to create
     * @param title The title for the content (can be null)
     * @return A configured StandardTabContent component
     */
    protected StandardTabContent createStandardContent(StandardTabContent.ContentType contentType, String title) {
        int[] dimensions = getContentDimensions();
        return new StandardTabContent(dimensions[0], dimensions[1], contentType, title);
    }
    
    /**
     * Initialize the tab content. This method should be called during the parent screen's init.
     * 
     * @param registerWidget Function to register widgets with the parent screen
     */
    public abstract void init(Consumer<Button> registerWidget);
    
    /**
     * Update the tab content with the latest data.
     * This method should be called periodically to refresh data.
     */
    public abstract void update();
    
    /**
     * Get the panel containing this tab's content.
     * 
     * @return The panel for this tab
     */
    public BCPanel getPanel() {
        return panel;
    }
    
    /**
     * Handle mouse click events specific to this tab.
     * 
     * @param mouseX Mouse X coordinate
     * @param mouseY Mouse Y coordinate
     * @param button Mouse button
     * @return True if the event was handled, false otherwise
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false; // Default implementation does nothing
    }
    
    /**
     * Handle mouse drag events specific to this tab.
     * 
     * @param mouseX Mouse X coordinate
     * @param mouseY Mouse Y coordinate
     * @param button Mouse button
     * @param dragX Drag X delta
     * @param dragY Drag Y delta
     * @return True if the event was handled, false otherwise
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false; // Default implementation does nothing
    }
    
    /**
     * Handle mouse scroll events specific to this tab.
     * 
     * @param mouseX Mouse X coordinate
     * @param mouseY Mouse Y coordinate
     * @param delta Scroll delta
     * @return True if the event was handled, false otherwise
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false; // Default implementation does nothing
    }
    
    /**
     * Called when the tab is activated/becomes visible.
     */
    public void onActivate() {
        // Default implementation does nothing
    }
    
    /**
     * Called when the tab is deactivated/hidden.
     */
    public void onDeactivate() {
        // Default implementation does nothing
    }
    
    /**
     * Handle the tab's rendering if needed.
     * In most cases, the panel handles this automatically.
     */
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Default implementation does nothing extra
    }
} 
