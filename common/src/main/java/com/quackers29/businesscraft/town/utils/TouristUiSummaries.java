package com.quackers29.businesscraft.town.utils;

import com.quackers29.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import com.quackers29.businesscraft.api.ITouristHelper;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Server-side display strings for tourism panels (no payout logic).
 */
public final class TouristUiSummaries {
    private static final int MAX_DESTINATION_PARTS = 3;
    private static final int MAX_RECENT = 3;

    private TouristUiSummaries() {
    }

    public static String formatWaitingAtPlatforms(ServerLevel level, Town town, TownInterfaceEntity entity) {
        if (town == null || entity == null) {
            return "—";
        }
        int searchRadius = entity.getSearchRadius();
        var platforms = entity.getPlatforms();
        if (platforms.isEmpty()) {
            return town.getTouristCount() > 0 ? town.getTouristCount() + " waiting (no platforms)" : "None waiting";
        }

        Map<String, Integer> byDestination = new HashMap<>();
        ITouristHelper helper = PlatformAccess.getTouristHelper();

        for (Platform platform : platforms) {
            if (!platform.isEnabled() || !platform.isComplete()) {
                continue;
            }
            BlockPos startPos = platform.getStartPos();
            BlockPos endPos = platform.getEndPos();
            AABB bounds = new AABB(
                    Math.min(startPos.getX(), endPos.getX()) - searchRadius,
                    Math.min(startPos.getY(), endPos.getY()) - 2,
                    Math.min(startPos.getZ(), endPos.getZ()) - searchRadius,
                    Math.max(startPos.getX(), endPos.getX()) + searchRadius,
                    Math.max(startPos.getY(), endPos.getY()) + 4,
                    Math.max(startPos.getZ(), endPos.getZ()) + searchRadius);

            for (Villager villager : level.getEntitiesOfClass(Villager.class, bounds)) {
                if (!helper.isTourist(villager)) {
                    continue;
                }
                if (!(villager instanceof TouristEntity tourist)) {
                    continue;
                }
                if (town.getId().equals(tourist.getOriginTownId())) {
                    String dest = tourist.getDestinationTownName();
                    if (dest == null || dest.isBlank()) {
                        dest = "Unknown";
                    }
                    byDestination.merge(dest, 1, Integer::sum);
                }
            }
        }

        int total = byDestination.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            long count = town.getTouristCount();
            return count > 0 ? count + " abroad / in transit" : "None waiting on platforms";
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(byDestination.entrySet());
        sorted.sort(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getValue()).reversed());

        StringBuilder sb = new StringBuilder();
        sb.append(total).append(" waiting: ");
        int parts = 0;
        for (Map.Entry<String, Integer> entry : sorted) {
            if (parts >= MAX_DESTINATION_PARTS) {
                sb.append("…");
                break;
            }
            if (parts > 0) {
                sb.append(", ");
            }
            sb.append(entry.getValue()).append("→").append(truncate(entry.getKey(), 14));
            parts++;
        }
        return sb.toString();
    }

    public static String formatRecentArrivals(Town town, Function<UUID, String> nameResolver) {
        if (town == null) {
            return "—";
        }
        List<VisitHistoryRecord> history = town.getVisitHistory();
        if (history == null || history.isEmpty()) {
            return "None yet";
        }

        int start = Math.max(0, history.size() - MAX_RECENT);
        List<VisitHistoryRecord> recent = history.subList(start, history.size());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recent.size(); i++) {
            VisitHistoryRecord rec = recent.get(i);
            if (i > 0) {
                sb.append(" · ");
            }
            String name = nameResolver.apply(rec.getOriginTownId());
            sb.append(truncate(name, 12));
            if (rec.getCount() > 1) {
                sb.append(" x").append(rec.getCount());
            }
        }
        return sb.toString();
    }

    public static Function<UUID, String> townNameResolver(ServerLevel level) {
        return id -> {
            if (id == null) {
                return "Unknown";
            }
            Town t = TownManager.get(level).getTown(id);
            return t != null ? t.getName() : "Unknown";
        };
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "?";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
