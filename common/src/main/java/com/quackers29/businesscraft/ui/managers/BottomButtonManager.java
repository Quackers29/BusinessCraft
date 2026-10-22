package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.client.showcase.ShowcaseButtonIds;
import com.quackers29.businesscraft.ui.templates.TownInterfaceTheme;
import com.quackers29.businesscraft.ui.builders.UIGridBuilder;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Manages bottom button grid creation and configuration for the Town Interface.
 * Extracted from TownInterfaceScreen to improve code organization.
 */
public class BottomButtonManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BottomButtonManager.class);
    private static final String SHOWCASE_PROPERTY = "businesscraft.showcase";

    // Button configuration interface
    public interface ButtonActionHandler {
        void onEditDetails();

        void onTradeResources();

        void onManageStorage();

        void onAssignJobs();

        void onViewVisitorHistory();

        void onSaveSettings();

        void onResetDefaults();

        void onManagePlatforms();

        void onGenericAction(String action);

        void onContractBoard();

        void onViewLeaderboard();
    }

    private UIGridBuilder bottomButtonsGrid;
    private final ButtonActionHandler actionHandler;

    // Screen positioning data
    private int screenLeftPos;
    private int screenTopPos;
    private int screenWidth;
    private int screenHeight;
    private int bottomGap = 10;
    private int bottomRowHeight = 30;

    /**
     * Creates a new bottom button manager.
     * 
     * @param actionHandler Handler for button actions
     */
    public BottomButtonManager(ButtonActionHandler actionHandler) {
        this.actionHandler = actionHandler;
    }

    /**
     * Updates the screen positioning for button calculations.
     * 
     * @param leftPos Screen left position
     * @param topPos  Screen top position
     * @param width   Screen width
     * @param height  Screen height
     */
    public void updateScreenPosition(int leftPos, int topPos, int width, int height) {
        updateScreenPosition(leftPos, topPos, width, height, 10, 30);
    }

    public void updateScreenPosition(int leftPos, int topPos, int width, int height,
            int bottomGap, int bottomRowHeight) {
        this.screenLeftPos = leftPos;
        this.screenTopPos = topPos;
        this.screenWidth = width;
        this.screenHeight = height;
        this.bottomGap = bottomGap;
        this.bottomRowHeight = bottomRowHeight;
    }

    /**
     * Creates the bottom button grid with proper positioning.
     */
    public void createBottomButtonsGrid() {
        bottomButtonsGrid = newBottomGrid(2);
    }

    private int buttonPanelY() {
        return this.screenTopPos + this.screenHeight + this.bottomGap;
    }

    private UIGridBuilder newBottomGrid(int columns) {
        int buttonPanelWidth = this.screenWidth - 40;
        int buttonPanelHeight = this.bottomRowHeight;
        int buttonPanelX = this.screenLeftPos + (this.screenWidth - buttonPanelWidth) / 2;
        int buttonPanelY = buttonPanelY();
        logBottomLayout(buttonPanelX, buttonPanelY, buttonPanelWidth, buttonPanelHeight);

        int verticalMargin = buttonPanelHeight <= 20 ? 2 : 5;
        int horizontalMargin = buttonPanelHeight <= 20 ? 3 : 5;
        int rowHeight = Math.max(8, buttonPanelHeight - (verticalMargin * 2));
        if (buttonPanelHeight > 20) {
            rowHeight = 20;
        }

        return new UIGridBuilder(buttonPanelX, buttonPanelY, buttonPanelWidth, buttonPanelHeight, 1, columns)
                .withBackgroundColor(TownInterfaceTheme.BACKGROUND_COLOR)
                .withBorderColor(TownInterfaceTheme.BORDER_COLOR)
                .withMargins(horizontalMargin, verticalMargin)
                .withSpacing(columns == 3 ? 10 : 20, 0)
                .withRowHeight(rowHeight);
    }

    private void logBottomLayout(int x, int y, int w, int h) {
        if (System.getProperty(SHOWCASE_PROPERTY) == null) {
            return;
        }
        LOGGER.info(
                "Showcase bottom grid: panelTopPos={} imageH={} gap={} rowH={} gridY={} gridH={} gridBottom={}",
                screenTopPos, screenHeight, bottomGap, bottomRowHeight, y, h, y + h);
    }

    /**
     * Updates the bottom buttons based on the active tab.
     * 
     * @param activeTab The currently active tab ID
     */
    public void updateBottomButtons(String activeTab) {
        // Calculate button panel dimensions (must be recalculated in case window was
        // resized)
        bottomButtonsGrid = newBottomGrid(2);

        // Default to overview if no active tab
        if (activeTab == null) {
            activeTab = "overview";
        }

        // Configure buttons based on active tab
        configureButtonsForTab(activeTab);
    }

    /**
     * Configures buttons for the specified tab.
     * 
     * @param tabId The tab ID to configure buttons for
     */
    private void configureButtonsForTab(String tabId) {
        switch (tabId) {
            case "overview":
                configureOverviewButtons();
                break;

            case "resources":
                configureResourcesButtons();
                break;

            case "population":
                configurePopulationButtons();
                break;

            case "production":
                configureProductionButtons();
                break;

            case "settings":
                configureSettingsButtons();
                break;

            default:
                configureDefaultButtons();
                break;
        }
    }

    /**
     * Configures buttons for the Production tab.
     */
    private void configureProductionButtons() {
        bottomButtonsGrid
                .addButtonWithTooltip(0, 0, ShowcaseButtonIds.PRODUCTION, "Production",
                        "View active production processes",
                        v -> actionHandler.onGenericAction("view_active_production"), TownInterfaceTheme.PRIMARY_COLOR)
                .addButtonWithTooltip(0, 1, ShowcaseButtonIds.UPGRADES, "Upgrades", "View town upgrades and research",
                        v -> actionHandler.onGenericAction("view_upgrades"), TownInterfaceTheme.SECONDARY_COLOR);
    }

    /**
     * Configures buttons for the Overview tab.
     */
    private void configureOverviewButtons() {
        bottomButtonsGrid
                .addButtonWithTooltip(0, 0, ShowcaseButtonIds.RANKING, "Ranking", "View town rankings",
                        v -> actionHandler.onViewLeaderboard(), TownInterfaceTheme.PRIMARY_COLOR)
                .addButtonWithTooltip(0, 1, ShowcaseButtonIds.MAP_VIEW, "Map View",
                        "View town map with surrounding areas",
                        v -> actionHandler.onManagePlatforms(), TownInterfaceTheme.SECONDARY_COLOR);
    }

    /**
     * Configures buttons for the Resources tab.
     */
    private void configureResourcesButtons() {
        bottomButtonsGrid = newBottomGrid(3);

        bottomButtonsGrid
                .addButtonWithTooltip(0, 0, ShowcaseButtonIds.TRADE, "Trade", "Trade resources with other towns",
                        v -> actionHandler.onTradeResources(), TownInterfaceTheme.PRIMARY_COLOR)
                .addButtonWithTooltip(0, 1, ShowcaseButtonIds.PAYMENT, "Payment", "View payment board and rewards",
                        v -> actionHandler.onManageStorage(), TownInterfaceTheme.SECONDARY_COLOR)
                .addButtonWithTooltip(0, 2, ShowcaseButtonIds.CONTRACTS, "Contracts", "View and manage contracts",
                        v -> actionHandler.onContractBoard(), TownInterfaceTheme.ACCENT_COLOR);
    }

    /**
     * Configures buttons for the Population tab.
     */
    private void configurePopulationButtons() {
        bottomButtonsGrid
                .addButtonWithTooltip(0, 0, ShowcaseButtonIds.ASSIGN_JOBS, "Assign Jobs",
                        "Assign jobs to town citizens",
                        v -> actionHandler.onAssignJobs(), TownInterfaceTheme.PRIMARY_COLOR)
                .addButtonWithTooltip(0, 1, ShowcaseButtonIds.VIEW_VISITORS, "View Visitors",
                        "View history of visitors to your town",
                        v -> actionHandler.onViewVisitorHistory(), TownInterfaceTheme.SECONDARY_COLOR);
    }

    /**
     * Configures buttons for the Settings tab.
     */
    private void configureSettingsButtons() {
        bottomButtonsGrid
                .addButtonWithTooltip(0, 0, ShowcaseButtonIds.RANKING, "Ranking", "View town rankings",
                        v -> actionHandler.onViewLeaderboard(), TownInterfaceTheme.PRIMARY_COLOR)
                .addButtonWithTooltip(0, 1, ShowcaseButtonIds.EDIT_NAME, "Edit Name", "Edit town name",
                        v -> actionHandler.onEditDetails(), TownInterfaceTheme.PRIMARY_COLOR);
    }

    /**
     * Configures default buttons for unknown tabs.
     */
    private void configureDefaultButtons() {
        bottomButtonsGrid
                .addButtonWithTooltip(0, 0, "Action 1", "Default action 1",
                        v -> actionHandler.onGenericAction("Action 1"), TownInterfaceTheme.PRIMARY_COLOR)
                .addButtonWithTooltip(0, 1, "Action 2", "Default action 2",
                        v -> actionHandler.onGenericAction("Action 2"), TownInterfaceTheme.SECONDARY_COLOR);
    }

    /**
     * Renders the bottom buttons grid.
     * 
     * @param graphics Graphics context
     * @param mouseX   Mouse X position
     * @param mouseY   Mouse Y position
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (bottomButtonsGrid != null) {
            bottomButtonsGrid.render(graphics, mouseX, mouseY);
        }
    }

    /**
     * Handles mouse clicks on the bottom buttons.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     * @return True if the click was handled
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (bottomButtonsGrid != null && button == 0) {
            return bottomButtonsGrid.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    /**
     * Gets the current bottom buttons grid.
     * 
     * @return The bottom buttons grid
     */
    public UIGridBuilder getBottomButtonsGrid() {
        return bottomButtonsGrid;
    }
}
