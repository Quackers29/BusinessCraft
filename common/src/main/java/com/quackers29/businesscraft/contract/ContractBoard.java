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

    public float getMarketPrice(String resourceId) {
        return savedData.getMarketPrices().getOrDefault(resourceId, 1.0f);
    }

    public void updateMarketPrice(String resourceId, float transactionPrice) {
        float currentPrice = getMarketPrice(resourceId);
        float alpha = 0.1f; // Learning rate (10% influence)
        float newPrice = (currentPrice * (1.0f - alpha)) + (transactionPrice * alpha);

        // Ensure price doesn't drop too low
        if (newPrice < 0.1f)
            newPrice = 0.1f;

        savedData.getMarketPrices().put(resourceId, newPrice);
        savedData.setDirty();
        LOGGER.info("Updated market price for {}: {} -> {} (Transaction: {})", resourceId, currentPrice, newPrice,
                transactionPrice);
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
                if (sc.isCompleted() && !sc.isDelivered()) {
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

    public void processCourierDelivery(UUID contractId, int amount) {
        Contract contract = getContract(contractId);

        // Handle SellContract (unified contract)
        if (contract instanceof SellContract sc) {
            sc.addDeliveredAmount(amount);
            savedData.setDirty();
            broadcastUpdate();

            if (sc.isDeliveryComplete() && !sc.isCompleted()) {
                // Delivery Complete!
                sc.complete();
                sc.expireNow(); // Expire immediately so contract shows as closed
                savedData.setDirty();
                broadcastUpdate();

                // Update Global Price Index
                if (sc.getQuantity() > 0 && sc.getHighestBid() > 0) {
                    float transactionPrice = sc.getHighestBid() / (float) sc.getQuantity();
                    updateMarketPrice(sc.getResourceId(), transactionPrice);
                }

                // Payout Reward to courier
                com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager
                        .get(level);
                com.quackers29.businesscraft.town.Town destTown = townManager.getTown(sc.getWinningTownId());

                if (destTown != null && sc.getCourierId() != null) {
                    // If Snail Mail, do nothing (money sink)
                    if (sc.isSnailMail()) {
                        LOGGER.info("Snail Mail delivery complete for contract {}. Reward {} voided.", sc.getId(),
                                sc.getCourierReward());
                    } else {
                        // Create reward item (Emeralds)
                        int rewardAmount = (int) sc.getCourierReward();
                        if (rewardAmount > 0) {
                            net.minecraft.world.item.ItemStack emeralds = new net.minecraft.world.item.ItemStack(
                                    net.minecraft.world.item.Items.EMERALD, rewardAmount);
                            List<net.minecraft.world.item.ItemStack> rewards = new ArrayList<>();
                            rewards.add(emeralds);

                            // Add to destination town's payment board for the courier
                            com.quackers29.businesscraft.town.data.RewardSource source = com.quackers29.businesscraft.town.data.RewardSource.COURIER_DELIVERY;
                            UUID rewardId = destTown.getPaymentBoard().addReward(source, rewards,
                                    sc.getCourierId().toString());

                            if (rewardId != null) {
                                destTown.getPaymentBoard().getRewardById(rewardId).ifPresent(entry -> {
                                    entry.addMetadata("contractId", contractId.toString());
                                });
                                LOGGER.info(
                                        "Contract {} delivery completed! Reward {} emeralds added to {} for courier {}",
                                        contractId, rewardAmount, destTown.getName(), sc.getCourierId());
                            }
                        }
                    }
                }
            }
        }
        // Legacy CourierContract support
        else if (contract instanceof CourierContract cc) {
            cc.addDeliveredAmount(amount);
            savedData.setDirty();
            broadcastUpdate();

            if (cc.isDelivered()) {
                // Contract Complete!
                cc.complete();
                savedData.setDirty();
                broadcastUpdate();

                // Payout Reward
                com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager
                        .get(level);
                com.quackers29.businesscraft.town.Town destTown = townManager.getTown(cc.getDestinationTownId());

                if (destTown != null && cc.getCourierId() != null) {
                    // Create reward item (Emeralds)
                    int rewardAmount = (int) cc.getReward();
                    if (rewardAmount > 0) {
                        net.minecraft.world.item.ItemStack emeralds = new net.minecraft.world.item.ItemStack(
                                net.minecraft.world.item.Items.EMERALD, rewardAmount);
                        List<net.minecraft.world.item.ItemStack> rewards = new ArrayList<>();
                        rewards.add(emeralds);

                        // Add to destination town's payment board for the courier
                        com.quackers29.businesscraft.town.data.RewardSource source = com.quackers29.businesscraft.town.data.RewardSource.COURIER_DELIVERY;
                        UUID rewardId = destTown.getPaymentBoard().addReward(source, rewards,
                                cc.getCourierId().toString());

                        if (rewardId != null) {
                            destTown.getPaymentBoard().getRewardById(rewardId).ifPresent(entry -> {
                                entry.addMetadata("contractId", contractId.toString());
                            });
                            LOGGER.info("Courier contract {} completed! Reward {} emeralds added to {} for courier {}",
                                    contractId, rewardAmount, destTown.getName(), cc.getCourierId());
                        }
                    }
                }
            }
        }
    }

    private void handleCourierAcceptance(SellContract sc, UUID contractId, UUID courierId, ServerLevel level) {
        // Verify player location at seller town
        net.minecraft.server.level.ServerPlayer player = level.getServer().getPlayerList()
                .getPlayer(courierId);
        if (player == null) {
            LOGGER.warn("Cannot accept courier job: player {} not found", courierId);
            return;
        }

        com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager
                .get(level);
        com.quackers29.businesscraft.town.Town sellerTown = townManager.getTown(sc.getIssuerTownId());

        if (sellerTown == null) {
            LOGGER.warn("Cannot accept courier job: seller town {} not found", sc.getIssuerTownId());
            return;
        }

        // Check distance
        double distSqr = player.blockPosition().distSqr(sellerTown.getPosition());
        double maxDist = sellerTown.getBoundaryRadius() + 10;
        if (distSqr > maxDist * maxDist) {
            LOGGER.warn("Cannot accept courier job: player too far from seller town (dist={}, max={})",
                    Math.sqrt(distSqr), maxDist);
            return;
        }

        sc.setCourierId(courierId);
        sc.setCourierAcceptedTime(System.currentTimeMillis());

        // Extend expiry for delivery (4 minutes)
        sc.extendExpiry(240000L);

        // Create contract items and add to seller town's payment board
        String destTownName = sc.getWinningTownName() != null ? sc.getWinningTownName() : "Unknown";
        net.minecraft.world.item.ItemStack contractItem = com.quackers29.businesscraft.util.ContractItemHelper
                .createContractItem(
                        sc.getResourceId(),
                        sc.getQuantity(),
                        contractId,
                        sc.getWinningTownId(),
                        destTownName,
                        sellerTown.getName());

        java.util.List<net.minecraft.world.item.ItemStack> rewards = new java.util.ArrayList<>();
        rewards.add(contractItem);

        com.quackers29.businesscraft.town.data.RewardSource source = com.quackers29.businesscraft.town.data.RewardSource.COURIER_PICKUP;
        UUID rewardId = sellerTown.getPaymentBoard().addReward(source, rewards,
                courierId.toString());

        if (rewardId != null) {
            sellerTown.getPaymentBoard().getRewardById(rewardId).ifPresent(entry -> {
                entry.addMetadata("contractId", contractId.toString());
            });
            LOGGER.info("Courier job {} accepted by {}. Pickup reward {} added to town {}",
                    contractId, courierId, rewardId, sellerTown.getName());
        }

        savedData.setDirty();
        broadcastUpdate();
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
                            // DON'T complete yet - needs courier & delivery
                            // sc.complete(); // REMOVED

                            // Set courier reward (Distance based)
                            // We need to get the seller town again as we are inside the loop
                            com.quackers29.businesscraft.town.Town sellerTown = townManager
                                    .getTown(sc.getIssuerTownId());
                            int courierCost = calculateCourierCost(winnerTown, sellerTown);
                            sc.setCourierReward(courierCost);

                            // Extend expiry for courier acceptance (3 min)
                            sc.extendExpiry(180000L);

                            LOGGER.info("Auction closed for contract {}: Winner={}, Bid={}, CourierReward={}",
                                    sc.getId(), highestBidder, highestBid, sc.getCourierReward());
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
                        // DELETE contract instead of moving to history
                        removeContract(sc.getId());
                        LOGGER.info("Deleted contract {} (no bids)", sc.getId());
                        savedData.setDirty();
                    }
                }
                // Check for Courier Acceptance Expiry (Auction closed, no courier, expired)
                else if (sc.isAuctionClosed() && !sc.isCourierAssigned() && sc.isExpired() && !sc.isCompleted()) {
                    // Assign Snail Mail
                    sc.setCourierId(SellContract.SNAIL_MAIL_UUID);
                    sc.setCourierAcceptedTime(System.currentTimeMillis());

                    // Extend expiry for Snail Mail delivery (2x courier time = 2 * 4 min = 8 min)
                    sc.extendExpiry(480000L);

                    LOGGER.info("Contract {} assigned to Snail Mail (courier acceptance expired)", sc.getId());
                    savedData.setDirty();
                    broadcastUpdate();
                }
                // Check for Snail Mail Delivery Completion (Snail Mail assigned, expired)
                else if (sc.isSnailMail() && sc.isExpired() && !sc.isCompleted()) {
                    // Snail Mail Delivery Complete
                    sc.complete();
                    LOGGER.info("Contract {} completed via Snail Mail delivery", sc.getId());
                    savedData.setDirty();
                    broadcastUpdate();
                }
            }
        }
    }

    public void addBid(UUID contractId, UUID bidder, float amount, ServerLevel level) {
        Contract contract = getContract(contractId);
        if (contract != null) {
            if (contract instanceof SellContract sc) {
                com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager
                        .get(level);

                // Check if this is a courier acceptance (contract has winner but no courier)
                if (sc.getWinningTownId() != null && !sc.isCourierAssigned() && !sc.isExpired()) {
                    // Handle courier assignment
                    handleCourierAcceptance(sc, contractId, bidder, level);
                    return;
                }

                // Otherwise, handle normal auction bidding
                com.quackers29.businesscraft.town.Town bidderTown = townManager.getTown(bidder);
                com.quackers29.businesscraft.town.Town sellerTown = townManager.getTown(sc.getIssuerTownId());

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

                // Calculate courier cost
                int courierCost = calculateCourierCost(bidderTown, sellerTown);

                // Round up bid amount (emeralds are integers)
                float roundedAmount = (float) Math.ceil(amount);
                int totalCost = (int) roundedAmount + courierCost;

                // Add the bid to the contract
                contract.addBid(bidder, bidderTown.getName(), roundedAmount);

                // ESCROW: Deduct total cost (Bid + Courier) from new bidder
                bidderTown.addResource(net.minecraft.world.item.Items.EMERALD, -totalCost);
                LOGGER.info("Escrowed {} emeralds ({} bid + {} courier) from town {} for contract {}",
                        totalCost, (int) roundedAmount, courierCost, bidderTown.getName(), contractId);

                // ESCROW: Refund emeralds to previous highest bidder
                if (previousHighestBidder != null) {
                    com.quackers29.businesscraft.town.Town previousTown = townManager.getTown(previousHighestBidder);
                    if (previousTown != null) {
                        int prevBid = (int) Math.ceil(previousHighestBid);
                        int prevCourierCost = calculateCourierCost(previousTown, sellerTown);
                        int refundAmount = prevBid + prevCourierCost;

                        previousTown.addResource(net.minecraft.world.item.Items.EMERALD, refundAmount);
                        LOGGER.info("Refunded {} emeralds ({} bid + {} courier) to town {} (outbid on contract {})",
                                refundAmount, prevBid, prevCourierCost, previousTown.getName(), contractId);
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
                    if (cc.getResourceId() != null) {
                        // Get town names for lore
                        String destTownName = "Unknown";
                        com.quackers29.businesscraft.town.Town destTown = townManager
                                .getTown(cc.getDestinationTownId());
                        if (destTown != null) {
                            destTownName = destTown.getName();
                        }

                        // Create contract item with NBT data and enchantments
                        net.minecraft.world.item.ItemStack contractItem = com.quackers29.businesscraft.util.ContractItemHelper
                                .createContractItem(
                                        cc.getResourceId(),
                                        cc.getQuantity(),
                                        contractId,
                                        cc.getDestinationTownId(),
                                        destTownName,
                                        sellerTown.getName());

                        // Create item stack list with the contract item
                        java.util.List<net.minecraft.world.item.ItemStack> rewards = new java.util.ArrayList<>();
                        rewards.add(contractItem);

                        // Add to payment board as a claimable reward for the courier
                        com.quackers29.businesscraft.town.data.RewardSource source = com.quackers29.businesscraft.town.data.RewardSource.COURIER_PICKUP;
                        java.util.UUID rewardId = sellerTown.getPaymentBoard().addReward(source, rewards,
                                bidder.toString());

                        if (rewardId != null) {
                            // Add metadata to link to contract
                            sellerTown.getPaymentBoard().getRewardById(rewardId).ifPresent(entry -> {
                                entry.addMetadata("contractId", contractId.toString());
                            });

                            LOGGER.info("Created courier pickup reward {} for town {} (courier={}) with contract item",
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

    public static int calculateCourierCost(com.quackers29.businesscraft.town.Town town1,
            com.quackers29.businesscraft.town.Town town2) {
        if (town1 == null || town2 == null)
            return 0;
        double distance = Math.sqrt(town1.getPosition().distSqr(town2.getPosition()));
        return (int) Math.ceil(distance / 10.0);
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
