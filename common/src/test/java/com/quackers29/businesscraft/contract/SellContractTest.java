package com.quackers29.businesscraft.contract;

import com.quackers29.businesscraft.config.ConfigLoader;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-004: Sell Contract Lifecycle (Test + Docs Loop).
 *
 * Covers:
 * - Creation and load validation (clamping of quantity [1,10M] and pricePerUnit [0.01,1M])
 * - State machine predicates and transitions: isAuctionClosed, isCourierAssigned,
 *   isSnailMail (via SNAIL_MAIL_UUID sentinel), isDeliveryComplete (via deliveredAmount),
 *   isDelivered flag, isCompleted (base)
 * - Timing controls for lifecycle phases: isExpired, expireNow, extendExpiry (always
 *   sets from call-time "now", not relative)
 * - Delivery accumulation (addDeliveredAmount crosses threshold)
 * - Full serialization round-trips (Sell-specific fields + base Contract fields + bids)
 *   exercising the exact save/loadAdditional paths used by ContractSavedData and packets.
 *
 * All tests are pure logic. No Town, ServerLevel, ContractBoard orchestration,
 * escrow, market, or registry access. CompoundTag is used (allowed per protocol).
 *
 * Some clamp + auction-closed + getCurrentBid + basic bid roundtrip logic overlaps
 * ContractBoardTest (T-003); this test adds dedicated coverage for the delivery,
 * courier, completion, and snail-mail portions of the lifecycle.
 *
 * Documentation: vault/Trade/Contracts/Sell Contract Lifecycle.md
 */
class SellContractTest {

    private static final UUID ISSUER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID WINNER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID COURIER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID BIDDER = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private int savedDefaultPop;
    private double savedCourierAcceptMin;
    private double savedCourierDeliveryMinPerM;
    private double savedSnailMailMinPerM;

    @BeforeEach
    void setUp() {
        savedDefaultPop = ConfigLoader.defaultStartingPopulation;
        savedCourierAcceptMin = ConfigLoader.contractCourierAcceptanceMinutes;
        savedCourierDeliveryMinPerM = ConfigLoader.contractCourierDeliveryMinutesPerMeter;
        savedSnailMailMinPerM = ConfigLoader.contractSnailMailDeliveryMinutesPerMeter;

        // Values not mutated by the pure Sell/Contract paths under test; saved/restored
        // for determinism and to follow protocol pattern for mutable static config.
        ConfigLoader.defaultStartingPopulation = 5;
    }

    @AfterEach
    void tearDown() {
        ConfigLoader.defaultStartingPopulation = savedDefaultPop;
        ConfigLoader.contractCourierAcceptanceMinutes = savedCourierAcceptMin;
        ConfigLoader.contractCourierDeliveryMinutesPerMeter = savedCourierDeliveryMinPerM;
        ConfigLoader.contractSnailMailDeliveryMinutesPerMeter = savedSnailMailMinPerM;
    }

    // --- helpers ---

    private SellContract sell(long qty, float pricePer) {
        return new SellContract(ISSUER, "SellerTown", 60_000L, "iron", qty, pricePer);
    }

    private SellContract sellWithDuration(long durationMs, long qty, float pricePer) {
        return new SellContract(ISSUER, "SellerTown", durationMs, "copper", qty, pricePer);
    }

    // --- clamping (validation on create and on every load) ---

    @Test
    void sellContract_ctor_clampsQuantityAndPrice() {
        // low/zero/neg -> lower bounds
        SellContract tooLow = new SellContract(ISSUER, "S", 1L, "x", 0L, 0.0f);
        // qty: max(1, min(10M, 0)) = 1; price: max(0.01, min(1M, 0)) = 0.01
        assertEquals(1L, tooLow.getQuantity());
        assertEquals(0.01f, tooLow.getPricePerUnit());

        // high -> upper bounds
        SellContract tooHigh = new SellContract(ISSUER, "S", 1L, "x", 20_000_000L, 2_000_000f);
        // qty: max(1, min(10M, 20M)) = 10M; price: max(0.01, min(1M, 2M)) = 1M
        assertEquals(10_000_000L, tooHigh.getQuantity());
        assertEquals(1_000_000f, tooHigh.getPricePerUnit());
    }

