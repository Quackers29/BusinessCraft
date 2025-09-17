package com.quackers29.businesscraft.event;

// BusinessCraft moved to platform-specific module
import com.quackers29.businesscraft.client.render.world.PlatformVisualizationRenderer;
import com.quackers29.businesscraft.client.render.world.TownBoundaryVisualizationRenderer;
import com.quackers29.businesscraft.client.render.world.VisualizationManager;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Use the new modular rendering system
        platformRenderer.render(event);
        boundaryRenderer.render(event);
        
        // Future: Additional renderer types can be added here
        // routeRenderer.render(event);
        // debugRenderer.render(event);
        // territoryRenderer.render(event);
    }
    
    /**
     * Clean up visualization state when the player changes worlds
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            VisualizationManager.getInstance().onLevelUnload();
            // Boundary data is cleaned up automatically by the renderer's cleanup method
        }
    }
}
