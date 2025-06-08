package com.yourdomain.businesscraft.ui.state;

import net.minecraft.network.chat.Component;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.resources.ResourceLocation;

/**
 * Manages the state for the TownBlockInterface UI
 * Centralizes all UI state and provides change notifications
 */
public class TownInterfaceState {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownInterfaceState.class);
    
    // State listeners
    private final List<Consumer<TownInterfaceState>> stateChangeListeners = new ArrayList<>();
    
    // UI State
    private String activeTabId = "town";
    private boolean isEditingTownName = false;
    private boolean isSettingPlatformPath = false;
    private UUID platformBeingEdited = null;
    private Component instructionsText = null;
    
    // Town Data State
    private String townName = "New Town";
    private int population = 5;
    private int touristCount = 0;
    private int maxTourists = 5;
    private int searchRadius = 10;
    private boolean autoCollectEnabled = false;
    private boolean taxesEnabled = false;
    
    // Resource State
    private Map<String, Integer> resources = new HashMap<>();
    
    // Visit History State
    private List<VisitHistoryEntry> visitHistory = new ArrayList<>();
    
    /**
     * Add a listener for state changes
     */
    public void addStateChangeListener(Consumer<TownInterfaceState> listener) {
        stateChangeListeners.add(listener);
    }
    
    /**
     * Remove a state change listener
     */
    public void removeStateChangeListener(Consumer<TownInterfaceState> listener) {
        stateChangeListeners.remove(listener);
    }
    
    /**
     * Notify all listeners of state changes
     */
    private void notifyStateChange() {
        for (Consumer<TownInterfaceState> listener : stateChangeListeners) {
            try {
                listener.accept(this);
            } catch (Exception e) {
                LOGGER.error("Error notifying state change listener", e);
            }
        }
    }
    
    // Getters and setters for UI state
    public String getActiveTabId() {
        return activeTabId;
    }
    
    public void setActiveTabId(String tabId) {
        if (!this.activeTabId.equals(tabId)) {
            this.activeTabId = tabId;
            notifyStateChange();
        }
    }
    
    public boolean isEditingTownName() {
        return isEditingTownName;
    }
    
    public void setEditingTownName(boolean editing) {
        if (this.isEditingTownName != editing) {
            this.isEditingTownName = editing;
            notifyStateChange();
        }
    }
    
    public boolean isSettingPlatformPath() {
        return isSettingPlatformPath;
    }
    
    public void setSettingPlatformPath(boolean setting) {
        if (this.isSettingPlatformPath != setting) {
            this.isSettingPlatformPath = setting;
            notifyStateChange();
        }
    }
    
    public UUID getPlatformBeingEdited() {
        return platformBeingEdited;
    }
    
    public void setPlatformBeingEdited(UUID platformId) {
        if (this.platformBeingEdited != platformId) {
            this.platformBeingEdited = platformId;
            notifyStateChange();
        }
    }
    
    public Component getInstructionsText() {
        return instructionsText;
    }
    
    public void setInstructionsText(Component text) {
        if (this.instructionsText != text) {
            this.instructionsText = text;
            notifyStateChange();
        }
    }
    
    // Getters and setters for town data state
    public String getTownName() {
        return townName;
    }
    
    public void setTownName(String name) {
        if (!this.townName.equals(name)) {
            this.townName = name;
            notifyStateChange();
        }
    }
    
    public int getPopulation() {
        return population;
    }
    
    public void setPopulation(int pop) {
        if (this.population != pop) {
            this.population = pop;
            notifyStateChange();
        }
    }
    
    public int getTouristCount() {
        return touristCount;
    }
    
    public void setTouristCount(int count) {
        if (this.touristCount != count) {
            this.touristCount = count;
            notifyStateChange();
        }
    }
    
    public int getMaxTourists() {
        return maxTourists;
    }
    
    public void setMaxTourists(int max) {
        if (this.maxTourists != max) {
            this.maxTourists = max;
            notifyStateChange();
        }
    }
    
    public int getSearchRadius() {
        return searchRadius;
    }
    
    public void setSearchRadius(int radius) {
        if (this.searchRadius != radius) {
            this.searchRadius = radius;
            notifyStateChange();
        }
    }
    
    public boolean isAutoCollectEnabled() {
        return autoCollectEnabled;
    }
    
    public void setAutoCollectEnabled(boolean enabled) {
        if (this.autoCollectEnabled != enabled) {
            this.autoCollectEnabled = enabled;
            notifyStateChange();
        }
    }
    
    public boolean isTaxesEnabled() {
        return taxesEnabled;
    }
    
    public void setTaxesEnabled(boolean enabled) {
        if (this.taxesEnabled != enabled) {
            this.taxesEnabled = enabled;
            notifyStateChange();
        }
    }
    
    // Resource state methods
    public Map<String, Integer> getResources() {
        return new HashMap<>(resources);
    }
    
    public void setResources(Map<String, Integer> newResources) {
        if (!this.resources.equals(newResources)) {
            this.resources = new HashMap<>(newResources);
            notifyStateChange();
        }
    }
    
    // Visit history state methods
    public List<VisitHistoryEntry> getVisitHistory() {
        return new ArrayList<>(visitHistory);
    }
    
    public void setVisitHistory(List<VisitHistoryEntry> history) {
        if (!this.visitHistory.equals(history)) {
            this.visitHistory = new ArrayList<>(history);
            notifyStateChange();
        }
    }
    
    /**
     * Record for storing visit history entries
     */
    public static class VisitHistoryEntry {
        private final long timestamp;
        private final String townName;
        private final int count;
        private final String direction;
        
        public VisitHistoryEntry(long timestamp, String townName, int count, String direction) {
            this.timestamp = timestamp;
            this.townName = townName;
            this.count = count;
            this.direction = direction;
        }
        
        public long getTimestamp() { return timestamp; }
        public String getTownName() { return townName; }
        public int getCount() { return count; }
        public String getDirection() { return direction; }
    }

    public static class ResourceEntry {
        private final String name;
        private final int amount;
        private final ResourceLocation icon;
        
        public ResourceEntry(String name, int amount, ResourceLocation icon) {
            this.name = name;
            this.amount = amount;
            this.icon = icon;
        }
        
        public String getName() { return name; }
        public int getAmount() { return amount; }
        public ResourceLocation getIcon() { return icon; }
    }
} 