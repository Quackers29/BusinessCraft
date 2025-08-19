package com.quackers29.businesscraft.town.utils;

import net.minecraft.world.entity.npc.Villager;
import java.util.UUID;

/**
 * STUB: Tourist utility class for unified architecture.
 * TODO: Move full implementation from forge module.
 */
public class TouristUtils {
    
    public static class TouristInfo {
        public final String originTownId;
        public final String destinationTownId;
        public final String originTownName;
        public final int originX;
        public final int originY;
        public final int originZ;
        
        public TouristInfo(String originTownId, String destinationTownId, String originTownName, int originX, int originY, int originZ) {
            this.originTownId = originTownId;
            this.destinationTownId = destinationTownId;
            this.originTownName = originTownName;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
        }
    }
    
    public static boolean isTourist(Villager villager) {
        // STUB: Always false for now
        return false;
    }
    
    public static TouristInfo extractTouristInfo(Villager villager) {
        // STUB: Return dummy info
        return new TouristInfo("00000000-0000-0000-0000-000000000000", "00000000-0000-0000-0000-000000000000", "Unknown", 0, 0, 0);
    }
}