package com.quackers29.businesscraft.contract;

import com.quackers29.businesscraft.contract.viewmodel.ContractSummaryViewModel;
import com.quackers29.businesscraft.contract.viewmodel.ContractSummaryViewModelBuilder;
import com.quackers29.businesscraft.contract.viewmodel.ContractSummaryViewModelBuilder.ContractListResult;
import com.quackers29.businesscraft.contract.viewmodel.ContractSummaryViewModelBuilder.Tab;
import com.quackers29.businesscraft.util.BCTimeUtils;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-020: Contract List and Detail ViewModels (Test + Docs Loop) — summary builder portion.
 *
 * Covers the list-building logic used by RequestContractListPacket:
 * - Tab filtering (only SellContract instances; AUCTION/ACTIVE/HISTORY state rules)
 * - Sorting (expiry asc for open tabs; creation desc for history)
 * - Paging math (clamping, slicing, hasMore, empty beyond-end pages)
 * - Per-contract summary construction: price formatting, highest-bid text, status strings,
 *   time remaining via BCTimeUtils, canBid / canAcceptCourier predicates
 * - Generic fallback path exercised via non-Sell contracts (they are filtered out of tab lists)
 *
 * All tests are pure logic. CompoundTag/BlockPos/UUID used (allowed). BCTimeUtils timezone
 * forced to UTC for deterministic date strings where exercised. No ItemStack or registries.
 *
 * Documentation: vault/Trade/Contracts/Contract List and Detail ViewModels.md
 */
class ContractSummaryViewModelBuilderTest {

    private static final UUID ISSUER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID BIDDER1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BIDDER2 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private String savedTimezone;

    @BeforeEach
    void setUp() {
        savedTimezone = BCTimeUtils.getTimezone();
        BCTimeUtils.setTimezone("UTC"); // deterministic formatting (matches BCTimeUtilsTest pattern)
    }

    @AfterEach
    void tearDown() {
        BCTimeUtils.setTimezone(savedTimezone);
    }

    // --- helpers to construct realistic SellContracts for tests ---

    private SellContract sellOpen(String res, long qty, float price, long durationMs) {
        return new SellContract(ISSUER, "TownIssuer", durationMs, res, qty, price);
    }

    private SellContract sellClosed(String res, long qty, float price, long durationMs, float winningBid) {
        SellContract sc = sellOpen(res, qty, price, durationMs);
        sc.setWinningTown(BIDDER1, "TownWinner");
        sc.setAcceptedBid(winningBid);
        return sc;
    }

    private SellContract sellDelivered(String res, long qty, float price, long durationMs) {
        SellContract sc = sellClosed(res, qty, price, durationMs, 999f);
        sc.setDelivered(true);
        return sc;
    }

    private CourierContract courierForGeneric() {
        // Non-sell contract to exercise generic fallback (filtered from tab results)
        return new CourierContract(ISSUER, "CourierIssuer", new BlockPos(10, 64, 10), 5,
                60_000L, "stone", 64, BIDDER1, "DestTown", 5.0f);
    }

    // --- filter + tab coverage ---

    @Test
    void build_auctionTab_includesOnlyOpenSellContracts() {
        List<Contract> contracts = new ArrayList<>();
        SellContract open = sellOpen("wood", 64, 1.25f, 120_000L);
        SellContract closed = sellClosed("iron", 32, 2.0f, 120_000L, 80f);
        SellContract delivered = sellDelivered("food", 16, 0.5f, 120_000L);
        contracts.add(open);
        contracts.add(closed);
        contracts.add(delivered);
        contracts.add(courierForGeneric());

        ContractListResult result = ContractSummaryViewModelBuilder.build(contracts, Tab.AUCTION, 0, 20, null, System.currentTimeMillis());

        assertEquals(1, result.contracts().size());
        assertEquals("wood", result.contracts().get(0).getResourceId());
        assertEquals(1, result.totalCount());
        assertFalse(result.hasMore());
    }

