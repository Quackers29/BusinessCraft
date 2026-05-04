package com.quackers29.businesscraft.event;

import com.quackers29.businesscraft.api.EventCallbacks;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerBoundaryTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerBoundaryTracker.class);
    private static final long MESSAGE_COOLDOWN_MS = 2000; // 2 seconds between messages
    private static final int TRACKING_INTERVAL_TICKS = 10; // Check every 10 ticks (0.5 seconds)
    private static final Map<UUID, PlayerBoundaryState> playerStates = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

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

    public static void initialize() {
        PlatformAccess.getEvents().registerPlayerTickCallback(PlayerBoundaryTracker::onPlayerTick);
        PlatformAccess.getEvents().registerPlayerLoginCallback(PlayerBoundaryTracker::onPlayerLogin);
        PlatformAccess.getEvents().registerPlayerLogoutCallback(PlayerBoundaryTracker::onPlayerLogout);
    }

    private static void onPlayerTick(ServerPlayer player, ServerLevel level, BlockPos position) {
        if (!ConfigLoader.playerTracking || !ConfigLoader.townBoundaryMessages) {
            return;
        }

        if (++tickCounter % TRACKING_INTERVAL_TICKS != 0) {
            return;
        }

        UUID playerId = player.getUUID();
        BlockPos currentPos = position;

        PlayerBoundaryState state = playerStates.computeIfAbsent(playerId, 
            k -> new PlayerBoundaryState(currentPos));

        if (state.lastPosition != null && state.lastPosition.distSqr(currentPos) < 16) { // 4 block threshold
            return;
        }

        state.lastPosition = currentPos;

        checkBoundaryTransition(player, level, state, currentPos);
    }

    private static void checkBoundaryTransition(ServerPlayer player, ServerLevel level, 
                                               PlayerBoundaryState state, BlockPos playerPos) {
        try {
            TownManager townManager = TownManager.get(level);
            Town newTown = findTownAtPosition(townManager, playerPos);

            if (state.currentTown != newTown) {
                handleBoundaryTransition(player, state, state.currentTown, newTown);
                state.currentTown = newTown;
            }
        } catch (Exception e) {
            LOGGER.warn("Error checking boundary transition for player {}: {}", 
                player.getName().getString(), e.getMessage());
        }
    }

    private static Town findTownAtPosition(TownManager townManager, BlockPos playerPos) {
        for (Town town : townManager.getAllTowns().values()) {
            double distance = Math.sqrt(playerPos.distSqr(town.getPosition()));
            if (distance <= town.getBoundaryRadius()) {
                return town;
            }
        }
        return null; // Player is not in any town boundary
    }

    private static void handleBoundaryTransition(ServerPlayer player, PlayerBoundaryState state, 
                                                Town oldTown, Town newTown) {
        if (!state.canSendNotification()) {
            return;
        }

        if (oldTown == null && newTown != null) {
            sendWelcomeMessage(player, newTown.getName());
            state.markNotificationSent();

        } else if (oldTown != null && newTown == null) {
            sendLeavingMessage(player, oldTown.getName());
            state.markNotificationSent();

        } else if (oldTown != null && newTown != null && !oldTown.getId().equals(newTown.getId())) {
            sendLeavingMessage(player, oldTown.getName());
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
            sendWelcomeMessage(player, newTown.getName());
            state.markNotificationSent();
        }
    }

    private static void sendWelcomeMessage(ServerPlayer player, String townName) {
        String message = "🏘️ Welcome to " + townName;
        Component messageComponent = Component.literal(message);

        player.displayClientMessage(messageComponent, true);

        LOGGER.info("Sent welcome message to {}: {}", player.getName().getString(), message);
    }

    private static void sendLeavingMessage(ServerPlayer player, String townName) {
        String message = "👋 Leaving " + townName;
        Component messageComponent = Component.literal(message);

        player.displayClientMessage(messageComponent, true);

        LOGGER.info("Sent leaving message to {}: {}", player.getName().getString(), message);
    }

    private static void onPlayerLogout(ServerPlayer player) {
        UUID playerId = player.getUUID();
        playerStates.remove(playerId);
        LOGGER.debug("Cleaned up boundary tracking data for player: {}", player.getName().getString());
    }

    private static void onPlayerLogin(ServerPlayer player, ServerLevel level, BlockPos position) {
        UUID playerId = player.getUUID();
        BlockPos initialPos = position;
        playerStates.put(playerId, new PlayerBoundaryState(initialPos));
        LOGGER.debug("Initialized boundary tracking for player: {}", player.getName().getString());
    }

    public static void clearAllTrackingData() {
        int count = playerStates.size();
        playerStates.clear();
        LOGGER.info("Cleared boundary tracking data for {} players", count);
    }
}
