package com.quackers29.businesscraft.town.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-024: Container Data Registration (Test + Docs Loop).
 *
 * Covers the modular ContainerDataHelper used by TownInterfaceEntity for
 * named live-synced menu fields (population, spawn toggle, radius, tourist stats).
 *
 * Documentation: vault/Town/Data Synchronization/Container Data Registration.md
 */
class ContainerDataHelperTest {

    private ContainerDataHelper helper;
    private AtomicInteger pop;
    private AtomicInteger spawnFlag;
    private AtomicInteger radius;

    @BeforeEach
    void setUp() {
        pop = new AtomicInteger(42);
        spawnFlag = new AtomicInteger(1);
        radius = new AtomicInteger(15);

        helper = ContainerDataHelper.builder("TestBlock")
                .addReadOnlyField("population", pop::get, "Current population")
                .addField("spawn_enabled", spawnFlag::get, spawnFlag::set, "Spawn flag 0/1")
                .addReadOnlyField("can_spawn", () -> 1, "Can spawn right now")
                .addField("search_radius", radius::get, v -> radius.set(Math.max(1, Math.min(v, 100))), "Search radius (clamped)")
                .build();
    }

    // --- registration & indexing ---

    @Test
    void builder_registersInOrder_andAssignsSequentialIndices() {
        // 4 fields registered -> indices 0..3
        assertEquals(4, helper.getCount());
        assertEquals(42, helper.get(0)); // population
        assertEquals(1, helper.get(1));  // spawn
        assertEquals(1, helper.get(2));  // can_spawn
        assertEquals(15, helper.get(3)); // radius
    }

    @Test
    void registerField_duplicateName_throwsIllegalArgument() {
        ContainerDataHelper h = new ContainerDataHelper("dup");
        h.registerReadOnlyField("foo", () -> 1, "first");
        assertThrows(IllegalArgumentException.class, () ->
                h.registerReadOnlyField("foo", () -> 2, "second"));
    }

    @Test
    void getFieldNames_returnsInsertionOrder() {
        var names = helper.getFieldNames();
        assertEquals(4, names.size());
        assertTrue(names.contains("population"));
        // LinkedHash preserves order
        String first = names.iterator().next();
        assertEquals("population", first);
    }

    // --- name-based access (used by entity code) ---

    @Test
    void getValue_byName_returnsLiveSupplierValue() {
        assertEquals(42, helper.getValue("population"));
        pop.set(99);
        assertEquals(99, helper.getValue("population"));
    }

    @Test
    void setValue_byName_writesThroughSetter() {
        helper.setValue("spawn_enabled", 0);
        assertEquals(0, spawnFlag.get());
        assertEquals(0, helper.getValue("spawn_enabled"));
    }

    @Test
    void getValue_unknownName_returnsZero_andDoesNotThrow() {
        assertEquals(0, helper.getValue("no_such_field"));
    }

    @Test
    void setValue_unknownName_isNoOp_andDoesNotThrow() {
        helper.setValue("no_such_field", 123);
        // still 4 fields, values unchanged
        assertEquals(4, helper.getCount());
        assertEquals(42, helper.getValue("population"));
    }

    // --- read-only enforcement ---

    @Test
    void setValue_onReadOnlyField_isNoOp() {
        int before = helper.getValue("population");
        helper.setValue("population", 777);
        assertEquals(before, helper.getValue("population"));
    }

    @Test
    void set_onReadOnlyIndex_isNoOp_forContainerDataContract() {
        int before = helper.get(0);
        helper.set(0, 999); // index 0 = population (readonly)
        assertEquals(before, helper.get(0));
    }

    // --- ContainerData index contract ---

    @Test
    void get_outOfRangeIndex_returnsZero() {
        assertEquals(0, helper.get(-1));
        assertEquals(0, helper.get(99));
    }

    @Test
    void set_outOfRangeIndex_isNoOp() {
        int before = helper.get(0);
        helper.set(-1, 5);
        helper.set(99, 5);
        assertEquals(before, helper.get(0));
    }

    @Test
    void set_validIndex_callsSetter_andUpdatesCache() {
        helper.set(3, 42); // search_radius
        assertEquals(42, radius.get());
        assertEquals(42, helper.get(3));
    }

    // --- dirty / markDirty behavior ---

    @Test
    void markDirty_forcesReReadOnNextGet_evenIfSupplierWouldBeStable() {
        // Our suppliers are live, but markDirty path is exercised
        pop.set(7);
        helper.markDirty("population");
        assertEquals(7, helper.getValue("population"));
    }

    @Test
    void markAllDirty_refreshesAllFields() {
        pop.set(100);
        spawnFlag.set(0);
        radius.set(5);
        helper.markAllDirty();
        assertEquals(100, helper.get(0));
        assertEquals(0, helper.get(1));
        assertEquals(5, helper.get(3));
    }

    @Test
    void markDirty_unknownName_isSilentNoOp() {
        // Should not throw or affect others
        helper.markDirty("ghost");
        assertEquals(42, helper.getValue("population"));
    }

    // --- builder and debug surface ---

    @Test
    void builder_addReadOnlyField_only_hasNoSetter() {
        // can_spawn (index 2) is readonly
        int before = helper.get(2);
        helper.set(2, 0);
        assertEquals(before, helper.get(2));
    }

    @Test
    void getDebugInfo_containsContextName_andAllRegisteredFields() {
        String dbg = helper.getDebugInfo();
        assertTrue(dbg.contains("ContainerData 'TestBlock'"));
        assertTrue(dbg.contains("population"));
        assertTrue(dbg.contains("search_radius"));
        assertTrue(dbg.contains("[READ-ONLY]"));
    }

    @Test
    void getField_returnsDataFieldForDirectAccess() {
        var f = helper.getField("spawn_enabled");
        assertNotNull(f);
        assertEquals("spawn_enabled", f.getName());
        assertFalse(f.isReadOnly());
        assertEquals("Spawn flag 0/1", f.getDescription());
    }

    @Test
    void getField_unknown_returnsNull() {
        assertNull(helper.getField("does_not_exist"));
    }

    // --- roundtrip + clamping simulation (mirrors entity usage) ---

    @Test
    void searchRadius_setClampsLikeEntitySetter() {
        helper.setValue("search_radius", 0);   // should clamp to 1
        assertEquals(1, helper.getValue("search_radius"));

        helper.setValue("search_radius", 150); // should clamp to 100
        assertEquals(100, helper.getValue("search_radius"));

        helper.setValue("search_radius", 50);
        assertEquals(50, helper.getValue("search_radius"));
    }

    @Test
    void getCount_matchesRegisteredFieldCount() {
        assertEquals(4, helper.getCount());
        // also via interface
        assertEquals(4, ((net.minecraft.world.inventory.ContainerData) helper).getCount());
    }
}