    @Test
    void build_activeTab_includesOnlyClosedNotDelivered() {
        List<Contract> contracts = new ArrayList<>();
        SellContract open = sellOpen("wood", 64, 1.25f, 120_000L);
        SellContract closed = sellClosed("iron", 32, 2.0f, 120_000L, 80f);
        SellContract delivered = sellDelivered("food", 16, 0.5f, 120_000L);
        contracts.add(open);
        contracts.add(closed);
        contracts.add(delivered);

        ContractListResult result = ContractSummaryViewModelBuilder.build(contracts, Tab.ACTIVE, 0, 20, null, System.currentTimeMillis());

        assertEquals(1, result.contracts().size());
        assertEquals("iron", result.contracts().get(0).getResourceId());
        assertEquals(1, result.totalCount());
    }

    @Test
    void build_historyTab_includesOnlyDelivered() {
        List<Contract> contracts = new ArrayList<>();
        SellContract open = sellOpen("wood", 64, 1.25f, 120_000L);
        SellContract closed = sellClosed("iron", 32, 2.0f, 120_000L, 80f);
        SellContract delivered = sellDelivered("food", 16, 0.5f, 120_000L);
        contracts.add(open);
        contracts.add(closed);
        contracts.add(delivered);

        ContractListResult result = ContractSummaryViewModelBuilder.build(contracts, Tab.HISTORY, 0, 20, null, System.currentTimeMillis());

        assertEquals(1, result.contracts().size());
        assertEquals("food", result.contracts().get(0).getResourceId());
        assertEquals(1, result.totalCount());
    }

    @Test
    void build_nonSellContracts_areIgnoredForAllTabs() {
        List<Contract> contracts = List.of(courierForGeneric(), courierForGeneric());
        ContractListResult r1 = ContractSummaryViewModelBuilder.build(contracts, Tab.AUCTION, 0, 20, null, System.currentTimeMillis());
        ContractListResult r2 = ContractSummaryViewModelBuilder.build(contracts, Tab.ACTIVE, 0, 20, null, System.currentTimeMillis());
        ContractListResult r3 = ContractSummaryViewModelBuilder.build(contracts, Tab.HISTORY, 0, 20, null, System.currentTimeMillis());
        assertEquals(0, r1.totalCount());
        assertEquals(0, r2.totalCount());
        assertEquals(0, r3.totalCount());
    }

    // --- sort rules ---

    @Test
    void build_auctionTab_sortsByExpiryAscending() {
        // Create two open contracts; force later expiry on first constructed by using different durations
        SellContract soon = sellOpen("a", 10, 1f, 10_000L);
        SellContract later = sellOpen("b", 10, 1f, 100_000L);
        List<Contract> contracts = List.of(later, soon); // insertion order opposite of desired

        ContractListResult result = ContractSummaryViewModelBuilder.build(contracts, Tab.AUCTION, 0, 20, null, System.currentTimeMillis());

        assertEquals(2, result.totalCount());
        assertEquals("a", result.contracts().get(0).getResourceId()); // sooner expiry first
        assertEquals("b", result.contracts().get(1).getResourceId());
    }

    @Test
    void build_historyTab_sortsByCreationDescending() {
        // Must be delivered to pass HISTORY filter
        SellContract first = sellDelivered("old", 10, 1f, 60_000L);
        SellContract second = sellDelivered("new", 10, 1f, 60_000L);
        // Force creation times via protected field (same package, allowed for test data)
        second.creationTime = first.creationTime + 5000;
        second.expiryTime = second.creationTime + 60_000L;
        List<Contract> contracts = List.of(first, second);

        ContractListResult result = ContractSummaryViewModelBuilder.build(contracts, Tab.HISTORY, 0, 20, null, System.currentTimeMillis());

        assertEquals(2, result.totalCount());
        assertEquals("new", result.contracts().get(0).getResourceId()); // newer creation first
        assertEquals("old", result.contracts().get(1).getResourceId());
    }

