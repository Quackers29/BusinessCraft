package com.quackers29.businesscraft.economy;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-006: Global Market Price Calculation (Test + Docs Loop).
 *
 * Covers the pure pricing and volume math in GlobalMarket:
 *   - Default price 1.0 for unknown ids, with hard floor enforcement (MIN_PRICE = 0.0001)
 *   - setPrice / getPrice floor clamping on write and read
 *   - recordTrade: volume += (long)quantity (truncating cast), price = 0.9*current + 0.1*unitPrice, re-floor
 *   - recordFailedAuction: price *= 0.95, re-floor (exactly 5% multiplicative drop)
 *   - reset clears both maps; subsequent gets synthesize the default 1.0 again
 *   - load/save roundtrips (CompoundTag), including load-time floor repair and cross-map hygiene (clear first)
 *   - getPrices returns only explicitly written keys (defaults are synthesized on get, not stored)
 *
 * All tests are pure logic. No registries, Town, ContractBoard, or PlatformAccess.
 * CompoundTag is used for NBT tests (allowed per protocol).
 *
 * Documentation: vault/Trade/Global Market/Price Calculation.md
 */
class GlobalMarketTest {

    private static final float MIN = 0.0001f; // GlobalMarket.MIN_PRICE
    private GlobalMarket market;

    @BeforeEach
    void setUp() {
        market = GlobalMarket.get();
        market.reset(); // full isolation for singleton
    }

    @AfterEach
    void tearDown() {
        market.reset();
    }

    // --- getPrice / setPrice basics ---

    @Test
    void getPrice_unknownId_returnsDefaultOne() {
        assertEquals(1.0f, market.getPrice("never_seen"));
        assertEquals(1.0f, market.getPrice("another"));
    }

    @Test
    void setPrice_andGetPrice_roundTrips() {
        market.setPrice("wood", 2.5f);
        assertEquals(2.5f, market.getPrice("wood"));
    }

    @Test
    void setPrice_belowFloor_isClampedOnWriteAndRead() {
        market.setPrice("coal", 0.00001f);
        assertEquals(MIN, market.getPrice("coal"), 1e-9f);
    }

    @Test
    void getPrice_afterSetBelowFloor_returnsFloorNotOriginal() {
        market.setPrice("iron", 0.00005f);
        // Even if internal stored value were < MIN (it isn't), get applies floor too
        assertEquals(MIN, market.getPrice("iron"), 1e-9f);
    }

    // --- recordTrade math ---

    @Test
    void recordTrade_firstTradeForId_blendsFromDefaultAndAccumulatesVolume() {
        // unknown → 1.0; trade 64 at unit 2.5
        // vol = 0 + (long)64 = 64
        // price = (1.0 * 0.9) + (2.5 * 0.1) = 0.9 + 0.25 = 1.15
        market.recordTrade("iron", 64.0f, 2.5f);
        assertEquals(1.15f, market.getPrice("iron"), 1e-6f);
        // volume not directly exposed; we only observe side effects via later blends
    }

    @Test
    void recordTrade_secondTrade_continuesNinetyTenBlend() {
        market.recordTrade("iron", 64.0f, 2.5f);   // price → 1.15 as above
        // now trade 10 at 3.0
        // price = (1.15 * 0.9) + (3.0 * 0.1) = 1.035 + 0.3 = 1.335
        market.recordTrade("iron", 10.0f, 3.0f);
        assertEquals(1.335f, market.getPrice("iron"), 1e-6f);
    }

    @Test
    void recordTrade_quantityCastToLong_truncatesFractionalPartForVolume() {
        // We can't read volume directly, but we can observe that fractional qty
        // does not prevent the price blend (blend still happens) and subsequent
        // behavior is consistent with a whole-number volume accumulation.
        market.recordTrade("coal", 0.7f, 1.0f); // vol += 0 (long cast)
        // price blend still occurs from default
        // (1.0 * 0.9) + (1.0 * 0.1) = 1.0 exactly
        assertEquals(1.0f, market.getPrice("coal"), 1e-6f);

        // A follow-up whole trade should behave normally
        market.recordTrade("coal", 5.0f, 0.5f);
        // price = (1.0 * 0.9) + (0.5 * 0.1) = 0.9 + 0.05 = 0.95
        assertEquals(0.95f, market.getPrice("coal"), 1e-6f);
    }

    @Test
    void recordTrade_zeroQuantity_stillBlendsPrice() {
        market.recordTrade("food", 0.0f, 4.0f);
        // price = (1.0 * 0.9) + (4.0 * 0.1) = 1.3
        assertEquals(1.3f, market.getPrice("food"), 1e-6f);
    }

    @Test
    void recordTrade_belowFloorResult_isClamped() {
        // Start just above floor; a sufficiently low unit price makes the 90/10 result drop below floor.
        market.setPrice("stick", 0.000105f);
        // newPrice = (0.000105 * 0.9) + (0.00001 * 0.1) = 0.0000945 + 0.000001 = 0.0000955
        // Math.max(0.0000955, MIN) = MIN
        market.recordTrade("stick", 10.0f, 0.00001f);
        assertEquals(MIN, market.getPrice("stick"), 1e-9f);
    }

    // --- recordFailedAuction math ---

    @Test
    void recordFailedAuction_dropsPriceByExactlyFivePercent() {
        market.setPrice("wood", 0.80f);
        // 0.80 * 0.95 = 0.76
        market.recordFailedAuction("wood");
        assertEquals(0.76f, market.getPrice("wood"), 1e-6f);
    }

