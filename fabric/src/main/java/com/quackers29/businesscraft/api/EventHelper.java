package com.quackers29.businesscraft.api;

/**
 * Interface for event handling operations
 */
public interface EventHelper {
    void registerModEvent(Object listener);
    void registerPlatformEvent(Object listener);
    void addServerStoppingListener(java.util.function.Consumer<Void> listener);
    void addServerStartedListener(java.util.function.Consumer<Void> listener);
    void addLevelUnloadListener(java.util.function.Consumer<Void> listener);
    void setActiveTownBlock(Object pos);
    void clearActiveTownBlock();
}
