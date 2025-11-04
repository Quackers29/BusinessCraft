package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.EventCallbacks;
import com.quackers29.businesscraft.api.EventHelper;
import com.quackers29.businesscraft.fabric.event.FabricEventCallbackHandler;

import java.util.function.Consumer;

/**
 * Fabric implementation of EventHelper
 */
public class FabricEventHelper implements EventHelper {
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void registerModEvent(Object listener) {
        // Fabric doesn't use mod event bus - events are registered directly
        // This method is kept for interface compatibility but does nothing
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void registerPlatformEvent(Object listener) {
        // Fabric doesn't use platform event bus - events are registered directly
        // This method is kept for interface compatibility but does nothing
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void addServerStoppingListener(Consumer<Void> listener) {
        // Will be handled in FabricEventCallbackHandler
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void addServerStartedListener(Consumer<Void> listener) {
        // Will be handled in FabricEventCallbackHandler
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void addLevelUnloadListener(Consumer<Void> listener) {
        // Will be handled in FabricEventCallbackHandler
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void setActiveTownBlock(Object pos) {
        // Implementation would go here
        // This is used for path creation mode tracking
    }

    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void clearActiveTownBlock() {
        // Implementation would go here
    }
    
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void registerPlayerTickCallback(EventCallbacks.PlayerTickCallback callback) {
        FabricEventCallbackHandler.registerPlayerTickCallback(callback);
    }
    
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void registerPlayerLoginCallback(EventCallbacks.PlayerLoginCallback callback) {
        FabricEventCallbackHandler.registerPlayerLoginCallback(callback);
    }
    
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void registerPlayerLogoutCallback(EventCallbacks.PlayerLogoutCallback callback) {
        FabricEventCallbackHandler.registerPlayerLogoutCallback(callback);
    }
    
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void registerRightClickBlockCallback(EventCallbacks.RightClickBlockCallback callback) {
        FabricEventCallbackHandler.registerRightClickBlockCallback(callback);
    }
    
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void registerClientTickCallback(EventCallbacks.ClientTickCallback callback) {
        FabricEventCallbackHandler.registerClientTickCallback(callback);
    }
    
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void registerKeyInputCallback(EventCallbacks.KeyInputCallback callback) {
        FabricEventCallbackHandler.registerKeyInputCallback(callback);
    }
    
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void registerMouseScrollCallback(EventCallbacks.MouseScrollCallback callback) {
        FabricEventCallbackHandler.registerMouseScrollCallback(callback);
    }
    
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void registerRenderLevelCallback(EventCallbacks.RenderLevelCallback callback) {
        FabricEventCallbackHandler.registerRenderLevelCallback(callback);
    }
    
    // @Override - temporarily removed due to classpath issue with common module interfaces
    public void registerLevelUnloadCallback(EventCallbacks.LevelUnloadCallback callback) {
        FabricEventCallbackHandler.registerLevelUnloadCallback(callback);
    }
}
