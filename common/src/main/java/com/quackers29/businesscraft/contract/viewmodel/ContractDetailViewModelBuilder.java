package com.quackers29.businesscraft.contract.viewmodel;

import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.util.BCTimeUtils;
import net.minecraft.server.level.ServerPlayer;

import java.text.DecimalFormat;
import java.util.*;

public class ContractDetailViewModelBuilder {

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.##");

    public static ContractDetailViewModel build(Contract contract, ServerPlayer player, long serverTime) {
        if (contract == null) {
            return null;
        }

        if (contract instanceof SellContract sc) {
            return buildSellContractDetail(sc, serverTime);
        }

        return buildGenericDetail(contract, serverTime);
    }

    private static ContractDetailViewModel buildSellContractDetail(SellContract sc, long serverTime) {

        UUID contractId = sc.getId();
        String resourceId = sc.getResourceId();
        long quantity = sc.getQuantity();
        String issuerTownName = sc.getIssuerTownName() != null ? sc.getIssuerTownName() : "Unknown";

        String timeRemainingDisplay = BCTimeUtils.formatTimeRemaining(sc.getExpiryTime(), serverTime);
        String createdDateDisplay = BCTimeUtils.formatDateTime(sc.getCreationTime());
        String expiresDateDisplay = BCTimeUtils.formatDateTime(sc.getExpiryTime());
        boolean isExpired = BCTimeUtils.isExpired(sc.getExpiryTime(), serverTime);

        float highestBid = sc.getHighestBid();
        String highestBidDisplay = highestBid > 0
                ? PRICE_FORMAT.format(highestBid) + " emeralds"
                : "No bids";

        String statusDisplay = calculateStatus(sc);

        String priceDisplay = PRICE_FORMAT.format(sc.getPricePerUnit()) + " emeralds/unit";

        String deliveryProgressDisplay = null;
        if (sc.isAuctionClosed() && !sc.isDelivered()) {
            long delivered = sc.getDeliveredAmount();
            long total = sc.getQuantity();
            deliveryProgressDisplay = delivered + "/" + total + " delivered";
        }

        String courierName = null;
        if (sc.isCourierAssigned()) {
            if (sc.isSnailMail()) {
                courierName = "Snail Mail";
            } else {
                // TODO: Look up player name from UUID
                courierName = "Courier (ID: " + sc.getCourierId().toString().substring(0, 8) + ")";
            }
        }

        String winningBidderName = sc.getWinningTownName();

        String tooltipText = buildTooltip(sc);
        
        String destinationTownName = sc.isAuctionClosed() ? winningBidderName : null;
        
        String courierRewardDisplay = PRICE_FORMAT.format(sc.getCourierReward()) + " ◎";
        
        String acceptedBidDisplay = sc.getAcceptedBid() > 0 
                ? PRICE_FORMAT.format(sc.getAcceptedBid()) + " ◎" 
                : null;

        boolean canBid = calculateCanBid(sc, serverTime);
        boolean canAcceptCourier = calculateCanAcceptCourier(sc);
        boolean isDelivered = sc.isDelivered();
        boolean isAuctionClosed = sc.isAuctionClosed();

        List<ContractDetailViewModel.BidDisplayInfo> bids = buildBidList(sc);

        return new ContractDetailViewModel(
                contractId,
                "sell",
                resourceId,
                (int) quantity,
                sc.getIssuerTownId(),
                issuerTownName,
                timeRemainingDisplay,
                highestBidDisplay,
                statusDisplay,
                priceDisplay,
                createdDateDisplay,
                expiresDateDisplay,
                deliveryProgressDisplay,
                courierName,
                winningBidderName,
                tooltipText,
                destinationTownName,
                courierRewardDisplay,
                acceptedBidDisplay,
                canBid,
                canAcceptCourier,
                isExpired,
                isDelivered,
                isAuctionClosed,
                bids
        );
    }

    private static List<ContractDetailViewModel.BidDisplayInfo> buildBidList(SellContract sc) {
        List<ContractDetailViewModel.BidDisplayInfo> result = new ArrayList<>();
        Map<UUID, Float> bids = sc.getBids();
        float highestBid = sc.getHighestBid();

        List<Map.Entry<UUID, Float>> sortedBids = new ArrayList<>(bids.entrySet());
        sortedBids.sort((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()));

        for (Map.Entry<UUID, Float> entry : sortedBids) {
            UUID bidderId = entry.getKey();
            float amount = entry.getValue();
            String bidderName = sc.getBidderName(bidderId);
            String amountDisplay = PRICE_FORMAT.format(amount) + " emeralds";
            boolean isHighest = Math.abs(amount - highestBid) < 0.001f;

            result.add(new ContractDetailViewModel.BidDisplayInfo(bidderName, amountDisplay, isHighest));
        }

        return result;
    }

    private static ContractDetailViewModel buildGenericDetail(Contract contract, long serverTime) {

        return new ContractDetailViewModel(
                contract.getId(),
                contract.getType(),
                "unknown",
                0,
                contract.getIssuerTownId(),
                contract.getIssuerTownName() != null ? contract.getIssuerTownName() : "Unknown",
                BCTimeUtils.formatTimeRemaining(contract.getExpiryTime(), serverTime),
                "N/A",
                "Unknown",
                "N/A",
                BCTimeUtils.formatDateTime(contract.getCreationTime()),
                BCTimeUtils.formatDateTime(contract.getExpiryTime()),
                null,
                null,
                null,
                "Contract ID: " + contract.getId(),
                null, // destinationTownName
                null, // courierRewardDisplay
                null, // acceptedBidDisplay
                false,
                false,
                BCTimeUtils.isExpired(contract.getExpiryTime(), serverTime),
                contract.isCompleted(),
                false,
                new ArrayList<>()
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
        return "Auction Open";
    }

    private static String buildTooltip(SellContract sc) {
        StringBuilder sb = new StringBuilder();
        sb.append(sc.getIssuerTownName()).append(" selling ");
        sb.append(sc.getQuantity()).append(" ").append(sc.getResourceId());
        sb.append("\nPrice per unit: ").append(PRICE_FORMAT.format(sc.getPricePerUnit())).append(" emeralds");
        sb.append("\nTotal value: ").append(PRICE_FORMAT.format(sc.getPricePerUnit() * sc.getQuantity())).append(" emeralds");
        if (sc.getHighestBid() > 0) {
            sb.append("\nHighest bid: ").append(PRICE_FORMAT.format(sc.getHighestBid())).append(" emeralds");
        }
        return sb.toString();
    }

    private static boolean calculateCanBid(SellContract sc, long serverTime) {
        if (sc.isAuctionClosed()) return false;
        if (BCTimeUtils.isExpired(sc.getExpiryTime(), serverTime)) return false;
        return true;
    }

    private static boolean calculateCanAcceptCourier(SellContract sc) {
        if (!sc.isAuctionClosed()) return false;
        if (sc.isCourierAssigned()) return false;
        if (sc.isDelivered()) return false;
        return true;
    }
}
