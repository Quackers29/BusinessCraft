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

    public ContractSavedData() {
    }

    public List<Contract> getContracts() {
        return activeContracts;
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
        LOGGER.debug("Saved {} contracts to saved data", activeContracts.size());

        return tag;
    }
}
