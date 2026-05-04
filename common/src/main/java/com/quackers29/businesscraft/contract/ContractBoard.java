package com.quackers29.businesscraft.contract;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.data.ContractSavedData;
import com.quackers29.businesscraft.debug.DebugConfig;
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
        return com.quackers29.businesscraft.economy.GlobalMarket.get().getPrice(resourceId);
    }

    public Map<String, Float> getAllMarketPrices() {
        Map<String, Float> merged = new HashMap<>(
                com.quackers29.businesscraft.production.ProductionRegistry.getAllEstimatedValues());
        merged.putAll(com.quackers29.businesscraft.economy.GlobalMarket.get().getPrices());
        return Collections.unmodifiableMap(merged);
    }

    public void updateMarketPrice(String resourceId, float quantity, float transactionPrice) {
        com.quackers29.businesscraft.economy.GlobalMarket.get().recordTrade(resourceId, quantity, transactionPrice);
    }

    /**
     * @deprecated Use updateMarketPrice(String, float, float) instead
     */
    @Deprecated
    public void updateMarketPrice(String resourceId, float transactionPrice) {
        updateMarketPrice(resourceId, 1.0f, transactionPrice);
    }

    public void tick(ServerLevel level) {
        closeAuctions();

        for (Contract contract : savedData.getContracts()) {
            if (contract instanceof SellContract sc) {
                if (sc.isCompleted() && !sc.isDelivered()) {
                    processContractDelivery(sc, level);
                }
            }
        }
    }

    public void processCourierDelivery(UUID contractId, long amount) {
        Contract contract = getContract(contractId);

        if (contract instanceof SellContract sc) {
            sc.addDeliveredAmount(amount);
            savedData.setDirty();
            broadcastUpdate();

            com.quackers29.businesscraft.town.TownManager manager = com.quackers29.businesscraft.town.TownManager
                    .get(level);
            com.quackers29.businesscraft.town.Town destination = manager.getTown(sc.getWinningTownId());

            if (destination != null && sc.getResourceId() != null) {
                net.minecraft.world.item.Item resourceItem = com.quackers29.businesscraft.util.ContractItemHelper
                        .getBaseItemForResource(sc.getResourceId());
                if (resourceItem != net.minecraft.world.item.Items.PAPER) {
                    destination.addResource(resourceItem, amount);
                }
            }

            if (sc.isDeliveryComplete() && !sc.isCompleted()) {
                sc.complete();
                sc.expireNow();
                savedData.setDirty();
                broadcastUpdate();

                com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager
                        .get(level);
                com.quackers29.businesscraft.town.Town destTown = townManager.getTown(sc.getWinningTownId());

                if (destTown != null && sc.getCourierId() != null) {
                    if (sc.isSnailMail()) {
                    } else {
                        int rewardAmount = (int) sc.getCourierReward();
                        if (rewardAmount > 0) {
                            net.minecraft.world.item.ItemStack emeralds = new net.minecraft.world.item.ItemStack(
                                    net.minecraft.world.item.Items.EMERALD, rewardAmount);
                            List<net.minecraft.world.item.ItemStack> rewards = new ArrayList<>();
                            rewards.add(emeralds);

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
        else if (contract instanceof CourierContract cc) {
            cc.addDeliveredAmount(amount);
            savedData.setDirty();
            broadcastUpdate();

            com.quackers29.businesscraft.town.TownManager manager = com.quackers29.businesscraft.town.TownManager
                    .get(level);
            com.quackers29.businesscraft.town.Town destination = manager.getTown(cc.getDestinationTownId());

            if (destination != null && cc.getResourceId() != null) {
                net.minecraft.world.item.Item resourceItem = com.quackers29.businesscraft.util.ContractItemHelper
                        .getBaseItemForResource(cc.getResourceId());
                if (resourceItem != net.minecraft.world.item.Items.PAPER) {
                    destination.addResource(resourceItem, amount);
                }
            }

            if (cc.isDelivered()) {
                cc.complete();
                savedData.setDirty();
                broadcastUpdate();

                com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager
                        .get(level);
                com.quackers29.businesscraft.town.Town destTown = townManager.getTown(cc.getDestinationTownId());

                if (destTown != null && cc.getCourierId() != null) {
                    int rewardAmount = (int) cc.getReward();
                    if (rewardAmount > 0) {
                        net.minecraft.world.item.ItemStack emeralds = new net.minecraft.world.item.ItemStack(
                                net.minecraft.world.item.Items.EMERALD, rewardAmount);
                        List<net.minecraft.world.item.ItemStack> rewards = new ArrayList<>();
                        rewards.add(emeralds);

                        com.quackers29.businesscraft.town.data.RewardSource source = com.quackers29.businesscraft.town.data.RewardSource.COURIER_DELIVERY;
                        UUID rewardId = destTown.getPaymentBoard().addReward(source, rewards,
                                cc.getCourierId().toString());

                        if (rewardId != null) {
                            destTown.getPaymentBoard().getRewardById(rewardId).ifPresent(entry -> {
                                entry.addMetadata("contractId", contractId.toString());
                            });
                        }
                    }
                }
            }
        }
    }

    private void handleCourierAcceptance(SellContract sc, UUID contractId, UUID courierId, ServerLevel level) {
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

        double distSqr = player.blockPosition().distSqr(sellerTown.getPosition());
        double maxDist = sellerTown.getBoundaryRadius() + 10;
        if (distSqr > maxDist * maxDist) {
            LOGGER.warn("Cannot accept courier job: player too far from seller town (dist={}, max={})",
                    Math.sqrt(distSqr), maxDist);
            return;
        }

        sc.setCourierId(courierId);
        sc.setCourierAcceptedTime(System.currentTimeMillis());
        sc.setCourierAcceptedTime(System.currentTimeMillis());

        double distance = 0;
        com.quackers29.businesscraft.town.Town destTown = townManager.getTown(sc.getWinningTownId());
        if (destTown != null) {
            distance = Math.sqrt(sellerTown.getPosition().distSqr(destTown.getPosition()));
        }
        long deliveryDuration = (long) (distance
                * com.quackers29.businesscraft.config.ConfigLoader.contractCourierDeliveryMinutesPerMeter * 60000L);

        sc.extendExpiry(deliveryDuration);

        String destTownName = sc.getWinningTownName() != null ? sc.getWinningTownName() : "Unknown";
        net.minecraft.world.item.ItemStack contractItem = com.quackers29.businesscraft.util.ContractItemHelper
                .createContractItem(
                        sc.getResourceId(),
                        (int) sc.getQuantity(),
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
                if (sc.isExpired() && !sc.isCompleted() && sc.getWinningTownId() == null) {
                    if (!sc.getBids().isEmpty()) {
                        UUID highestBidder = sc.getHighestBidder();
                        float highestBid = sc.getHighestBid();

                        if (highestBidder != null) {
                            com.quackers29.businesscraft.town.Town winnerTown = townManager.getTown(highestBidder);
                            if (winnerTown != null) {
                                sc.setWinningTown(highestBidder, winnerTown.getName());
                            } else {
                                sc.setWinningTownId(highestBidder);
                            }

                            sc.setAcceptedBid(highestBid);

                            if (sc.getQuantity() > 0 && highestBid > 0) {
                                float transactionPrice = highestBid / (float) sc.getQuantity();
                                updateMarketPrice(sc.getResourceId(), (float) sc.getQuantity(), transactionPrice);
                            }

                            com.quackers29.businesscraft.town.Town sellerTown = townManager
                                    .getTown(sc.getIssuerTownId());

                            if (winnerTown != null && sellerTown != null) {
                                int cost = (int) highestBid;
                                if (cost > 0) {
                                    sellerTown.addResource(net.minecraft.world.item.Items.EMERALD, cost);
                                    winnerTown.removeEscrowResource(net.minecraft.world.item.Items.EMERALD, cost);

                                }

                                net.minecraft.world.item.Item item = com.quackers29.businesscraft.util.ContractItemHelper
                                        .getBaseItemForResource(sc.getResourceId());
                                if (item != null && item != net.minecraft.world.item.Items.PAPER) {
                                    sellerTown.removeEscrowResource(item, sc.getQuantity());
                                }
                            }

                            // Set courier reward (Distance based)
                            int courierCost = calculateCourierCost(winnerTown, sellerTown);
                            sc.setCourierReward(courierCost);

                            sc.extendExpiry(
                                    (long) (com.quackers29.businesscraft.config.ConfigLoader.contractCourierAcceptanceMinutes
                                            * 60000L));

                            savedData.setDirty();
                        }
                    } else {
                        com.quackers29.businesscraft.town.Town sellerTown = townManager.getTown(sc.getIssuerTownId());
                        if (sellerTown != null) {
                            net.minecraft.world.item.Item item = com.quackers29.businesscraft.util.ContractItemHelper
                                    .getBaseItemForResource(sc.getResourceId());

                            if (item != null && item != net.minecraft.world.item.Items.PAPER) {
                                sellerTown.addResource(item, sc.getQuantity());
                                sellerTown.removeEscrowResource(item, sc.getQuantity());

                                // Apply supply pressure: failed auction drops price slightly
                                com.quackers29.businesscraft.economy.GlobalMarket.get().recordFailedAuction(sc.getResourceId());

                                LOGGER.info(
                                        "Refunded {} {} to town {} (auction {} had no bids). GPI reduced due to oversupply.",
                                        sc.getQuantity(), sc.getResourceId(), sellerTown.getName(), sc.getId());
                            }
                        }
                        removeContract(sc.getId());
                        savedData.setDirty();
                    }
                }
                else if (sc.isAuctionClosed() && !sc.isCourierAssigned() && sc.isExpired() && !sc.isCompleted()) {
                    sc.setCourierId(SellContract.SNAIL_MAIL_UUID);
                    sc.setCourierAcceptedTime(System.currentTimeMillis());

                    double distance = 0;
                    com.quackers29.businesscraft.town.Town seller = townManager.getTown(sc.getIssuerTownId());
                    com.quackers29.businesscraft.town.Town winner = townManager.getTown(sc.getWinningTownId());
                    if (seller != null && winner != null) {
                        distance = Math.sqrt(seller.getPosition().distSqr(winner.getPosition()));
                    }
                    long smDuration = (long) (distance
                            * com.quackers29.businesscraft.config.ConfigLoader.contractSnailMailDeliveryMinutesPerMeter
                            * 60000L);

                    sc.extendExpiry(smDuration);

                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                            "Contract {} assigned to Snail Mail (courier acceptance expired)", sc.getId());
                    savedData.setDirty();
                    broadcastUpdate();
                }
                else if (sc.isSnailMail() && sc.isExpired() && !sc.isCompleted()) {
                    sc.complete();
                    savedData.setDirty();
                    broadcastUpdate();
                }
            }
        }
    }

    private void processContractDelivery(SellContract sc, ServerLevel level) {
        sc.setDelivered(true);
        savedData.setDirty();

        com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager
                .get(level);

        if (sc.getWinningTownId() == null) {
            return;
        }

        com.quackers29.businesscraft.town.Town buyerTown = townManager.getTown(sc.getWinningTownId());

        if (buyerTown != null) {
            net.minecraft.world.item.Item item = com.quackers29.businesscraft.util.ContractItemHelper
                    .getBaseItemForResource(sc.getResourceId());

            if (item != null && item != net.minecraft.world.item.Items.PAPER) {
                long amountRemaining = sc.getQuantity() - sc.getDeliveredAmount();

                if (amountRemaining > 0) {
                    buyerTown.addResource(item, amountRemaining);
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

                if (sc.getWinningTownId() != null && !sc.isCourierAssigned() && !sc.isExpired()) {
                    handleCourierAcceptance(sc, contractId, bidder, level);
                    return;
                }

                com.quackers29.businesscraft.town.Town bidderTown = townManager.getTown(bidder);
                com.quackers29.businesscraft.town.Town sellerTown = townManager.getTown(sc.getIssuerTownId());

                if (bidderTown == null) {
                    LOGGER.warn("Cannot place bid: bidder town {} not found", bidder);
                    return;
                }

                if (bidder.equals(contract.getIssuerTownId())) {
                    LOGGER.warn("Town {} attempted to bid on their own contract {}", bidderTown.getName(), contractId);
                    return;
                }

                UUID previousHighestBidder = contract.getHighestBidder();
                float previousHighestBid = contract.getHighestBid();

                int courierCost = calculateCourierCost(bidderTown, sellerTown);

                float roundedAmount = (float) Math.ceil(amount);
                int totalCost = (int) roundedAmount + courierCost;

                contract.addBid(bidder, bidderTown.getName(), roundedAmount);

                bidderTown.addResource(net.minecraft.world.item.Items.EMERALD, -totalCost);
                bidderTown.addEscrowResource(net.minecraft.world.item.Items.EMERALD, totalCost);

                if (previousHighestBidder != null) {
                    com.quackers29.businesscraft.town.Town previousTown = townManager.getTown(previousHighestBidder);
                    if (previousTown != null) {
                        int prevBid = (int) Math.ceil(previousHighestBid);
                        int prevCourierCost = calculateCourierCost(previousTown, sellerTown);
                        int refundAmount = prevBid + prevCourierCost;

                        previousTown.addResource(net.minecraft.world.item.Items.EMERALD, refundAmount);
                        previousTown.removeEscrowResource(net.minecraft.world.item.Items.EMERALD, refundAmount);
                    }
                }

                savedData.setDirty();
                broadcastUpdate();
            } else if (contract instanceof CourierContract cc) {
                if (!cc.isAccepted() && !cc.isExpired()) {
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

                    double distSqr = player.blockPosition().distSqr(sellerTown.getPosition());
                    double maxDist = sellerTown.getBoundaryRadius() + 10;
                    if (distSqr > maxDist * maxDist) {
                        LOGGER.warn("Cannot accept courier contract: player too far from seller town (dist={}, max={})",
                                Math.sqrt(distSqr), maxDist);
                        return;
                    }

                    cc.setCourierId(bidder);
                    cc.setAcceptedTime(System.currentTimeMillis());

                    double distance = 0;
                    com.quackers29.businesscraft.town.Town destTown = townManager.getTown(cc.getDestinationTownId());
                    if (destTown != null) {
                        distance = Math.sqrt(sellerTown.getPosition().distSqr(destTown.getPosition()));
                    }
                    long deliveryDuration = (long) (distance
                            * com.quackers29.businesscraft.config.ConfigLoader.contractCourierDeliveryMinutesPerMeter
                            * 60000L);

                    cc.extendExpiry(deliveryDuration);

                    if (cc.getResourceId() != null) {
                        String destTownName = "Unknown";
                        if (destTown != null) {
                            destTownName = destTown.getName();
                        }

                        net.minecraft.world.item.ItemStack contractItem = com.quackers29.businesscraft.util.ContractItemHelper
                                .createContractItem(
                                        cc.getResourceId(),
                                        (int) cc.getQuantity(),
                                        contractId,
                                        cc.getDestinationTownId(),
                                        destTownName,
                                        sellerTown.getName());

                        java.util.List<net.minecraft.world.item.ItemStack> rewards = new java.util.ArrayList<>();
                        rewards.add(contractItem);

                        com.quackers29.businesscraft.town.data.RewardSource source = com.quackers29.businesscraft.town.data.RewardSource.COURIER_PICKUP;
                        java.util.UUID rewardId = sellerTown.getPaymentBoard().addReward(source, rewards,
                                bidder.toString());

                        if (rewardId != null) {
                            sellerTown.getPaymentBoard().getRewardById(rewardId).ifPresent(entry -> {
                                entry.addMetadata("contractId", contractId.toString());
                            });

                        } else {
                            LOGGER.warn("Failed to create courier pickup reward for town {}", sellerTown.getName());
                        }
                    }

                    savedData.setDirty();
                    broadcastUpdate();
                }
            }
        }
    }

    public static int calculateCourierCost(com.quackers29.businesscraft.town.Town town1,
            com.quackers29.businesscraft.town.Town town2) {
        if (town1 == null || town2 == null) {
            return 0;
        }
        double distance = Math.sqrt(town1.getPosition().distSqr(town2.getPosition()));
        return (int) Math.ceil(distance / 10.0);
    }

    private void broadcastUpdate() {
        try {
            if (PlatformAccess.getNetworkMessages() != null) {
                Map<String, Float> prices = getAllMarketPrices();
                PlatformAccess.getNetworkMessages().sendToAllPlayers(
                        new com.quackers29.businesscraft.network.packets.ui.ContractSyncPacket(
                                savedData.getContracts(), prices));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to broadcast contract update", e);
        }
    }
}
