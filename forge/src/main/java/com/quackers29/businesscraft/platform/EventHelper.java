package com.quackers29.businesscraft.platform;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Platform abstraction interface for event system operations.
 * This interface provides a common API for handling events across mod loaders.
 */
public interface EventHelper {
    
    /**
     * Registers a block interaction event handler.
     * @param handler The interaction handler
     */
    void registerBlockInteractionEvent(BlockInteractionHandler handler);
    
    /**
     * Registers a server starting event handler.
     * @param handler The server starting handler
     */
    void registerServerStartingEvent(ServerStartingHandler handler);
    
    /**
     * Registers a server stopping event handler.
     * @param handler The server stopping handler
     */
    void registerServerStoppingEvent(ServerStoppingHandler handler);
    
    /**
     * Registers a player login event handler.
     * @param handler The player login handler
     */
    void registerPlayerLoginEvent(PlayerLoginHandler handler);
    
    /**
     * Registers a player logout event handler.
     * @param handler The player logout handler
     */
    void registerPlayerLogoutEvent(PlayerLogoutHandler handler);
    
    /**
     * Registers a client setup event handler.
     * @param handler The client setup handler
     */
    void registerClientSetupEvent(ClientSetupHandler handler);
    
    /**
     * Registers an entity renderer registration event handler.
     * @param handler The entity renderer registration handler
     */
    void registerEntityRendererRegistrationEvent(EntityRendererRegistrationHandler handler);
    
    /**
     * Registers a GUI overlay registration event handler.
     * @param handler The GUI overlay registration handler
     */
    void registerGuiOverlayRegistrationEvent(GuiOverlayRegistrationHandler handler);
    
    /**
     * Registers a render level event handler.
     * @param handler The render level handler
     */
    void registerRenderLevelEvent(RenderLevelHandler handler);
    
    /**
     * Registers a player tick event handler.
     * @param handler The player tick handler
     */
    void registerPlayerTickEvent(PlayerTickHandler handler);
    
    /**
     * Registers an entity attribute registration event handler.
     * @param handler The entity attribute registration handler
     */
    void registerEntityAttributeRegistrationEvent(EntityAttributeRegistrationHandler handler);
    
    /**
     * Functional interface for block interaction events.
     */
    @FunctionalInterface
    interface BlockInteractionHandler {
        InteractionResult onBlockInteraction(Player player, Level level, InteractionHand hand, 
                                           BlockPos pos, BlockState state, BlockHitResult hitResult);
    }
    
    /**
     * Functional interface for server starting events.
     */
    @FunctionalInterface
    interface ServerStartingHandler {
        void onServerStarting();
    }
    
    /**
     * Functional interface for server stopping events.
     */
    @FunctionalInterface
    interface ServerStoppingHandler {
        void onServerStopping();
    }
    
    /**
     * Functional interface for player login events.
     */
    @FunctionalInterface
    interface PlayerLoginHandler {
        void onPlayerLogin(Player player);
    }
    
    /**
     * Functional interface for player logout events.
     */
    @FunctionalInterface
    interface PlayerLogoutHandler {
        void onPlayerLogout(Player player);
    }
    
    /**
     * Functional interface for client setup events.
     */
    @FunctionalInterface
    interface ClientSetupHandler {
        void onClientSetup();
    }
    
    /**
     * Functional interface for entity renderer registration events.
     */
    @FunctionalInterface
    interface EntityRendererRegistrationHandler {
        <T extends LivingEntity> void registerEntityRenderer(EntityType<T> entityType, 
                                                           EntityRendererProvider<T> rendererProvider);
    }
    
    /**
     * Functional interface for GUI overlay registration events.
     */
    @FunctionalInterface
    interface GuiOverlayRegistrationHandler {
        void registerOverlay(String id, Object overlay);
    }
    
    /**
     * Functional interface for render level events.
     */
    @FunctionalInterface
    interface RenderLevelHandler {
        void onRenderLevel(PoseStack poseStack, MultiBufferSource bufferSource, Level level);
    }
    
    /**
     * Functional interface for player tick events.
     */
    @FunctionalInterface
    interface PlayerTickHandler {
        void onPlayerTick(Player player);
    }
    
    /**
     * Functional interface for entity attribute registration events.
     */
    @FunctionalInterface
    interface EntityAttributeRegistrationHandler {
        <T extends LivingEntity> void registerEntityAttributes(EntityType<T> entityType, 
                                                              AttributeSupplier.Builder attributeBuilder);
    }
}