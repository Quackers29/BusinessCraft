package com.yourdomain.businesscraft.ui.managers;

import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.ui.modal.core.BCModalScreen;
import com.yourdomain.businesscraft.ui.screens.BaseTownScreen;
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
        
        LOGGER.debug("ModalCoordinator initialized for screen: {}", screen.getClass().getSimpleName());
    }
    
    /**
     * Shows the trade resources modal screen with input and output slots.
     * 
     * @param onClose Optional callback when modal is closed
     */
    public void showTradeModal(Consumer<Void> onClose) {
        try {
            TradeModalManager.showTradeResourcesModal(
                screen,
                blockPos,
                screen -> {
                    LOGGER.debug("Trade modal closed");
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
     * @param onClose Optional callback when modal is closed
     */
    public void showStorageModal(Consumer<Void> onClose) {
        try {
            StorageModalManager.showStorageModal(
                screen,
                blockPos,
                menu,
                screen -> {
                    LOGGER.debug("Storage modal closed");
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
                LOGGER.debug("Visitor modal closed");
                if (onClose != null) {
                    onClose.accept(closedModal);
                }
            });
            
            LOGGER.debug("Visitor modal created successfully");
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
     * @param onClose Optional callback when screen is closed
     */
    public void showVisitorHistoryModal(Consumer<Void> onClose) {
        try {
            // Get the tab panel from the screen
            Object tabPanelObj = screen.getTabPanel();
            if (!(tabPanelObj instanceof com.yourdomain.businesscraft.ui.components.containers.BCTabPanel)) {
                throw new IllegalStateException("Tab panel not available or wrong type");
            }
            
            com.yourdomain.businesscraft.ui.components.containers.BCTabPanel tabPanel = 
                (com.yourdomain.businesscraft.ui.components.containers.BCTabPanel) tabPanelObj;
            
            VisitorHistoryManager.showVisitorHistoryScreen(
                screen,
                blockPos,
                tabPanel,
                screen -> {
                    LOGGER.debug("Visitor history screen closed");
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
        LOGGER.debug("ModalCoordinator cleanup completed");
        // Currently no resources to clean up, but method provided for future use
    }
}