    // --- paging math (hand-computed) ---

    @Test
    void build_paging_page0Size2Of5_hasMoreTrue() {
        // 5 open contracts
        List<Contract> contracts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            contracts.add(sellOpen("r" + i, 10, 1f, 60_000L));
        }
        // page=0, size=2 -> indices [0,2), total=5, hasMore=true
        ContractListResult result = ContractSummaryViewModelBuilder.build(contracts, Tab.AUCTION, 0, 2, null, System.currentTimeMillis());
        assertEquals(2, result.contracts().size());
        assertEquals(0, result.page());
        assertEquals(2, result.pageSize());
        assertEquals(5, result.totalCount());
        assertTrue(result.hasMore());
    }

    @Test
    void build_paging_page2Size2Of5_returnsLastOne_hasMoreFalse() {
        List<Contract> contracts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            contracts.add(sellOpen("r" + i, 10, 1f, 60_000L));
        }
        // start=4, end=5 (min(4+2,5)), hasMore=false
        ContractListResult result = ContractSummaryViewModelBuilder.build(contracts, Tab.AUCTION, 2, 2, null, System.currentTimeMillis());
        assertEquals(1, result.contracts().size());
        assertEquals(5, result.totalCount());
        assertFalse(result.hasMore());
    }

    @Test
    void build_paging_pageBeyondEnd_returnsEmpty_hasMoreFalse() {
        List<Contract> contracts = List.of(sellOpen("only", 10, 1f, 60_000L));
        ContractListResult result = ContractSummaryViewModelBuilder.build(contracts, Tab.AUCTION, 5, 10, null, System.currentTimeMillis());
        assertEquals(0, result.contracts().size());
        assertEquals(1, result.totalCount());
        assertFalse(result.hasMore());
    }

    @Test
    void build_pageSizeZeroOrNegative_clampsToDefault20() {
        List<Contract> contracts = new ArrayList<>();
        for (int i = 0; i < 25; i++) contracts.add(sellOpen("r" + i, 1, 1f, 60_000L));
        ContractListResult r1 = ContractSummaryViewModelBuilder.build(contracts, Tab.AUCTION, 0, 0, null, System.currentTimeMillis());
        ContractListResult r2 = ContractSummaryViewModelBuilder.build(contracts, Tab.AUCTION, 0, -5, null, System.currentTimeMillis());
        assertEquals(20, r1.contracts().size());
        assertEquals(20, r2.contracts().size());
    }

    // --- summary field formatting and flags (hand-computed where arithmetic appears) ---

    @Test
    void buildSellSummary_noBids_showsNoBidsAndCanBidTrueIfOpen() {
        SellContract sc = sellOpen("coal", 128, 0.75f, 180_000L);
        long serverTime = sc.getCreationTime() + 10_000L; // still open
        ContractListResult result = ContractSummaryViewModelBuilder.build(List.of(sc), Tab.AUCTION, 0, 20, null, serverTime);
        ContractSummaryViewModel vm = result.contracts().get(0);
        assertEquals("No bids", vm.getHighestBidDisplay());
        assertTrue(vm.canBid());
        assertFalse(vm.canAcceptCourier());
        assertEquals("0.75 emeralds/unit", vm.getPriceDisplay());
        assertEquals("coal", vm.getResourceId());
        assertEquals(128, vm.getQuantity());
    }

    @Test
    void buildSellSummary_withHighestBid_formatsAndDisablesBidOnceClosed() {
        SellContract sc = sellOpen("iron", 50, 3.0f, 60_000L);
        sc.addBid(BIDDER1, "BidderOne", 175f);
        sc.addBid(BIDDER2, "BidderTwo", 190.5f); // higher wins
        sc.setWinningTown(BIDDER2, "BidderTwo");
        sc.setAcceptedBid(190.5f);
        long serverTime = sc.getCreationTime() + 70_000L; // after close
        ContractListResult result = ContractSummaryViewModelBuilder.build(List.of(sc), Tab.ACTIVE, 0, 20, null, serverTime);
        ContractSummaryViewModel vm = result.contracts().get(0);
        assertEquals("190.5 emeralds", vm.getHighestBidDisplay());
        assertFalse(vm.canBid()); // closed
        assertTrue(vm.canAcceptCourier()); // closed + no courier yet + !delivered
        assertEquals("3 emeralds/unit", vm.getPriceDisplay()); // #,##0.## drops .0
    }

    @Test
    void buildSellSummary_statusStrings_coverAllBranches() {
        long now = System.currentTimeMillis();

        SellContract del = sellDelivered("x", 10, 1f, 60_000L);
        assertEquals("Delivered", buildOneSummary(del, now, Tab.HISTORY).getStatusDisplay());

        SellContract snail = sellClosed("x", 10, 1f, 60_000L, 50f);
        snail.setCourierId(SellContract.SNAIL_MAIL_UUID);
        snail.setCourierAcceptedTime(now);
        assertEquals("Snail Mail", buildOneSummary(snail, now, Tab.ACTIVE).getStatusDisplay());

        SellContract transit = sellClosed("x", 100, 1f, 60_000L, 50f);
        transit.setCourierId(BIDDER1);
        transit.setCourierAcceptedTime(now);
        transit.setDeliveredAmount(40);
        assertEquals("In Transit (40/100)", buildOneSummary(transit, now, Tab.ACTIVE).getStatusDisplay());

        SellContract assigned = sellClosed("x", 100, 1f, 60_000L, 50f);
        assigned.setCourierId(BIDDER1);
        assigned.setCourierAcceptedTime(now);
        assertEquals("Courier Assigned", buildOneSummary(assigned, now, Tab.ACTIVE).getStatusDisplay());

        SellContract awaiting = sellClosed("x", 10, 1f, 60_000L, 50f);
        assertEquals("Awaiting Courier", buildOneSummary(awaiting, now, Tab.ACTIVE).getStatusDisplay());

        SellContract open = sellOpen("x", 10, 1f, 60_000L);
        assertEquals("Auction", buildOneSummary(open, now, Tab.AUCTION).getStatusDisplay());
    }

    private ContractSummaryViewModel buildOneSummary(SellContract sc, long serverTime, Tab tab) {
        return ContractSummaryViewModelBuilder.build(List.of(sc), tab, 0, 20, null, serverTime).contracts().get(0);
    }

    @Test
    void buildSellSummary_timeRemaining_usesServerTimeSnapshot_notLiveClock() {
        SellContract sc = sellOpen("stone", 200, 1f, 120_000L);
        long creation = sc.getCreationTime();
        long serverTime = creation + 30_000L; // 90s remaining -> 1m 30s
        ContractSummaryViewModel vm = buildOneSummary(sc, serverTime, Tab.AUCTION);
        // 120s - 30s = 90s remaining -> formatDuration yields "1m 30s"
        assertEquals("1m 30s", vm.getTimeRemainingDisplay());
        assertFalse(vm.isExpired());
    }

    @Test
    void buildSellSummary_expiredAtSnapshot_marksExpiredAndDisablesBid() {
        SellContract sc = sellOpen("stone", 200, 1f, 60_000L);
        long past = sc.getExpiryTime() + 5000;
        ContractSummaryViewModel vm = buildOneSummary(sc, past, Tab.AUCTION);
        assertEquals("Expired", vm.getTimeRemainingDisplay());
        assertTrue(vm.isExpired());
        assertFalse(vm.canBid());
    }

    @Test
    void build_emptyList_totalZero_hasMoreFalse() {
        ContractListResult result = ContractSummaryViewModelBuilder.build(List.of(), Tab.AUCTION, 0, 20, null, System.currentTimeMillis());
        assertEquals(0, result.totalCount());
        assertEquals(0, result.contracts().size());
        assertFalse(result.hasMore());
    }
}
