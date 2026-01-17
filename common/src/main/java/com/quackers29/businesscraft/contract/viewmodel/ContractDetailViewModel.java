package com.quackers29.businesscraft.contract.viewmodel;

import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Full view-model for contract detail display.
 * Contains all data needed to render the contract detail screen.
 * All display strings are pre-calculated on the server.
 */
public class ContractDetailViewModel {

    // Basic contract info (same as summary)
    private final UUID contractId;
    private final String contractType;
    private final String resourceId;
    private final int quantity;
    private final String issuerTownName;
    
    // Pre-calculated display strings
    private final String timeRemainingDisplay;
    private final String highestBidDisplay;
    private final String statusDisplay;
    private final String priceDisplay;
    private final String createdDateDisplay;
    private final String expiresDateDisplay;
    private final String deliveryProgressDisplay; // "5/10 delivered" or null
    private final String courierName; // Assigned courier name or null
    private final String winningBidderName; // Winning town name or null
    private final String tooltipText;
    
    // Server-calculated action flags
    private final boolean canBid;
    private final boolean canAcceptCourier;
    private final boolean isExpired;
    private final boolean isDelivered;
    private final boolean isAuctionClosed;
    
    // Bid list (sorted by amount, highest first)
    private final List<BidDisplayInfo> bids;

    /**
     * Bid display info record.
     */
    public record BidDisplayInfo(
            String bidderName,
            String amountDisplay,
            boolean isHighest
    ) {
        public BidDisplayInfo(FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readUtf(), buf.readBoolean());
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(bidderName);
            buf.writeUtf(amountDisplay);
            buf.writeBoolean(isHighest);
        }
    }

    public ContractDetailViewModel(
            UUID contractId,
            String contractType,
            String resourceId,
            int quantity,
            String issuerTownName,
            String timeRemainingDisplay,
            String highestBidDisplay,
            String statusDisplay,
            String priceDisplay,
            String createdDateDisplay,
            String expiresDateDisplay,
            String deliveryProgressDisplay,
            String courierName,
            String winningBidderName,
            String tooltipText,
            boolean canBid,
            boolean canAcceptCourier,
            boolean isExpired,
            boolean isDelivered,
            boolean isAuctionClosed,
            List<BidDisplayInfo> bids) {
        this.contractId = contractId;
        this.contractType = contractType;
        this.resourceId = resourceId;
        this.quantity = quantity;
        this.issuerTownName = issuerTownName;
        this.timeRemainingDisplay = timeRemainingDisplay;
        this.highestBidDisplay = highestBidDisplay;
        this.statusDisplay = statusDisplay;
        this.priceDisplay = priceDisplay;
        this.createdDateDisplay = createdDateDisplay;
        this.expiresDateDisplay = expiresDateDisplay;
        this.deliveryProgressDisplay = deliveryProgressDisplay;
        this.courierName = courierName;
        this.winningBidderName = winningBidderName;
        this.tooltipText = tooltipText;
        this.canBid = canBid;
        this.canAcceptCourier = canAcceptCourier;
        this.isExpired = isExpired;
        this.isDelivered = isDelivered;
        this.isAuctionClosed = isAuctionClosed;
        this.bids = bids != null ? new ArrayList<>(bids) : new ArrayList<>();
    }

    /**
     * Deserialize from network buffer.
     */
    public ContractDetailViewModel(FriendlyByteBuf buf) {
        this.contractId = buf.readUUID();
        this.contractType = buf.readUtf();
        this.resourceId = buf.readUtf();
        this.quantity = buf.readInt();
        this.issuerTownName = buf.readUtf();
        this.timeRemainingDisplay = buf.readUtf();
        this.highestBidDisplay = buf.readUtf();
        this.statusDisplay = buf.readUtf();
        this.priceDisplay = buf.readUtf();
        this.createdDateDisplay = buf.readUtf();
        this.expiresDateDisplay = buf.readUtf();
        this.deliveryProgressDisplay = buf.readBoolean() ? buf.readUtf() : null;
        this.courierName = buf.readBoolean() ? buf.readUtf() : null;
        this.winningBidderName = buf.readBoolean() ? buf.readUtf() : null;
        this.tooltipText = buf.readUtf();
        this.canBid = buf.readBoolean();
        this.canAcceptCourier = buf.readBoolean();
        this.isExpired = buf.readBoolean();
        this.isDelivered = buf.readBoolean();
        this.isAuctionClosed = buf.readBoolean();

        // Read bids
        int bidCount = buf.readInt();
        this.bids = new ArrayList<>(bidCount);
        for (int i = 0; i < bidCount; i++) {
            this.bids.add(new BidDisplayInfo(buf));
        }
    }

    /**
     * Serialize to network buffer.
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(contractId);
        buf.writeUtf(contractType);
        buf.writeUtf(resourceId);
        buf.writeInt(quantity);
        buf.writeUtf(issuerTownName);
        buf.writeUtf(timeRemainingDisplay);
        buf.writeUtf(highestBidDisplay);
        buf.writeUtf(statusDisplay);
        buf.writeUtf(priceDisplay);
        buf.writeUtf(createdDateDisplay);
        buf.writeUtf(expiresDateDisplay);

        // Nullable fields
        buf.writeBoolean(deliveryProgressDisplay != null);
        if (deliveryProgressDisplay != null) buf.writeUtf(deliveryProgressDisplay);

        buf.writeBoolean(courierName != null);
        if (courierName != null) buf.writeUtf(courierName);

        buf.writeBoolean(winningBidderName != null);
        if (winningBidderName != null) buf.writeUtf(winningBidderName);

        buf.writeUtf(tooltipText);
        buf.writeBoolean(canBid);
        buf.writeBoolean(canAcceptCourier);
        buf.writeBoolean(isExpired);
        buf.writeBoolean(isDelivered);
        buf.writeBoolean(isAuctionClosed);

        // Write bids
        buf.writeInt(bids.size());
        for (BidDisplayInfo bid : bids) {
            bid.toBytes(buf);
        }
    }

    // Getters
    public UUID getContractId() { return contractId; }
    public String getContractType() { return contractType; }
    public String getResourceId() { return resourceId; }
    public int getQuantity() { return quantity; }
    public String getIssuerTownName() { return issuerTownName; }
    public String getTimeRemainingDisplay() { return timeRemainingDisplay; }
    public String getHighestBidDisplay() { return highestBidDisplay; }
    public String getStatusDisplay() { return statusDisplay; }
    public String getPriceDisplay() { return priceDisplay; }
    public String getCreatedDateDisplay() { return createdDateDisplay; }
    public String getExpiresDateDisplay() { return expiresDateDisplay; }
    public String getDeliveryProgressDisplay() { return deliveryProgressDisplay; }
    public String getCourierName() { return courierName; }
    public String getWinningBidderName() { return winningBidderName; }
    public String getTooltipText() { return tooltipText; }
    public boolean canBid() { return canBid; }
    public boolean canAcceptCourier() { return canAcceptCourier; }
    public boolean isExpired() { return isExpired; }
    public boolean isDelivered() { return isDelivered; }
    public boolean isAuctionClosed() { return isAuctionClosed; }
    public List<BidDisplayInfo> getBids() { return bids; }
}
