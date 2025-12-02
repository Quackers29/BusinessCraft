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

    @Override
    public void tick() {
        long currentTime = System.currentTimeMillis();

        // Check every CHECK_INTERVAL ticks
        if (currentTime - lastContractCheckTime < CHECK_INTERVAL * 50) { // 50ms per tick approx
            return;
        }

        lastContractCheckTime = currentTime;

        tryCreateContract();
        tryBidOnContracts();
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
                        !sc.isExpired())
                .count();

        if (activeCount >= MAX_ACTIVE_CONTRACTS) {
            return;
        }

        // Check multiple resources for excess
        checkAndCreateContract(board, level, "wood", Items.OAK_LOG);
        checkAndCreateContract(board, level, "iron", Items.IRON_INGOT);
        checkAndCreateContract(board, level, "coal", Items.COAL);
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
                        !sc.isExpired())
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
            float pricePerUnit = 1.0f;

            SellContract contract = new SellContract(
                    town.getId(),
                    town.getName(),
                    60000L,
                    resourceId,
                    sellQuantity,
                    pricePerUnit);

            board.addContract(contract);
            LOGGER.info("Town {} created sell contract for {} {}",
                    town.getName(), sellQuantity, resourceId);
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
                Item item = null;
                int currentCount = 0;

                switch (resourceId) {
                    case "wood":
                        item = Items.OAK_LOG;
                        currentCount = town.getResourceCount(Items.OAK_LOG);
                        break;
                    case "iron":
                        item = Items.IRON_INGOT;
                        currentCount = town.getResourceCount(Items.IRON_INGOT);
                        break;
                    case "coal":
                        item = Items.COAL;
                        currentCount = town.getResourceCount(Items.COAL);
                        break;
                }

                if (item != null && currentCount < NEED_THRESHOLD) {
                    // Calculate bid (base price * 1.1)
                    float bid = sc.getPricePerUnit() * sc.getQuantity() * 1.1f;

                    // Place bid
                    board.addBid(sc.getId(), town.getId(), bid);
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
