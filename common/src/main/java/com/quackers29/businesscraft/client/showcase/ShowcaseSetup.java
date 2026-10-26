package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Server-side world preparation for showcase routes (platforms, contracts, etc.).
 */
public final class ShowcaseSetup {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Showcase");
    private static volatile boolean prepared;

    private ShowcaseSetup() {
    }

    public static void prepareOnce(MinecraftServer server, BlockPos townPos) {
        if (prepared || server == null || townPos == null) {
            return;
        }
        server.execute(() -> {
            if (prepared) {
                return;
            }
            ServerLevel level = server.overworld();
            BlockEntity be = level.getBlockEntity(townPos);
            if (!(be instanceof TownInterfaceEntity townInterface)) {
                LOGGER.warn("Showcase setup: no town interface at {}", townPos);
                return;
            }
            Town town = townInterface.getTown();
            if (town == null) {
                LOGGER.warn("Showcase setup: town interface has no town");
                return;
            }
            if (!ShowcasePlatforms.ensure(level, townPos)) {
                LOGGER.warn("Showcase setup: platform interface was not placed at {}", townPos);
            }
            seedAuctionContract(level, town);
            seedVisitHistory(level, town, townPos, townInterface);
            prepared = true;
            LOGGER.info("Showcase setup complete for {}", town.getName());
        });
    }

    private static void seedVisitHistory(ServerLevel level, Town town, BlockPos townPos,
            TownInterfaceEntity townInterface) {
        if (!town.getVisitHistory().isEmpty()) {
            return;
        }
        for (Town other : TownManager.get(level).getAllTowns().values()) {
            if (other.getId().equals(town.getId())) {
                continue;
            }
            town.recordVisit(other.getId(), 3, other.getPosition());
            LOGGER.info("Showcase setup: seeded visit from {} to {}", other.getName(), town.getName());
            townInterface.setChanged();
            level.sendBlockUpdated(townPos, townInterface.getBlockState(), townInterface.getBlockState(),
                    Block.UPDATE_ALL);
            return;
        }
    }

    private static void seedAuctionContract(ServerLevel level, Town town) {
        ContractBoard board = ContractBoard.get(level);
        boolean hasOpenAuction = board.getContracts().stream()
                .anyMatch(c -> c instanceof SellContract sc && !sc.isExpired() && sc.getWinningTownId() == null);
        if (hasOpenAuction) {
            return;
        }
        SellContract contract = new SellContract(
                town.getId(),
                town.getName(),
                3_600_000L,
                "emerald",
                1,
                1.0f);
        board.addContract(contract);
        LOGGER.info("Showcase setup: seeded auction contract {}", contract.getId());
    }

    public static void resetForTests() {
        prepared = false;
    }

    public static String runStep(MinecraftServer server, BlockPos townPos, String step) {
        if (step == null || !step.startsWith("setup:")) {
            return null;
        }
        if (server == null) {
            return "setup without server";
        }
        prepareOnce(server, townPos);
        return null;
    }
}
