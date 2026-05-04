package com.quackers29.businesscraft.event;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.client.render.world.PlatformVisualizationRenderer;
import com.quackers29.businesscraft.client.render.world.TownBoundaryVisualizationRenderer;
import com.quackers29.businesscraft.client.render.world.VisualizationManager;
import net.minecraft.world.level.Level;

/**
 * Client-side rendering events for modular world visualization system.
 * Manages the new modular 3D line rendering framework for platform and other visualizations.
 */
public class ClientRenderEvents {
    
    private static PlatformVisualizationRenderer platformRenderer;
    
    private static TownBoundaryVisualizationRenderer boundaryRenderer;
    
    private static boolean initialized = false;
    
    /**
     * Initialize event callbacks. Should be called during mod initialization.
     * Also initializes renderers lazily to avoid class loading issues on Fabric.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        platformRenderer = new PlatformVisualizationRenderer();
        boundaryRenderer = new TownBoundaryVisualizationRenderer();
        
        VisualizationManager.getInstance().registerRenderer(
            VisualizationManager.TYPE_PLATFORM, 
            platformRenderer
        );
        
        VisualizationManager.getInstance().registerRenderer(
            VisualizationManager.TYPE_TOWN_BOUNDARY, 
            boundaryRenderer
        );
        
        PlatformAccess.getEvents().registerRenderLevelCallback(ClientRenderEvents::onRenderLevelStage);
        PlatformAccess.getEvents().registerLevelUnloadCallback(ClientRenderEvents::onLevelUnload);
        
        initialized = true;
    }
    
    private static void onRenderLevelStage(String renderStage, float partialTick, Object eventObject) {
        if (!initialized) {
            initialize();
        }
        
        if (platformRenderer != null) {
            platformRenderer.render(renderStage, partialTick, eventObject);
        }
        if (boundaryRenderer != null) {
            boundaryRenderer.render(renderStage, partialTick, eventObject);
        }
    }
    
    /**
     * Clean up visualization state when the player changes worlds
     */
    private static void onLevelUnload(Level level) {
        if (level.isClientSide()) {
            VisualizationManager.getInstance().onLevelUnload();
        }
    }
    
    /**
     * Get the platform renderer (for platform-specific rendering code)
     */
    public static PlatformVisualizationRenderer getPlatformRenderer() {
        if (!initialized) {
            initialize();
        }
        return platformRenderer;
    }
    
    /**
     * Get the boundary renderer (for platform-specific rendering code)
     */
    public static TownBoundaryVisualizationRenderer getBoundaryRenderer() {
        if (!initialized) {
            initialize();
        }
        return boundaryRenderer;
    }
}
