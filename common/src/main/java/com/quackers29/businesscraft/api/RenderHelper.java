package com.quackers29.businesscraft.api;

public interface RenderHelper {
    void renderOverlay(Object guiGraphics, float partialTick, int screenWidth, int screenHeight);

    void registerOverlay(String overlayId, OverlayRenderer overlay);

    void unregisterOverlay(String overlayId);

    void registerWorldRenderCallback(String renderStage, WorldRenderCallback callback);

    Object getPoseStack(Object renderEvent);

    Object getCamera(Object renderEvent);

    float getPartialTick(Object renderEvent);

    String getRenderStage(Object renderEvent);

    boolean isRenderStage(Object renderEvent, String stageName);

    @FunctionalInterface
    interface OverlayRenderer {
        void render(Object guiGraphics, float partialTick, int screenWidth, int screenHeight);
    }

    @FunctionalInterface
    interface WorldRenderCallback {
        void onRender(String renderStage, float partialTick, Object renderEvent);
    }

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
