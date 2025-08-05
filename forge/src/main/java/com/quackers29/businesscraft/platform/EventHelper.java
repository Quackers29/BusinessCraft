package com.quackers29.businesscraft.platform;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

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
}