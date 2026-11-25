package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.economy.ResourceType;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds the towns of {@link ShowcaseProductionLayout} on a server that has {@code [production]} and
 * {@code [research]} switched on, using the same town, upgrade and resource APIs the game does. Stock is
 * set as a share of each town's storage cap, so it tracks the loaded CSVs and config.
 */
final class ShowcaseProductionWorld {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/PlayProduction");

    private final List<UUID> ids = new ArrayList<>();
    private final List<BlockPos> positions = new ArrayList<>();

    /** Places and seeds every town at height {@code surface}. Returns the summary for the ready file. */
    String build(ServerLevel level, int surface) {
        if (!ConfigLoader.productionEnabled || !ConfigLoader.researchEnabled) {
            throw new IllegalStateException("[production] and [research] must be enabled in businesscraft.toml; "
                    + "start this world with: bash tools/showcase-play.sh start --production");
        }
        ShowcaseTownNames.reset();
        for (ShowcaseProductionLayout.TownSpec spec : ShowcaseProductionLayout.TOWNS) {
            BlockPos pos = new BlockPos(spec.dx(), surface, spec.dz());
            positions.add(pos);
            ids.add(MultiplayerSmokeServer.placeTown(level, pos, spec.name()));
        }
        StringBuilder detail = new StringBuilder("production=on research=on towns=").append(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            ShowcaseProductionLayout.TownSpec spec = ShowcaseProductionLayout.TOWNS.get(i);
            Town town = TownManager.get(level).getTown(ids.get(i));
            if (town == null) {
                throw new IllegalStateException("town " + spec.name() + " was not registered");
            }
            ShowcaseProductionSeed.apply(town, spec);
            ShowcasePlayServer.placeSign(level, positions.get(i).offset(ShowcaseProductionLayout.SIGN_DX, 0,
                    ShowcaseProductionLayout.SIGN_DZ), 0, spec.sign());
            LOGGER.info("PRODUCTION_TOWN {}", describe(town));
        }
        for (BlockPos pos : positions) {
            TownInterfaceEntity entity = MultiplayerSmokeServer.townEntity(level, pos);
            entity.setChanged();
            entity.updateInterfaceVM();
            entity.syncAllDirtyTownVMsToPlayers();
        }
        return detail + " layout=" + ShowcaseProductionLayout.summary();
    }

    private static String describe(Town town) {
        StringBuilder out = new StringBuilder(town.getName()).append(" pop=").append(town.getPopulation())
                .append(" happiness=").append(Math.round(town.getHappiness()))
                .append(" border=").append(town.getBoundaryRadius())
                .append(" wu=").append(town.getWorkUnits()).append(" nodes=")
                .append(town.getUpgrades().getUpgradeLevels());
        for (ResourceType type : ResourceRegistry.getAll()) {
            out.append(' ').append(type.getId()).append('=').append(Math.round(town.getTrading().getStock(type.getId())))
                    .append('/').append(Math.round(town.getTrading().getStorageCap(type.getId())));
        }
        return out.toString();
    }

    /** One line per town: the numbers that show recipes ticking, happiness moving and research running. */
    void report(ServerLevel level) {
        int contracts = 0;
        for (Contract contract : ContractBoard.get(level).getContracts()) {
            if (!contract.isExpired() && !contract.isCompleted()) {
                contracts++;
            }
        }
        StringBuilder out = new StringBuilder("contracts=").append(contracts).append(' ');
        for (UUID id : ids) {
            Town town = TownManager.get(level).getTown(id);
            if (town != null) {
                out.append(describe(town)).append(" research=").append(town.getUpgrades().getCurrentResearchNode())
                        .append("; ");
            }
        }
        LOGGER.info("PRODUCTION_STATUS {}", out);
    }
}
