package com.quackers29.businesscraft.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-015: Platform Data Model (Test + Docs Loop).
 *
 * Covers the pure data model, validation, destination management, NBT
 * roundtrips, defensive copies, and equals/hash for Platform (the core
 * representation of a town's tourist path endpoint + allowed destinations).
 *
 * No Minecraft bootstrap required — only BlockPos, CompoundTag, UUID (allowed
 * per protocol; confirmed in T-001 and others).
 *
 * Documentation: vault/Town/Platforms/Platform Data Model.md
 */
class PlatformTest {

    private static final UUID ID1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DEST_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DEST_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final BlockPos START = new BlockPos(100, 64, 200);
    private static final BlockPos END = new BlockPos(150, 64, 200);

    // --- ctor + basic state ---

    @Test
    void defaultCtor_initializesRandomIdDefaultNameAndEnabled() {
        Platform p = new Platform();
        assertNotNull(p.getId());
        assertEquals("New Platform", p.getName());
        assertTrue(p.isEnabled());
        assertNull(p.getStartPos());
        assertNull(p.getEndPos());
        assertTrue(p.hasNoEnabledDestinations());
    }

    @Test
    void idCtor_preservesIdAndDefaults() {
        Platform p = new Platform(ID1);
        assertEquals(ID1, p.getId());
        assertEquals("New Platform", p.getName());
        assertTrue(p.isEnabled());
    }

    @Test
    void fullCtor_preservesAllState() {
        Platform p = new Platform("Main Rail", false, START, END);
        assertNotNull(p.getId()); // still random
        assertEquals("Main Rail", p.getName());
        assertFalse(p.isEnabled());
        assertEquals(START, p.getStartPos());
        assertEquals(END, p.getEndPos());
    }

    // --- isComplete validation ---

    @Test
    void isComplete_falseWhenBothNull() {
        Platform p = new Platform();
        assertFalse(p.isComplete());
    }

    @Test
    void isComplete_falseWhenOnlyStartSet() {
        Platform p = new Platform();
        p.setStartPos(START);
        assertFalse(p.isComplete());
    }

    @Test
    void isComplete_falseWhenOnlyEndSet() {
        Platform p = new Platform();
        p.setEndPos(END);
        assertFalse(p.isComplete());
    }

    @Test
    void isComplete_trueWhenBothPositionsSet() {
        Platform p = new Platform("P", true, START, END);
        assertTrue(p.isComplete());
    }

    // --- destination set management + hasNo + getDestinations transform ---

    @Test
    void hasNoEnabledDestinations_trueForNewPlatform() {
        Platform p = new Platform();
        assertTrue(p.hasNoEnabledDestinations());
    }

    @Test
    void enableDestination_addsAndHasNoBecomesFalse() {
        Platform p = new Platform();
        p.enableDestination(DEST_A);
        assertFalse(p.hasNoEnabledDestinations());
        assertTrue(p.isDestinationEnabled(DEST_A));
    }

    @Test
    void enableDestination_duplicateIsIdempotent_setSizeOne() {
        Platform p = new Platform();
        p.enableDestination(DEST_A);
        p.enableDestination(DEST_A);
        assertEquals(1, p.getEnabledDestinations().size());
    }

    @Test
    void disableDestination_removesAndHasNoBecomesTrueWhenLast() {
        Platform p = new Platform();
        p.enableDestination(DEST_A);
        p.enableDestination(DEST_B);
        p.disableDestination(DEST_A);
        assertFalse(p.isDestinationEnabled(DEST_A));
        assertTrue(p.isDestinationEnabled(DEST_B));
        p.disableDestination(DEST_B);
        assertTrue(p.hasNoEnabledDestinations());
    }

    @Test
    void disableNonMember_isNoOp() {
        Platform p = new Platform();
        p.disableDestination(DEST_A); // no crash
        assertTrue(p.hasNoEnabledDestinations());
    }

    @Test
    void getEnabledDestinations_returnsDefensiveCopy_mutatingCopyDoesNotAffectOriginal() {
        Platform p = new Platform();
        p.enableDestination(DEST_A);
        Set<UUID> copy = p.getEnabledDestinations();
        copy.add(DEST_B);
        // original must be unaffected
        assertEquals(1, p.getEnabledDestinations().size());
        assertFalse(p.isDestinationEnabled(DEST_B));
    }

    @Test
    void getDestinations_returnsMapWithTrueValues_sizeMatches_andDefensive() {
        Platform p = new Platform();
        p.enableDestination(DEST_A);
        p.enableDestination(DEST_B);
        var map = p.getDestinations();
        assertEquals(2, map.size());
        assertTrue(map.get(DEST_A));
        assertTrue(map.get(DEST_B));
        // mutate map — original unaffected
        map.put(UUID.randomUUID(), false);
        assertEquals(2, p.getDestinations().size());
    }

    @Test
    void setDestinationEnabled_trueAdds_falseRemoves() {
        Platform p = new Platform();
        p.setDestinationEnabled(DEST_A, true);
        assertTrue(p.isDestinationEnabled(DEST_A));
        p.setDestinationEnabled(DEST_A, false);
        assertFalse(p.isDestinationEnabled(DEST_A));
    }

    @Test
    void clearEnabledDestinations_emptiesSet() {
        Platform p = new Platform();
        p.enableDestination(DEST_A);
        p.enableDestination(DEST_B);
        p.clearEnabledDestinations();
        assertTrue(p.hasNoEnabledDestinations());
    }

    // --- name/enabled setters ---

    @Test
    void setNameAndSetEnabled_workAndAffectState() {
        Platform p = new Platform();
        p.setName("Express Line");
        p.setEnabled(false);
        assertEquals("Express Line", p.getName());
        assertFalse(p.isEnabled());
    }

    // --- NBT roundtrips (core + positions + destinations) ---

    @Test
    void nbtRoundtrip_fullState_preservesEverything_andEquals() {
        Platform original = new Platform("NBT Test", true, START, END);
        original.enableDestination(DEST_A);
        original.enableDestination(DEST_B);

        CompoundTag tag = original.save();
        Platform loaded = new Platform(tag);

        assertEquals(original, loaded);
        assertEquals("NBT Test", loaded.getName());
        assertTrue(loaded.isEnabled());
        assertEquals(START, loaded.getStartPos());
        assertEquals(END, loaded.getEndPos());
        assertTrue(loaded.isDestinationEnabled(DEST_A));
        assertTrue(loaded.isDestinationEnabled(DEST_B));
        assertTrue(loaded.isComplete());
    }

    @Test
    void nbtRoundtrip_noPositions_yieldsNulls_andNotComplete() {
        Platform original = new Platform("No Path", true, null, null);
        original.enableDestination(DEST_A);

        CompoundTag tag = original.save();
        Platform loaded = Platform.fromNBT(tag);

        assertEquals(original, loaded);
        assertNull(loaded.getStartPos());
        assertNull(loaded.getEndPos());
        assertFalse(loaded.isComplete());
        assertTrue(loaded.isDestinationEnabled(DEST_A));
    }

    @Test
    void nbtRoundtrip_zeroDestinations_writesCountZero_andHasNoTrue() {
        Platform original = new Platform("AnyTown", true, START, END);
        // no dests enabled

        CompoundTag tag = original.toNBT();
        Platform loaded = new Platform(tag);

        assertTrue(loaded.hasNoEnabledDestinations());
        assertEquals(0, loaded.getDestinations().size());
        // roundtrip equality
        assertEquals(original, loaded);
    }

    @Test
    void nbtLoad_missingOptionalPositions_andMissingDestinations_isSafe() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", ID1);
        tag.putString("Name", "Minimal");
        tag.putBoolean("Enabled", true);
        // deliberately no StartX/EndX and no "Destinations"

        Platform p = new Platform(tag);
        assertEquals(ID1, p.getId());
        assertEquals("Minimal", p.getName());
        assertTrue(p.isEnabled());
        assertNull(p.getStartPos());
        assertNull(p.getEndPos());
        assertTrue(p.hasNoEnabledDestinations());
    }

    @Test
    void toNBT_and_fromNBT_areAliasesFor_save_and_tagCtor() {
        Platform p1 = new Platform("Alias", false, START, END);
        p1.enableDestination(DEST_A);

        Platform p2 = Platform.fromNBT(p1.toNBT());
        assertEquals(p1, p2);
    }

    // --- equals / hashCode ---

    @Test
    void equals_sameStateIncludingDests_isTrue_andHashMatches() {
        Platform a = new Platform(ID1);
        a.setName("Same");
        a.setEnabled(true);
        a.setStartPos(START);
        a.setEndPos(END);
        a.enableDestination(DEST_A);

        Platform b = new Platform(ID1);
        b.setName("Same");
        b.setEnabled(true);
        b.setStartPos(START);
        b.setEndPos(END);
        b.enableDestination(DEST_A);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differsOnDestSet_isFalse() {
        Platform a = new Platform(ID1);
        a.enableDestination(DEST_A);

        Platform b = new Platform(ID1);
        b.enableDestination(DEST_B);

        assertNotEquals(a, b);
    }

    @Test
    void equals_nullOrWrongType_isFalse() {
        Platform p = new Platform();
        assertNotEquals(p, null);
        assertNotEquals(p, "not a platform");
    }

    // --- set pos after construction ---

    @Test
    void setStartPos_setEndPos_affectIsComplete() {
        Platform p = new Platform();
        assertFalse(p.isComplete());
        p.setStartPos(START);
        p.setEndPos(END);
        assertTrue(p.isComplete());
    }
}
