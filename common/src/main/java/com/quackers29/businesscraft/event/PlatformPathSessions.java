package com.quackers29.businesscraft.event;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player platform path creation state. Two players editing paths on the same town
 * block do not share click progress.
 */
public final class PlatformPathSessions {
    public static final class Session {
        public final BlockPos townBlockPos;
        public final UUID platformId;
        boolean awaitingSecondClick;
        long lastClickTime;

        Session(BlockPos townBlockPos, UUID platformId) {
            this.townBlockPos = townBlockPos.immutable();
            this.platformId = platformId;
        }
    }

    private static final Map<UUID, Session> BY_PLAYER = new ConcurrentHashMap<>();

    private PlatformPathSessions() {
    }

    public static void begin(UUID playerId, BlockPos townBlockPos, UUID platformId) {
        BY_PLAYER.put(playerId, new Session(townBlockPos, platformId));
    }

    public static Session get(UUID playerId) {
        return BY_PLAYER.get(playerId);
    }

    public static void clear(UUID playerId) {
        BY_PLAYER.remove(playerId);
    }

    public static void clearAll() {
        BY_PLAYER.clear();
    }
}
