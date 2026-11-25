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
    private final List<Contract> contracts;
    private final TownResolver towns;

    /** Town lookup that does not need a {@link ServerLevel}. The simulation uses this. */
    @FunctionalInterface
    public interface TownResolver {
        com.quackers29.businesscraft.town.Town town(UUID id);
    }

    private ContractBoard(ServerLevel level) {
        this.level = level;
        this.savedData = ContractSavedData.get(level);
        this.contracts = this.savedData.getContracts();
        this.towns = id -> id == null ? null
                : com.quackers29.businesscraft.town.TownManager.get(level).getTown(id);
    }

    private ContractBoard(TownResolver towns) {
        this.level = null;
        this.savedData = null;
        this.contracts = new ArrayList<>();
        this.towns = towns == null ? id -> null : towns;
    }

    /** In-memory board for the headless production simulation. Not stored on a world. */
    public static ContractBoard headless(TownResolver towns) {
        return new ContractBoard(towns);
    }

    public static ContractBoard get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, ContractBoard::new);
    }

    public static void clearInstances() {
        INSTANCES.clear();
    }

    public void addContract(Contract contract) {
        contracts.add(contract);
        markDirty();
        broadcastUpdate();
        if (contract instanceof SellContract sc) {
            com.quackers29.businesscraft.town.Town issuer = town(sc.getIssuerTownId());
            com.quackers29.businesscraft.debug.production.ProductionEvents.contract(
                    issuer, sc.getIssuerTownName(), sc.getIssuerTownId().toString(),
                    com.quackers29.businesscraft.debug.production.ProductionEvent.CONTRACT_LISTED,
                    sc.getId().toString(), sc.getResourceId(), sc.getQuantity(), sc.getPricePerUnit(),
                    "", "");
        }
    }

    public void removeContract(UUID contractId) {
        Contract existing = getContract(contractId);
        boolean removed = contracts.removeIf(c -> c.getId().equals(contractId));
        if (!removed) {
            return;
        }
        markDirty();
        broadcastUpdate();
        if (existing instanceof SellContract sc && !sc.isCompleted()) {
            com.quackers29.businesscraft.town.Town issuer = town(sc.getIssuerTownId());
            String type = sc.isExpired() && sc.getWinningTownId() == null
                    ? com.quackers29.businesscraft.debug.production.ProductionEvent.CONTRACT_EXPIRED
                    : com.quackers29.businesscraft.debug.production.ProductionEvent.CONTRACT_CANCELLED;
            com.quackers29.businesscraft.debug.production.ProductionEvents.contract(
                    issuer, sc.getIssuerTownName(),
                    sc.getIssuerTownId() == null ? "" : sc.getIssuerTownId().toString(),
                    type, sc.getId().toString(), sc.getResourceId(), sc.getQuantity(), sc.getPricePerUnit(),
                    "", "");
        }
    }

    public void updateContract(Contract contract) {
        markDirty();
    }

    public Contract getContract(UUID contractId) {
        for (Contract c : contracts) {
            if (c.getId().equals(contractId)) {
                return c;
            }
        }
        return null;
    }

    private com.quackers29.businesscraft.town.Town town(UUID id) {
        if (id == null || towns == null) {
            return null;
        }
        return towns.town(id);
    }

    private void markDirty() {
        if (savedData != null) {
            savedData.setDirty();
        }
    }

    public List<Contract> getContracts() {
        return Collections.unmodifiableList(contracts);
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

        for (Contract contract : contracts) {
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
            markDirty();
            broadcastUpdate();

            com.quackers29.businesscraft.town.Town destination = town(sc.getWinningTownId());

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
                markDirty();
                broadcastUpdate();

                com.quackers29.businesscraft.town.Town destTown = town(sc.getWinningTownId());

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
            markDirty();
            broadcastUpdate();

            com.quackers29.businesscraft.town.Town destination = town(cc.getDestinationTownId());

            if (destination != null && cc.getResourceId() != null) {
                net.minecraft.world.item.Item resourceItem = com.quackers29.businesscraft.util.ContractItemHelper
                        .getBaseItemForResource(cc.getResourceId());
                if (resourceItem != net.minecraft.world.item.Items.PAPER) {
                    destination.addResource(resourceItem, amount);
                }
            }

            if (cc.isDelivered()) {
                cc.complete();
                markDirty();
                broadcastUpdate();

                com.quackers29.businesscraft.town.Town destTown = town(cc.getDestinationTownId());

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

        com.quackers29.businesscraft.town.Town sellerTown = town(sc.getIssuerTownId());

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
        sc.setCourierAcceptedTime(com.quackers29.businesscraft.util.GameClock.millis());

        double distance = 0;
        com.quackers29.businesscraft.town.Town destTown = town(sc.getWinningTownId());
        if (destTown != null) {
            distance = Math.sqrt(sellerTown.getPosition().distSqr(destTown.getPosition()));
        }
        long deliveryDuration = (long) (distance
                * com.quackers29.businesscraft.config.ConfigLoader.contractCourierDeliveryMinutesPerMeter * 60000L);

        sc.extendExpiry(deliveryDuration);
        String destTownName = sc.getWinningTownName() != null ? sc.getWinningTownName() : "Unknown";
        com.quackers29.businesscraft.debug.production.ProductionEvents.contract(
                sellerTown, sellerTown.getName(), sellerTown.getId().toString(),
                com.quackers29.businesscraft.debug.production.ProductionEvent.CONTRACT_COURIER_DISPATCHED,
                sc.getId().toString(), sc.getResourceId(), sc.getQuantity(), sc.getPricePerUnit(),
                sc.getWinningTownId() == null ? courierId.toString() : sc.getWinningTownId().toString(),
                destTown == null ? destTownName : destTown.getName());

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

        markDirty();
        broadcastUpdate();
    }

    private void closeAuctions() {
        List<Contract> contractsCopy = new ArrayList<>(contracts);

        for (Contract contract : contractsCopy) {
            if (contract instanceof SellContract sc) {
                if (sc.isExpired() && !sc.isCompleted() && sc.getWinningTownId() == null) {
                    if (!sc.getBids().isEmpty()) {
                        UUID highestBidder = sc.getHighestBidder();
                        float highestBid = sc.getHighestBid();

                        if (highestBidder != null) {
                            com.quackers29.businesscraft.town.Town winnerTown = town(highestBidder);
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

                            com.quackers29.businesscraft.town.Town sellerTown = town(sc.getIssuerTownId());

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

                            markDirty();
                            com.quackers29.businesscraft.debug.production.ProductionEvents.contract(
                                    winnerTown, winnerTown == null ? "" : winnerTown.getName(),
                                    highestBidder.toString(),
                                    com.quackers29.businesscraft.debug.production.ProductionEvent.CONTRACT_WON,
                                    sc.getId().toString(), sc.getResourceId(), sc.getQuantity(),
                                    sc.getQuantity() == 0 ? 0 : highestBid / sc.getQuantity(),
                                    sc.getIssuerTownId() == null ? "" : sc.getIssuerTownId().toString(),
                                    sellerTown == null ? sc.getIssuerTownName() : sellerTown.getName());
                        }
                    } else {
                        com.quackers29.businesscraft.town.Town sellerTown = town(sc.getIssuerTownId());
                        if (sellerTown != null) {
                            net.minecraft.world.item.Item item = com.quackers29.businesscraft.util.ContractItemHelper
                                    .getBaseItemForResource(sc.getResourceId());

                            if (item != null && item != net.minecraft.world.item.Items.PAPER) {
                                sellerTown.addResource(item, sc.getQuantity());
                                sellerTown.removeEscrowResource(item, sc.getQuantity());

                                // Apply supply pressure: failed auction drops price slightly
                                com.quackers29.businesscraft.economy.GlobalMarket.get().recordFailedAuction(sc.getResourceId());

                                DebugConfig.debug(LOGGER, DebugConfig.GLOBAL_MARKET,
                                        "Refunded {} {} to town {} (auction {} had no bids). GPI reduced due to oversupply.",
                                        sc.getQuantity(), sc.getResourceId(), sellerTown.getName(), sc.getId());
                            }
                        }
                        removeContract(sc.getId());
                        markDirty();
                    }
                }
                else if (sc.isAuctionClosed() && !sc.isCourierAssigned() && sc.isExpired() && !sc.isCompleted()) {
                    sc.setCourierId(SellContract.SNAIL_MAIL_UUID);
                    sc.setCourierAcceptedTime(com.quackers29.businesscraft.util.GameClock.millis());

                    double distance = 0;
                    com.quackers29.businesscraft.town.Town seller = town(sc.getIssuerTownId());
                    com.quackers29.businesscraft.town.Town winner = town(sc.getWinningTownId());
                    if (seller != null && winner != null) {
                        distance = Math.sqrt(seller.getPosition().distSqr(winner.getPosition()));
                    }
                    long smDuration = (long) (distance
                            * com.quackers29.businesscraft.config.ConfigLoader.contractSnailMailDeliveryMinutesPerMeter
                            * 60000L);

                    sc.extendExpiry(smDuration);

                    com.quackers29.businesscraft.debug.production.ProductionEvents.contract(
                            seller, seller == null ? sc.getIssuerTownName() : seller.getName(),
                            sc.getIssuerTownId() == null ? "" : sc.getIssuerTownId().toString(),
                            com.quackers29.businesscraft.debug.production.ProductionEvent.CONTRACT_COURIER_DISPATCHED,
                            sc.getId().toString(), sc.getResourceId(), sc.getQuantity(), sc.getPricePerUnit(),
                            SellContract.SNAIL_MAIL_UUID.toString(), "snail_mail");

                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                            "Contract {} assigned to Snail Mail (courier acceptance expired)", sc.getId());
                    markDirty();
                    broadcastUpdate();
                }
                else if (sc.isSnailMail() && sc.isExpired() && !sc.isCompleted()) {
                    sc.complete();
                    markDirty();
                    broadcastUpdate();
                }
            }
        }
    }

    private void processContractDelivery(SellContract sc, ServerLevel ignoredLevel) {
        sc.setDelivered(true);
        markDirty();

        if (sc.getWinningTownId() == null) {
            return;
        }

        com.quackers29.businesscraft.town.Town buyerTown = town(sc.getWinningTownId());
        com.quackers29.businesscraft.town.Town sellerTown = town(sc.getIssuerTownId());

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
        com.quackers29.businesscraft.debug.production.ProductionEvents.contract(
                buyerTown, buyerTown == null ? "" : buyerTown.getName(),
                sc.getWinningTownId().toString(),
                com.quackers29.businesscraft.debug.production.ProductionEvent.CONTRACT_DELIVERED,
                sc.getId().toString(), sc.getResourceId(), sc.getQuantity(), sc.getPricePerUnit(),
                sc.getIssuerTownId() == null ? "" : sc.getIssuerTownId().toString(),
                sellerTown == null ? sc.getIssuerTownName() : sellerTown.getName());
    }

    public void addBid(UUID contractId, UUID bidder, float amount, ServerLevel level) {
        Contract contract = getContract(contractId);
        if (contract != null) {
            if (contract instanceof SellContract sc) {
                if (sc.getWinningTownId() != null && !sc.isCourierAssigned() && !sc.isExpired()) {
                    if (level == null) {
                        return;
                    }
                    handleCourierAcceptance(sc, contractId, bidder, level);
                    return;
                }

                com.quackers29.businesscraft.town.Town bidderTown = town(bidder);
                com.quackers29.businesscraft.town.Town sellerTown = town(sc.getIssuerTownId());

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
                    com.quackers29.businesscraft.town.Town previousTown = town(previousHighestBidder);
                    if (previousTown != null) {
                        int prevBid = (int) Math.ceil(previousHighestBid);
                        int prevCourierCost = calculateCourierCost(previousTown, sellerTown);
                        int refundAmount = prevBid + prevCourierCost;

                        previousTown.addResource(net.minecraft.world.item.Items.EMERALD, refundAmount);
                        previousTown.removeEscrowResource(net.minecraft.world.item.Items.EMERALD, refundAmount);
                    }
                }

                markDirty();
                broadcastUpdate();
                com.quackers29.businesscraft.debug.production.ProductionEvents.contract(
                        bidderTown, bidderTown.getName(), bidderTown.getId().toString(),
                        com.quackers29.businesscraft.debug.production.ProductionEvent.CONTRACT_BID,
                        sc.getId().toString(), sc.getResourceId(), sc.getQuantity(), roundedAmount / Math.max(1L, sc.getQuantity()),
                        sc.getIssuerTownId() == null ? "" : sc.getIssuerTownId().toString(),
                        sellerTown == null ? sc.getIssuerTownName() : sellerTown.getName());
            } else if (contract instanceof CourierContract cc) {
                if (!cc.isAccepted() && !cc.isExpired()) {
                    if (level == null) {
                        return;
                    }
                    net.minecraft.server.level.ServerPlayer player = level.getServer().getPlayerList()
                            .getPlayer(bidder);
                    if (player == null) {
                        LOGGER.warn("Cannot accept courier contract: player {} not found", bidder);
                        return;
                    }

                    com.quackers29.businesscraft.town.Town sellerTown = town(cc.getIssuerTownId());

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
                    cc.setAcceptedTime(com.quackers29.businesscraft.util.GameClock.millis());

                    double distance = 0;
                    com.quackers29.businesscraft.town.Town destTown = town(cc.getDestinationTownId());
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

                    markDirty();
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
            if (PlatformAccess.networkMessages != null) {
                Map<String, Float> prices = getAllMarketPrices();
                PlatformAccess.getNetworkMessages().sendToAllPlayers(
                        new com.quackers29.businesscraft.network.packets.ui.ContractSyncPacket(
                                contracts, prices));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to broadcast contract update", e);
        }
    }
}
