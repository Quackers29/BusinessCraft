package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
// import net.minecraftforge.items.ItemStackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Helper class for NBT data management and persistence.
 * Extracted from TownBlockEntity to improve code organization.
 * 
 * This class handles the complex NBT save/load operations including
 * legacy data migration and backward compatibility.
 */
public class NBTDataHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(NBTDataHelper.class);
    
    /**
     * Saves all town block entity data to NBT
     * 
     * @param tag The NBT tag to save to
     * @param itemHandler The item handler to serialize
     * @param townId The town UUID
     * @param name The town name
     * @param pathStart Legacy path start position
     * @param pathEnd Legacy path end position
     * @param platformManager The platform manager
     */
    public void saveToNBT(CompoundTag tag, Object itemHandler, UUID townId, String name,
                         BlockPos pathStart, BlockPos pathEnd, PlatformManager platformManager) {
        
        // Save inventory
        tag.put("inventory", (CompoundTag) com.quackers29.businesscraft.api.PlatformAccess.getItemHandlers().serializeNBT(itemHandler));
        
        // Save local data to tag
        if (townId != null) {
            tag.putUUID("TownId", townId);
        }
        
        tag.putString("name", name != null ? name : "");
        
        // Save legacy path data for backward compatibility
        if (pathStart != null) {
            CompoundTag startPos = new CompoundTag();
            startPos.putInt("x", pathStart.getX());
            startPos.putInt("y", pathStart.getY());
            startPos.putInt("z", pathStart.getZ());
            tag.put("PathStart", startPos);
        }
        
        if (pathEnd != null) {
            CompoundTag endPos = new CompoundTag();
            endPos.putInt("x", pathEnd.getX());
            endPos.putInt("y", pathEnd.getY());
            endPos.putInt("z", pathEnd.getZ());
            tag.put("PathEnd", endPos);
        }
        
        // Save platforms using platform manager
        platformManager.saveToNBT(tag);
    }
    
    /**
     * Loads town block entity data from NBT
     * 
     * @param tag The NBT tag to load from
     * @param itemHandler The item handler to deserialize to
     * @param level The level for town lookups
     * @param platformManager The platform manager
     * @return LoadResult containing the loaded data
     */
    public LoadResult loadFromNBT(CompoundTag tag, Object itemHandler, Level level, PlatformManager platformManager) {
        LoadResult result = new LoadResult();
        
        // Load inventory
        PlatformAccess.getItemHandlers().deserializeNBT(itemHandler, tag.getCompound("inventory"));
        
        // Load paths from the tag itself as a fallback
        if (tag.contains("PathStart")) {
            CompoundTag startPos = tag.getCompound("PathStart");
            result.pathStart = new BlockPos(
                startPos.getInt("x"),
                startPos.getInt("y"),
                startPos.getInt("z")
            );
        }
        
        if (tag.contains("PathEnd")) {
            CompoundTag endPos = tag.getCompound("PathEnd");
            result.pathEnd = new BlockPos(
                endPos.getInt("x"),
                endPos.getInt("y"),
                endPos.getInt("z")
            );
        }
        
        // Load town data and handle legacy migration
        if (tag.contains("TownId")) {
            result.townId = tag.getUUID("TownId");
            if (level instanceof ServerLevel sLevel) {
                result.town = TownManager.get(sLevel).getTown(result.townId);
                
                if (result.town != null && tag.contains("visitHistory")) {
                    LegacyVisitHistoryMigration.apply(tag, result.town);
                }
                
                if (result.town != null) {
                    // Update local values from the Town object, but prefer the ones we already loaded
                    result.touristSpawningEnabled = result.town.isTouristSpawningEnabled();
                    
                    // Only use town paths if we don't have any
                    if (result.pathStart == null && result.town.getPathStart() != null) {
                        result.pathStart = result.town.getPathStart();
                    }
                    
                    if (result.pathEnd == null && result.town.getPathEnd() != null) {
                        result.pathEnd = result.town.getPathEnd();
                    }
                }
            }
        }
        
        if (tag.contains("name")) {
            result.name = tag.getString("name");
        }

        // Load platforms using platform manager. Platforms without an interface block are dropped.
        platformManager.loadFromNBT(tag);

        return result;
    }
    
    /**
     * Result class containing all loaded data from NBT
     */
    public static class LoadResult {
        public UUID townId;
        public String name;
        public Town town;
        public BlockPos pathStart;
        public BlockPos pathEnd;
        public boolean touristSpawningEnabled = true;
        
        /**
         * Gets a summary of loaded data for debugging
         */
        public String getSummary() {
            return String.format("TownId: %s, Name: %s, PathStart: %s, PathEnd: %s, SpawningEnabled: %s",
                townId, name, pathStart, pathEnd, touristSpawningEnabled);
        }
    }
} 
