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

public class ContractSummaryViewModelBuilder {

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.##");
    private static final int DEFAULT_PAGE_SIZE = 20;

    public enum Tab {
        AUCTION,
        ACTIVE,
        HISTORY
    }

    public record ContractListResult(
            List<ContractSummaryViewModel> contracts,
            int page,
            int pageSize,
            int totalCount,
            boolean hasMore
    ) {}

    public static ContractListResult build(
            List<Contract> allContracts,
            Tab tab,
            int page,
            int pageSize,
            ServerPlayer player,
            long serverTime) {

        if (pageSize <= 0) pageSize = DEFAULT_PAGE_SIZE;
        if (page < 0) page = 0;

        List<Contract> filtered = filterByTab(allContracts, tab);

        sortContracts(filtered, tab);

        int totalCount = filtered.size();
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalCount);
        boolean hasMore = endIndex < totalCount;

        List<Contract> pageContracts = (startIndex < totalCount)
                ? filtered.subList(startIndex, endIndex)
                : List.of();

        List<ContractSummaryViewModel> summaries = new ArrayList<>();
        for (Contract contract : pageContracts) {
            summaries.add(buildSummary(contract, serverTime));
        }

        return new ContractListResult(summaries, page, pageSize, totalCount, hasMore);
    }

    private static List<Contract> filterByTab(List<Contract> contracts, Tab tab) {
        List<Contract> result = new ArrayList<>();

        for (Contract contract : contracts) {
            if (contract instanceof SellContract sc) {
                boolean auctionClosed = sc.isAuctionClosed();
                boolean delivered = sc.isDelivered();

                switch (tab) {
                    case AUCTION -> {
                        if (!auctionClosed) {
                            result.add(contract);
                        }
                    }
                    case ACTIVE -> {
                        if (auctionClosed && !delivered) {
                            result.add(contract);
                        }
                    }
                    case HISTORY -> {
                        if (delivered) {
                            result.add(contract);
                        }
                    }
                }
            }
        }

        return result;
    }

    private static void sortContracts(List<Contract> contracts, Tab tab) {
        switch (tab) {
            case AUCTION, ACTIVE -> {
                contracts.sort(Comparator.comparingLong(Contract::getExpiryTime));
            }
            case HISTORY -> {
                contracts.sort(Comparator.comparingLong(Contract::getCreationTime).reversed());
            }
        }
    }

    private static ContractSummaryViewModel buildSummary(Contract contract, long serverTime) {
        if (contract instanceof SellContract sc) {
            return buildSellContractSummary(sc, serverTime);
        }

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

    private static ContractSummaryViewModel buildSellContractSummary(
            SellContract sc, long serverTime) {

        UUID contractId = sc.getId();
        String resourceId = sc.getResourceId();
        long quantity = sc.getQuantity();
        UUID issuerTownId = sc.getIssuerTownId();
        String issuerTownName = sc.getIssuerTownName();

        String timeRemainingDisplay = BCTimeUtils.formatTimeRemaining(sc.getExpiryTime(), serverTime);
        boolean isExpired = sc.getExpiryTime() < serverTime;

        float highestBid = sc.getHighestBid();
        String highestBidDisplay = highestBid > 0
                ? PRICE_FORMAT.format(highestBid) + " emeralds"
                : "No bids";

        String statusDisplay = calculateStatus(sc);

        String priceDisplay = PRICE_FORMAT.format(sc.getPricePerUnit()) + " emeralds/unit";

        boolean canBid = calculateCanBid(sc, serverTime);
        boolean canAcceptCourier = calculateCanAcceptCourier(sc);
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

    private static String calculateStatus(SellContract sc) {
        if (sc.isDelivered()) {
            return "Delivered";
        }
        if (sc.isAuctionClosed()) {
            if (sc.isCourierAssigned()) {
                if (sc.isSnailMail()) {
                    return "Snail Mail";
                }
                long delivered = sc.getDeliveredAmount();
                long total = sc.getQuantity();
                if (delivered > 0) {
                    return "In Transit (" + delivered + "/" + total + ")";
                }
                return "Courier Assigned";
            }
            return "Awaiting Courier";
        }
        return "Auction";
    }

    private static boolean calculateCanBid(SellContract sc, long serverTime) {
        if (sc.isAuctionClosed()) return false;

        if (sc.getExpiryTime() < serverTime) return false;

        return true;
    }

    private static boolean calculateCanAcceptCourier(SellContract sc) {
        if (!sc.isAuctionClosed()) return false;

        if (sc.isCourierAssigned()) return false;

        if (sc.isDelivered()) return false;

        return true;
    }
}
