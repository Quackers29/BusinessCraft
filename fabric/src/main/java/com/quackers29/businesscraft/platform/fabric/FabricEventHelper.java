package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.EventHelper;
import com.quackers29.businesscraft.platform.EventHelper.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric implementation of EventHelper using Yarn mappings.
 * Implements cross-platform event handling using Fabric Event API.
 */
public class FabricEventHelper implements EventHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricEventHelper.class);
    
    @Override
    public void registerBlockInteractionEvent(BlockInteractionHandler handler) {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            try {
                Object result = handler.onBlockInteraction(
                    player, 
                    world, 
                    hand, 
                    hitResult.getBlockPos(), 
                    world.getBlockState(hitResult.getBlockPos()), 
                    hitResult
                );
                
                // Convert platform-specific result to Fabric ActionResult
                if (result != null && result.toString().equals("SUCCESS")) {
                    return ActionResult.SUCCESS;
                } else if (result != null && result.toString().equals("FAIL")) {
                    return ActionResult.FAIL;
                }
                return ActionResult.PASS;
                
            } catch (Exception e) {
                LOGGER.error("Error in block interaction handler: {}", e.getMessage(), e);
                return ActionResult.PASS;
            }
        });
        LOGGER.debug("Registered block interaction event handler for Fabric");
    }
    
    @Override
    public void registerServerStartingEvent(ServerStartingHandler handler) {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> handler.onServerStarting());
    }
    
    @Override
    public void registerServerStoppingEvent(ServerStoppingHandler handler) {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> handler.onServerStopping());
    }
    
    @Override
    public void registerPlayerLoginEvent(PlayerLoginHandler handler) {
        ServerPlayConnectionEvents.JOIN.register((networkHandler, sender, server) -> {
            try {
                handler.onPlayerLogin(networkHandler.player);
            } catch (Exception e) {
                LOGGER.error("Error in player login handler: {}", e.getMessage(), e);
            }
        });
        LOGGER.debug("Registered player login event handler for Fabric");
    }
    
    @Override
    public void registerPlayerLogoutEvent(PlayerLogoutHandler handler) {
        ServerPlayConnectionEvents.DISCONNECT.register((networkHandler, server) -> {
            try {
                handler.onPlayerLogout(networkHandler.player);
            } catch (Exception e) {
                LOGGER.error("Error in player logout handler: {}", e.getMessage(), e);
            }
        });
        LOGGER.debug("Registered player logout event handler for Fabric");
    }
    
    @Override
    public void registerClientSetupEvent(ClientSetupHandler handler) {
        // TODO: Implement client setup event handling
    }
    
    @Override
    public void registerEntityRendererRegistrationEvent(EntityRendererRegistrationHandler handler) {
        // TODO: Implement entity renderer registration
    }
    
    @Override
    public void registerGuiOverlayRegistrationEvent(GuiOverlayRegistrationHandler handler) {
        // TODO: Implement GUI overlay registration
    }
    
    @Override
    public void registerRenderLevelEvent(RenderLevelHandler handler) {
        // TODO: Implement render level event handling
    }
    
    @Override
    public void registerPlayerTickEvent(PlayerTickHandler handler) {
        // TODO: Implement player tick event handling
    }
    
    @Override
    public void registerEntityAttributeRegistrationEvent(EntityAttributeRegistrationHandler handler) {
        // TODO: Implement entity attribute registration
    }
}