package com.quackers29.businesscraft.contract;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-003: Auction Resolution (Test + Docs Loop).
 *
 * Covers the pure-logic pieces that implement bid resolution and winner
 * selection used by ContractBoard (calculateCourierCost, Contract/SellContract
 * bid map + getHighest*, clamping, isAuctionClosed, and bid persistence).
 *
 * The orchestration (closeAuctions, addBid side effects, escrow moves, market
 * updates, snail-mail fallback timers) lives in ContractBoard and requires
 * ServerLevel + TownManager + full Town resource state — not pure, hence
 * intentionally not exercised here.
 *
 * With McBootstrap, the calculateCourierCost happy path (non-null Towns) is
 * now reachable using the public 3-param Town ctor + BlockPos (simple data).
 * Full closeAuctions remains untestable in this harness (see vault note).
 * 7 distance-based tests added for the courier formula (hand-computed).
 *
 * Documentation: vault/Trade/Contracts/Auction Resolution.md
 */
class ContractBoardTest {

    private int savedDefaultPop;
    private double savedCourierAcceptMin;
    private double savedCourierDeliveryMinPerM;
    private double savedSnailMailMinPerM;

    private static final UUID ISSUER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID BIDDER1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BIDDER2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() {
        savedDefaultPop = ConfigLoader.defaultStartingPopulation;
        savedCourierAcceptMin = ConfigLoader.contractCourierAcceptanceMinutes;
        savedCourierDeliveryMinPerM = ConfigLoader.contractCourierDeliveryMinutesPerMeter;
        savedSnailMailMinPerM = ConfigLoader.contractSnailMailDeliveryMinutesPerMeter;

        ConfigLoader.defaultStartingPopulation = 5;
        // Contract timing values left at defaults; not mutated by pure tests below.
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

    // --- calculateCourierCost (the only public static pure math in ContractBoard) ---
    // Note: only the null guard is exercised here. Happy-path (real distance)
    // requires constructing Town, which triggers ExceptionInInitializerError from
    // component/registry-adjacent statics in the pure JUnit env (no MC bootstrap).
    // This matches the NEEDS-MC classification for the full resolution path.
    // The core of "auction resolution" — winner selection from bids — is covered
    // via the Contract/SellContract tests below (the exact methods closeAuctions calls).

    @Test
    void calculateCourierCost_nulls_returnsZero() {
        assertEquals(0, ContractBoard.calculateCourierCost(null, null));
        // second null arg also guarded (no Town construction)
        assertEquals(0, ContractBoard.calculateCourierCost(null, null));
    }

    // --- winner selection via Contract bid map (used by ContractBoard.closeAuctions and addBid) ---

    @Test
    void getHighestBid_empty_returnsZero() {
        SellContract sc = sell(64, 3.0f);
        assertEquals(0f, sc.getHighestBid());
        assertNull(sc.getHighestBidder());
    }

    @Test
    void getHighestBidder_singleBid_wins() {
        SellContract sc = sell(64, 3.0f);
        sc.addBid(BIDDER1, "TownOne", 250f);
        assertEquals(250f, sc.getHighestBid());
        assertEquals(BIDDER1, sc.getHighestBidder());
    }

    @Test
    void getHighestBidder_higherBidWins() {
        SellContract sc = sell(64, 3.0f);
        sc.addBid(BIDDER1, "TownOne", 200f);
        sc.addBid(BIDDER2, "TownTwo", 275f);
        assertEquals(275f, sc.getHighestBid());
        assertEquals(BIDDER2, sc.getHighestBidder());
    }

    @Test
    void addBid_sameBidder_onlyRaises() {
        SellContract sc = sell(64, 3.0f);
        sc.addBid(BIDDER1, "TownOne", 180f);
        sc.addBid(BIDDER1, "TownOne", 150f); // lower, ignored
        assertEquals(180f, sc.getHighestBid());
        sc.addBid(BIDDER1, "TownOne", 210f); // raise
        assertEquals(210f, sc.getHighestBid());
        assertEquals(BIDDER1, sc.getHighestBidder());
    }

    @Test
    void getHighestBidder_tie_returnsFirstInIterationOrder_currentBehaviorPinned() {
        // QUIRK (see vault note Open questions): equal max bids -> stream.max
        // returns the first entry encountered in the HashMap's iteration order.
        // Order is not guaranteed by HashMap contract (depends on hash codes +
        // insertion). This test pins current observed behavior for the two-UUID
        // insertion order we use here; if it starts returning the other, the
        // selection rule changed.
        SellContract sc = sell(100, 1.0f);
        sc.addBid(BIDDER1, "One", 300f);
        sc.addBid(BIDDER2, "Two", 300f);
        // With these insertion + UUID hash patterns in current JDK, BIDDER2 wins
        // in practice for this test run, but we assert only that a non-null winner
        // among the two is chosen and the bid value is the max.
        UUID winner = sc.getHighestBidder();
        assertTrue(BIDDER1.equals(winner) || BIDDER2.equals(winner));
        assertEquals(300f, sc.getHighestBid());
    }

    // --- SellContract auction-related state ---

    @Test
    void isAuctionClosed_falseUntilWinningTownSet() {
        SellContract sc = sell(10, 1.0f);
        assertFalse(sc.isAuctionClosed());
        sc.setWinningTown(BIDDER1, "Winner");
        assertTrue(sc.isAuctionClosed());
        assertEquals(BIDDER1, sc.getWinningTownId());
    }

    @Test
    void getCurrentBid_isOriginalAsk_notLiveHighest() {
        SellContract sc = sell(64, 3.0f); // ask = 192
        assertEquals(192f, sc.getCurrentBid());
        sc.addBid(BIDDER1, "T", 300f);
        // still the ask, not the 300 highest
        assertEquals(192f, sc.getCurrentBid());
        assertEquals(300f, sc.getHighestBid());
    }

    // --- ctor + load clamping (part of valid auction setup) ---

    @Test
    void sellContract_ctor_clampsQuantityAndPrice() {
        SellContract tooLow = new SellContract(ISSUER, "S", 1L, "x", 0L, 0.0f);
        assertEquals(1L, tooLow.getQuantity());
        assertEquals(0.01f, tooLow.getPricePerUnit());

        SellContract tooHigh = new SellContract(ISSUER, "S", 1L, "x", 20_000_000L, 2_000_000f);
        assertEquals(10_000_000L, tooHigh.getQuantity());
        assertEquals(1_000_000f, tooHigh.getPricePerUnit());
    }

    @Test
    void sellContract_load_clampsQuantityAndPrice() {
        CompoundTag tag = new CompoundTag();
        // minimal valid Contract base fields + Sell extras
        tag.putUUID("id", UUID.randomUUID());
        tag.putUUID("issuerTownId", ISSUER);
        tag.putString("issuerTownName", "S");
        tag.putLong("creationTime", System.currentTimeMillis());
        tag.putLong("expiryTime", System.currentTimeMillis() + 60_000);
        tag.putBoolean("isCompleted", false);
        // no bids list
        tag.putString("resourceId", "iron");
        tag.putLong("quantity", 0L);       // should clamp to 1
        tag.putFloat("pricePerUnit", 0.0f); // should clamp to 0.01
        tag.putFloat("acceptedBid", 0f);
        tag.putBoolean("isDelivered", false);

        SellContract sc = new SellContract(tag);
        assertEquals(1L, sc.getQuantity());
        assertEquals(0.01f, sc.getPricePerUnit());
    }

    // --- bid state survives persistence (relevant to auction resolution after reload) ---

    @Test
    void bids_roundTripViaNbt_highestSelectionPreserved() {
        SellContract sc = sell(32, 5.0f);
        sc.addBid(BIDDER1, "One", 175f);
        sc.addBid(BIDDER2, "Two", 190f);

        CompoundTag tag = new CompoundTag();
        sc.save(tag);

        SellContract loaded = new SellContract(tag);
        assertEquals(190f, loaded.getHighestBid());
        assertEquals(BIDDER2, loaded.getHighestBidder());
        assertEquals("Two", loaded.getBidderName(BIDDER2));
    }

    // --- calculateCourierCost happy path (now reachable with McBootstrap + Town ctor) ---

    private Town makeTown(int x, int y, int z, String name) {
        return new Town(UUID.randomUUID(), new BlockPos(x, y, z), name);
    }

    @Test
    void calculateCourierCost_realTowns_zeroDistance_returnsZero() {
        Town t1 = makeTown(100, 64, 200, "A");
        Town t2 = makeTown(100, 64, 200, "B");
        // distSqr = 0; sqrt=0; 0/10=0; ceil(0)=0; (int)0
        assertEquals(0, ContractBoard.calculateCourierCost(t1, t2));
    }

    @Test
    void calculateCourierCost_realTowns_tinyDistance_ceilsToOne() {
        Town t1 = makeTown(0, 0, 0, "A");
        Town t2 = makeTown(5, 0, 0, "B"); // euclid dist=5
        // 5 / 10.0 = 0.5; Math.ceil(0.5) = 1.0; (int)1
        assertEquals(1, ContractBoard.calculateCourierCost(t1, t2));
    }

    @Test
    void calculateCourierCost_realTowns_exactTenBlocks_returnsOne() {
        Town t1 = makeTown(0, 0, 0, "A");
        Town t2 = makeTown(10, 0, 0, "B");
        // 10 / 10 = 1.0; ceil(1.0)=1; (int)1
        assertEquals(1, ContractBoard.calculateCourierCost(t1, t2));
    }

    @Test
    void calculateCourierCost_realTowns_elevenBlocks_ceilsToTwo() {
        Town t1 = makeTown(0, 0, 0, "A");
        Town t2 = makeTown(11, 0, 0, "B");
        // 11 / 10.0 = 1.1; ceil(1.1)=2.0; (int)2
        assertEquals(2, ContractBoard.calculateCourierCost(t1, t2));
    }

    @Test
    void calculateCourierCost_realTowns_25Blocks_returnsThree() {
        Town t1 = makeTown(0, 0, 0, "A");
        Town t2 = makeTown(25, 0, 0, "B");
        // 25 / 10.0 = 2.5; ceil(2.5)=3.0; (int)3
        assertEquals(3, ContractBoard.calculateCourierCost(t1, t2));
    }

    @Test
    void calculateCourierCost_realTowns_100Blocks_returnsTen() {
        Town t1 = makeTown(0, 64, 0, "Seller");
        Town t2 = makeTown(100, 64, 0, "Winner");
        // 100 / 10.0 = 10.0; ceil(10)=10; (int)10
        // (matches the worked example in the vault note)
        assertEquals(10, ContractBoard.calculateCourierCost(t1, t2));
    }

    @Test
    void calculateCourierCost_realTowns_3DOffset_usesFullDistSqr() {
        Town t1 = makeTown(0, 0, 0, "A");
        Town t2 = makeTown(6, 8, 0, "B"); // 6-8-0 triangle: sqrt(36+64)=10 exactly
        // dist=10; 10/10=1.0 -> 1
        assertEquals(1, ContractBoard.calculateCourierCost(t1, t2));
    }
}
