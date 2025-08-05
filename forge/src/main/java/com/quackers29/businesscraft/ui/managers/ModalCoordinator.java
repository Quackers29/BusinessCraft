package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.ui.modal.core.BCModalScreen;
import com.quackers29.businesscraft.ui.screens.BaseTownScreen;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Coordinates modal screen management for town interfaces.
 * Extracted from TownInterfaceScreen to centralize modal handling and reduce coupling.
 */
public class ModalCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModalCoordinator.class);
    
    private final BaseTownScreen<?> screen;
    private final TownInterfaceMenu menu;
    private final BlockPos blockPos;
    
    /**
     * Creates a new ModalCoordinator.
     * 
     * @param screen The parent screen
     * @param menu The town interface menu
     */
    public ModalCoordinator(BaseTownScreen<?> screen, TownInterfaceMenu menu) {
        this.screen = screen;
        this.menu = menu;
        this.blockPos = menu.getBlockPos();
        
        DebugConfig.debug(LOGGER, DebugConfig.MODAL_MANAGERS, "ModalCoordinator initialized for screen: {}", screen.getClass().getSimpleName());
    }
    
    /**
     * Shows the trade resources modal screen with input and output slots.
     * 
     * @param targetTab The tab to return to when modal closes
     * @param onClose Optional callback when modal is closed
     */
    public void showTradeModal(String targetTab, Consumer<Void> onClose) {
        try {
            TradeModalManager.showTradeResourcesModal(
                screen,
                blockPos,
                targetTab,
                modalScreen -> {
                    DebugConfig.debug(LOGGER, DebugConfig.MODAL_MANAGERS, "Trade modal closed, returning to tab: {}", targetTab);
                    if (onClose != null) {
                        onClose.accept(null);
                    }
                }
            );
        } catch (Exception e) {
            LOGGER.error("Failed to show trade modal", e);
            screen.sendChatMessage("Unable to open trade interface");
        }
    }
    
    /**
     * Shows the storage modal screen with chest-like storage interface.
     * 
     * @param targetTab The tab to return to when modal closes
     * @param onClose Optional callback when modal is closed
     */
    public void showStorageModal(String targetTab, Consumer<Void> onClose) {
        try {
            StorageModalManager.showStorageModal(
                screen,
                blockPos,
                menu,
                targetTab,
                modalScreen -> {
                    DebugConfig.debug(LOGGER, DebugConfig.MODAL_MANAGERS, "Storage modal closed, returning to tab: {}", targetTab);
                    if (onClose != null) {
                        onClose.accept(null);
                    }
                }
            );
        } catch (Exception e) {
            LOGGER.error("Failed to show storage modal", e);
            screen.sendChatMessage("Unable to open storage interface");
        }
    }
    
    /**
     * Shows the visitor list modal screen.
     * 
     * @param onClose Optional callback when modal is closed
     * @return The created modal screen, or null if creation failed
     */
    public BCModalScreen showVisitorModal(Consumer<BCModalScreen> onClose) {
        try {
            BCModalScreen createdModal = VisitorModalManager.showVisitorListModal(closedModal -> {
                DebugConfig.debug(LOGGER, DebugConfig.MODAL_MANAGERS, "Visitor modal closed");
                if (onClose != null) {
                    onClose.accept(closedModal);
                }
            });
            
            DebugConfig.debug(LOGGER, DebugConfig.MODAL_MANAGERS, "Visitor modal created successfully");
            return createdModal;
        } catch (Exception e) {
            LOGGER.error("Failed to show visitor modal", e);
            screen.sendChatMessage("Unable to open visitor list");
            return null;
        }
    }
    
    /**
     * Shows the visitor history screen.
     * 
     * @param targetTab The tab to return to when modal closes
     * @param onClose Optional callback when screen is closed
     */
    public void showVisitorHistoryModal(String targetTab, Consumer<Void> onClose) {
        try {
            VisitorHistoryManager.showVisitorHistoryScreen(
                screen,
                blockPos,
                targetTab,
                screen -> {
                    DebugConfig.debug(LOGGER, DebugConfig.MODAL_MANAGERS, "Visitor history screen closed, returning to tab: {}", targetTab);
                    if (onClose != null) {
                        onClose.accept(null);
                    }
                }
            );
        } catch (Exception e) {
            LOGGER.error("Failed to show visitor history screen", e);
            screen.sendChatMessage("Unable to open visitor history");
        }
    }
    
    /**
     * Validates that required dependencies are available.
     * 
     * @return true if all dependencies are valid
     */
    public boolean validateDependencies() {
        if (screen == null) {
            LOGGER.error("Screen is null");
            return false;
        }
        
        if (menu == null) {
            LOGGER.error("Menu is null");
            return false;
        }
        
        if (blockPos == null) {
            LOGGER.error("Block position is null");
            return false;
        }
        
        return true;
    }
    
    /**
     * Performs cleanup when the coordinator is no longer needed.
     */
    public void cleanup() {
        DebugConfig.debug(LOGGER, DebugConfig.MODAL_MANAGERS, "ModalCoordinator cleanup completed");
        // Currently no resources to clean up, but method provided for future use
    }
}