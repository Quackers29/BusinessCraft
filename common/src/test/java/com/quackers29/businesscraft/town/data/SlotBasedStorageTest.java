package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.testutil.McBootstrap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-013: Slot-Based Storage (Test + Docs Loop, re-opened via McBootstrap).
 *
 * Direct unit tests for SlotBasedStorage (the chest-like fixed-N-slot model used
 * by the 18-slot payment buffer and trade UIs). McBootstrap initializes registries
 * so real ItemStack/Items can be constructed and NBT save/of roundtrips work.
 *
 * Covers the two-pass addItem allocation, remove aggregation, index guards,
 * find helpers, NBT slot-count guard + "id" presence rule, copy semantics,
 * and pins the documented add/remove matching asymmetry quirk.
 *
 * Documentation: vault/Town/Storage/Slot-Based Storage.md
 * (note was complete before tests; this extends only the Test coverage section)
 */
class SlotBasedStorageTest {

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    // --- construction & basic accessors ---

    @Test
    void ctor_variousSizes_slotCountMatches_andAllStartEmpty() {
        SlotBasedStorage s0 = new SlotBasedStorage(0);
        assertEquals(0, s0.getSlotCount());
        assertTrue(s0.isEmpty());

        SlotBasedStorage s2 = new SlotBasedStorage(2);
        assertEquals(2, s2.getSlotCount());
        assertTrue(s2.getSlot(0).isEmpty());
        assertTrue(s2.getSlot(1).isEmpty());

        SlotBasedStorage s18 = new SlotBasedStorage(18);
        assertEquals(18, s18.getSlotCount());
    }

    @Test
    void getSlot_outOfRange_returnsEmpty_noThrow() {
        SlotBasedStorage s = new SlotBasedStorage(3);
        assertTrue(s.getSlot(-1).isEmpty());
        assertTrue(s.getSlot(3).isEmpty());
        assertTrue(s.getSlot(99).isEmpty());
    }

    @Test
    void getSlot_returnsDefensiveCopy_modifyingReturnedDoesNotAffectStorage() {
        SlotBasedStorage s = new SlotBasedStorage(1);
        s.setSlot(0, new ItemStack(Items.EMERALD, 5));

        ItemStack view = s.getSlot(0);
        view.setCount(99); // mutate the copy

        assertEquals(5, s.getSlot(0).getCount()); // internal unchanged
    }

    @Test
    void setSlot_andClearSlot_boundsAreNoOps_andNormalizeEmpty() {
        SlotBasedStorage s = new SlotBasedStorage(2);
        s.setSlot(-1, new ItemStack(Items.DIAMOND, 1)); // no-op
        s.setSlot(2, new ItemStack(Items.DIAMOND, 1));  // no-op
        assertTrue(s.isEmpty());

        s.setSlot(0, new ItemStack(Items.EMERALD, 7));
        s.clearSlot(0);
        assertTrue(s.getSlot(0).isEmpty());

        // set empty normalizes to the EMPTY sentinel path
        s.setSlot(1, ItemStack.EMPTY);
        assertTrue(s.getSlot(1).isEmpty());
    }

    @Test
    void setSlot_copiesInput_subsequentMutationOfArgDoesNotAffectSlot() {
        SlotBasedStorage s = new SlotBasedStorage(1);
        ItemStack arg = new ItemStack(Items.EMERALD, 4);
        s.setSlot(0, arg);
        arg.setCount(99);

        assertEquals(4, s.getSlot(0).getCount());
    }

    // --- find helpers ---

    @Test
    void findEmptySlot_returnsFirstEmpty_orMinusOneWhenFull() {
        SlotBasedStorage s = new SlotBasedStorage(3);
        assertEquals(0, s.findEmptySlot());

        s.setSlot(0, new ItemStack(Items.STONE, 1));
        assertEquals(1, s.findEmptySlot());

        s.setSlot(1, new ItemStack(Items.STONE, 1));
        s.setSlot(2, new ItemStack(Items.STONE, 1));
        assertEquals(-1, s.findEmptySlot());
    }

    @Test
    void findStackableSlot_emptyInput_returnsMinusOne() {
        SlotBasedStorage s = new SlotBasedStorage(2);
        assertEquals(-1, s.findStackableSlot(ItemStack.EMPTY));
    }

    @Test
    void findStackableSlot_noMatchOrNoSpace_returnsMinusOne() {
        SlotBasedStorage s = new SlotBasedStorage(2);
        s.setSlot(0, new ItemStack(Items.EMERALD, 64)); // full
        s.setSlot(1, new ItemStack(Items.DIAMOND, 1));

        assertEquals(-1, s.findStackableSlot(new ItemStack(Items.EMERALD, 1))); // no space in matching
        assertEquals(-1, s.findStackableSlot(new ItemStack(Items.BREAD, 1)));   // no match at all
    }

