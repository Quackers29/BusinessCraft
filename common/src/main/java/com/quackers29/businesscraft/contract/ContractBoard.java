package com.quackers29.businesscraft.contract;

import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ContractBoard {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContractBoard.class);
    private static final ContractBoard INSTANCE = new ContractBoard();
    private static final String DATA_FILENAME = "contract_board.dat";

    private final List<Contract> activeContracts = new ArrayList<>();
    private boolean dirty = false;

    private ContractBoard() {
    }

    public static ContractBoard getInstance() {
        return INSTANCE;
    }

    public void addContract(Contract contract) {
        activeContracts.add(contract);
        dirty = true;
    }

    public void removeContract(UUID contractId) {
        activeContracts.removeIf(c -> c.getId().equals(contractId));
        dirty = true;
    }

    public void updateContract(Contract contract) {
        // Since we hold references, modifying the object directly updates it in the
        // list.
        // We just need to mark as dirty to ensure save.
        dirty = true;
    }

    public Contract getContract(UUID contractId) {
        for (Contract c : activeContracts) {
            if (c.getId().equals(contractId)) {
                return c;
            }
        }
        return null;
    }

    public List<Contract> getContracts() {
        return Collections.unmodifiableList(activeContracts);
    }

    public void tick() {
        // Remove expired or completed contracts
        Iterator<Contract> iterator = activeContracts.iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            Contract c = iterator.next();
            if (c.isExpired() || c.isCompleted()) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            dirty = true;
        }

        if (dirty) {
            save();
            dirty = false;
        }
    }

    public void save() {
        try {
            File file = PlatformAccess.platform.getConfigDirectory().resolve(DATA_FILENAME).toFile();
            CompoundTag root = new CompoundTag();
            ListTag list = new ListTag();

            for (Contract c : activeContracts) {
                CompoundTag contractTag = new CompoundTag();
                contractTag.putString("type", c.getType());
                c.save(contractTag);
                list.add(contractTag);
            }

            root.put("contracts", list);
            NbtIo.writeCompressed(root, file);
        } catch (Exception e) {
            LOGGER.error("Failed to save contract board data", e);
        }
    }

    public void load() {
        try {
            File file = PlatformAccess.platform.getConfigDirectory().resolve(DATA_FILENAME).toFile();
            if (!file.exists())
                return;

            CompoundTag root = NbtIo.readCompressed(file);
            if (root.contains("contracts")) {
                ListTag list = root.getList("contracts", Tag.TAG_COMPOUND);
                activeContracts.clear();

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
                        activeContracts.add(contract);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load contract board data", e);
        }
    }
}
