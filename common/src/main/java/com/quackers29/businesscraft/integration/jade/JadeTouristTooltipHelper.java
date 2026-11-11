package com.quackers29.businesscraft.integration.jade;

import com.quackers29.businesscraft.entity.TouristEntity;
import net.minecraft.nbt.CompoundTag;

/**
 * Resolves tourist Jade labels: server NBT first, then synced entity data (client fallback).
 */
public final class JadeTouristTooltipHelper {

    private JadeTouristTooltipHelper() {
    }

    public static String resolveOrigin(CompoundTag serverData, String syncedOrigin) {
        if (serverData != null && serverData.contains(JadeTooltipNbt.ORIGIN)) {
            return serverData.getString(JadeTooltipNbt.ORIGIN);
        }
        return blankToEmpty(syncedOrigin);
    }

    public static String resolveDestination(CompoundTag serverData, String syncedDest) {
        if (serverData != null && serverData.contains(JadeTooltipNbt.DESTINATION)) {
            return serverData.getString(JadeTooltipNbt.DESTINATION);
        }
        String synced = blankToEmpty(syncedDest);
        if (!synced.isEmpty()) {
            return synced;
        }
        return "";
    }

    public static String resolveDestination(CompoundTag serverData, TouristEntity tourist) {
        String fromNbt = resolveDestination(serverData, tourist.getSyncedDestLabel());
        if (!fromNbt.isEmpty()) {
            return fromNbt;
        }
        if (tourist.isAnyTownDestination()) {
            return TouristEntity.ANY_TOWN_NAME;
        }
        return "";
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