    @Test
    void findStackableSlot_returnsFirstWithSpace() {
        SlotBasedStorage s = new SlotBasedStorage(3);
        s.setSlot(0, new ItemStack(Items.EMERALD, 64)); // full, no remaining space
        s.setSlot(1, new ItemStack(Items.EMERALD, 10)); // first with space

        int idx = s.findStackableSlot(new ItemStack(Items.EMERALD, 10));
        // Hand-computed: loop returns lowest i with matching item+tags and space>0
        assertEquals(1, idx);
    }

    // --- addItem (two-pass: stack then empty) ---

    @Test
    void addItem_emptyOrZeroCount_returnsFalse_noMutation() {
        SlotBasedStorage s = new SlotBasedStorage(2);
        assertFalse(s.addItem(ItemStack.EMPTY));
        assertFalse(s.addItem(new ItemStack(Items.EMERALD, 0)));
        assertTrue(s.isEmpty());
    }

    @Test
    void addItem_basicStacksFirst_thenEmpties_secondPassSplits() {
        SlotBasedStorage s = new SlotBasedStorage(3);
        // 30 emerald -> slot 0
        assertTrue(s.addItem(new ItemStack(Items.EMERALD, 30)));
        // 40 more: 34 to fill slot0 to 64, 6 into slot1
        assertTrue(s.addItem(new ItemStack(Items.EMERALD, 40)));
        // 5 diamond -> first empty (slot 2)
        assertTrue(s.addItem(new ItemStack(Items.DIAMOND, 5)));

        // Hand-computed state:
        // slot0: 64 emerald, slot1: 6 emerald, slot2: 5 diamond
        assertEquals(64, s.getSlot(0).getCount());
        assertEquals(6, s.getSlot(1).getCount());
        assertEquals(5, s.getSlot(2).getCount());
        assertEquals(70, s.getTotalCount(Items.EMERALD));
    }

    @Test
    void addItem_partialFit_returnsTrue_andLeavesRemainderOut() {
        SlotBasedStorage s = new SlotBasedStorage(1);
        s.setSlot(0, new ItemStack(Items.EMERALD, 60)); // 4 space

        // try to add 10 -> only 4 fit
        boolean placed = s.addItem(new ItemStack(Items.EMERALD, 10));
        assertTrue(placed);
        assertEquals(64, s.getSlot(0).getCount());
        // (we can't observe the caller's remainder here; return value signals partial)
    }

    @Test
    void addItem_respectsTagsForStacking_butNotForRemove() {
        SlotBasedStorage s = new SlotBasedStorage(4);

        ItemStack plain = new ItemStack(Items.EMERALD, 5);
        ItemStack tagged = new ItemStack(Items.EMERALD, 3);
        CompoundTag tag = new CompoundTag();
        tag.putString("Custom", "A");
        tagged.getOrCreateTag().put("Custom", tag);  // make NBT differ

        // Different tags -> do not stack via isSameItemSameTags
        assertTrue(s.addItem(plain));
        assertTrue(s.addItem(tagged));
        // Two separate stacks of emerald
        assertEquals(2, countNonEmptySlots(s));
        assertEquals(8, s.getTotalCount(Items.EMERALD));

        // But removeItem uses raw item == , so pulls from both indiscriminately
        ItemStack removed = s.removeItem(Items.EMERALD, 10);
        assertEquals(8, removed.getCount());
        assertEquals(0, s.getTotalCount(Items.EMERALD));
    }

    @Test
    void addItem_sameItemSameTags_accumulatesIntoExisting() {
        SlotBasedStorage s = new SlotBasedStorage(2);
        s.addItem(new ItemStack(Items.EMERALD, 10));
        s.addItem(new ItemStack(Items.EMERALD, 20));
        // 10 + 20 = 30 in slot 0
        assertEquals(30, s.getSlot(0).getCount());
        assertTrue(s.getSlot(1).isEmpty());
    }

    // --- removeItem ---

    @Test
    void removeItem_nonPositive_returnsEmpty_noMutation() {
        SlotBasedStorage s = new SlotBasedStorage(1);
        s.setSlot(0, new ItemStack(Items.EMERALD, 5));
        assertTrue(s.removeItem(Items.EMERALD, 0).isEmpty());
        assertTrue(s.removeItem(Items.EMERALD, -3).isEmpty());
        assertEquals(5, s.getTotalCount(Items.EMERALD));
    }

