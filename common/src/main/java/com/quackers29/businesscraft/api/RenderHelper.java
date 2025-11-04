package com.quackers29.businesscraft.api;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Platform-agnostic interface for rendering operations.
 * Provides abstractions for overlay rendering and world rendering stages.
 */
public interface RenderHelper {
    /**
     * Render overlay on the screen (platform-agnostic version of IGuiOverlay)
     * @param guiGraphics The GUI graphics context
     * @param partialTick Partial tick for interpolation
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     */
    void renderOverlay(GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight);
    
    /**
     * Register an overlay renderer
     * @param overlayId Unique identifier for the overlay
     * @param overlay The overlay renderer to register
     */
    void registerOverlay(String overlayId, OverlayRenderer overlay);
    
    /**
     * Unregister an overlay renderer
     * @param overlayId Unique identifier for the overlay
     */
    void unregisterOverlay(String overlayId);
    
    /**
     * Register a world render callback
     * @param renderStage The render stage name (e.g., "AFTER_TRANSLUCENT_BLOCKS")
     * @param callback The callback to invoke at that stage
     */
    void registerWorldRenderCallback(String renderStage, WorldRenderCallback callback);
    
    /**
     * Get the PoseStack from a render event object
     * @param renderEvent The platform-specific render event object
     * @return The PoseStack, or null if not available
     */
    Object getPoseStack(Object renderEvent);
    
    /**
     * Get the camera from a render event object
     * @param renderEvent The platform-specific render event object
     * @return The camera, or null if not available
     */
    Object getCamera(Object renderEvent);
    
    /**
     * Get the partial tick from a render event object
     * @param renderEvent The platform-specific render event object
     * @return The partial tick value
     */
    float getPartialTick(Object renderEvent);
    
    /**
     * Get the render stage name from a render event object
     * @param renderEvent The platform-specific render event object
     * @return The render stage name as a string
     */
    String getRenderStage(Object renderEvent);
    
    /**
     * Check if a render stage matches a given stage name
     * @param renderEvent The platform-specific render event object
     * @param stageName The stage name to check
     * @return true if the stages match
     */
    boolean isRenderStage(Object renderEvent, String stageName);
    
    /**
     * Platform-agnostic overlay renderer interface
     */
    @FunctionalInterface
    interface OverlayRenderer {
        void render(GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight);
    }
    
    /**
     * Platform-agnostic world render callback interface
     */
    @FunctionalInterface
    interface WorldRenderCallback {
        /**
         * Called when rendering at a specific stage
         * @param renderStage The render stage name
         * @param partialTick Partial tick for interpolation
         * @param renderEvent The platform-specific render event object
         */
        void onRender(String renderStage, float partialTick, Object renderEvent);
    }
    
    /**
     * Render stage constants (platform-agnostic names)
     */
    class RenderStage {
        public static final String AFTER_TRANSLUCENT_BLOCKS = "AFTER_TRANSLUCENT_BLOCKS";
        public static final String AFTER_ENTITIES = "AFTER_ENTITIES";
        public static final String AFTER_PARTICLES = "AFTER_PARTICLES";
        public static final String AFTER_WEATHER = "AFTER_WEATHER";
        public static final String AFTER_SKY = "AFTER_SKY";
        public static final String AFTER_SOLID_BLOCKS = "AFTER_SOLID_BLOCKS";
        public static final String AFTER_CUTOUT_MIPPED_BLOCKS = "AFTER_CUTOUT_MIPPED_BLOCKS";
        public static final String AFTER_CUTOUT_BLOCKS = "AFTER_CUTOUT_BLOCKS";
        public static final String AFTER_TRIPWIRE_BLOCKS = "AFTER_TRIPWIRE_BLOCKS";
    }
}