    @Test
    void sellContract_load_clampsQuantityAndPrice() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", UUID.randomUUID());
        tag.putUUID("issuerTownId", ISSUER);
        tag.putString("issuerTownName", "S");
        tag.putLong("creationTime", System.currentTimeMillis());
        tag.putLong("expiryTime", System.currentTimeMillis() + 60_000);
        tag.putBoolean("isCompleted", false);
        tag.putString("resourceId", "iron");
        tag.putLong("quantity", 0L);       // bad -> clamp to 1
        tag.putFloat("pricePerUnit", 0.0f); // bad -> clamp to 0.01
        tag.putFloat("acceptedBid", 0f);
        tag.putBoolean("isDelivered", false);
        // no bids, no courier/delivery fields (optionals)

        SellContract sc = new SellContract(tag);
        // clamps re-applied in loadAdditional exactly as in ctor
        assertEquals(1L, sc.getQuantity());
        assertEquals(0.01f, sc.getPricePerUnit());
    }

    // --- auction state (isAuctionClosed via winning) ---

    @Test
    void isAuctionClosed_falseUntilWinningTownSet() {
        SellContract sc = sell(64, 3.0f);
        assertFalse(sc.isAuctionClosed());
        assertNull(sc.getWinningTownId());
        assertNull(sc.getWinningTownName());

        sc.setWinningTown(WINNER, "WinnerTown");
        assertTrue(sc.isAuctionClosed());
        assertEquals(WINNER, sc.getWinningTownId());
        assertEquals("WinnerTown", sc.getWinningTownName());

        // set ID only clears name (as prod does)
        sc.setWinningTownId(BIDDER);
        assertTrue(sc.isAuctionClosed());
        assertEquals(BIDDER, sc.getWinningTownId());
        assertNull(sc.getWinningTownName());
    }

    @Test
    void getCurrentBid_isOriginalAsk_notLiveHighest() {
        SellContract sc = sell(64, 3.0f); // ask = 192
        assertEquals(192f, sc.getCurrentBid());

        sc.addBid(BIDDER, "B", 250f);
        // still the original ask price*quantity, not the highest live bid
        assertEquals(192f, sc.getCurrentBid());
        assertEquals(250f, sc.getHighestBid());
    }

    @Test
    void getAmount_returnsIntCastOfQuantity() {
        SellContract sc = sell(128, 1.5f);
        assertEquals(128, sc.getAmount()); // post-clamp safe
    }

    // --- delivery state machine (isDeliveryComplete via amount, separate isDelivered flag) ---

    @Test
    void isDeliveryComplete_falseAtZero_andTrueWhenDeliveredAmountReachesOrExceeds() {
        SellContract sc = sell(100, 1.0f);
        assertEquals(0L, sc.getDeliveredAmount());
        assertFalse(sc.isDeliveryComplete());
        assertFalse(sc.isDelivered());

        sc.setDeliveredAmount(99);
        assertFalse(sc.isDeliveryComplete());

        sc.setDeliveredAmount(100);
        assertTrue(sc.isDeliveryComplete()); // >=

        sc.setDeliveredAmount(150);
        assertTrue(sc.isDeliveryComplete()); // stays true when over
    }

    @Test
    void addDeliveredAmount_accumulates_andFlipsIsDeliveryComplete_onThresholdCross() {
        SellContract sc = sell(50, 2.0f);
        assertFalse(sc.isDeliveryComplete());

        sc.addDeliveredAmount(20);
        assertEquals(20L, sc.getDeliveredAmount());
        assertFalse(sc.isDeliveryComplete());

        sc.addDeliveredAmount(30);
        assertEquals(50L, sc.getDeliveredAmount());
        assertTrue(sc.isDeliveryComplete()); // crossed on this add

        sc.addDeliveredAmount(5);
        assertEquals(55L, sc.getDeliveredAmount());
        assertTrue(sc.isDeliveryComplete());
    }

    @Test
    void isDelivered_flag_isIndependentOfAmountBasedIsDeliveryComplete() {
        SellContract sc = sell(10, 1.0f);
        assertFalse(sc.isDelivered());
        assertFalse(sc.isDeliveryComplete());

        sc.addDeliveredAmount(10);
        assertTrue(sc.isDeliveryComplete());
        assertFalse(sc.isDelivered()); // flag not auto-set by amount

        sc.setDelivered(true);
        assertTrue(sc.isDelivered());
    }

    // --- courier assignment state (including snail sentinel) ---

    @Test
    void isCourierAssigned_and_isSnailMail_trackCourierIdIncludingSentinel() {
        SellContract sc = sell(20, 4.0f);
        assertFalse(sc.isCourierAssigned());
        assertFalse(sc.isSnailMail());

        sc.setCourierId(COURIER);
        assertTrue(sc.isCourierAssigned());
        assertFalse(sc.isSnailMail());
        assertEquals(COURIER, sc.getCourierId());

        sc.setCourierId(SellContract.SNAIL_MAIL_UUID);
        assertTrue(sc.isCourierAssigned());
        assertTrue(sc.isSnailMail());
        assertEquals(SellContract.SNAIL_MAIL_UUID, sc.getCourierId());
    }

    @Test
    void courierReward_and_acceptedTime_roundTripWithAssignment() {
        SellContract sc = sell(5, 10.0f);
        sc.setCourierId(COURIER);
        sc.setCourierReward(17.0f);
        sc.setCourierAcceptedTime(1234567890L);

        assertEquals(17.0f, sc.getCourierReward());
        assertEquals(1234567890L, sc.getCourierAcceptedTime());
    }

    // --- base Contract lifecycle (completion + expiry controls) ---

    @Test
    void isCompleted_falseUntilCompleteCalled_idempotent() {
        SellContract sc = sell(30, 1.0f);
        assertFalse(sc.isCompleted());

        sc.complete();
        assertTrue(sc.isCompleted());

        sc.complete(); // idempotent
        assertTrue(sc.isCompleted());
    }

    @Test
    void isExpired_falseWithFutureDuration_thenTrueAfterExpireNow() {
        // large duration ensures still future even after ctor overhead
        SellContract sc = sellWithDuration(10_000_000_000L, 10, 1.0f);
        assertFalse(sc.isExpired());

        sc.expireNow();
        assertTrue(sc.isExpired());
    }

    @Test
    void extendExpiry_setsFromCallTime_notRelativeToPrior_remaining_pinningCurrentBehavior() {
        // QUIRK/PIN: extendExpiry does `expiry = now + delta`, discarding any prior
        // remaining time (or past-due state). This is used to grant fresh windows
        // (acceptance, delivery, snail) from the decision instant. Test pins it.
        SellContract sc = sellWithDuration(1L, 10, 1.0f);
        sc.expireNow();
        assertTrue(sc.isExpired());

        sc.extendExpiry(60_000L);
        assertFalse(sc.isExpired()); // full 60s window from *this call's now*
    }

    @Test
    void expireNow_forcesExpired_andExtendCanRevive() {
        SellContract sc = sell(10, 1.0f);
        sc.expireNow();
        assertTrue(sc.isExpired());

        sc.extendExpiry(5000L);
        assertFalse(sc.isExpired());
    }

    // --- full NBT round-trips (base + sell-specific, all optional paths) ---

    @Test
    void bidsAndWinningAndAccepted_roundTrip_highestSelectionPreserved() {
        SellContract sc = sell(32, 5.0f);
        sc.addBid(BIDDER, "One", 175f);
        sc.addBid(WINNER, "Two", 190f);
        sc.setWinningTown(WINNER, "Two");
        sc.setAcceptedBid(190f);

        CompoundTag tag = new CompoundTag();
        sc.save(tag);

        SellContract loaded = new SellContract(tag);
        assertEquals(190f, loaded.getHighestBid());
        assertEquals(WINNER, loaded.getHighestBidder());
        assertEquals("Two", loaded.getBidderName(WINNER));
        assertTrue(loaded.isAuctionClosed());
        assertEquals(190f, loaded.getAcceptedBid());
        assertEquals(WINNER, loaded.getWinningTownId());
        assertEquals("Two", loaded.getWinningTownName());
    }

    @Test
    void courierAndDeliveryState_roundTripViaNbt_snailAndDeliveredOver() {
        SellContract sc = sell(64, 2.5f);
        sc.setWinningTown(WINNER, "Winner");
        sc.setAcceptedBid(200f);
        sc.setCourierId(SellContract.SNAIL_MAIL_UUID);
        sc.setCourierReward(12.0f);
        sc.setCourierAcceptedTime(9876543210L);
        sc.setDeliveredAmount(70L); // > qty
        sc.setDelivered(true);
        sc.complete();

        CompoundTag tag = new CompoundTag();
        sc.save(tag);

        SellContract loaded = new SellContract(tag);
        assertTrue(loaded.isSnailMail());
        assertEquals(12.0f, loaded.getCourierReward());
        assertEquals(9876543210L, loaded.getCourierAcceptedTime());
        assertEquals(70L, loaded.getDeliveredAmount());
        assertTrue(loaded.isDeliveryComplete());
        assertTrue(loaded.isDelivered());
        assertTrue(loaded.isCompleted());
    }

    @Test
    void fullSellState_roundTripViaNbt_preservesAllFieldsAndBids() {
        SellContract sc = sell(100, 1.25f);
        sc.addBid(BIDDER, "BidderT", 140f);
        sc.setBuyerTownId(WINNER); // even if underused
        sc.setWinningTown(WINNER, "WinnerT");
        sc.setAcceptedBid(140f);
        sc.setCourierId(COURIER);
        sc.setCourierReward(8.0f);
        sc.setCourierAcceptedTime(1111111111L);
        sc.addDeliveredAmount(150); // >= qty=100 to make isDeliveryComplete true
        sc.setDelivered(true);
        sc.complete();
        // leave expiry/creation as set by ctor

        CompoundTag tag = new CompoundTag();
        sc.save(tag);

        SellContract loaded = new SellContract(tag);

        assertEquals("iron", loaded.getResourceId());
        assertEquals(100L, loaded.getQuantity());
        assertEquals(1.25f, loaded.getPricePerUnit());
        assertEquals(140f, loaded.getAcceptedBid());
        assertEquals(WINNER, loaded.getBuyerTownId());
        assertEquals(WINNER, loaded.getWinningTownId());
        assertEquals("WinnerT", loaded.getWinningTownName());
        assertTrue(loaded.isCourierAssigned());
        assertFalse(loaded.isSnailMail());
        assertEquals(COURIER, loaded.getCourierId());
        assertEquals(8.0f, loaded.getCourierReward());
        assertEquals(1111111111L, loaded.getCourierAcceptedTime());
        assertEquals(150L, loaded.getDeliveredAmount());
        assertTrue(loaded.isDelivered());
        assertTrue(loaded.isDeliveryComplete());
        assertTrue(loaded.isCompleted());
        assertEquals(140f, loaded.getHighestBid());
        assertEquals(BIDDER, loaded.getHighestBidder());
        assertEquals("BidderT", loaded.getBidderName(BIDDER));
    }

    @Test
    void buyerTownId_roundTrips_evenThoughUnderusedInAuctionFlow() {
        SellContract sc = sell(10, 1.0f);
        assertNull(sc.getBuyerTownId());

        sc.setBuyerTownId(WINNER);
        assertEquals(WINNER, sc.getBuyerTownId());

        CompoundTag tag = new CompoundTag();
        sc.save(tag);
        SellContract loaded = new SellContract(tag);
        assertEquals(WINNER, loaded.getBuyerTownId());
    }

    @Test
    void getBidderName_fallsBackToUnknown_whenNotProvided() {
        SellContract sc = sell(5, 1.0f);
        sc.addBid(BIDDER, null, 50f); // name null -> not stored

        assertEquals("Unknown Town", sc.getBidderName(BIDDER));

        CompoundTag tag = new CompoundTag();
        sc.save(tag);
        SellContract loaded = new SellContract(tag);
        assertEquals("Unknown Town", loaded.getBidderName(BIDDER));
    }

    @Test
    void clampingSurvivesSaveLoad_badValuesInTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", UUID.randomUUID());
        tag.putUUID("issuerTownId", ISSUER);
        tag.putString("issuerTownName", "S");
        tag.putLong("creationTime", System.currentTimeMillis());
        tag.putLong("expiryTime", System.currentTimeMillis() + 60_000);
        tag.putBoolean("isCompleted", false);
        tag.putString("resourceId", "gold");
        tag.putLong("quantity", 999_999_999L); // > 10M -> clamp on load
        tag.putFloat("pricePerUnit", 2_000_000f); // > 1M -> clamp on load
        tag.putFloat("acceptedBid", 0f);
        tag.putBoolean("isDelivered", false);
        tag.putLong("deliveredAmount", 0L);

        SellContract loaded = new SellContract(tag);
        assertEquals(10_000_000L, loaded.getQuantity());
        assertEquals(1_000_000f, loaded.getPricePerUnit());
    }
}
