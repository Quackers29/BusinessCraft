package com.quackers29.businesscraft.contract;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public class SellContract extends Contract {
    private String resourceId;
    private long quantity;
    private float pricePerUnit;
    private UUID buyerTownId;
    private UUID winningTownId;
    private String winningTownName;
    private float acceptedBid;
    private boolean isDelivered;

    private UUID courierId;
    private float courierReward;
    private long courierAcceptedTime;
    private long deliveredAmount;

    public static final UUID SNAIL_MAIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public boolean isSnailMail() {
        return SNAIL_MAIL_UUID.equals(courierId);
    }

    public SellContract(UUID issuerTownId, String issuerTownName, long duration, String resourceId, long quantity,
            float pricePerUnit) {
        super(issuerTownId, issuerTownName, duration);
        this.resourceId = resourceId;
        this.quantity = Math.max(1L, Math.min(10_000_000L, quantity));
        this.pricePerUnit = Math.max(0.01f, Math.min(1_000_000f, pricePerUnit));

        this.buyerTownId = null;
        this.winningTownId = null;
        this.winningTownName = null;
        this.acceptedBid = 0f;
        this.isDelivered = false;

        this.courierId = null;
        this.courierReward = 0f;
        this.courierAcceptedTime = 0L;
        this.deliveredAmount = 0;
    }

    public SellContract(CompoundTag tag) {
        super(tag);
    }

    public String getResourceId() {
        return resourceId;
    }

    public long getQuantity() {
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

    public String getWinningTownName() {
        return winningTownName;
    }

    public void setWinningTownId(UUID winningTownId) {
        this.winningTownId = winningTownId;
        this.winningTownName = null; // Clear cached name when setting new ID
    }

    public void setWinningTown(UUID winningTownId, String winningTownName) {
        this.winningTownId = winningTownId;
        this.winningTownName = winningTownName;
    }

    public float getAcceptedBid() {
        return acceptedBid;
    }

    public void setAcceptedBid(float acceptedBid) {
        this.acceptedBid = acceptedBid;
    }

    public boolean isAuctionClosed() {
        return winningTownId != null;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public void setDelivered(boolean delivered) {
        this.isDelivered = delivered;
    }

    public UUID getCourierId() {
        return courierId;
    }

    public void setCourierId(UUID courierId) {
        this.courierId = courierId;
    }

    public float getCourierReward() {
        return courierReward;
    }

    public void setCourierReward(float courierReward) {
        this.courierReward = courierReward;
    }

    public long getCourierAcceptedTime() {
        return courierAcceptedTime;
    }

    public void setCourierAcceptedTime(long courierAcceptedTime) {
        this.courierAcceptedTime = courierAcceptedTime;
    }

    public long getDeliveredAmount() {
        return deliveredAmount;
    }

    public void setDeliveredAmount(long deliveredAmount) {
        this.deliveredAmount = deliveredAmount;
    }

    public void addDeliveredAmount(long amount) {
        this.deliveredAmount += amount;
    }

    public boolean isCourierAssigned() {
        return courierId != null;
    }

    public boolean isDeliveryComplete() {
        return deliveredAmount >= quantity;
    }

    public int getAmount() {
        return (int) quantity;
    }

    public float getCurrentBid() {
        return pricePerUnit * quantity;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putString("resourceId", resourceId);
        tag.putLong("quantity", quantity);
        tag.putFloat("pricePerUnit", pricePerUnit);
        if (buyerTownId != null) {
            tag.putUUID("buyerTownId", buyerTownId);
        }
        if (winningTownId != null) {
            tag.putUUID("winningTownId", winningTownId);
        }
        if (winningTownName != null) {
            tag.putString("winningTownName", winningTownName);
        }
        tag.putFloat("acceptedBid", acceptedBid);
        tag.putBoolean("isDelivered", isDelivered);

        if (courierId != null) {
            tag.putUUID("courierId", courierId);
        }
        tag.putFloat("courierReward", courierReward);
        tag.putLong("courierAcceptedTime", courierAcceptedTime);
        tag.putLong("deliveredAmount", deliveredAmount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag) {
        resourceId = tag.getString("resourceId");
        long loadedQty = tag.getLong("quantity");
        this.quantity = Math.max(1L, Math.min(10_000_000L, loadedQty)); // Sanitize loaded quantity
        float loadedPrice = tag.getFloat("pricePerUnit");
        this.pricePerUnit = Math.max(0.01f, Math.min(1_000_000f, loadedPrice));
        if (tag.hasUUID("buyerTownId")) {
            buyerTownId = tag.getUUID("buyerTownId");
        }
        if (tag.hasUUID("winningTownId")) {
            winningTownId = tag.getUUID("winningTownId");
        }
        if (tag.contains("winningTownName")) {
            winningTownName = tag.getString("winningTownName");
        }
        acceptedBid = tag.getFloat("acceptedBid");
        isDelivered = tag.getBoolean("isDelivered");

        if (tag.hasUUID("courierId")) {
            courierId = tag.getUUID("courierId");
        }
        if (tag.contains("courierReward")) {
            courierReward = tag.getFloat("courierReward");
        }
        if (tag.contains("courierAcceptedTime")) {
            courierAcceptedTime = tag.getLong("courierAcceptedTime");
        }
        if (tag.contains("deliveredAmount")) {
            deliveredAmount = tag.getLong("deliveredAmount");
        }
    }

    @Override
    public String getType() {
        return "sell";
    }
}
