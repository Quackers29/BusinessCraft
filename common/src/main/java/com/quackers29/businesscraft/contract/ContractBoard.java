package com.quackers29.businesscraft.contract;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.data.ContractSavedData;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;

public class ContractBoard {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContractBoard.class);
    private static final Map<ServerLevel, ContractBoard> INSTANCES = new HashMap<>();

    private final ContractSavedData savedData;
    private final ServerLevel level;

    private ContractBoard(ServerLevel level) {
        this.level = level;
        this.savedData = ContractSavedData.get(level);
    }

    public static ContractBoard get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, ContractBoard::new);
    }

    public static void clearInstances() {
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
        for (Contract contract : savedData.getContracts()) {
            if (contract instanceof SellContract sc) {
                if (sc.isCompleted() && sc.isExpired() && !sc.isDelivered()) {
                    processContractDelivery(sc, level);
                }
            }
        }
    }

    private void processContractDelivery(SellContract sc, ServerLevel level) {
        // Mark as delivered so we don't process again
        sc.setDelivered(true);
        savedData.setDirty();

        com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager
                .get(level);

        // If there is no winning town (e.g. failed auction), we don't need to do any
        // delivery
        if (sc.getWinningTownId() == null) {
            return;
        }

        com.quackers29.businesscraft.town.Town sellerTown = townManager.getTown(sc.getIssuerTownId());
        com.quackers29.businesscraft.town.Town buyerTown = townManager.getTown(sc.getWinningTownId());

        if (sellerTown != null && buyerTown != null) {
            // Transfer Items (already escrowed from seller at auction start)
            net.minecraft.world.item.Item item = null;
            if ("wood".equals(sc.getResourceId()))
                item = net.minecraft.world.item.Items.OAK_LOG;
            else if ("iron".equals(sc.getResourceId()))
                item = net.minecraft.world.item.Items.IRON_INGOT;
            else if ("coal".equals(sc.getResourceId()))
                item = net.minecraft.world.item.Items.COAL;

            if (item != null) {
                // Just add to buyer (resources already deducted from seller at auction start)
                buyerTown.addResource(item, sc.getQuantity());

                LOGGER.info("Delivered {} {} from {} to {} (escrowed resources transferred)",
                        sc.getQuantity(), sc.getResourceId(), sellerTown.getName(), buyerTown.getName());
            }

            // Transfer Money (Emeralds already escrowed from buyer when they bid)
            int cost = (int) sc.getAcceptedBid();
            if (cost > 0) {
                // Just add to seller (emeralds already deducted from buyer when they won)
                sellerTown.addResource(net.minecraft.world.item.Items.EMERALD, cost);

                LOGGER.info("Delivered {} emeralds from {} to {} (escrowed emeralds transferred)",
                        cost, buyerTown.getName(), sellerTown.getName());
            }
        }
    }

    private void closeAuctions() {
        com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager
                .get(level);

        List<Contract> contractsCopy = new ArrayList<>(savedData.getContracts());

        for (Contract contract : contractsCopy) {
            if (contract instanceof SellContract sc) {
                // Check if auction expired without being closed yet
                if (sc.isExpired() && !sc.isCompleted() && sc.getWinningTownId() == null) {
                    if (!sc.getBids().isEmpty()) {
                        // Auction has bids - close it with winner
                        UUID highestBidder = sc.getHighestBidder();
                        float highestBid = sc.getHighestBid();

                        if (highestBidder != null) {
                            // Get winner town name for caching
                            com.quackers29.businesscraft.town.Town winnerTown = townManager.getTown(highestBidder);
                            if (winnerTown != null) {
                                sc.setWinningTown(highestBidder, winnerTown.getName());
                            } else {
                                sc.setWinningTownId(highestBidder);
                            }

                            sc.setAcceptedBid(highestBid);
                            sc.complete();
                            sc.extendExpiry(90000L); // 90 seconds for Active phase

                            // Auto-create CourierContract for delivery from seller to buyer
                            com.quackers29.businesscraft.town.Town sellerTown = townManager
                                    .getTown(sc.getIssuerTownId());
                            if (sellerTown != null) {
                                CourierContract courier = new CourierContract(
                                        sc.getIssuerTownId(), // Seller is the issuer (source)
                                        sc.getIssuerTownName(),
                                        sellerTown.getPosition(),
                                        sellerTown.getBoundaryRadius(),
                                        180000L, // 3 min for courier acceptance
                                        sc.getResourceId(),
                                        sc.getQuantity(),
                                        highestBidder, // Buyer is the destination
                                        winnerTown != null ? winnerTown.getName() : "Unknown",
                                        highestBid * 0.25f); // 25% reward
                                addContract(courier);
                                LOGGER.info(
                                        "Created auto-courier {} for SellContract {} (seller={} [{}] to buyer={} [{}])",
                                        courier.getId(), sc.getId(), sc.getIssuerTownId(), sc.getIssuerTownName(),
                                        highestBidder, winnerTown != null ? winnerTown.getName() : "Unknown");
                            }

                            LOGGER.info("Auction closed for contract {}: Winner={}, Bid={}",
                                    sc.getId(), highestBidder, highestBid);
                            savedData.setDirty();
                        }
                    } else {
                        // ESCRO refund: Auction failed (no bids) - return resources to seller
                        com.quackers29.businesscraft.town.Town sellerTown = townManager.getTown(sc.getIssuerTownId());
                        if (sellerTown != null) {
                            net.minecraft.world.item.Item item = null;
                            if ("wood".equals(sc.getResourceId()))
                                item = net.minecraft.world.item.Items.OAK_LOG;
                            else if ("iron".equals(sc.getResourceId()))
                                item = net.minecraft.world.item.Items.IRON_INGOT;
                            else if ("coal".equals(sc.getResourceId()))
                                item = net.minecraft.world.item.Items.COAL;

                            if (item != null) {
                                sellerTown.addResource(item, sc.getQuantity());
                                LOGGER.info("Refunded {} {} to town {} (auction {} had no bids)",
                                        sc.getQuantity(), sc.getResourceId(), sellerTown.getName(), sc.getId());
                            }
                        }
                        // Mark as completed so it moves to history
                        sc.complete();
                        savedData.setDirty();
                    }
                }
            }
        }
    }

    public void addBid(UUID contractId, UUID bidder, float amount, ServerLevel level) {
        Contract contract = getContract(contractId);
        if (contract != null) {
            if (contract instanceof SellContract) {
                com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager
                        .get(level);
                com.quackers29.businesscraft.town.Town bidderTown = townManager.getTown(bidder);

                if (bidderTown == null) {
                    LOGGER.warn("Cannot place bid: bidder town {} not found", bidder);
                    return;
                }

                // Prevent self-bidding
                if (bidder.equals(contract.getIssuerTownId())) {
                    LOGGER.warn("Town {} attempted to bid on their own contract {}", bidderTown.getName(), contractId);
                    return;
                }

                // Get previous highest bidder (if exists and different from new bidder)
                UUID previousHighestBidder = contract.getHighestBidder();
                float previousHighestBid = contract.getHighestBid();

                // Add the bid to the contract
                contract.addBid(bidder, amount);

                // ESCROW: Deduct emeralds from new bidder
                bidderTown.addResource(net.minecraft.world.item.Items.EMERALD, -(int) amount);
                LOGGER.info("Escrowed {} emeralds from town {} for bid on contract {}",
                        (int) amount, bidderTown.getName(), contractId);

                // ESCROW: Refund emeralds to previous highest bidder (if different)
                if (previousHighestBidder != null && !previousHighestBidder.equals(bidder)) {
                    com.quackers29.businesscraft.town.Town previousTown = townManager.getTown(previousHighestBidder);
                    if (previousTown != null) {
                        previousTown.addResource(net.minecraft.world.item.Items.EMERALD, (int) previousHighestBid);
                        LOGGER.info("Refunded {} emeralds to town {} (outbid on contract {})",
                                (int) previousHighestBid, previousTown.getName(), contractId);
                    }
                }

                savedData.setDirty();
                broadcastUpdate();
            } else if (contract instanceof CourierContract cc) {
                // Handle Courier Contract Acceptance
                if (!cc.isAccepted() && !cc.isExpired()) {
                    // Verify player location
                    net.minecraft.server.level.ServerPlayer player = level.getServer().getPlayerList()
                            .getPlayer(bidder);
                    if (player == null) {
                        LOGGER.warn("Cannot accept courier contract: player {} not found", bidder);
                        return;
                    }

                    com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager
                            .get(level);
                    com.quackers29.businesscraft.town.Town sellerTown = townManager.getTown(cc.getIssuerTownId());

                    if (sellerTown == null) {
                        LOGGER.warn("Cannot accept courier contract: seller town {} not found", cc.getIssuerTownId());
                        return;
                    }

                    // Check distance (allow slightly larger radius for leniency)
                    double distSqr = player.blockPosition().distSqr(sellerTown.getPosition());
                    double maxDist = sellerTown.getBoundaryRadius() + 10; // +10 buffer
                    if (distSqr > maxDist * maxDist) {
                        LOGGER.warn("Cannot accept courier contract: player too far from seller town (dist={}, max={})",
                                Math.sqrt(distSqr), maxDist);
                        return;
                    }

                    cc.setCourierId(bidder);
                    cc.setAcceptedTime(System.currentTimeMillis());

                    // Update expiry to 4 minutes from now for delivery
                    cc.extendExpiry(240000L); // 4 minutes

                    // Transfer items to Seller Town's Payment Buffer
                    net.minecraft.world.item.Item item = null;
                    if ("wood".equals(cc.getResourceId()))
                        item = net.minecraft.world.item.Items.OAK_LOG;
                    else if ("iron".equals(cc.getResourceId()))
                        item = net.minecraft.world.item.Items.IRON_INGOT;
                    else if ("coal".equals(cc.getResourceId()))
                        item = net.minecraft.world.item.Items.COAL;

                    if (item != null) {
                        // Create item stack list
                        java.util.List<net.minecraft.world.item.ItemStack> rewards = new java.util.ArrayList<>();
                        rewards.add(new net.minecraft.world.item.ItemStack(item, cc.getQuantity()));

                        // Add to payment board as a claimable reward for the courier
                        com.quackers29.businesscraft.town.data.RewardSource source = com.quackers29.businesscraft.town.data.RewardSource.COURIER_PICKUP;
                        java.util.UUID rewardId = sellerTown.getPaymentBoard().addReward(source, rewards,
                                bidder.toString());

                        if (rewardId != null) {
                            // Add metadata to link to contract
                            sellerTown.getPaymentBoard().getRewardById(rewardId).ifPresent(entry -> {
                                entry.addMetadata("contractId", contractId.toString());
                            });

                            LOGGER.info("Created courier pickup reward {} for town {} (courier={})",
                                    rewardId, sellerTown.getName(), bidder);
                        } else {
                            LOGGER.warn("Failed to create courier pickup reward for town {}", sellerTown.getName());
                        }
                    }

                    LOGGER.info("Courier contract {} accepted by {}. New expiry in 4 mins.", contractId, bidder);
                    savedData.setDirty();
                    broadcastUpdate();
                }
            }
        }
    }

    private void broadcastUpdate() {
        try {
            if (PlatformAccess.getNetworkMessages() != null) {
                PlatformAccess.getNetworkMessages().sendToAllPlayers(
                        new com.quackers29.businesscraft.network.packets.ui.ContractSyncPacket(
                                savedData.getContracts()));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to broadcast contract update", e);
        }
    }
}
