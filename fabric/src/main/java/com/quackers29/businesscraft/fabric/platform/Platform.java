package com.quackers29.businesscraft.fabric.platform;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

/**
 * Fabric implementation of Platform - represents a tourist platform in BusinessCraft
 */
public class Platform {
    private UUID id;
    private String name;
    private boolean enabled;
    private Object startPos;
    private Object endPos;
    private Set<UUID> enabledDestinations = new HashSet<>();
    private Runnable changeCallback;

    public Platform() {
        this.id = UUID.randomUUID();
        this.name = "New Platform";
        this.enabled = true;
    }

    public Platform(UUID id) {
        this.id = id;
        this.name = "New Platform";
        this.enabled = true;
    }

    public Platform(String name, boolean enabled, Object startPos, Object endPos) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.enabled = enabled;
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public static Platform fromNBT(Object tag) {
        return new Platform(); // Placeholder
    }

    public Object save() {
        return new Object(); // Placeholder
    }

    public void tick() {
        // TODO: Implement platform tick logic
    }

    public void setChangeCallback(Runnable callback) {
        this.changeCallback = callback;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Object getStartPos() { return startPos; }
    public void setStartPos(Object startPos) { this.startPos = startPos; }
    public Object getEndPos() { return endPos; }
    public void setEndPos(Object endPos) { this.endPos = endPos; }
    public Set<UUID> getEnabledDestinations() { return enabledDestinations; }
}
