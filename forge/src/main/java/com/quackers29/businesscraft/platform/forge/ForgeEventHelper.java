package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.EventHelper;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge implementation of the EventHelper interface using MinecraftForge event bus.
 */
public class ForgeEventHelper implements EventHelper {
    
    private final List<BlockInteractionHandler> blockInteractionHandlers = new ArrayList<>();
    private final List<ServerStartingHandler> serverStartingHandlers = new ArrayList<>();
    private final List<ServerStoppingHandler> serverStoppingHandlers = new ArrayList<>();
    private final List<PlayerLoginHandler> playerLoginHandlers = new ArrayList<>();
    private final List<PlayerLogoutHandler> playerLogoutHandlers = new ArrayList<>();
    
    public ForgeEventHelper() {
        // Register this instance as an event listener
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @Override
    public void registerBlockInteractionEvent(BlockInteractionHandler handler) {
        blockInteractionHandlers.add(handler);
    }
    
    @Override
    public void registerServerStartingEvent(ServerStartingHandler handler) {
        serverStartingHandlers.add(handler);
    }
    
    @Override
    public void registerServerStoppingEvent(ServerStoppingHandler handler) {
        serverStoppingHandlers.add(handler);
    }
    
    @Override
    public void registerPlayerLoginEvent(PlayerLoginHandler handler) {
        playerLoginHandlers.add(handler);
    }
    
    @Override
    public void registerPlayerLogoutEvent(PlayerLogoutHandler handler) {
        playerLogoutHandlers.add(handler);
    }
    
    // Forge event handlers that delegate to our platform-agnostic handlers
    
    @SubscribeEvent
    public void onBlockInteraction(PlayerInteractEvent.RightClickBlock event) {
        for (BlockInteractionHandler handler : blockInteractionHandlers) {
            InteractionResult result = handler.onBlockInteraction(
                event.getEntity(),
                event.getLevel(),
                event.getHand(),
                event.getPos(),
                event.getLevel().getBlockState(event.getPos()),
                event.getHitVec()
            );
            
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
                break;
            }
        }
    }
    
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        for (ServerStartingHandler handler : serverStartingHandlers) {
            handler.onServerStarting();
        }
    }
    
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        for (ServerStoppingHandler handler : serverStoppingHandlers) {
            handler.onServerStopping();
        }
    }
    
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        for (PlayerLoginHandler handler : playerLoginHandlers) {
            handler.onPlayerLogin(event.getEntity());
        }
    }
    
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        for (PlayerLogoutHandler handler : playerLogoutHandlers) {
            handler.onPlayerLogout(event.getEntity());
        }
    }
}