package com.quackers29.businesscraft.contract.viewmodel;

import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.SellContract;
import net.minecraft.server.level.ServerPlayer;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Server-side builder for ContractDetailViewModel.
 * Builds full contract details including bid list.
 */
public class ContractDetailViewModelBuilder {

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.##");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm");

    /**
     * Build a full detail view-model for a single contract.
     *
     * @param contract The contract to build details for
     * @param player The player requesting the data
     * @param serverTime Current server time in millis
     * @return Full detail view-model, or null if contract is null
     */
    public static ContractDetailViewModel build(Contract contract, ServerPlayer player, long serverTime) {
        if (contract == null) {
            return null;
        }

        if (contract instanceof SellContract sc) {
            return buildSellContractDetail(sc, player, serverTime);
        }

        // Fallback for unknown contract types
        return buildGenericDetail(contract, player, serverTime);
    }

    /**
     * Build detail for a SellContract.
     */
    private static ContractDetailViewModel buildSellContractDetail(
            SellContract sc, ServerPlayer player, long serverTime) {

        UUID contractId = sc.getId();
        String resourceId = sc.getResourceId();
        int quantity = sc.getQuantity();
        String issuerTownName = sc.getIssuerTownName() != null ? sc.getIssuerTownName() : "Unknown";

        // Time displays
        String timeRemainingDisplay = formatTimeRemaining(sc.getExpiryTime(), serverTime);
        String createdDateDisplay = DATE_FORMAT.format(new Date(sc.getCreationTime()));
        String expiresDateDisplay = DATE_FORMAT.format(new Date(sc.getExpiryTime()));
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

        // Delivery progress
        String deliveryProgressDisplay = null;
        if (sc.isAuctionClosed() && !sc.isDelivered()) {
            int delivered = sc.getDeliveredAmount();
            int total = sc.getQuantity();
            deliveryProgressDisplay = delivered + "/" + total + " delivered";
        }

        // Courier name
        String courierName = null;
        if (sc.isCourierAssigned()) {
            if (sc.isSnailMail()) {
                courierName = "Snail Mail";
            } else {
                // TODO: Look up player name from UUID
                courierName = "Courier (ID: " + sc.getCourierId().toString().substring(0, 8) + ")";
            }
        }

        // Winning bidder name
        String winningBidderName = sc.getWinningTownName();

        // Tooltip
        String tooltipText = buildTooltip(sc);
        
        // Destination town name (for courier contracts - SellContract destination is the winning bidder town)
        String destinationTownName = sc.isAuctionClosed() ? winningBidderName : null;
        
        // Courier reward display
        String courierRewardDisplay = PRICE_FORMAT.format(sc.getCourierReward()) + " ◎";
        
        // Accepted bid display (final price)
        String acceptedBidDisplay = sc.getAcceptedBid() > 0 
                ? PRICE_FORMAT.format(sc.getAcceptedBid()) + " ◎" 
                : null;

        // Action flags
        boolean canBid = calculateCanBid(sc, player, serverTime);
        boolean canAcceptCourier = calculateCanAcceptCourier(sc, player);
        boolean isDelivered = sc.isDelivered();
        boolean isAuctionClosed = sc.isAuctionClosed();

        // Build bid list (sorted by amount, highest first)
        List<ContractDetailViewModel.BidDisplayInfo> bids = buildBidList(sc);

        return new ContractDetailViewModel(
                contractId,
                "sell",
                resourceId,
                quantity,
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

    /**
     * Build bid list from contract.
     */
    private static List<ContractDetailViewModel.BidDisplayInfo> buildBidList(SellContract sc) {
        List<ContractDetailViewModel.BidDisplayInfo> result = new ArrayList<>();
        Map<UUID, Float> bids = sc.getBids();
        float highestBid = sc.getHighestBid();

        // Sort by bid amount (highest first)
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

    /**
     * Build generic detail for unknown contract types.
     */
    private static ContractDetailViewModel buildGenericDetail(
            Contract contract, ServerPlayer player, long serverTime) {

        return new ContractDetailViewModel(
                contract.getId(),
                contract.getType(),
                "unknown",
                0,
                contract.getIssuerTownId(),
                contract.getIssuerTownName() != null ? contract.getIssuerTownName() : "Unknown",
                formatTimeRemaining(contract.getExpiryTime(), serverTime),
                "N/A",
                "Unknown",
                "N/A",
                DATE_FORMAT.format(new Date(contract.getCreationTime())),
                DATE_FORMAT.format(new Date(contract.getExpiryTime())),
                null,
                null,
                null,
                "Contract ID: " + contract.getId(),
                null, // destinationTownName
                null, // courierRewardDisplay
                null, // acceptedBidDisplay
                false,
                false,
                contract.getExpiryTime() < serverTime,
                contract.isCompleted(),
                false,
                new ArrayList<>()
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
        return "Auction Open";
    }

    /**
     * Build tooltip text.
     */
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

    /**
     * Calculate whether the player can bid on this contract.
     */
    private static boolean calculateCanBid(SellContract sc, ServerPlayer player, long serverTime) {
        if (sc.isAuctionClosed()) return false;
        if (sc.getExpiryTime() < serverTime) return false;
        return true;
    }

    /**
     * Calculate whether the player can accept courier duty for this contract.
     */
    private static boolean calculateCanAcceptCourier(SellContract sc, ServerPlayer player) {
        if (!sc.isAuctionClosed()) return false;
        if (sc.isCourierAssigned()) return false;
        if (sc.isDelivered()) return false;
        return true;
    }

    /**
     * Format time remaining as a display string.
     */
    private static String formatTimeRemaining(long expiryTime, long serverTime) {
        long remaining = expiryTime - serverTime;

        if (remaining <= 0) {
            return "Expired";
        }

        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}
