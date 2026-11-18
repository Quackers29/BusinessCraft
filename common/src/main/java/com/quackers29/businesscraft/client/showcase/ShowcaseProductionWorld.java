package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.economy.ResourceType;
import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.town.components.TownUpgradeComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
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
            seed(town, spec);
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

    private static void seed(Town town, ShowcaseProductionLayout.TownSpec spec) {
        TownUpgradeComponent upgrades = town.getUpgrades();
        for (ShowcaseProductionLayout.Unlock unlock : spec.unlocks()) {
            if (UpgradeRegistry.get(unlock.node()) == null) {
                throw new IllegalStateException(spec.name() + ": unknown upgrade node " + unlock.node());
            }
            for (int level = 0; level < unlock.level(); level++) {
                upgrades.unlockNode(unlock.node());
            }
        }
        for (String recipe : spec.stalled()) {
            if (ProductionRegistry.get(recipe) == null) {
                throw new IllegalStateException(spec.name() + ": unknown recipe " + recipe);
            }
            // The flat modifier is added to the upgrade modifier; cancelling it locks the recipe.
            upgrades.addFlatModifier(recipe, -upgrades.getModifier(recipe));
        }
        if (spec.population() > 0) {
            town.setPopulation(spec.population());
        }
        for (ShowcaseProductionLayout.Stock stock : spec.stock()) {
            ResourceType type = ResourceRegistry.get(stock.resource());
            Item item = type == null ? null : PlatformAccess.getRegistry().getItem(type.getMcItemId());
            if (item == null) {
                throw new IllegalStateException(spec.name() + ": unknown resource " + stock.resource());
            }
            long target = Math.round(town.getTrading().getStorageCap(stock.resource()) * stock.percentOfCap() / 100.0);
            town.addResource(item, target - town.getResourceCount(item));
        }
        if (spec.workUnitPercent() >= 0) {
            town.setWorkUnits(town.getWorkUnitCap() * spec.workUnitPercent() / 100);
        }
    }

    private static String describe(Town town) {
        StringBuilder out = new StringBuilder(town.getName()).append(" pop=").append(town.getPopulation())
                .append(" happiness=").append(Math.round(town.getHappiness()))
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
        StringBuilder out = new StringBuilder();
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
