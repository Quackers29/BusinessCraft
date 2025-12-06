package com.quackers29.businesscraft.data;

import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.contract.CourierContract;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ContractSavedData extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContractSavedData.class);
    public static final String NAME = "businesscraft_contracts";

    private final List<Contract> activeContracts = new ArrayList<>();
    private final java.util.Map<String, Float> marketPrices = new java.util.HashMap<>();

    public ContractSavedData() {
    }

    public List<Contract> getContracts() {
        return activeContracts;
    }

    public java.util.Map<String, Float> getMarketPrices() {
        return marketPrices;
    }

    public static ContractSavedData create() {
        return new ContractSavedData();
    }

    public static ContractSavedData load(CompoundTag tag) {
        ContractSavedData data = new ContractSavedData();

        if (tag.contains("contracts")) {
            ListTag list = tag.getList("contracts", Tag.TAG_COMPOUND);

            for (int i = 0; i < list.size(); i++) {
                CompoundTag contractTag = list.getCompound(i);
                String type = contractTag.getString("type");
                Contract contract = null;

                if ("sell".equals(type)) {
                    contract = new SellContract(contractTag);
                } else if ("courier".equals(type)) {
                    contract = new CourierContract(contractTag);
                }

                if (contract != null) {
                    data.activeContracts.add(contract);
                }
            }

            LOGGER.info("Loaded {} contracts from saved data", data.activeContracts.size());
        }

        if (tag.contains("marketPrices")) {
            CompoundTag pricesTag = tag.getCompound("marketPrices");
            for (String key : pricesTag.getAllKeys()) {
                data.marketPrices.put(key, pricesTag.getFloat(key));
            }
            LOGGER.info("Loaded market prices for {} items", data.marketPrices.size());
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (Contract c : activeContracts) {
            CompoundTag contractTag = new CompoundTag();
            contractTag.putString("type", c.getType());
            c.save(contractTag);
            list.add(contractTag);
        }

        tag.put("contracts", list);

        CompoundTag pricesTag = new CompoundTag();
        for (java.util.Map.Entry<String, Float> entry : marketPrices.entrySet()) {
            pricesTag.putFloat(entry.getKey(), entry.getValue());
        }
        tag.put("marketPrices", pricesTag);

        LOGGER.debug("Saved {} contracts and {} market prices", activeContracts.size(), marketPrices.size());

        return tag;
    }

    public static ContractSavedData get(net.minecraft.server.level.ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ContractSavedData::load, ContractSavedData::create, NAME);
    }
}
