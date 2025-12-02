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
        broadcastUpdate();
    }

    public void removeContract(UUID contractId) {
        activeContracts.removeIf(c -> c.getId().equals(contractId));
        dirty = true;
        broadcastUpdate();
    }

    public void updateContract(Contract contract) {
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
        closeAuctions();

        // Keep expired contracts for History tab, but limit total to 100
        while (activeContracts.size() > 100) {
            Contract oldest = activeContracts.stream()
                    .min((c1, c2) -> Long.compare(c1.getExpiryTime(), c2.getExpiryTime()))
                    .orElse(null);
            if (oldest != null) {
                activeContracts.remove(oldest);
                dirty = true;
            } else {
                break;
            }
        }

        if (dirty) {
            save();
            dirty = false;
        }
    }

    private void closeAuctions() {
        for (Contract contract : activeContracts) {
            if (contract instanceof SellContract sc) {
                if (sc.isExpired() && sc.getWinningTownId() == null && !sc.getBids().isEmpty()) {
                    UUID highestBidder = sc.getHighestBidder();
                    float highestBid = sc.getHighestBid();

                    if (highestBidder != null) {
                        sc.setWinningTownId(highestBidder);
                        sc.setAcceptedBid(highestBid);
                        sc.complete();
                        sc.extendExpiry(90000L); // 90 seconds for Active phase
                        LOGGER.info("Auction closed for contract {}: Winner={}, Bid={}",
                                sc.getId(), highestBidder, highestBid);
                        dirty = true;
                    }
                }
            }
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

    public void addBid(UUID contractId, UUID bidder, float amount) {
        Contract contract = getContract(contractId);
        if (contract != null) {
            contract.addBid(bidder, amount);
            save();
            broadcastUpdate();
        }
    }

    private void broadcastUpdate() {
        try {
            LOGGER.info("Broadcasting contract update to all players. Contract count: {}", activeContracts.size());
            if (PlatformAccess.getNetworkMessages() != null) {
                PlatformAccess.getNetworkMessages().sendToAllPlayers(
                        new com.quackers29.businesscraft.network.packets.ui.ContractSyncPacket(activeContracts));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to broadcast contract update", e);
        }
    }
}
