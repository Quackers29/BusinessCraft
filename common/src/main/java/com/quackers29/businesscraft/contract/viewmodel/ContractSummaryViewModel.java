package com.quackers29.businesscraft.contract.viewmodel;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

/**
 * Lightweight view-model for contract list display.
 * Contains only the data needed to render a contract summary in the list view.
 * All display strings are pre-calculated on the server.
 */
public class ContractSummaryViewModel {

    private final UUID contractId;
    private final String contractType; // "sell", "courier"
    private final String resourceId;
    private final int quantity;
    private final UUID issuerTownId;
    private final String issuerTownName;
    
    // Pre-calculated display strings (server-authoritative)
    private final String timeRemainingDisplay; // "5m 30s" or "Expired"
    private final String highestBidDisplay;    // "150 emeralds" or "No bids"
    private final String statusDisplay;        // "Auction", "Awaiting Courier", "In Transit", "Delivered"
    private final String priceDisplay;         // "10.5 emeralds/unit"
    
    // Server-calculated action flags
    private final boolean canBid;
    private final boolean canAcceptCourier;
    private final boolean isExpired;
    private final boolean isDelivered;

    public ContractSummaryViewModel(
            UUID contractId,
            String contractType,
            String resourceId,
            int quantity,
            UUID issuerTownId,
            String issuerTownName,
            String timeRemainingDisplay,
            String highestBidDisplay,
            String statusDisplay,
            String priceDisplay,
            boolean canBid,
            boolean canAcceptCourier,
            boolean isExpired,
            boolean isDelivered) {
        this.contractId = contractId;
        this.contractType = contractType;
        this.resourceId = resourceId;
        this.quantity = quantity;
        this.issuerTownId = issuerTownId;
        this.issuerTownName = issuerTownName;
        this.timeRemainingDisplay = timeRemainingDisplay;
        this.highestBidDisplay = highestBidDisplay;
        this.statusDisplay = statusDisplay;
        this.priceDisplay = priceDisplay;
        this.canBid = canBid;
        this.canAcceptCourier = canAcceptCourier;
        this.isExpired = isExpired;
        this.isDelivered = isDelivered;
    }

    /**
     * Deserialize from network buffer.
     */
    public ContractSummaryViewModel(FriendlyByteBuf buf) {
        this.contractId = buf.readUUID();
        this.contractType = buf.readUtf();
        this.resourceId = buf.readUtf();
        this.quantity = buf.readInt();
        this.issuerTownId = buf.readUUID();
        this.issuerTownName = buf.readUtf();
        this.timeRemainingDisplay = buf.readUtf();
        this.highestBidDisplay = buf.readUtf();
        this.statusDisplay = buf.readUtf();
        this.priceDisplay = buf.readUtf();
        this.canBid = buf.readBoolean();
        this.canAcceptCourier = buf.readBoolean();
        this.isExpired = buf.readBoolean();
        this.isDelivered = buf.readBoolean();
    }

    /**
     * Serialize to network buffer.
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(contractId);
        buf.writeUtf(contractType);
        buf.writeUtf(resourceId);
        buf.writeInt(quantity);
        buf.writeUUID(issuerTownId);
        buf.writeUtf(issuerTownName);
        buf.writeUtf(timeRemainingDisplay);
        buf.writeUtf(highestBidDisplay);
        buf.writeUtf(statusDisplay);
        buf.writeUtf(priceDisplay);
        buf.writeBoolean(canBid);
        buf.writeBoolean(canAcceptCourier);
        buf.writeBoolean(isExpired);
        buf.writeBoolean(isDelivered);
    }

    // Getters
    public UUID getContractId() { return contractId; }
    public String getContractType() { return contractType; }
    public String getResourceId() { return resourceId; }
    public int getQuantity() { return quantity; }
    public UUID getIssuerTownId() { return issuerTownId; }
    public String getIssuerTownName() { return issuerTownName; }
    public String getTimeRemainingDisplay() { return timeRemainingDisplay; }
    public String getHighestBidDisplay() { return highestBidDisplay; }
    public String getStatusDisplay() { return statusDisplay; }
    public String getPriceDisplay() { return priceDisplay; }
    public boolean canBid() { return canBid; }
    public boolean canAcceptCourier() { return canAcceptCourier; }
    public boolean isExpired() { return isExpired; }
    public boolean isDelivered() { return isDelivered; }
}
