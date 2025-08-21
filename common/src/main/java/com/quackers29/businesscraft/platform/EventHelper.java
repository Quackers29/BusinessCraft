package com.quackers29.businesscraft.platform;

/**
 * Platform-agnostic interface for event operations.
 * This interface provides a common API for event handling across mod loaders.
 * 
 * PRAGMATIC APPROACH: Uses generic functional interfaces to avoid Minecraft dependencies
 * while maintaining compatibility with existing code.
 */
public interface EventHelper {
    
    // Functional interfaces using generic types for platform compatibility
    @FunctionalInterface
    interface BlockInteractionHandler {
        Object onBlockInteraction(Object player, Object level, Object hand, 
                                Object pos, Object state, Object hitResult);
    }
    
    @FunctionalInterface
    interface ServerStartingHandler {
        void onServerStarting();
    }
    
    @FunctionalInterface
    interface ServerStoppingHandler {
        void onServerStopping();
    }
    
    @FunctionalInterface
    interface PlayerLoginHandler {
        void onPlayerLogin(Object player);
    }
    
    @FunctionalInterface
    interface PlayerLogoutHandler {
        void onPlayerLogout(Object player);
    }
    
    @FunctionalInterface
    interface ClientSetupHandler {
        void onClientSetup();
    }
    
    @FunctionalInterface
    interface EntityRendererRegistrationHandler {
        void registerEntityRenderer();
    }
    
    @FunctionalInterface
    interface GuiOverlayRegistrationHandler {
        void registerOverlay(String name, Object overlay);
    }
    
    @FunctionalInterface
    interface RenderLevelHandler {
        void onRenderLevel(Object poseStack, Object projectionMatrix, Object level);
    }
    
    @FunctionalInterface
    interface PlayerTickHandler {
        void onPlayerTick(Object player);
    }
    
    @FunctionalInterface
    interface EntityAttributeRegistrationHandler {
        void registerEntityAttributes();
    }
    
    // Event handler registration methods
    void registerBlockInteractionEvent(BlockInteractionHandler handler);
    void registerServerStartingEvent(ServerStartingHandler handler);
    void registerServerStoppingEvent(ServerStoppingHandler handler);
    void registerPlayerLoginEvent(PlayerLoginHandler handler);
    void registerPlayerLogoutEvent(PlayerLogoutHandler handler);
    void registerClientSetupEvent(ClientSetupHandler handler);
    void registerEntityRendererRegistrationEvent(EntityRendererRegistrationHandler handler);
    void registerGuiOverlayRegistrationEvent(GuiOverlayRegistrationHandler handler);
    void registerRenderLevelEvent(RenderLevelHandler handler);
    void registerPlayerTickEvent(PlayerTickHandler handler);
    void registerEntityAttributeRegistrationEvent(EntityAttributeRegistrationHandler handler);
}