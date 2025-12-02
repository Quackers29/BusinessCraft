package com.quackers29.businesscraft.contract;

import com.quackers29.businesscraft.data.ContractSavedData;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ContractBoard {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContractBoard.class);
    private static final Map<ServerLevel, ContractBoard> INSTANCES = new HashMap<>();

    private final ContractSavedData savedData;
    private final ServerLevel level;

    private ContractBoard(ServerLevel level) {
        this.level = level;
        this.savedData = level.getDataStorage().computeIfAbsent(
                ContractSavedData::load,
                ContractSavedData::create,
                ContractSavedData.NAME);
        LOGGER.info("ContractBoard initialized for level: {}", level.dimension().location());
    }

    public static ContractBoard get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, key -> new ContractBoard(level));
    }

    public static void clearInstances() {
        LOGGER.info("Clearing {} ContractBoard instances", INSTANCES.size());
        INSTANCES.clear();
    }

    public void addContract(Contract contract) {
        savedData.getContracts().add(contract);
        savedData.setDirty();
        broadcastUpdate();
    }

    public void removeContract(UUID contractId) {
        savedData.getContracts().removeIf(c -> c.getId().equals(contractId));
        savedData.setDirty();
        broadcastUpdate();
    }

    public void updateContract(Contract contract) {
        savedData.setDirty();
    }

    public Contract getContract(UUID contractId) {
        for (Contract c : savedData.getContracts()) {
            if (c.getId().equals(contractId)) {
                return c;
            }
        }
        return null;
    }

    public List<Contract> getContracts() {
        return Collections.unmodifiableList(savedData.getContracts());
    }

    public void tick(ServerLevel level) {
        closeAuctions();

        // Keep expired contracts for History tab, but limit total to 100
        while (savedData.getContracts().size() > 100) {
            Contract oldest = savedData.getContracts().stream()
                    .min((c1, c2) -> Long.compare(c1.getExpiryTime(), c2.getExpiryTime()))
                    .orElse(null);
            if (oldest != null) {
                savedData.getContracts().remove(oldest);
                savedData.setDirty();
            } else {
                break;
            }
        }

        // Check for contracts that have finished their "Active" phase (delivery time)
        for (Contract c : savedData.getContracts()) {
            if (c.isCompleted() && c.isExpired() && c instanceof SellContract sc && !sc.isDelivered()) {
                processContractDelivery(sc, level);
            }
        }

        savedData.setDirty();
    }

    private void processContractDelivery(SellContract sc, ServerLevel level) {
        // Mark as delivered so we don't process again
        sc.setDelivered(true);
        savedData.setDirty();

        com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager
                .get(level);
        com.quackers29.businesscraft.town.Town sellerTown = townManager.getTown(sc.getIssuerTownId());
        com.quackers29.businesscraft.town.Town buyerTown = townManager.getTown(sc.getWinningTownId());

        if (sellerTown != null && buyerTown != null) {
            // Transfer Items
            net.minecraft.world.item.Item item = null;
            if ("wood".equals(sc.getResourceId()))
                item = net.minecraft.world.item.Items.OAK_LOG;
            else if ("iron".equals(sc.getResourceId()))
                item = net.minecraft.world.item.Items.IRON_INGOT;
            else if ("coal".equals(sc.getResourceId()))
                item = net.minecraft.world.item.Items.COAL;

            if (item != null) {
                // Remove from seller (if they still have it)
                sellerTown.addResource(item, -sc.getQuantity());

                // Add to buyer
                buyerTown.addResource(item, sc.getQuantity());

                LOGGER.info("Transferred {} {} from {} to {}",
                        sc.getQuantity(), sc.getResourceId(), sellerTown.getName(), buyerTown.getName());
            }

            // Transfer Money (Emeralds)
            int cost = (int) sc.getAcceptedBid();
            if (cost > 0) {
                buyerTown.addResource(net.minecraft.world.item.Items.EMERALD, -cost);
                sellerTown.addResource(net.minecraft.world.item.Items.EMERALD, cost);

                LOGGER.info("Transferred {} emeralds from {} to {}",
                        cost, buyerTown.getName(), sellerTown.getName());
            }
        }
    }

    private void closeAuctions() {
        for (Contract contract : savedData.getContracts()) {
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
                        savedData.setDirty();
                    }
                }
            }
        }
    }

    public void addBid(UUID contractId, UUID bidder, float amount) {
        Contract contract = getContract(contractId);
        if (contract != null) {
            contract.addBid(bidder, amount);
            savedData.setDirty();
            broadcastUpdate();
        }
    }

    private void broadcastUpdate() {
        try {
            LOGGER.info("Broadcasting contract update to all players. Contract count: {}",
                    savedData.getContracts().size());
            if (com.quackers29.businesscraft.api.PlatformAccess.getNetworkMessages() != null) {
                com.quackers29.businesscraft.api.PlatformAccess.getNetworkMessages().sendToAllPlayers(
                        new com.quackers29.businesscraft.network.packets.ui.ContractSyncPacket(
                                savedData.getContracts()));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to broadcast contract update", e);
        }
    }
}
