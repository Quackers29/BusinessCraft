package com.quackers29.businesscraft.contract.viewmodel;

import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.util.BCTimeUtils;
import net.minecraft.server.level.ServerPlayer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Server-side builder for ContractSummaryViewModel.
 * Handles filtering, sorting, and pagination of contracts.
 * All business logic for display strings is calculated here.
 */
public class ContractSummaryViewModelBuilder {

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.##");
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Tab types for contract filtering.
     */
    public enum Tab {
        AUCTION,  // Open auctions (no winner yet)
        ACTIVE,   // In progress (winner assigned, not delivered)
        HISTORY   // Completed (delivered)
    }

    /**
     * Result of building a contract list with pagination metadata.
     */
    public record ContractListResult(
            List<ContractSummaryViewModel> contracts,
            int page,
            int pageSize,
            int totalCount,
            boolean hasMore
    ) {}

    /**
     * Build a paginated list of contract summaries for the specified tab.
     *
     * @param allContracts All contracts from the server
     * @param tab Which tab to filter for
     * @param page Page number (0-indexed)
     * @param pageSize Number of items per page
     * @param player The player requesting the data (for canBid/canAcceptCourier checks)
     * @param serverTime Current server time in millis
     * @return Paginated result with summaries and metadata
     */
    public static ContractListResult build(
            List<Contract> allContracts,
            Tab tab,
            int page,
            int pageSize,
            ServerPlayer player,
            long serverTime) {

        if (pageSize <= 0) pageSize = DEFAULT_PAGE_SIZE;
        if (page < 0) page = 0;

        // Filter contracts by tab
        List<Contract> filtered = filterByTab(allContracts, tab, serverTime);

        // Sort contracts
        sortContracts(filtered, tab);

        // Calculate pagination
        int totalCount = filtered.size();
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalCount);
        boolean hasMore = endIndex < totalCount;

        // Extract page slice
        List<Contract> pageContracts = (startIndex < totalCount)
                ? filtered.subList(startIndex, endIndex)
                : List.of();

        // Build view-models
        List<ContractSummaryViewModel> summaries = new ArrayList<>();
        for (Contract contract : pageContracts) {
            summaries.add(buildSummary(contract, player, serverTime));
        }

        return new ContractListResult(summaries, page, pageSize, totalCount, hasMore);
    }

    /**
     * Filter contracts by tab type.
     */
    private static List<Contract> filterByTab(List<Contract> contracts, Tab tab, long serverTime) {
        List<Contract> result = new ArrayList<>();

        for (Contract contract : contracts) {
            if (contract instanceof SellContract sc) {
                boolean auctionClosed = sc.isAuctionClosed();
                boolean delivered = sc.isDelivered();

                switch (tab) {
                    case AUCTION -> {
                        // Open auctions: no winner yet
                        if (!auctionClosed) {
                            result.add(contract);
                        }
                    }
                    case ACTIVE -> {
                        // Active: has winner but not delivered
                        if (auctionClosed && !delivered) {
                            result.add(contract);
                        }
                    }
                    case HISTORY -> {
                        // History: delivered
                        if (delivered) {
                            result.add(contract);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Sort contracts based on tab type.
     */
    private static void sortContracts(List<Contract> contracts, Tab tab) {
        switch (tab) {
            case AUCTION, ACTIVE -> {
                // Sort by expiry time (soonest first)
                contracts.sort(Comparator.comparingLong(Contract::getExpiryTime));
            }
            case HISTORY -> {
                // Sort by creation time (most recent first)
                contracts.sort(Comparator.comparingLong(Contract::getCreationTime).reversed());
            }
        }
    }

    /**
     * Build a single contract summary view-model.
     */
    private static ContractSummaryViewModel buildSummary(Contract contract, ServerPlayer player, long serverTime) {
        if (contract instanceof SellContract sc) {
            return buildSellContractSummary(sc, player, serverTime);
        }

        // Fallback for unknown contract types
        return new ContractSummaryViewModel(
                contract.getId(),
                contract.getType(),
                "unknown",
                0,
                contract.getIssuerTownId(),
                contract.getIssuerTownName(),
                BCTimeUtils.formatTimeRemaining(contract.getExpiryTime(), serverTime),
                "N/A",
                "Unknown",
                "N/A",
                false,
                false,
                contract.getExpiryTime() < serverTime,
                contract.isCompleted()
        );
    }

    /**
     * Build summary for a SellContract.
     */
    private static ContractSummaryViewModel buildSellContractSummary(
            SellContract sc, ServerPlayer player, long serverTime) {

        UUID contractId = sc.getId();
        String resourceId = sc.getResourceId();
        int quantity = sc.getQuantity();
        UUID issuerTownId = sc.getIssuerTownId();
        String issuerTownName = sc.getIssuerTownName();

        // Time display
        String timeRemainingDisplay = BCTimeUtils.formatTimeRemaining(sc.getExpiryTime(), serverTime);
        boolean isExpired = sc.getExpiryTime() < serverTime;

        // Bid display
        float highestBid = sc.getHighestBid();
        String highestBidDisplay = highestBid > 0
                ? PRICE_FORMAT.format(highestBid) + " emeralds"
                : "No bids";

        // Status display
        String statusDisplay = calculateStatus(sc);

        // Price display
        String priceDisplay = PRICE_FORMAT.format(sc.getPricePerUnit()) + " emeralds/unit";

        // Action flags (server-calculated)
        boolean canBid = calculateCanBid(sc, player, serverTime);
        boolean canAcceptCourier = calculateCanAcceptCourier(sc, player);
        boolean isDelivered = sc.isDelivered();

        return new ContractSummaryViewModel(
                contractId,
                "sell",
                resourceId,
                quantity,
                issuerTownId,
                issuerTownName,
                timeRemainingDisplay,
                highestBidDisplay,
                statusDisplay,
                priceDisplay,
                canBid,
                canAcceptCourier,
                isExpired,
                isDelivered
        );
    }

    /**
     * Calculate the status display string for a contract.
     */
    private static String calculateStatus(SellContract sc) {
        if (sc.isDelivered()) {
            return "Delivered";
        }
        if (sc.isAuctionClosed()) {
            if (sc.isCourierAssigned()) {
                if (sc.isSnailMail()) {
                    return "Snail Mail";
                }
                int delivered = sc.getDeliveredAmount();
                int total = sc.getQuantity();
                if (delivered > 0) {
                    return "In Transit (" + delivered + "/" + total + ")";
                }
                return "Courier Assigned";
            }
            return "Awaiting Courier";
        }
        return "Auction";
    }

    /**
     * Calculate whether the player can bid on this contract.
     */
    private static boolean calculateCanBid(SellContract sc, ServerPlayer player, long serverTime) {
        // Cannot bid if auction is closed
        if (sc.isAuctionClosed()) return false;

        // Cannot bid if expired
        if (sc.getExpiryTime() < serverTime) return false;

        // Cannot bid on own contract
        // TODO: Check if player owns the issuing town
        // For now, allow bidding (the detailed check happens on the actual bid action)

        return true;
    }

    /**
     * Calculate whether the player can accept courier duty for this contract.
     */
    private static boolean calculateCanAcceptCourier(SellContract sc, ServerPlayer player) {
        // Must have a winner
        if (!sc.isAuctionClosed()) return false;

        // Must not have a courier yet
        if (sc.isCourierAssigned()) return false;

        // Must not be delivered
        if (sc.isDelivered()) return false;

        // TODO: Check if player is near the seller town
        // For now, allow acceptance (the detailed check happens on the actual accept action)

        return true;
    }
}
