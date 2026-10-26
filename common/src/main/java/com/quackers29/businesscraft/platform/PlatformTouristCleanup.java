package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.api.ITouristHelper;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.town.utils.TownNotificationUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Set;
import java.util.UUID;

/**
 * When a platform is removed, loaded tourists that were spawned on it are dismissed and the origin
 * town's tourist count is reduced. Tourists in unloaded chunks are left alone; their existing expiry
 * path still updates the count when that chunk loads.
 */
public final class PlatformTouristCleanup {
    private PlatformTouristCleanup() {
    }

    public static boolean taggedForPlatform(Set<String> tags, UUID platformId) {
        return tags != null && platformId != null
                && tags.contains(ITouristHelper.TAG_PLATFORM_PREFIX + platformId);
    }

    public static UUID originTownId(Set<String> tags) {
        if (tags == null) {
            return null;
        }
        String prefix = ITouristHelper.TAG_FROM_TOWN_PREFIX;
        for (String tag : tags) {
            if (tag != null && tag.startsWith(prefix)) {
                try {
                    return UUID.fromString(tag.substring(prefix.length()));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public static int dismissLoaded(ServerLevel level, UUID platformId) {
        if (level == null || platformId == null) {
            return 0;
        }
        int dismissed = 0;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof TouristEntity tourist)) {
                continue;
            }
            if (!taggedForPlatform(tourist.getTags(), platformId)) {
                continue;
            }
            UUID origin = originTownId(tourist.getTags());
            if (origin != null) {
                TownNotificationUtils.removeTouristFromOrigin(level, origin);
            }
            tourist.discard();
            dismissed++;
        }
        return dismissed;
    }
}
