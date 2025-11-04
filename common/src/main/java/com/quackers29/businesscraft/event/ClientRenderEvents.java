package com.quackers29.businesscraft.event;

import com.quackers29.businesscraft.api.EventCallbacks;
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
    
    // Platform visualization renderer using the new modular system
    private static final PlatformVisualizationRenderer platformRenderer = new PlatformVisualizationRenderer();
    
    // Town boundary visualization renderer
    private static final TownBoundaryVisualizationRenderer boundaryRenderer = new TownBoundaryVisualizationRenderer();
    
    static {
        // Register the platform renderer with the visualization manager
        VisualizationManager.getInstance().registerRenderer(
            VisualizationManager.TYPE_PLATFORM, 
            platformRenderer
        );
        
        // Register the town boundary renderer with the visualization manager
        VisualizationManager.getInstance().registerRenderer(
            VisualizationManager.TYPE_TOWN_BOUNDARY, 
            boundaryRenderer
        );
    }
    
    /**
     * Initialize event callbacks. Should be called during mod initialization.
     */
    public static void initialize() {
        PlatformAccess.getEvents().registerRenderLevelCallback(ClientRenderEvents::onRenderLevelStage);
        PlatformAccess.getEvents().registerLevelUnloadCallback(ClientRenderEvents::onLevelUnload);
    }
    
    private static void onRenderLevelStage(String renderStage, float partialTick, Object eventObject) {
        // The renderers need RenderLevelStageEvent, which is Forge-specific.
        // The actual rendering will be handled in the Forge event handler.
        // This method is kept for potential future use or non-render logic.
    }
    
    /**
     * Clean up visualization state when the player changes worlds
     */
    private static void onLevelUnload(Level level) {
        if (level.isClientSide()) {
            VisualizationManager.getInstance().onLevelUnload();
            // Boundary data is cleaned up automatically by the renderer's cleanup method
        }
    }
    
    /**
     * Get the platform renderer (for platform-specific rendering code)
     */
    public static PlatformVisualizationRenderer getPlatformRenderer() {
        return platformRenderer;
    }
    
    /**
     * Get the boundary renderer (for platform-specific rendering code)
     */
    public static TownBoundaryVisualizationRenderer getBoundaryRenderer() {
        return boundaryRenderer;
    }
}
