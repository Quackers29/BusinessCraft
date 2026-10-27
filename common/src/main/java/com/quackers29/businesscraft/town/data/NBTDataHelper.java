package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Helper class for NBT data management and persistence on the Town Centre block entity.
 */
public class NBTDataHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(NBTDataHelper.class);

    public void saveToNBT(CompoundTag tag, Object itemHandler, UUID townId, String name,
            PlatformManager platformManager) {

        tag.put("inventory", (CompoundTag) PlatformAccess.getItemHandlers().serializeNBT(itemHandler));

        if (townId != null) {
            tag.putUUID("TownId", townId);
        }

        tag.putString("name", name != null ? name : "");

        platformManager.saveToNBT(tag);
    }

    public LoadResult loadFromNBT(CompoundTag tag, Object itemHandler, Level level, PlatformManager platformManager) {
        LoadResult result = new LoadResult();

        PlatformAccess.getItemHandlers().deserializeNBT(itemHandler, tag.getCompound("inventory"));

        if (tag.contains("TownId")) {
            result.townId = tag.getUUID("TownId");
            if (level instanceof ServerLevel sLevel) {
                result.town = TownManager.get(sLevel).getTown(result.townId);

                if (result.town != null && tag.contains("visitHistory")) {
                    LegacyVisitHistoryMigration.apply(tag, result.town);
                }

                if (result.town != null) {
                    result.touristSpawningEnabled = result.town.isTouristSpawningEnabled();
                }
            }
        }

        if (tag.contains("name")) {
            result.name = tag.getString("name");
        }

        platformManager.loadFromNBT(tag);

        return result;
    }

    public static class LoadResult {
        public UUID townId;
        public String name;
        public Town town;
        public boolean touristSpawningEnabled = true;

        public String getSummary() {
            return String.format("TownId: %s, Name: %s, SpawningEnabled: %s",
                    townId, name, touristSpawningEnabled);
        }
    }
}