    @Test
    void removeItem_walksLowToHigh_aggregates_andBlanksEmptiedSlots() {
        SlotBasedStorage s = new SlotBasedStorage(3);
        s.setSlot(0, new ItemStack(Items.EMERALD, 4));
        s.setSlot(1, new ItemStack(Items.EMERALD, 6));
        s.setSlot(2, new ItemStack(Items.DIAMOND, 2));

        // remove 5 emerald: takes all 4 from 0 (blanks it), 1 from 1
        ItemStack rem = s.removeItem(Items.EMERALD, 5);
        assertEquals(5, rem.getCount());
        assertTrue(s.getSlot(0).isEmpty());
        assertEquals(5, s.getSlot(1).getCount());
        assertEquals(2, s.getTotalCount(Items.DIAMOND));
    }

    @Test
    void removeItem_insufficientStock_returnsPartial_andMayExhaust() {
        SlotBasedStorage s = new SlotBasedStorage(1);
        s.setSlot(0, new ItemStack(Items.EMERALD, 3));

        ItemStack rem = s.removeItem(Items.EMERALD, 10);
        assertEquals(3, rem.getCount());
        assertEquals(0, s.getTotalCount(Items.EMERALD));
        assertTrue(s.isEmpty());
    }

    @Test
    void removeItem_onlyMatchesOnRawItem_ignoresTags() {
        SlotBasedStorage s = new SlotBasedStorage(2);
        ItemStack plain = new ItemStack(Items.EMERALD, 2);
        ItemStack tagged = new ItemStack(Items.EMERALD, 3);
        tagged.getOrCreateTag().putString("x", "y");
        s.addItem(plain);
        s.addItem(tagged);

        // remove sees them as same Item despite tags (as documented)
        ItemStack rem = s.removeItem(Items.EMERALD, 99);
        assertEquals(5, rem.getCount());
    }

    // --- totals / empty / clear ---

    @Test
    void getTotalCount_absentItem_isZero_presentSumsAcrossSlots() {
        SlotBasedStorage s = new SlotBasedStorage(3);
        s.setSlot(0, new ItemStack(Items.EMERALD, 7));
        s.setSlot(2, new ItemStack(Items.EMERALD, 11));
        assertEquals(0, s.getTotalCount(Items.DIAMOND));
        assertEquals(18, s.getTotalCount(Items.EMERALD));
    }

    @Test
    void isEmpty_andClear() {
        SlotBasedStorage s = new SlotBasedStorage(2);
        assertTrue(s.isEmpty());
        s.setSlot(0, new ItemStack(Items.BREAD, 1));
        assertFalse(s.isEmpty());
        s.clear();
        assertTrue(s.isEmpty());
        assertEquals(0, s.getTotalCount(Items.BREAD));
    }

    // --- NBT roundtrips ---

    @Test
    void toNbt_alwaysEmitsSlotCount_andN_slotEntries_onlyNonEmptyCarryId() {
        SlotBasedStorage s = new SlotBasedStorage(2);
        s.setSlot(0, new ItemStack(Items.EMERALD, 9));

        CompoundTag nbt = s.toNBT();
        assertEquals(2, nbt.getInt("SlotCount"));
        ListTag slots = nbt.getList("Slots", 10 /* TAG_COMPOUND */);
        assertEquals(2, slots.size());
        // slot 0 has payload
        assertTrue(slots.getCompound(0).contains("id"));
        // slot 1 is present but has no id (empty)
        assertFalse(slots.getCompound(1).contains("id"));
    }

    @Test
    void fromNbt_slotCountMismatch_throws_andDoesNotPartiallyLoad() {
        SlotBasedStorage s = new SlotBasedStorage(3);
        CompoundTag bad = new CompoundTag();
        bad.putInt("SlotCount", 5); // wrong
        bad.put("Slots", new ListTag());

        assertThrows(IllegalArgumentException.class, () -> s.fromNBT(bad));
        // storage remains clean (ctor state)
        assertTrue(s.isEmpty());
    }

    @Test
    void nbt_roundtrip_preservesExactSlots_andEmptyMarkers() {
        SlotBasedStorage orig = new SlotBasedStorage(4);
        orig.setSlot(1, new ItemStack(Items.EMERALD, 64));
        orig.setSlot(3, new ItemStack(Items.DIAMOND, 12));

        CompoundTag tag = orig.toNBT();

        SlotBasedStorage loaded = new SlotBasedStorage(4);
        loaded.fromNBT(tag);

        assertEquals(64, loaded.getSlot(1).getCount());
        assertEquals(Items.EMERALD, loaded.getSlot(1).getItem());
        assertEquals(12, loaded.getSlot(3).getCount());
        assertTrue(loaded.getSlot(0).isEmpty());
        assertTrue(loaded.getSlot(2).isEmpty());
    }

