package com.quackers29.businesscraft.event;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.town.viewmodel.MarketViewModelSyncHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common event handler for town-related logic.
 */
public class TownEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownEventHandler.class);

    public static void initialize() {
        PlatformAccess.getEvents().registerPlayerLoginCallback(TownEventHandler::onPlayerLogin);
    }

    private static void onPlayerLogin(ServerPlayer player, ServerLevel level, BlockPos position) {
        MarketViewModelSyncHelper.syncToPlayer(player);
        LOGGER.info("Synced Global Market data to player {}", player.getName().getString());
    }
}
