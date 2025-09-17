package com.quackers29.businesscraft.ui.modal.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * Handles mouse events, tooltips, and user interactions for modal inventory screens.
 * Extracted from BCModalInventoryScreen to follow single responsibility principle.
 * Manages click handling, drag operations, tooltips, and keyboard events.
 */
public class ModalEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModalEventHandler.class);
    
    private final ModalRenderingEngine renderingEngine;
    
    // Mouse state tracking
    private boolean isDragging = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private final Set<Integer> affectedDragSlots = new HashSet<>();
    
    // Tooltip state
    private String customTooltip = null;
    private int tooltipX = 0;
    private int tooltipY = 0;
    
    // Click debouncing
    private long lastClickTime = 0;
    private static final long CLICK_DEBOUNCE_MS = 100;
    
    public ModalEventHandler(ModalRenderingEngine renderingEngine) {
        this.renderingEngine = renderingEngine;
    }
    
    /**
     * Handles mouse click events.
     */
    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        // Debounce rapid clicks
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < CLICK_DEBOUNCE_MS) {
            return false;
        }
        lastClickTime = currentTime;
        
        // Track mouse position for potential drag operations
        dragStartX = (int) mouseX;
        dragStartY = (int) mouseY;
        
        LOGGER.debug("Mouse click at ({}, {}) with button {}", mouseX, mouseY, button);
        return false; // Allow other handlers to process
    }
    
    /**
     * Handles mouse drag events.
     */
    public boolean handleMouseDrag(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) { // Left mouse button
            if (!isDragging && (Math.abs(dragX) > 3 || Math.abs(dragY) > 3)) {
                // Start drag operation
                isDragging = true;
                affectedDragSlots.clear();
                LOGGER.debug("Started drag operation");
            }
            
            if (isDragging) {
                // Continue drag operation
                updateDragAffectedSlots((int) mouseX, (int) mouseY);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Handles mouse release events.
     */
    public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
        if (isDragging && button == 0) {
            // End drag operation
            isDragging = false;
            
            if (!affectedDragSlots.isEmpty()) {
                LOGGER.debug("Completed drag operation affecting {} slots", affectedDragSlots.size());
                // Process drag results here if needed
            }
            
            affectedDragSlots.clear();
            return true;
        }
        
        return false;
    }
    
    /**
     * Handles slot click events with custom logic.
     */
    public boolean handleSlotClick(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (slot == null) return false;
        
        // Log slot interaction for debugging
        LOGGER.debug("Slot {} clicked with button {} and type {}", slotId, mouseButton, clickType);
        
        // Handle special click types
        switch (clickType) {
            case QUICK_MOVE:
                return handleQuickMove(slot, slotId);
                
            case QUICK_CRAFT:
                return handleQuickCraft(slot, slotId, mouseButton);
                
            case CLONE:
                return handleClone(slot, slotId);
                
            case THROW:
                return handleThrow(slot, slotId, mouseButton);
                
            default:
                // Standard pickup/place behavior - let vanilla handle it
                return false;
        }
    }
    
    /**
     * Handles quick move (shift-click) operations.
     */
    private boolean handleQuickMove(Slot slot, int slotId) {
        if (!slot.hasItem()) return false;
        
        ItemStack stack = slot.getItem();
        LOGGER.debug("Quick move for slot {} with item {}", slotId, stack.getDisplayName().getString());
        
        // Custom quick move logic could go here
        // For now, let vanilla handle it
        return false;
    }
    
    /**
     * Handles quick craft operations.
     */
    private boolean handleQuickCraft(Slot slot, int slotId, int mouseButton) {
        LOGGER.debug("Quick craft for slot {} with button {}", slotId, mouseButton);
        
        // Track slots affected by quick craft
        affectedDragSlots.add(slotId);
        
        return false; // Let vanilla handle the actual crafting
    }
    
    /**
     * Handles clone (middle-click) operations.
     */
    private boolean handleClone(Slot slot, int slotId) {
        if (!slot.hasItem()) return false;
        
        ItemStack stack = slot.getItem();
        LOGGER.debug("Clone for slot {} with item {}", slotId, stack.getDisplayName().getString());
        
        // Creative mode middle-click cloning - let vanilla handle it
        return false;
    }
    
    /**
     * Handles throw (Q key) operations.
     */
    private boolean handleThrow(Slot slot, int slotId, int mouseButton) {
        if (!slot.hasItem()) return false;
        
        ItemStack stack = slot.getItem();
        int throwCount = mouseButton == 1 ? stack.getCount() : 1; // Right click = throw all
        
        LOGGER.debug("Throw {} of item {} from slot {}", throwCount, stack.getDisplayName().getString(), slotId);
        
        // Let vanilla handle the throwing
        return false;
    }
    
    /**
     * Updates the set of slots affected by current drag operation.
     */
    private void updateDragAffectedSlots(int mouseX, int mouseY) {
        // This would typically check which slots the mouse is currently over
        // and add them to the affected slots set
        
        // Placeholder implementation - would need actual slot bounds checking
        // affectedDragSlots.add(getSlotUnderMouse(mouseX, mouseY));
    }
    
    /**
     * Renders custom tooltips.
     */
    public void renderCustomTooltips(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
        if (customTooltip != null && !customTooltip.isEmpty()) {
            renderTooltip(guiGraphics, font, customTooltip, tooltipX, tooltipY);
        }
        
        // Render drag state information
        if (isDragging) {
            renderDragInfo(guiGraphics, font, mouseX, mouseY);
        }
    }
    
    /**
     * Renders a tooltip at the specified position.
     */
    public void renderTooltip(GuiGraphics guiGraphics, Font font, String tooltip, int x, int y) {
        if (tooltip == null || tooltip.isEmpty()) return;
        
        String[] lines = tooltip.split("\\n");
        int maxWidth = 0;
        
        // Calculate tooltip dimensions
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        
        int tooltipHeight = lines.length * font.lineHeight;
        
        // Adjust position to keep tooltip on screen
        int adjustedX = Math.max(5, Math.min(x, guiGraphics.guiWidth() - maxWidth - 10));
        int adjustedY = Math.max(5, Math.min(y - tooltipHeight - 5, guiGraphics.guiHeight() - tooltipHeight - 10));
        
        // Render tooltip background
        renderingEngine.renderTooltipBackground(guiGraphics, adjustedX, adjustedY, maxWidth, tooltipHeight);
        
        // Render tooltip text
        for (int i = 0; i < lines.length; i++) {
            renderingEngine.renderLabel(guiGraphics, font, lines[i], 
                                      adjustedX, adjustedY + i * font.lineHeight, 0xFFFFFFFF);
        }
    }
    
    /**
     * Renders drag operation information.
     */
    private void renderDragInfo(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
        if (affectedDragSlots.isEmpty()) return;
        
        String dragInfo = String.format("Dragging over %d slot(s)", affectedDragSlots.size());
        renderTooltip(guiGraphics, font, dragInfo, mouseX + 10, mouseY - 20);
    }
    
    /**
     * Sets a custom tooltip to be displayed.
     */
    public void setCustomTooltip(String tooltip, int x, int y) {
        this.customTooltip = tooltip;
        this.tooltipX = x;
        this.tooltipY = y;
    }
    
    /**
     * Clears the custom tooltip.
     */
    public void clearCustomTooltip() {
        this.customTooltip = null;
    }
    
    /**
     * Handles keyboard input.
     */
    public boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case 256: // Escape key
                LOGGER.debug("Escape key pressed");
                return true; // Signal that we want to close
                
            case 69: // E key (inventory toggle)
                LOGGER.debug("Inventory key pressed");
                return true; // Signal that we want to close
                
            case 81: // Q key (throw item)
                if (isDragging) {
                    // Cancel drag operation
                    isDragging = false;
                    affectedDragSlots.clear();
                    LOGGER.debug("Drag operation cancelled by Q key");
                    return true;
                }
                break;
                
            default:
                // Let other handlers process unknown keys
                break;
        }
        
        return false;
    }
    
    /**
     * Checks if a point is within a rectangular area.
     */
    public boolean isPointInArea(int pointX, int pointY, int areaX, int areaY, int areaWidth, int areaHeight) {
        return pointX >= areaX && pointX <= areaX + areaWidth &&
               pointY >= areaY && pointY <= areaY + areaHeight;
    }
    
    /**
     * Calculates the distance between two points.
     */
    public double distanceBetweenPoints(int x1, int y1, int x2, int y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Gets information about the current drag state.
     */
    public DragState getDragState() {
        return new DragState(isDragging, dragStartX, dragStartY, new HashSet<>(affectedDragSlots));
    }
    
    /**
     * Resets all event handler state.
     */
    public void reset() {
        isDragging = false;
        dragStartX = 0;
        dragStartY = 0;
        affectedDragSlots.clear();
        customTooltip = null;
        tooltipX = 0;
        tooltipY = 0;
        lastClickTime = 0;
    }
    
    /**
     * Immutable drag state information.
     */
    public static class DragState {
        public final boolean isDragging;
        public final int startX;
        public final int startY;
        public final Set<Integer> affectedSlots;
        
        public DragState(boolean isDragging, int startX, int startY, Set<Integer> affectedSlots) {
            this.isDragging = isDragging;
            this.startX = startX;
            this.startY = startY;
            this.affectedSlots = affectedSlots;
        }
    }
}
