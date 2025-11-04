package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.EventCallbacks;
import com.quackers29.businesscraft.api.EventHelper;
import com.quackers29.businesscraft.fabric.event.FabricEventCallbackHandler;

import java.util.function.Consumer;

/**
 * Fabric implementation of EventHelper
 * Note: @Override annotations are omitted because EventHelper interface from common module
 * is only available at runtime via JAR, not at compile time. Methods still implement the interface correctly at runtime.
 */
public class FabricEventHelper implements EventHelper {
    public void registerModEvent(Object listener) {
        // Fabric doesn't use mod event bus - events are registered directly
        // This method is kept for interface compatibility but does nothing
    }

    public void registerPlatformEvent(Object listener) {
        // Fabric doesn't use platform event bus - events are registered directly
        // This method is kept for interface compatibility but does nothing
    }

    public void addServerStoppingListener(Consumer<Void> listener) {
        // Will be handled in FabricEventCallbackHandler
    }

    public void addServerStartedListener(Consumer<Void> listener) {
        // Will be handled in FabricEventCallbackHandler
    }

    public void addLevelUnloadListener(Consumer<Void> listener) {
        // Will be handled in FabricEventCallbackHandler
    }

    public void setActiveTownBlock(Object pos) {
        com.quackers29.businesscraft.fabric.event.FabricModEvents.setActiveTownBlock(pos);
    }

    public void clearActiveTownBlock() {
        com.quackers29.businesscraft.fabric.event.FabricModEvents.clearActiveTownBlock();
    }
    
    public void registerPlayerTickCallback(EventCallbacks.PlayerTickCallback callback) {
        FabricEventCallbackHandler.registerPlayerTickCallback(callback);
    }
    
    public void registerPlayerLoginCallback(EventCallbacks.PlayerLoginCallback callback) {
        FabricEventCallbackHandler.registerPlayerLoginCallback(callback);
    }
    
    public void registerPlayerLogoutCallback(EventCallbacks.PlayerLogoutCallback callback) {
        FabricEventCallbackHandler.registerPlayerLogoutCallback(callback);
    }
    
    public void registerRightClickBlockCallback(EventCallbacks.RightClickBlockCallback callback) {
        FabricEventCallbackHandler.registerRightClickBlockCallback(callback);
    }
    
    public void registerClientTickCallback(EventCallbacks.ClientTickCallback callback) {
        FabricEventCallbackHandler.registerClientTickCallback(callback);
    }
    
    public void registerKeyInputCallback(EventCallbacks.KeyInputCallback callback) {
        FabricEventCallbackHandler.registerKeyInputCallback(callback);
    }
    
    public void registerMouseScrollCallback(EventCallbacks.MouseScrollCallback callback) {
        FabricEventCallbackHandler.registerMouseScrollCallback(callback);
    }
    
    public void registerRenderLevelCallback(EventCallbacks.RenderLevelCallback callback) {
        FabricEventCallbackHandler.registerRenderLevelCallback(callback);
    }
    
    public void registerLevelUnloadCallback(EventCallbacks.LevelUnloadCallback callback) {
        FabricEventCallbackHandler.registerLevelUnloadCallback(callback);
    }
}
