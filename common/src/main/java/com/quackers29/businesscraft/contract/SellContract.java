package com.quackers29.businesscraft.contract;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public class SellContract extends Contract {
    private String resourceId;
    private int quantity;
    private float pricePerUnit;
    private UUID buyerTownId;
    private UUID winningTownId; // Town that won the auction
    private float acceptedBid; // Winning bid amount
    private boolean isDelivered; // Track if items/money have been transferred

    public SellContract(UUID issuerTownId, String issuerTownName, long duration, String resourceId, int quantity,
            float pricePerUnit) {
        super(issuerTownId, issuerTownName, duration);
        this.resourceId = resourceId;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.buyerTownId = null;
        this.winningTownId = null;
        this.acceptedBid = 0f;
        this.isDelivered = false;
    }

    public SellContract(CompoundTag tag) {
        super(tag);
    }

    public String getResourceId() {
        return resourceId;
    }

    public int getQuantity() {
        return quantity;
    }

    public float getPricePerUnit() {
        return pricePerUnit;
    }

    public UUID getBuyerTownId() {
        return buyerTownId;
    }

    public void setBuyerTownId(UUID buyerTownId) {
        this.buyerTownId = buyerTownId;
    }

    public UUID getWinningTownId() {
        return winningTownId;
    }

    public void setWinningTownId(UUID winningTownId) {
        this.winningTownId = winningTownId;
    }

    public float getAcceptedBid() {
        return acceptedBid;
    }

    public void setAcceptedBid(float acceptedBid) {
        this.acceptedBid = acceptedBid;
    }

    public boolean isAuctionClosed() {
        return isExpired() && winningTownId != null;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public void setDelivered(boolean delivered) {
        this.isDelivered = delivered;
    }

    // UI convenience methods
    public int getAmount() {
        return quantity;
    }

    public float getCurrentBid() {
        // TODO: Implement bidding system tracking
        // For now, return the price per unit times quantity as a placeholder
        return pricePerUnit * quantity;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putString("resourceId", resourceId);
        tag.putInt("quantity", quantity);
        tag.putFloat("pricePerUnit", pricePerUnit);
        if (buyerTownId != null) {
            tag.putUUID("buyerTownId", buyerTownId);
        }
        if (winningTownId != null) {
            tag.putUUID("winningTownId", winningTownId);
        }
        tag.putFloat("acceptedBid", acceptedBid);
        tag.putBoolean("isDelivered", isDelivered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag) {
        resourceId = tag.getString("resourceId");
        quantity = tag.getInt("quantity");
        pricePerUnit = tag.getFloat("pricePerUnit");
        if (tag.hasUUID("buyerTownId")) {
            buyerTownId = tag.getUUID("buyerTownId");
        }
        if (tag.hasUUID("winningTownId")) {
            winningTownId = tag.getUUID("winningTownId");
        }
        acceptedBid = tag.getFloat("acceptedBid");
        isDelivered = tag.getBoolean("isDelivered");
    }

    @Override
    public String getType() {
        return "sell";
    }
}
