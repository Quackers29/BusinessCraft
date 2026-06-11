package com.quackers29.businesscraft.contract;

import com.quackers29.businesscraft.contract.viewmodel.ContractDetailViewModel;
import com.quackers29.businesscraft.contract.viewmodel.ContractDetailViewModelBuilder;
import com.quackers29.businesscraft.util.BCTimeUtils;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-020: Contract List and Detail ViewModels (Test + Docs Loop) — detail builder portion.
 *
 * Covers the single-contract detail building used by RequestContractDetailPacket:
 * - Sell-specific rich path (20+ fields, sorted bid list, progress, courier, tooltip, rewards)
 * - Generic fallback for non-Sell contracts (CourierContract here)
 * - Status, canBid, canAcceptCourier, bid-list sorting + isHighest tolerance (duplicated logic paths)
 * - Date/time display delegation and isExpired using BCTimeUtils with controlled serverTime
 *
 * Pure logic only. Uses BlockPos for CourierContract ctor (allowed per protocol). Timezone forced
 * to UTC. Hand-computed expects for bid ordering, isHighest tolerance, total value in tooltip, etc.
 *
 * Documentation: vault/Trade/Contracts/Contract List and Detail ViewModels.md
 */
class ContractDetailViewModelBuilderTest {

    private static final UUID ISSUER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID BIDDER1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BIDDER2 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID WINNER = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private String savedTimezone;

    @BeforeEach
    void setUp() {
        savedTimezone = BCTimeUtils.getTimezone();
        BCTimeUtils.setTimezone("UTC");
    }

    @AfterEach
    void tearDown() {
        BCTimeUtils.setTimezone(savedTimezone);
    }

    private SellContract sellBase(String res, long qty, float price, long durMs) {
        return new SellContract(ISSUER, "IssuerTown", durMs, res, qty, price);
    }

    private CourierContract courierGeneric() {
        return new CourierContract(ISSUER, "IssuerTown", new BlockPos(0, 64, 0), 3,
                60_000L, "gravel", 32, WINNER, "DestTown", 4.0f);
    }

    // --- generic fallback ---

    @Test
    void build_nullContract_returnsNull() {
        assertNull(ContractDetailViewModelBuilder.build(null, null, System.currentTimeMillis()));
    }

    @Test
    void build_nonSellContract_usesGenericFallback() {
        CourierContract cc = courierGeneric();
        long serverTime = cc.getCreationTime() + 10_000L;
        ContractDetailViewModel vm = ContractDetailViewModelBuilder.build(cc, null, serverTime);
        assertNotNull(vm);
        assertEquals("courier", vm.getContractType()); // from getType
        assertEquals("unknown", vm.getResourceId());
        assertEquals(0, vm.getQuantity());
        assertFalse(vm.canBid());
        assertFalse(vm.canAcceptCourier());
        assertFalse(vm.isAuctionClosed());
        assertEquals(0, vm.getBids().size());
        assertNotNull(vm.getTimeRemainingDisplay());
        assertNotNull(vm.getCreatedDateDisplay());
    }

    // --- sell detail fields and formatting ---

    @Test
    void buildSellDetail_basicFields_priceTooltipDeliveryProgress() {
        SellContract sc = sellBase("copper", 256, 1.25f, 180_000L);
        sc.addBid(BIDDER1, "TownB", 300f);
        sc.addBid(BIDDER2, "TownC", 340.75f);
        sc.setWinningTown(WINNER, "TownC");
        sc.setAcceptedBid(340.75f);
        sc.setCourierReward(12.0f);
        // simulate partial courier delivery
        sc.setDeliveredAmount(128);

        long serverTime = sc.getCreationTime() + 45_000L;
        ContractDetailViewModel vm = ContractDetailViewModelBuilder.build(sc, null, serverTime);

        assertEquals("sell", vm.getContractType());
        assertEquals("copper", vm.getResourceId());
        assertEquals(256, vm.getQuantity());
        assertEquals("1.25 emeralds/unit", vm.getPriceDisplay());
        assertEquals("340.75 emeralds", vm.getHighestBidDisplay());
        assertEquals("TownC", vm.getWinningBidderName());
        assertEquals("12 ◎", vm.getCourierRewardDisplay());
        assertEquals("340.75 ◎", vm.getAcceptedBidDisplay());
        assertEquals("128/256 delivered", vm.getDeliveryProgressDisplay());
        assertEquals("TownC", vm.getDestinationTownName());
        assertTrue(vm.isAuctionClosed());
        assertFalse(vm.isDelivered());
        assertTrue(vm.canAcceptCourier()); // still assignable
        // tooltip contains issuer, qty, price, total (1.25*256=320), highest
        String tip = vm.getTooltipText();
        assertTrue(tip.contains("IssuerTown selling 256 copper"));
        assertTrue(tip.contains("Price per unit: 1.25 emeralds"));
        assertTrue(tip.contains("Total value: 320 emeralds"));
        assertTrue(tip.contains("Highest bid: 340.75 emeralds"));
    }

