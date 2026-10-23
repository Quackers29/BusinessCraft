package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.town.Town;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * One-shot copy of block-entity {@code visitHistory} into the town save.
 * Later chunk loads that still contain the old list do not append it again.
 */
public final class LegacyVisitHistoryMigration {
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyVisitHistoryMigration.class);

    private LegacyVisitHistoryMigration() {
    }

    public static void apply(CompoundTag tag, Town town) {
        if (tag == null || town == null || !tag.contains("visitHistory")) {
            return;
        }
        if (!town.isLegacyVisitHistoryMigrated()) {
            ListTag historyTag = tag.getList("visitHistory", Tag.TAG_COMPOUND);
            LOGGER.info("Migrating {} visit history records to Town {}", historyTag.size(), town.getName());
            for (int i = 0; i < historyTag.size(); i++) {
                CompoundTag visitTag = historyTag.getCompound(i);
                UUID originTownId = null;
                if (visitTag.contains("townId")) {
                    originTownId = visitTag.getUUID("townId");
                } else if (visitTag.contains("town")) {
                    String townName = visitTag.getString("town");
                    originTownId = UUID.nameUUIDFromBytes(townName.getBytes());
                }
                if (originTownId == null) {
                    continue;
                }
                BlockPos originPos = BlockPos.ZERO;
                if (visitTag.contains("pos")) {
                    CompoundTag posTag = visitTag.getCompound("pos");
                    originPos = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
                }
                town.importLegacyVisit(visitTag.getLong("timestamp"), originTownId, visitTag.getInt("count"), originPos);
            }
            town.setLegacyVisitHistoryMigrated(true);
            town.markDirty();
        }
        tag.remove("visitHistory");
    }
}
