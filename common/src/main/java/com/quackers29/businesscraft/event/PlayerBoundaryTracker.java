package com.quackers29.businesscraft.event;

// BusinessCraft moved to platform-specific module
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "businesscraft")
public class PlayerBoundaryTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerBoundaryTracker.class);
    
    // Rate limiting constants
    private static final long MESSAGE_COOLDOWN_MS = 2000; // 2 seconds between messages
    private static final int TRACKING_INTERVAL_TICKS = 10; // Check every 10 ticks (0.5 seconds)
    
    // Player tracking data
    private static final Map<UUID, PlayerBoundaryState> playerStates = new ConcurrentHashMap<>();
    private static int tickCounter = 0;
    
    /**
     * Internal class to track player boundary state
     */
    private static class PlayerBoundaryState {
        public Town currentTown = null;
        public BlockPos lastPosition = null;
        public long lastNotificationTime = 0;
        
        public PlayerBoundaryState(BlockPos initialPosition) {
            this.lastPosition = initialPosition;
        }
        
        public boolean canSendNotification() {
            return System.currentTimeMillis() - lastNotificationTime >= MESSAGE_COOLDOWN_MS;
        }
        
        public void markNotificationSent() {
            this.lastNotificationTime = System.currentTimeMillis();
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Early return if player tracking is disabled
        if (!ConfigLoader.playerTracking || !ConfigLoader.townBoundaryMessages) {
            return;
        }
        
        // Only process on server side and end phase
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // Rate limiting - only check every TRACKING_INTERVAL_TICKS
        if (++tickCounter % TRACKING_INTERVAL_TICKS != 0) {
            return;
        }
        
        ServerPlayer player = (ServerPlayer) event.player;
        ServerLevel level = player.serverLevel();
        UUID playerId = player.getUUID();
        BlockPos currentPos = player.blockPosition();
        
        // Get or create player state
        PlayerBoundaryState state = playerStates.computeIfAbsent(playerId, 
            k -> new PlayerBoundaryState(currentPos));
        
        // Skip if player hasn't moved significantly (optimization)
        if (state.lastPosition != null && state.lastPosition.distSqr(currentPos) < 16) { // 4 block threshold
            return;
        }
        
        // Update last position
        state.lastPosition = currentPos;
        
        // Check for town boundary changes
        checkBoundaryTransition(player, level, state, currentPos);
    }
    
    /**
     * Checks if the player has crossed any town boundaries and sends appropriate messages
     */
    private static void checkBoundaryTransition(ServerPlayer player, ServerLevel level, 
                                               PlayerBoundaryState state, BlockPos playerPos) {
        try {
            TownManager townManager = TownManager.get(level);
            Town newTown = findTownAtPosition(townManager, playerPos);
            
            // Check for boundary transition
            if (state.currentTown != newTown) {
                handleBoundaryTransition(player, state, state.currentTown, newTown);
                state.currentTown = newTown;
            }
        } catch (Exception e) {
            LOGGER.warn("Error checking boundary transition for player {}: {}", 
                player.getName().getString(), e.getMessage());
        }
    }
    
    /**
     * Finds the town that contains the given position, if any
     */
    private static Town findTownAtPosition(TownManager townManager, BlockPos playerPos) {
        for (Town town : townManager.getAllTowns().values()) {
            double distance = Math.sqrt(playerPos.distSqr(town.getPosition()));
            if (distance <= town.getBoundaryRadius()) {
                return town;
            }
        }
        return null; // Player is not in any town boundary
    }
    
    /**
     * Handles the boundary transition and sends appropriate messages
     */
    private static void handleBoundaryTransition(ServerPlayer player, PlayerBoundaryState state, 
                                                Town oldTown, Town newTown) {
        // Rate limiting check
        if (!state.canSendNotification()) {
            return;
        }
        
        if (oldTown == null && newTown != null) {
            // Entering a town
            sendWelcomeMessage(player, newTown.getName());
            state.markNotificationSent();
            
        } else if (oldTown != null && newTown == null) {
            // Leaving a town
            sendLeavingMessage(player, oldTown.getName());
            state.markNotificationSent();
            
        } else if (oldTown != null && newTown != null && !oldTown.getId().equals(newTown.getId())) {
            // Moving from one town to another (rare case with overlapping boundaries)
            sendLeavingMessage(player, oldTown.getName());
            // Small delay to avoid message spam, but still send the welcome message
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
            sendWelcomeMessage(player, newTown.getName());
            state.markNotificationSent();
        }
    }
    
    /**
     * Sends a welcome message to the player above the hotbar (action bar)
     */
    private static void sendWelcomeMessage(ServerPlayer player, String townName) {
        String message = "🏘️ Welcome to " + townName;
        Component messageComponent = Component.literal(message);
        
        // Send as action bar message (appears above hotbar, not in chat)
        player.displayClientMessage(messageComponent, true);
        
        LOGGER.info("Sent welcome message to {}: {}", player.getName().getString(), message);
    }
    
    /**
     * Sends a leaving message to the player above the hotbar (action bar)
     */
    private static void sendLeavingMessage(ServerPlayer player, String townName) {
        String message = "👋 Leaving " + townName;
        Component messageComponent = Component.literal(message);
        
        // Send as action bar message (appears above hotbar, not in chat)
        player.displayClientMessage(messageComponent, true);
        
        LOGGER.info("Sent leaving message to {}: {}", player.getName().getString(), message);
    }
    
    /**
     * Clean up player data when they leave the server
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        playerStates.remove(playerId);
        LOGGER.debug("Cleaned up boundary tracking data for player: {}", event.getEntity().getName().getString());
    }
    
    /**
     * Initialize player data when they join the server
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            BlockPos initialPos = player.blockPosition();
            playerStates.put(playerId, new PlayerBoundaryState(initialPos));
            LOGGER.debug("Initialized boundary tracking for player: {}", player.getName().getString());
        }
    }
    
    /**
     * Utility method to clear all tracking data (useful for debugging or server restart)
     */
    public static void clearAllTrackingData() {
        int count = playerStates.size();
        playerStates.clear();
        LOGGER.info("Cleared boundary tracking data for {} players", count);
    }
}