    @Test
    void nbt_roundtrip_forNeverUsedStorage_stillWritesAllSlotEntries() {
        SlotBasedStorage empty = new SlotBasedStorage(2);
        CompoundTag tag = empty.toNBT();
        ListTag list = tag.getList("Slots", 10);
        assertEquals(2, list.size()); // every slot index is recorded even when empty
    }

    // --- copy / copyFrom ---

    @Test
    void copy_isDeep_andIndependent() {
        SlotBasedStorage orig = new SlotBasedStorage(2);
        orig.setSlot(0, new ItemStack(Items.EMERALD, 15));

        SlotBasedStorage c = orig.copy();
        assertEquals(15, c.getSlot(0).getCount());
        c.getSlot(0).setCount(99); // mutate copy's view
        assertEquals(15, orig.getSlot(0).getCount()); // orig untouched
    }

    @Test
    void copyFrom_smallerSource_blanksExtraSlotsInTarget() {
        SlotBasedStorage src = new SlotBasedStorage(2);
        src.setSlot(0, new ItemStack(Items.EMERALD, 3));

        SlotBasedStorage dst = new SlotBasedStorage(4);
        dst.setSlot(3, new ItemStack(Items.BREAD, 9)); // will be blanked

        dst.copyFrom(src);

        assertEquals(3, dst.getSlot(0).getCount());
        assertTrue(dst.getSlot(1).isEmpty());
        assertTrue(dst.getSlot(2).isEmpty());
        assertTrue(dst.getSlot(3).isEmpty());
    }

    @Test
    void copyFrom_largerSource_copiesOnlyPrefix() {
        SlotBasedStorage src = new SlotBasedStorage(4);
        src.setSlot(0, new ItemStack(Items.EMERALD, 1));
        src.setSlot(3, new ItemStack(Items.DIAMOND, 2));

        SlotBasedStorage dst = new SlotBasedStorage(2);
        dst.copyFrom(src);

        assertEquals(1, dst.getSlot(0).getCount());
        assertTrue(dst.getSlot(1).isEmpty());
        // dst never had slot 3
    }

    // --- quirk pinning (documented in vault note Open questions) ---

    @Test
    void addRemove_asymmetry_sameItemDifferentTags_doNotStackOnAdd_butRemoveSeesBoth() {
        // QUIRK (pinned): addItem / findStackable use isSameItemSameTags (tags matter).
        // removeItem / getTotalCount use raw getItem() == (tags ignored).
        // For the payment buffer this is usually fine (rewards are plain stacks),
        // but means you can "hide" tagged variants from stacking while still having
        // them drained by a bulk removeItem(emerald, N).
        SlotBasedStorage s = new SlotBasedStorage(3);

        ItemStack a = new ItemStack(Items.EMERALD, 4);
        ItemStack b = new ItemStack(Items.EMERALD, 5);
        CompoundTag tb = new CompoundTag();
        tb.putInt("Unique", 42);
        b.getOrCreateTag().put("Unique", tb);

        s.addItem(a);
        s.addItem(b);
        // Because tags differ they land in separate slots (no stacking)
        assertEquals(2, countNonEmptySlots(s));

        // removeItem still aggregates across both because it only checks item type
        ItemStack pulled = s.removeItem(Items.EMERALD, 100);
        assertEquals(9, pulled.getCount());
        assertEquals(0, s.getTotalCount(Items.EMERALD));
    }

    @Test
    void zeroSlotStorage_allOpsDegenerateGracefully() {
        SlotBasedStorage s = new SlotBasedStorage(0);
        assertEquals(0, s.getSlotCount());
        assertEquals(-1, s.findEmptySlot());
        assertEquals(-1, s.findStackableSlot(new ItemStack(Items.EMERALD, 1)));
        assertFalse(s.addItem(new ItemStack(Items.EMERALD, 1)));
        assertTrue(s.removeItem(Items.EMERALD, 1).isEmpty());
        assertEquals(0, s.getTotalCount(Items.EMERALD));
        assertTrue(s.isEmpty());
        // NBT still roundtrips the declared 0
        CompoundTag tag = s.toNBT();
        assertEquals(0, tag.getInt("SlotCount"));
        SlotBasedStorage loaded = new SlotBasedStorage(0);
        loaded.fromNBT(tag);
        assertEquals(0, loaded.getSlotCount());
    }

    // --- small utility for tests ---

    private int countNonEmptySlots(SlotBasedStorage s) {
        int n = 0;
        for (int i = 0; i < s.getSlotCount(); i++) {
            if (!s.getSlot(i).isEmpty()) n++;
        }
        return n;
    }
}
