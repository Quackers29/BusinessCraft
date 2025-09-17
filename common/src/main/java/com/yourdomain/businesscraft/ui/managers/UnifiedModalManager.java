package com.yourdomain.businesscraft.ui.managers;

import com.yourdomain.businesscraft.ui.modal.core.BCModalScreen;
import com.yourdomain.businesscraft.ui.modal.core.BCPopupScreen;
import com.yourdomain.businesscraft.ui.modal.specialized.BCModalInventoryScreen;
import com.yourdomain.businesscraft.ui.modal.specialized.BCModalGridScreen;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.menu.TradeMenu;
import com.yourdomain.businesscraft.menu.StorageMenu;
import com.yourdomain.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import com.yourdomain.businesscraft.ui.components.containers.BCTabPanel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/**
 * Unified modal manager that consolidates common modal creation patterns.
 * Reduces code duplication across individual modal managers.
 */
public class UnifiedModalManager {
    
    /**
     * Modal types supported by the unified manager.
     */
    public enum ModalType {
        TOWN_NAME_POPUP,
        VISITOR_LIST,
        TRADE_RESOURCES,
        STORAGE,
        VISITOR_HISTORY
    }
    
    /**
     * Configuration for modal creation.
     */
    public static class ModalConfig {
        private final ModalType type;
        private String currentTownName;
        private BlockPos blockPos;
        private Screen parentScreen;
        private TownInterfaceMenu townMenu;
        private BCTabPanel tabPanel;
        private String targetTab;
        private Consumer<Object> onCloseCallback;
        
        private ModalConfig(ModalType type) {
            this.type = type;
        }
        
        public static ModalConfig of(ModalType type) {
            return new ModalConfig(type);
        }
        
        public ModalConfig withTownName(String townName) {
            this.currentTownName = townName;
            return this;
        }
        
        public ModalConfig withBlockPos(BlockPos blockPos) {
            this.blockPos = blockPos;
            return this;
        }
        
        public ModalConfig withParentScreen(Screen parentScreen) {
            this.parentScreen = parentScreen;
            return this;
        }
        
        public ModalConfig withTownMenu(TownInterfaceMenu townMenu) {
            this.townMenu = townMenu;
            return this;
        }
        
        public ModalConfig withTabPanel(BCTabPanel tabPanel) {
            this.tabPanel = tabPanel;
            return this;
        }
        
        public ModalConfig withTargetTab(String targetTab) {
            this.targetTab = targetTab;
            return this;
        }
        
        public ModalConfig withCloseCallback(Consumer<Object> callback) {
            this.onCloseCallback = callback;
            return this;
        }
        
        // Getters
        public ModalType getType() { return type; }
        public String getCurrentTownName() { return currentTownName; }
        public BlockPos getBlockPos() { return blockPos; }
        public Screen getParentScreen() { return parentScreen; }
        public TownInterfaceMenu getTownMenu() { return townMenu; }
        public BCTabPanel getTabPanel() { return tabPanel; }
        public String getTargetTab() { return targetTab; }
        public Consumer<Object> getOnCloseCallback() { return onCloseCallback; }
    }
    
    /**
     * Creates a modal based on the provided configuration.
     * 
     * @param config Modal configuration
     * @return The created modal object (type depends on modal type)
     */
    public static Object createModal(ModalConfig config) {
        switch (config.getType()) {
            case TOWN_NAME_POPUP:
                return TownNamePopupManager.showChangeTownNamePopup(
                    config.getCurrentTownName(),
                    config.getBlockPos(),
                    popup -> safeCallback(config.getOnCloseCallback(), null)
                );
                
            case VISITOR_LIST:
                return VisitorModalManager.showVisitorListModal(
                    modal -> safeCallback(config.getOnCloseCallback(), null)
                );
                
            case TRADE_RESOURCES:
                String tradeTargetTab = config.getTargetTab() != null ? config.getTargetTab() : "resources";
                TradeModalManager.showTradeResourcesModal(
                    config.getParentScreen(),
                    config.getBlockPos(),
                    tradeTargetTab,
                    screen -> safeCallback(config.getOnCloseCallback(), screen)
                );
                return null; // TradeModalManager doesn't return the modal
                
            case STORAGE:
                String storageTargetTab = config.getTargetTab() != null ? config.getTargetTab() : "resources";
                StorageModalManager.showStorageModal(
                    config.getParentScreen(),
                    config.getBlockPos(),
                    config.getTownMenu(),
                    storageTargetTab,
                    screen -> safeCallback(config.getOnCloseCallback(), screen)
                );
                return null; // StorageModalManager doesn't return the modal
                
            case VISITOR_HISTORY:
                String historyTargetTab = config.getTargetTab() != null ? config.getTargetTab() : "population";
                VisitorHistoryManager.showVisitorHistoryScreen(
                    config.getParentScreen(),
                    config.getBlockPos(),
                    historyTargetTab,
                    screen -> safeCallback(config.getOnCloseCallback(), screen)
                );
                return null; // VisitorHistoryManager doesn't return the modal
                
            default:
                throw new IllegalArgumentException("Unsupported modal type: " + config.getType());
        }
    }
    
    /**
     * Safely invokes a callback if it's not null.
     * 
     * @param callback The callback to invoke
     * @param parameter The parameter to pass to the callback
     */
    private static void safeCallback(Consumer<Object> callback, Object parameter) {
        if (callback != null) {
            callback.accept(parameter);
        }
    }
    
    /**
     * Convenience method for creating a town name popup.
     */
    public static BCPopupScreen createTownNamePopup(String currentName, BlockPos blockPos, Consumer<Object> onClose) {
        ModalConfig config = ModalConfig.of(ModalType.TOWN_NAME_POPUP)
            .withTownName(currentName)
            .withBlockPos(blockPos)
            .withCloseCallback(onClose);
        return (BCPopupScreen) createModal(config);
    }
    
    /**
     * Convenience method for creating a visitor list modal.
     */
    public static BCModalScreen createVisitorListModal(Consumer<Object> onClose) {
        ModalConfig config = ModalConfig.of(ModalType.VISITOR_LIST)
            .withCloseCallback(onClose);
        return (BCModalScreen) createModal(config);
    }
    
    /**
     * Convenience method for creating trade resources modal.
     */
    public static void createTradeResourcesModal(Screen parent, BlockPos blockPos, String targetTab, Consumer<Object> onClose) {
        ModalConfig config = ModalConfig.of(ModalType.TRADE_RESOURCES)
            .withParentScreen(parent)
            .withBlockPos(blockPos)
            .withTargetTab(targetTab)
            .withCloseCallback(onClose);
        createModal(config);
    }
    
    /**
     * Convenience method for creating storage modal.
     */
    public static void createStorageModal(Screen parent, BlockPos blockPos, TownInterfaceMenu townMenu, String targetTab, Consumer<Object> onClose) {
        ModalConfig config = ModalConfig.of(ModalType.STORAGE)
            .withParentScreen(parent)
            .withBlockPos(blockPos)
            .withTownMenu(townMenu)
            .withTargetTab(targetTab)
            .withCloseCallback(onClose);
        createModal(config);
    }
    
    /**
     * Convenience method for creating visitor history modal.
     */
    public static void createVisitorHistoryModal(Screen parent, BlockPos blockPos, String targetTab, Consumer<Object> onClose) {
        ModalConfig config = ModalConfig.of(ModalType.VISITOR_HISTORY)
            .withParentScreen(parent)
            .withBlockPos(blockPos)
            .withTargetTab(targetTab)
            .withCloseCallback(onClose);
        createModal(config);
    }
} 