package com.quackers29.businesscraft.data;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step 3f: town SavedData schema version stamp + stepwise migration runner.
 * Pure CompoundTag logic — no McBootstrap / Town construction required.
 */
class TownSavedDataVersionTest {

    @Test
    void readDataVersion_absentKey_treatedAsCurrent() {
        CompoundTag tag = new CompoundTag();
        assertEquals(TownSavedData.CURRENT_DATA_VERSION, TownSavedData.readDataVersion(tag));
        assertEquals(1, TownSavedData.CURRENT_DATA_VERSION);
    }

    @Test
    void readDataVersion_presentKey_returnsStoredInt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TownSavedData.DATA_VERSION_KEY, 3);
        assertEquals(3, TownSavedData.readDataVersion(tag));
    }

    @Test
    void save_writesCurrentDataVersion() {
        TownSavedData data = TownSavedData.create();
        CompoundTag tag = data.save(new CompoundTag());
        assertTrue(tag.contains(TownSavedData.DATA_VERSION_KEY));
        assertEquals(TownSavedData.CURRENT_DATA_VERSION, tag.getInt(TownSavedData.DATA_VERSION_KEY));
        assertTrue(tag.contains("towns"));
    }

    @Test
    void applyMigrations_fromEqualsTo_isNoOp() {
        CompoundTag tag = new CompoundTag();
        tag.putString("marker", "keep");
        TownSavedData.applyMigrations(tag, 1, 1, (from, t) -> {
            fail("step migrator must not run when from == to");
            return t;
        });
        assertEquals("keep", tag.getString("marker"));
    }

    @Test
    void applyMigrations_stepwise_runsEachVersionOnce() {
        // Simulate future bump: test-only migrator 1→2 stamps a flag (production has no 1→2 yet)
        CompoundTag tag = new CompoundTag();
        tag.putInt(TownSavedData.DATA_VERSION_KEY, 1);

        TownSavedData.applyMigrations(tag, 1, 2, (from, t) -> {
            assertEquals(1, from);
            t.putBoolean("migrated_1_to_2", true);
            return t;
        });

        assertTrue(tag.getBoolean("migrated_1_to_2"));
    }

    @Test
    void applyMigrations_multiStep_chainsInOrder() {
        CompoundTag tag = new CompoundTag();
        StringBuilder order = new StringBuilder();

        TownSavedData.applyMigrations(tag, 1, 3, (from, t) -> {
            order.append(from).append("->").append(from + 1).append(';');
            t.putInt("lastFrom", from);
            return t;
        });

        assertEquals("1->2;2->3;", order.toString());
        assertEquals(2, tag.getInt("lastFrom"));
    }
}
