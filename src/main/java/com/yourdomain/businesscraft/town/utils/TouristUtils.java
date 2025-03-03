package com.yourdomain.businesscraft.town.utils;

import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.town.Town;
import net.minecraft.world.entity.npc.Villager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for tourist-related operations
 * Provides centralized methods for tagging and identifying tourists
 */
public class TouristUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TouristUtils.class);

    // Tag constants to ensure consistency across codebase
    public static final String TAG_TYPE_TOURIST = "type_tourist";
    public static final String TAG_FROM_TOWN_PREFIX = "from_town_";
    public static final String TAG_FROM_NAME_PREFIX = "from_name_";
    public static final String TAG_POS_PREFIX = "pos_";
    public static final String TAG_PLATFORM_PREFIX = "platform_";
    public static final String TAG_DEST_TOWN_PREFIX = "dest_town_";
    public static final String TAG_DEST_NAME_PREFIX = "dest_name_";

    /**
     * Adds standard tourist tags to a villager entity
     * 
     * @param villager The villager entity to tag
     * @param town The origin town
     * @param platform The platform the tourist is spawned from
     */
    public static void addStandardTouristTags(Villager villager, Town town, Platform platform) {
        addStandardTouristTags(villager, town, platform, null, null);
    }
    
    /**
     * Adds standard tourist tags to a villager entity with destination information
     * 
     * @param villager The villager entity to tag
     * @param town The origin town
     * @param platform The platform the tourist is spawned from
     * @param destinationTownId The destination town ID
     * @param destinationName The destination town name
     */
    public static void addStandardTouristTags(Villager villager, Town town, Platform platform, 
                                             String destinationTownId, String destinationName) {
        if (villager == null || town == null) {
            LOGGER.error("Cannot add tourist tags: villager or town is null");
            return;
        }

        // Add the type tag
        villager.addTag(TAG_TYPE_TOURIST);
        
        // Add town origin tags
        villager.addTag(TAG_FROM_TOWN_PREFIX + town.getId());
        villager.addTag(TAG_FROM_NAME_PREFIX + town.getName());
        
        // Add position tag
        String posTag = TAG_POS_PREFIX + villager.getBlockX() + "_" + 
                villager.getBlockY() + "_" + villager.getBlockZ();
        villager.addTag(posTag);
        
        // Add platform tag if available
        if (platform != null) {
            villager.addTag(TAG_PLATFORM_PREFIX + platform.getId());
        }
        
        // Add destination tags if available
        if (destinationTownId != null) {
            villager.addTag(TAG_DEST_TOWN_PREFIX + destinationTownId);
        }
        
        if (destinationName != null) {
            villager.addTag(TAG_DEST_NAME_PREFIX + destinationName);
        }
        
        LOGGER.debug("Added tourist tags to villager: {}", villager.getTags());
    }

    /**
     * Checks if a villager is a tourist
     * 
     * @param villager The villager to check
     * @return true if the villager is a tourist, false otherwise
     */
    public static boolean isTourist(Villager villager) {
        return villager != null && villager.getTags().contains(TAG_TYPE_TOURIST);
    }

    /**
     * Extracts the origin town ID from a tourist's tags
     * 
     * @param villager The tourist villager
     * @return The origin town ID or null if not found
     */
    public static String getOriginTownId(Villager villager) {
        if (!isTourist(villager)) {
            return null;
        }
        
        for (String tag : villager.getTags()) {
            if (tag.startsWith(TAG_FROM_TOWN_PREFIX)) {
                return tag.substring(TAG_FROM_TOWN_PREFIX.length());
            }
        }
        
        return null;
    }

    /**
     * Extracts all tourist information from a villager's tags
     * 
     * @param villager The tourist villager
     * @return A TouristInfo object containing extracted information or null if not a tourist
     */
    public static TouristInfo extractTouristInfo(Villager villager) {
        if (!isTourist(villager)) {
            return null;
        }
        
        TouristInfo info = new TouristInfo();
        
        for (String tag : villager.getTags()) {
            if (tag.startsWith(TAG_FROM_TOWN_PREFIX)) {
                info.originTownId = tag.substring(TAG_FROM_TOWN_PREFIX.length());
            } else if (tag.startsWith(TAG_FROM_NAME_PREFIX)) {
                info.originTownName = tag.substring(TAG_FROM_NAME_PREFIX.length());
            } else if (tag.startsWith(TAG_PLATFORM_PREFIX)) {
                info.platformId = tag.substring(TAG_PLATFORM_PREFIX.length());
            } else if (tag.startsWith(TAG_DEST_TOWN_PREFIX)) {
                info.destinationTownId = tag.substring(TAG_DEST_TOWN_PREFIX.length());
            } else if (tag.startsWith(TAG_DEST_NAME_PREFIX)) {
                info.destinationTownName = tag.substring(TAG_DEST_NAME_PREFIX.length());
            } else if (tag.startsWith(TAG_POS_PREFIX)) {
                try {
                    String[] parts = tag.substring(TAG_POS_PREFIX.length()).split("_");
                    if (parts.length == 3) {
                        info.originX = Integer.parseInt(parts[0]);
                        info.originY = Integer.parseInt(parts[1]);
                        info.originZ = Integer.parseInt(parts[2]);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Error parsing position from tag: {}", tag, e);
                }
            }
        }
        
        return info.isValid() ? info : null;
    }

    /**
     * Data class to hold tourist information extracted from tags
     */
    public static class TouristInfo {
        public String originTownId;
        public String originTownName;
        public String platformId;
        public String destinationTownId;
        public String destinationTownName;
        public int originX;
        public int originY;
        public int originZ;
        
        public boolean isValid() {
            return originTownId != null;
        }
    }
} 