    @Test
    void buildSellDetail_bidList_sortedDescendingWithIsHighestTolerance() {
        SellContract sc = sellBase("wheat", 100, 0.5f, 60_000L);
        sc.addBid(BIDDER1, "Low", 40f);
        sc.addBid(BIDDER2, "High", 55f);
        sc.addBid(WINNER, "Winner", 55.1f); // >0.001 away from 55 so only the max is marked highest (tolerance pinning)

        long now = sc.getCreationTime();
        ContractDetailViewModel vm = ContractDetailViewModelBuilder.build(sc, null, now);

        List<ContractDetailViewModel.BidDisplayInfo> bids = vm.getBids();
        assertEquals(3, bids.size());
        // sorted desc by amount: 55.1, 55, 40
        assertEquals("Winner", bids.get(0).bidderName());
        assertTrue(bids.get(0).isHighest());
        assertEquals("55.1 emeralds", bids.get(0).amountDisplay());

        assertEquals("High", bids.get(1).bidderName());
        assertFalse(bids.get(1).isHighest()); // 0.1 > 0.001 tolerance, not marked
        assertEquals("55 emeralds", bids.get(1).amountDisplay());

        assertEquals("Low", bids.get(2).bidderName());
        assertFalse(bids.get(2).isHighest());
    }

    @Test
    void buildSellDetail_courierSnailNameAndStatus() {
        SellContract sc = sellBase("logs", 64, 2f, 60_000L);
        sc.setWinningTown(WINNER, "WinTown");
        sc.setAcceptedBid(130f);
        sc.setCourierId(SellContract.SNAIL_MAIL_UUID);
        sc.setCourierAcceptedTime(sc.getCreationTime());

        ContractDetailViewModel vm = ContractDetailViewModelBuilder.build(sc, null, sc.getCreationTime());
        assertEquals("Snail Mail", vm.getCourierName());
        assertEquals("Snail Mail", vm.getStatusDisplay());
    }

    @Test
    void buildSellDetail_canBidFalseWhenClosedOrExpired() {
        SellContract sc = sellBase("gold", 10, 10f, 60_000L);
        long future = sc.getExpiryTime() + 1000;
        // still open auction but snapshot after expiry
        ContractDetailViewModel vm = ContractDetailViewModelBuilder.build(sc, null, future);
        assertTrue(vm.isExpired());
        assertFalse(vm.canBid());
        assertFalse(vm.isAuctionClosed());

        // close it
        sc.setWinningTown(BIDDER1, "B1");
        ContractDetailViewModel vm2 = ContractDetailViewModelBuilder.build(sc, null, sc.getCreationTime());
        assertFalse(vm2.canBid());
        assertTrue(vm2.isAuctionClosed());
    }

    @Test
    void buildSellDetail_canAcceptCourier_requiresClosedNotAssignedNotDelivered() {
        SellContract base = sellBase("x", 5, 1f, 60_000L);
        base.setWinningTown(BIDDER1, "W");
        assertTrue(ContractDetailViewModelBuilder.build(base, null, base.getCreationTime()).canAcceptCourier());

        SellContract assigned = sellBase("x", 5, 1f, 60_000L);
        assigned.setWinningTown(BIDDER1, "W");
        assigned.setCourierId(BIDDER2);
        assertFalse(ContractDetailViewModelBuilder.build(assigned, null, assigned.getCreationTime()).canAcceptCourier());

        SellContract done = sellBase("x", 5, 1f, 60_000L);
        done.setWinningTown(BIDDER1, "W");
        done.setDelivered(true);
        assertFalse(ContractDetailViewModelBuilder.build(done, null, done.getCreationTime()).canAcceptCourier());
    }

    @Test
    void buildSellDetail_statusOpenVsAuctionOpenStringDifference() {
        // Documents the duplication quirk between the two builders
        SellContract sc = sellBase("y", 1, 1f, 60_000L);
        ContractDetailViewModel detail = ContractDetailViewModelBuilder.build(sc, null, sc.getCreationTime());
        // Detail uses "Auction Open"
        assertEquals("Auction Open", detail.getStatusDisplay());
    }

    @Test
    void buildSellDetail_dateDisplays_useBCTimeUtils_andIsExpired() {
        SellContract sc = sellBase("obsidian", 8, 99f, 120_000L);
        long serverTime = sc.getCreationTime() + 30_000L;
        ContractDetailViewModel vm = ContractDetailViewModelBuilder.build(sc, null, serverTime);
        // created and expires should be non-null MM/dd HH:mm style strings (UTC)
        assertNotNull(vm.getCreatedDateDisplay());
        assertNotNull(vm.getExpiresDateDisplay());
        assertFalse(vm.isExpired());
        // move serverTime past
        ContractDetailViewModel vmPast = ContractDetailViewModelBuilder.build(sc, null, sc.getExpiryTime() + 1);
        assertTrue(vmPast.isExpired());
    }

    @Test
    void buildSellDetail_noBids_highestIsNoBids_andEmptyBidList() {
        SellContract sc = sellBase("dirt", 999, 0.01f, 60_000L);
        ContractDetailViewModel vm = ContractDetailViewModelBuilder.build(sc, null, sc.getCreationTime());
        assertEquals("No bids", vm.getHighestBidDisplay());
        assertEquals(0, vm.getBids().size());
        assertNull(vm.getAcceptedBidDisplay());
    }
}
