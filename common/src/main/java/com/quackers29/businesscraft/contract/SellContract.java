package com.quackers29.businesscraft.contract;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public class SellContract extends Contract {
    private String resourceId;
    private int quantity;
    private float pricePerUnit;
    private UUID buyerTownId;

    public SellContract(UUID issuerTownId, long duration, String resourceId, int quantity, float pricePerUnit) {
        super(issuerTownId, duration);
        this.resourceId = resourceId;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.buyerTownId = null;
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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putString("resourceId", resourceId);
        tag.putInt("quantity", quantity);
        tag.putFloat("pricePerUnit", pricePerUnit);
        if (buyerTownId != null) {
            tag.putUUID("buyerTownId", buyerTownId);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag) {
        resourceId = tag.getString("resourceId");
        quantity = tag.getInt("quantity");
        pricePerUnit = tag.getFloat("pricePerUnit");
        if (tag.hasUUID("buyerTownId")) {
            buyerTownId = tag.getUUID("buyerTownId");
        }
    }

    @Override
    public String getType() {
        return "sell";
    }
}
