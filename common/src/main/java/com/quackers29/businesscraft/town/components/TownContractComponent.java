package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class TownContractComponent implements TownComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownContractComponent.class);

    private final Town town;
    private long lastContractCheckTime = 0;
    private static final int CHECK_INTERVAL = 100; // Check every 100 ticks (5 seconds)
    private static final int EXCESS_THRESHOLD = 200; // Create contract if resource > 200
    private static final int NEED_THRESHOLD = 100; // Bid if resource < 100
    private static final int MAX_ACTIVE_CONTRACTS = 3;

    public TownContractComponent(Town town) {
        this.town = town;
    }

    private java.util.Map<UUID, Long> pendingBids = new java.util.HashMap<>();

    @Override
    public void tick() {
        long currentTime = System.currentTimeMillis();

        // Process pending bids every tick (or every few ticks)
        processPendingBids(currentTime);

        // Check every CHECK_INTERVAL ticks
        if (currentTime - lastContractCheckTime < CHECK_INTERVAL * 50) { // 50ms per tick approx
            return;
        }

        lastContractCheckTime = currentTime;

        tryCreateContract();
        scanForBids(currentTime);
    }

    private static final float MAX_BID_MULTIPLIER = 3.0f; // Don't pay more than 3x base price

    private void processPendingBids(long currentTime) {
        if (pendingBids.isEmpty())
            return;

        // Get the server level
        net.minecraft.server.level.ServerLevel level = null;
        for (com.quackers29.businesscraft.town.TownManager manager : com.quackers29.businesscraft.town.TownManager
                .getAllInstances()) {
            if (manager.getTown(town.getId()) != null) {
                level = manager.getLevel();
                break;
            }
        }

        if (level == null)
            return;
        ContractBoard board = ContractBoard.get(level);

        java.util.Iterator<java.util.Map.Entry<UUID, Long>> iterator = pendingBids.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<UUID, Long> entry = iterator.next();
            if (currentTime >= entry.getValue()) {
                UUID contractId = entry.getKey();
                iterator.remove();

                // Re-evaluate and place bid
                Contract contract = board.getContract(contractId);
                if (contract instanceof SellContract sc && !sc.isExpired()) {
                    // Don't bid if auction is closed (courier phase)
                    if (sc.isAuctionClosed()) {
                        continue;
                    }

                    // Check if already the highest bidder (again, just in case)
                    if (town.getId().equals(sc.getHighestBidder())) {
                        continue;
                    }

                    // Calculate bid again to be fresh
                    // Calculate bid again to be fresh
                    float currentHighest = sc.getHighestBid();
                    float basePrice = sc.getPricePerUnit() * sc.getQuantity();

                    // Calculate courier cost
                    com.quackers29.businesscraft.town.Town sellerTown = com.quackers29.businesscraft.town.TownManager
                            .get(level).getTown(sc.getIssuerTownId());
                    int courierCost = ContractBoard.calculateCourierCost(town, sellerTown);

                    float bid = (float) Math.ceil((currentHighest > 0 ? currentHighest : basePrice) * 1.1f);

                    // Check max bid limit (Total budget = 3x base price)
                    float maxTotalBudget = basePrice * MAX_BID_MULTIPLIER;
                    float maxBid = maxTotalBudget - courierCost;

                    if (bid > maxBid) {
                        LOGGER.debug("Town {} skipping bid on {} - total cost too high (Bid {} + Courier {} > {})",
                                town.getName(), sc.getId(), bid, courierCost, maxTotalBudget);
                        continue;
                    }

                    // ESCROW: Check if town has enough emeralds before bidding (Bid + Courier)
                    int emeraldCount = town.getResourceCount(Items.EMERALD);
                    if (emeraldCount < (bid + courierCost)) {
                        LOGGER.debug(
                                "Town {} cannot bid {} on contract {} - insufficient emeralds ({} available, need {})",
                                town.getName(), bid, sc.getId(), emeraldCount, (int) (bid + courierCost));
                        continue;
                    }

                    // Place bid
                    board.addBid(sc.getId(), town.getId(), bid, level);
                    LOGGER.info("Town {} bid {} on contract {} (after delay)",
                            town.getName(), bid, sc.getId());
                }
            }
        }
    }

    private void scanForBids(long currentTime) {
        // Get the server level
        net.minecraft.server.level.ServerLevel level = null;
        for (com.quackers29.businesscraft.town.TownManager manager : com.quackers29.businesscraft.town.TownManager
                .getAllInstances()) {
            if (manager.getTown(town.getId()) != null) {
                level = manager.getLevel();
                break;
            }
        }

        if (level == null) {
            return;
        }

        ContractBoard board = ContractBoard.get(level);
        List<Contract> contracts = board.getContracts();

        for (Contract contract : contracts) {
            if (contract instanceof SellContract sc) {
                // Don't bid on own contracts
                if (sc.getIssuerTownId().equals(town.getId())) {
                    continue;
                }

                // Don't bid on expired contracts
                if (sc.isExpired()) {
                    continue;
                }

                // Don't bid if auction is closed (courier phase)
                if (sc.isAuctionClosed()) {
                    continue;
                }

                // Don't schedule if already pending
                if (pendingBids.containsKey(sc.getId())) {
                    continue;
                }

                // Check if town needs this resource
                String resourceId = sc.getResourceId();

                // Look up the resource type from ResourceRegistry
                com.quackers29.businesscraft.economy.ResourceType resourceType = com.quackers29.businesscraft.economy.ResourceRegistry
                        .get(resourceId);

                // Skip if resource not found in registry
                if (resourceType == null) {
                    continue;
                }

                // Get the canonical item for this resource
                net.minecraft.resources.ResourceLocation itemLoc = resourceType.getCanonicalItemId();
                Object itemObj = com.quackers29.businesscraft.api.PlatformAccess.getRegistry().getItem(itemLoc);

                if (!(itemObj instanceof Item)) {
                    continue;
                }

                Item item = (Item) itemObj;
                int currentCount = town.getResourceCount(item);

                if (item != null && currentCount < NEED_THRESHOLD) {
                    // Check if already the highest bidder
                    if (town.getId().equals(sc.getHighestBidder())) {
                        continue;
                    }

                    // Pre-check max bid (approximate)
                    float currentHighest = sc.getHighestBid();
                    float basePrice = sc.getPricePerUnit() * sc.getQuantity();

                    // Calculate courier cost
                    com.quackers29.businesscraft.town.Town sellerTown = com.quackers29.businesscraft.town.TownManager
                            .get(level).getTown(sc.getIssuerTownId());
                    int courierCost = ContractBoard.calculateCourierCost(town, sellerTown);

                    float projectedBid = (float) Math.ceil((currentHighest > 0 ? currentHighest : basePrice) * 1.1f);

                    // Check max bid limit (Total budget = 3x base price)
                    float maxTotalBudget = basePrice * MAX_BID_MULTIPLIER;
                    float maxBid = maxTotalBudget - courierCost;

                    if (projectedBid > maxBid) {
                        continue;
                    }

                    // Schedule bid with 1s delay
                    pendingBids.put(sc.getId(), currentTime + 1000);
                }
            }
        }
    }

    private void tryCreateContract() {
        // Get the server level from any of the active TownManager instances
        // Since this town exists, we know its manager is loaded
        net.minecraft.server.level.ServerLevel level = null;
        for (com.quackers29.businesscraft.town.TownManager manager : com.quackers29.businesscraft.town.TownManager
                .getAllInstances()) {
            if (manager.getTown(town.getId()) != null) {
                // Found our manager, get its level
                level = getServerLevelFromManager(manager);
                break;
            }
        }

        if (level == null) {
            LOGGER.warn("Could not find ServerLevel for town {}", town.getName());
            return;
        }

        ContractBoard board = ContractBoard.get(level);
        long activeCount = board.getContracts().stream()
                .filter(c -> c instanceof SellContract sc &&
                        sc.getIssuerTownId().equals(town.getId()) &&
                        !sc.isExpired() && !sc.isCompleted())
                .count();

        if (activeCount >= MAX_ACTIVE_CONTRACTS) {
            return;
        }

        // Check all resources for excess (excluding currency)
        for (java.util.Map.Entry<Item, Integer> entry : town.getAllResources().entrySet()) {
            Item item = entry.getKey();

            // Skip emeralds - they are the currency
            if (item == Items.EMERALD) {
                continue;
            }

            // Look up the resource type from ResourceRegistry
            com.quackers29.businesscraft.economy.ResourceType resourceType = com.quackers29.businesscraft.economy.ResourceRegistry
                    .getFor(item);

            // Only create contracts for registered resources
            if (resourceType == null) {
                continue;
            }

            String resourceId = resourceType.getId();
            checkAndCreateContract(board, level, resourceId, item);
        }
    }

    private net.minecraft.server.level.ServerLevel getServerLevelFromManager(
            com.quackers29.businesscraft.town.TownManager manager) {
        return manager.getLevel();
    }

    private void checkAndCreateContract(ContractBoard board, net.minecraft.server.level.ServerLevel level,
            String resourceId, Item item) {
        // Check if already at max contracts
        long activeCount = board.getContracts().stream()
                .filter(c -> c instanceof SellContract sc &&
                        sc.getIssuerTownId().equals(town.getId()) &&
                        !sc.isExpired() && !sc.isCompleted())
                .count();

        if (activeCount >= MAX_ACTIVE_CONTRACTS) {
            return;
        }

        int resourceCount = town.getResourceCount(item);

        if (resourceCount > EXCESS_THRESHOLD) {
            // Check if town already has an active contract for this resource
            boolean hasContract = board.getContracts().stream()
                    .anyMatch(c -> c instanceof SellContract sc &&
                            sc.getIssuerTownId().equals(town.getId()) &&
                            resourceId.equals(sc.getResourceId()) &&
                            !sc.isExpired());

            if (hasContract) {
                LOGGER.debug("Town {} already has an active {} contract, skipping creation",
                        town.getName(), resourceId);
                return;
            }

            // Create a sell contract for excess resource
            int sellQuantity = (resourceCount - EXCESS_THRESHOLD) / 2;
            if (sellQuantity <= 0) {
                LOGGER.debug("Insufficient excess resources for {} ({} - {} = {}), skipping contract creation",
                        resourceId, resourceCount, EXCESS_THRESHOLD, sellQuantity);
                return;
            }
            float pricePerUnit = 1.0f;

            SellContract contract = new SellContract(
                    town.getId(),
                    town.getName(),
                    60000L,
                    resourceId,
                    sellQuantity,
                    pricePerUnit);

            board.addContract(contract);

            // ESCROW: Immediately deduct resources from seller
            town.addResource(item, -sellQuantity);

            LOGGER.info("Town {} created sell contract for {} {} (escrowed {} resources)",
                    town.getName(), sellQuantity, resourceId, sellQuantity);
        }
    }

    private void tryBidOnContracts() {
        // Get the server level
        net.minecraft.server.level.ServerLevel level = null;
        for (com.quackers29.businesscraft.town.TownManager manager : com.quackers29.businesscraft.town.TownManager
                .getAllInstances()) {
            if (manager.getTown(town.getId()) != null) {
                level = manager.getLevel();
                break;
            }
        }

        if (level == null) {
            return;
        }

        ContractBoard board = ContractBoard.get(level);
        List<Contract> contracts = board.getContracts();

        for (Contract contract : contracts) {
            if (contract instanceof SellContract sc) {
                // Don't bid on own contracts
                if (sc.getIssuerTownId().equals(town.getId())) {
                    continue;
                }

                // Don't bid on expired contracts
                if (sc.isExpired()) {
                    continue;
                }

                // Check if town needs this resource
                String resourceId = sc.getResourceId();

                // Look up the resource type from ResourceRegistry
                com.quackers29.businesscraft.economy.ResourceType resourceType = com.quackers29.businesscraft.economy.ResourceRegistry
                        .get(resourceId);

                // Skip if resource not found in registry
                if (resourceType == null) {
                    continue;
                }

                // Get the canonical item for this resource
                net.minecraft.resources.ResourceLocation itemLoc = resourceType.getCanonicalItemId();
                Object itemObj = com.quackers29.businesscraft.api.PlatformAccess.getRegistry().getItem(itemLoc);

                if (!(itemObj instanceof Item)) {
                    continue;
                }

                Item item = (Item) itemObj;
                int currentCount = town.getResourceCount(item);

                if (item != null && currentCount < NEED_THRESHOLD) {
                    // Check if already the highest bidder
                    if (town.getId().equals(sc.getHighestBidder())) {
                        continue;
                    }

                    // Calculate bid (Current Highest or Base Price * 1.1)
                    float currentHighest = sc.getHighestBid();
                    float basePrice = sc.getPricePerUnit() * sc.getQuantity();
                    float bid = (currentHighest > 0 ? currentHighest : basePrice) * 1.1f;

                    // ESCROW: Check if town has enough emeralds before bidding
                    int emeraldCount = town.getResourceCount(Items.EMERALD);
                    if (emeraldCount < bid) {
                        LOGGER.debug("Town {} cannot bid {} on contract {} - insufficient emeralds ({} available)",
                                town.getName(), bid, sc.getId(), emeraldCount);
                        continue;
                    }

                    // Place bid (escrow will be handled by ContractBoard)
                    board.addBid(sc.getId(), town.getId(), bid, level);
                    LOGGER.info("Town {} bid {} on contract {} for {}",
                            town.getName(), bid, sc.getId(), resourceId);
                }
            }
        }
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putLong("lastContractCheckTime", lastContractCheckTime);
    }

    @Override
    public void load(CompoundTag tag) {
        lastContractCheckTime = tag.getLong("lastContractCheckTime");
    }
}
