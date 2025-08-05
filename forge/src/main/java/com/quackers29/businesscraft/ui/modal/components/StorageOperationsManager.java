package com.quackers29.businesscraft.ui.modal.components;

import com.quackers29.businesscraft.menu.StorageMenu;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.network.packets.storage.PersonalStorageRequestPacket;
import com.quackers29.businesscraft.network.packets.storage.CommunalStoragePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

import java.util.Map;
import java.util.UUID;

/**
 * Manages storage-specific operations for modal inventory screens.
 * Extracted from BCModalInventoryScreen to follow single responsibility principle.
 * Handles personal/communal storage switching, storage visualization, and storage operations.
 */
public class StorageOperationsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageOperationsManager.class);
    
    private final ModalRenderingEngine renderingEngine;
    
    // Storage UI configuration
    private static final int STORAGE_MODE_TOGGLE_WIDTH = 80;
    private static final int STORAGE_MODE_TOGGLE_HEIGHT = 16;
    private static final int STORAGE_INFO_HEIGHT = 40;
    
    // Storage colors
    private static final int PERSONAL_STORAGE_COLOR = 0xFF4A90E2;    // Blue
    private static final int COMMUNAL_STORAGE_COLOR = 0xFF50C878;    // Green
    private static final int STORAGE_BACKGROUND_COLOR = 0xFF2A2A2A; // Dark gray
    
    public StorageOperationsManager(ModalRenderingEngine renderingEngine) {
        this.renderingEngine = renderingEngine;
    }
    
    /**
     * Renders all storage-related elements.
     */
    public void renderStorageElements(GuiGraphics guiGraphics, Font font, StorageMenu storageMenu,
                                    int leftPos, int topPos, int imageWidth, int mouseX, int mouseY) {
        if (storageMenu == null) return;
        
        // Calculate storage area bounds
        int storageAreaY = topPos + 30;
        
        // Render storage background
        renderStorageBackground(guiGraphics, leftPos, storageAreaY, imageWidth);
        
        // Render storage mode toggle
        renderStorageModeToggle(guiGraphics, font, storageMenu, leftPos, storageAreaY, mouseX, mouseY);
        
        // Render storage information panel
        renderStorageInfoPanel(guiGraphics, font, storageMenu, leftPos, storageAreaY);
        
        // Render storage capacity indicator
        renderStorageCapacity(guiGraphics, font, storageMenu, leftPos, storageAreaY, imageWidth);
    }
    
    /**
     * Renders the storage background.
     */
    private void renderStorageBackground(GuiGraphics guiGraphics, int leftPos, int topPos, int imageWidth) {
        // Main storage area background
        renderingEngine.renderSectionBackground(guiGraphics, leftPos + 8, topPos, imageWidth - 16, STORAGE_INFO_HEIGHT,
                                              STORAGE_BACKGROUND_COLOR);
    }
    
    /**
     * Renders the storage mode toggle button.
     */
    private void renderStorageModeToggle(GuiGraphics guiGraphics, Font font, StorageMenu storageMenu,
                                       int leftPos, int topPos, int mouseX, int mouseY) {
        int toggleX = leftPos + 10;
        int toggleY = topPos + 5;
        
        boolean isPersonalMode = storageMenu.isPersonalStorageMode();
        boolean isHovered = isMouseOverStorageModeToggle(mouseX, mouseY, toggleX, toggleY);
        
        String modeText = isPersonalMode ? "Personal" : "Communal";
        int modeColor = isPersonalMode ? PERSONAL_STORAGE_COLOR : COMMUNAL_STORAGE_COLOR;
        
        // Render toggle button with current mode color
        renderingEngine.renderSectionBackground(guiGraphics, toggleX, toggleY, STORAGE_MODE_TOGGLE_WIDTH, 
                                              STORAGE_MODE_TOGGLE_HEIGHT, modeColor & 0x80FFFFFF);
        
        // Render mode text
        renderingEngine.renderCenteredLabel(guiGraphics, font, modeText, toggleX, toggleY + 4, 
                                          STORAGE_MODE_TOGGLE_WIDTH, 0xFFFFFFFF);
        
        // Render hover effect
        if (isHovered) {
            renderingEngine.renderSectionBackground(guiGraphics, toggleX, toggleY, STORAGE_MODE_TOGGLE_WIDTH,
                                                  STORAGE_MODE_TOGGLE_HEIGHT, 0x40FFFFFF);
        }
    }
    
    /**
     * Renders the storage information panel.
     */
    private void renderStorageInfoPanel(GuiGraphics guiGraphics, Font font, StorageMenu storageMenu, 
                                      int leftPos, int topPos) {
        int infoX = leftPos + 100;
        int infoY = topPos + 5;
        int infoWidth = 60;
        
        // Storage stats
        boolean isPersonalMode = storageMenu.isPersonalStorageMode();
        
        if (isPersonalMode) {
            // Personal storage stats (placeholder - actual implementation would track storage data)
            // TODO: Replace with actual storage counts when StorageMenu API is expanded
            String personalInfo = "Personal Storage";
            renderingEngine.renderLabel(guiGraphics, font, "Personal Storage:", infoX, infoY, PERSONAL_STORAGE_COLOR);
            renderingEngine.renderLabel(guiGraphics, font, "Ready", infoX, infoY + 10, 0xFFDDDDDD);
        } else {
            // Communal storage stats (placeholder)
            renderingEngine.renderLabel(guiGraphics, font, "Communal Storage:", infoX, infoY, COMMUNAL_STORAGE_COLOR);
            renderingEngine.renderLabel(guiGraphics, font, "Ready", infoX, infoY + 10, 0xFFDDDDDD);
        }
        
        // Last access info (placeholder)
        renderingEngine.renderLabel(guiGraphics, font, "Status: Active", infoX, infoY + 20, 0xFF888888);
    }
    
    /**
     * Renders storage capacity visualization.
     */
    private void renderStorageCapacity(GuiGraphics guiGraphics, Font font, StorageMenu storageMenu,
                                     int leftPos, int topPos, int imageWidth) {
        int capacityY = topPos + STORAGE_INFO_HEIGHT - 15;
        int capacityWidth = imageWidth - 20;
        int capacityHeight = 8;
        int capacityX = leftPos + 10;
        
        boolean isPersonalMode = storageMenu.isPersonalStorageMode();
        
        // TODO: Replace with actual storage capacity when StorageMenu API is expanded
        float usedRatio = 0.3f; // Placeholder ratio
        int progressColor = isPersonalMode ? PERSONAL_STORAGE_COLOR : COMMUNAL_STORAGE_COLOR;
        
        // Render capacity bar
        renderingEngine.renderProgressBar(guiGraphics, capacityX, capacityY, capacityWidth, capacityHeight,
                                        usedRatio, 0xFF333333, progressColor);
        
        // Capacity percentage text (placeholder)
        String capacityText = String.format("%.0f%% full", usedRatio * 100);
        int textX = capacityX + capacityWidth + 5;
        renderingEngine.renderLabel(guiGraphics, font, capacityText, textX, capacityY, 0xFFDDDDDD);
    }
    
    /**
     * Checks if mouse is over the storage mode toggle.
     */
    public boolean isMouseOverStorageModeToggle(int mouseX, int mouseY, int toggleX, int toggleY) {
        return mouseX >= toggleX && mouseX <= toggleX + STORAGE_MODE_TOGGLE_WIDTH &&
               mouseY >= toggleY && mouseY <= toggleY + STORAGE_MODE_TOGGLE_HEIGHT;
    }
    
    /**
     * Handles storage mode toggle click.
     */
    public boolean handleStorageModeToggle(StorageMenu storageMenu, int mouseX, int mouseY, int leftPos, int topPos) {
        int toggleX = leftPos + 10;
        int toggleY = topPos + 5;
        
        if (!isMouseOverStorageModeToggle(mouseX, mouseY, toggleX, toggleY)) {
            return false;
        }
        
        if (storageMenu != null) {
            try {
                // TODO: Replace with actual storage mode toggle when StorageMenu API is expanded
                boolean newPersonalMode = !storageMenu.isPersonalStorageMode();
                // storageMenu.setPersonalStorageMode(newPersonalMode); // TODO: Implement when API is available
                
                DebugConfig.debug(LOGGER, DebugConfig.STORAGE_OPERATIONS, "Toggled storage mode to: {}", newPersonalMode ? "Personal" : "Communal");
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to toggle storage mode", e);
            }
        }
        
        return true; // Consumed the click even if toggle failed
    }
    
    /**
     * Loads personal storage items from the menu.
     */
    public void loadPersonalStorageItems(StorageMenu storageMenu) {
        if (storageMenu == null) return;
        
        try {
            // TODO: Replace with actual storage data request when StorageMenu API is expanded
            DebugConfig.debug(LOGGER, DebugConfig.STORAGE_OPERATIONS, "Personal storage ready (placeholder implementation)");
        } catch (Exception e) {
            LOGGER.error("Failed to load personal storage items", e);
        }
    }
    
    /**
     * Requests personal storage data from the server.
     * TODO: Implement when StorageMenu API is expanded
     */
    private void requestPersonalStorageData(StorageMenu storageMenu) {
        // Placeholder implementation
        DebugConfig.debug(LOGGER, DebugConfig.STORAGE_OPERATIONS, "Personal storage data request (placeholder)");
    }
    
    /**
     * Requests communal storage data from the server.
     * TODO: Implement when StorageMenu API is expanded
     */
    private void requestCommunalStorageData(StorageMenu storageMenu) {
        // Placeholder implementation
        DebugConfig.debug(LOGGER, DebugConfig.STORAGE_OPERATIONS, "Communal storage data request (placeholder)");
    }
    
    /**
     * Handles storage item transfer operations.
     */
    public boolean handleStorageTransfer(StorageMenu storageMenu, ItemStack itemStack, boolean toStorage) {
        if (storageMenu == null || itemStack.isEmpty()) return false;
        
        try {
            // TODO: Implement actual storage transfers when StorageMenu API is expanded
            DebugConfig.debug(LOGGER, DebugConfig.STORAGE_OPERATIONS, "Storage transfer {} (placeholder implementation)", toStorage ? "to storage" : "from storage");
            return true; // Placeholder success
        } catch (Exception e) {
            LOGGER.error("Failed to transfer storage item", e);
            return false;
        }
    }
    
    /**
     * Gets storage-related tooltip text for the given position.
     */
    public String getStorageTooltip(StorageMenu storageMenu, int mouseX, int mouseY, int leftPos, int topPos) {
        if (storageMenu == null) return null;
        
        int toggleX = leftPos + 10;
        int toggleY = topPos + 5;
        
        if (isMouseOverStorageModeToggle(mouseX, mouseY, toggleX, toggleY)) {
            boolean isPersonalMode = storageMenu.isPersonalStorageMode();
            if (isPersonalMode) {
                return "Switch to Communal Storage\n(Shared with all town members)";
            } else {
                return "Switch to Personal Storage\n(Only accessible by you)";
            }
        }
        
        // Check if mouse is over capacity bar
        int capacityY = topPos + STORAGE_INFO_HEIGHT - 15;
        if (mouseY >= capacityY && mouseY <= capacityY + 8) {
            boolean isPersonalMode = storageMenu.isPersonalStorageMode();
            
            if (isPersonalMode) {
                // TODO: Replace with actual storage counts when StorageMenu API is expanded
                return "Personal Storage: Ready for use";
            } else {
                return "Communal Storage: Ready for use";
            }
        }
        
        return null;
    }
    
    /**
     * Validates storage operations based on current mode and permissions.
     */
    public boolean validateStorageOperation(StorageMenu storageMenu, String operation) {
        if (storageMenu == null) return false;
        
        boolean isPersonalMode = storageMenu.isPersonalStorageMode();
        
        switch (operation.toLowerCase()) {
            case "read":
                // Both modes allow reading
                return true;
                
            case "write":
                // Personal storage always allows writing for the owner
                if (isPersonalMode) return true;
                
                // TODO: Replace with actual permission check when StorageMenu API is expanded
                return true; // Placeholder - allow communal storage operations
                
            case "transfer":
                // Similar to write permissions
                // TODO: Replace with actual permission check when StorageMenu API is expanded
                return true; // Placeholder - allow transfer operations
                
            default:
                LOGGER.warn("Unknown storage operation: {}", operation);
                return false;
        }
    }
}