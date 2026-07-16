package com.quackers29.businesscraft.data;

import com.quackers29.businesscraft.town.Town;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Per-level town registry persistence.
 * <p>
 * Root NBT includes {@code dataVersion} so future schema breaks can run stepwise
 * migrators before towns are parsed. Unmarked saves (pre-versioning) are treated
 * as {@link #CURRENT_DATA_VERSION} because they already match today's format.
 */
public class TownSavedData extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownSavedData.class);

    /** Stable SavedData file id (same pattern as {@link ContractSavedData#NAME}). */
    public static final String NAME = "businesscraft_towns";
    public static final String DATA_VERSION_KEY = "dataVersion";
    /** Current on-disk schema for this SavedData file. Bump when adding a migrator. */
    public static final int CURRENT_DATA_VERSION = 1;

    private final Map<UUID, Town> towns = new ConcurrentHashMap<>();

    public Map<UUID, Town> getTowns() {
        return towns;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag townsTag = new CompoundTag();
        towns.forEach((id, town) -> {
            CompoundTag townTag = new CompoundTag();
            town.save(townTag);
            townsTag.put(id.toString(), townTag);
        });
        tag.put("towns", townsTag);
        tag.putInt(DATA_VERSION_KEY, CURRENT_DATA_VERSION);

        return tag;
    }

    public void loadFromNbt(CompoundTag tag) {
        boolean missingVersion = !tag.contains(DATA_VERSION_KEY);
        int from = readDataVersion(tag);

        if (from > CURRENT_DATA_VERSION) {
            LOGGER.warn(
                    "Town save dataVersion {} is newer than supported {}; loading best-effort (beta worlds may not be forward-compatible)",
                    from, CURRENT_DATA_VERSION);
        } else if (from < CURRENT_DATA_VERSION) {
            applyMigrations(tag, from, CURRENT_DATA_VERSION, TownSavedData::migrateStep);
            setDirty();
        }

        if (missingVersion) {
            // Stamp version on next save so unmarked v1 worlds get the key
            setDirty();
        }

        towns.clear();
        if (tag.contains("towns")) {
            CompoundTag townsTag = tag.getCompound("towns");
            townsTag.getAllKeys().forEach(key -> {
                UUID id = UUID.fromString(key);
                towns.put(id, Town.load(townsTag.getCompound(key)));
            });
        }
    }

    /**
     * Unmarked worlds already match format 1 (first versioned schema).
     */
    public static int readDataVersion(CompoundTag tag) {
        if (tag == null || !tag.contains(DATA_VERSION_KEY)) {
            return CURRENT_DATA_VERSION;
        }
        return tag.getInt(DATA_VERSION_KEY);
    }

    /**
     * Runs stepwise migrators {@code from → from+1 → … → to}. Package-visible for tests.
     *
     * @param step receives the version being migrated <em>from</em> and the tag; returns the
     *             (possibly same) tag after that single step
     */
    static void applyMigrations(CompoundTag tag, int from, int to,
            BiFunction<Integer, CompoundTag, CompoundTag> step) {
        int version = from;
        CompoundTag current = tag;
        while (version < to) {
            current = step.apply(version, current);
            version++;
        }
    }

    /**
     * Single-step migrator. Add {@code case n:} when bumping {@link #CURRENT_DATA_VERSION} to n+1.
     */
    static CompoundTag migrateStep(int fromVersion, CompoundTag tag) {
        switch (fromVersion) {
            // case 1: return migrate1to2(tag);
            default:
                LOGGER.warn("No town-save migrator registered for dataVersion {} → {}; leaving tag unchanged",
                        fromVersion, fromVersion + 1);
                return tag;
        }
    }

    public static TownSavedData create() {
        return new TownSavedData();
    }

    public static TownSavedData load(CompoundTag tag) {
        TownSavedData data = create();
        data.loadFromNbt(tag);
        return data;
    }
}