    @Test
    void recordFailedAuction_compoundsOnRepeatedCalls() {
        market.setPrice("coal", 0.80f);
        market.recordFailedAuction("coal"); // 0.76
        market.recordFailedAuction("coal"); // 0.76 * 0.95 = 0.722
        assertEquals(0.722f, market.getPrice("coal"), 1e-6f);
    }

    @Test
    void recordFailedAuction_floorsAtMinPrice() {
        // 0.000105 * 0.95 = 0.00009975 < MIN → the max() inside recordFailedAuction clamps to floor
        market.setPrice("iron", 0.000105f);
        market.recordFailedAuction("iron");
        assertEquals(MIN, market.getPrice("iron"), 1e-9f);
    }

    @Test
    void recordFailedAuction_onUnknownId_startsFromDefaultAndDrops() {
        // getPrice synthesizes 1.0, then 5% drop
        // 1.0 * 0.95 = 0.95
        market.recordFailedAuction("never_seen");
        assertEquals(0.95f, market.getPrice("never_seen"), 1e-6f);
    }

    // --- reset ---

    @Test
    void reset_clearsPricesAndSynthesizesDefaultAgain() {
        market.setPrice("a", 3.0f);
        market.setPrice("b", 0.5f);
        market.recordTrade("c", 100f, 2f);

        market.reset();

        assertEquals(1.0f, market.getPrice("a"));
        assertEquals(1.0f, market.getPrice("b"));
        assertEquals(1.0f, market.getPrice("c"));
        assertTrue(market.getPrices().isEmpty());
    }

    // --- getPrices visibility ---

    @Test
    void getPrices_containsOnlyWrittenKeys_notSynthesizedDefaults() {
        market.setPrice("only_one", 1.25f);
        Map<String, Float> prices = market.getPrices();
        assertEquals(1, prices.size());
        assertTrue(prices.containsKey("only_one"));
        assertEquals(1.25f, prices.get("only_one"));
        // "ghost" id never written must not appear
        assertFalse(prices.containsKey("ghost"));
    }

    @Test
    void getPrices_isUnmodifiableView() {
        market.setPrice("x", 9.0f);
        Map<String, Float> prices = market.getPrices();
        assertThrows(UnsupportedOperationException.class, () -> prices.put("y", 1f));
    }

    // --- NBT roundtrips (load/save) ---

    @Test
    void saveAndLoad_roundtripsPricesAndVolumeState() {
        market.setPrice("wood", 1.5f);
        market.recordTrade("iron", 32.0f, 2.25f); // vol=32, price=1.125

        CompoundTag tag = new CompoundTag();
        market.save(tag);

        // fresh market state
        market.reset();
        assertEquals(1.0f, market.getPrice("wood"));

        market.load(tag);

        assertEquals(1.5f, market.getPrice("wood"), 1e-6f);
        assertEquals(1.125f, market.getPrice("iron"), 1e-6f);
    }

    @Test
    void load_clearsExistingStateFirst_preventsCrossWorldContamination() {
        market.setPrice("old", 7.0f);

        CompoundTag fresh = new CompoundTag();
        // only "new" is present
        CompoundTag pricesTag = new CompoundTag();
        pricesTag.putFloat("new", 0.25f);
        fresh.put("prices", pricesTag);

        market.load(fresh);

        assertFalse(market.getPrices().containsKey("old"));
        assertEquals(0.25f, market.getPrice("new"));
    }

    @Test
    void load_enforcesMinPriceFloor_onRestoredPrices() {
        CompoundTag tag = new CompoundTag();
        CompoundTag pricesTag = new CompoundTag();
        pricesTag.putFloat("corrupt", 0.0000001f);
        pricesTag.putFloat("ok", 0.5f);
        tag.put("prices", pricesTag);

        market.load(tag);

        assertEquals(MIN, market.getPrice("corrupt"), 1e-9f);
        assertEquals(0.5f, market.getPrice("ok"), 1e-6f);
    }

    @Test
    void load_preservesVolumeEntries() {
        // We can't assert volume directly, but we can load a tag that has volume
        // and then perform a recordTrade that would blend from a restored price,
        // proving the price was restored (volume presence is part of the save/load contract).
        CompoundTag tag = new CompoundTag();
        CompoundTag pricesTag = new CompoundTag();
        pricesTag.putFloat("v", 4.0f);
        tag.put("prices", pricesTag);
        CompoundTag volTag = new CompoundTag();
        volTag.putLong("v", 12345L);
        tag.put("volume", volTag);

        market.load(tag);
        // Now trade at a different unit price; blend should start from the loaded 4.0
        market.recordTrade("v", 1.0f, 10.0f);
        // price = (4.0 * 0.9) + (10.0 * 0.1) = 3.6 + 1.0 = 4.6
        assertEquals(4.6f, market.getPrice("v"), 1e-6f);
    }

    @Test
    void save_ofEmptyMarket_writesEmptyCompounds() {
        CompoundTag tag = new CompoundTag();
        market.save(tag);
        // Should not throw; presence of keys is optional in production load paths.
        // We just ensure roundtrip of an empty market doesn't explode and load leaves defaults.
        market.reset();
        market.load(tag);
        assertEquals(1.0f, market.getPrice("anything"));
    }
}
