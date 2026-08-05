package com.quackers29.businesscraft.debug;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.contract.CourierContract;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.data.TownSavedData;
import com.quackers29.businesscraft.economy.GlobalMarket;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds a full-picture SNBT-ready snapshot of BusinessCraft persisted state.
 * <p>
 * Inner town / contract / market / platform compounds come from existing
 * {@code save}/{@code toNBT} paths so new persisted keys appear automatically.
 */
public final class TownDataSnapshotBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownDataSnapshotBuilder.class);

    /** Envelope version for this dump format (independent of {@link TownSavedData#CURRENT_DATA_VERSION}). */
    public static final int DUMP_VERSION = 1;

    private TownDataSnapshotBuilder() {
    }

    public record DumpResult(CompoundTag root, int townCount, int contractCount, boolean marketPresent) {
    }

    /**
     * Assembles a dump envelope from all loaded levels on the given server.
     */
    public static DumpResult build(MinecraftServer server) {
        long now = System.currentTimeMillis();
        CompoundTag root = createEnvelope(now, server);

        CompoundTag levelsTag = new CompoundTag();
        int townCount = 0;
        int contractCount = 0;

        for (ServerLevel level : server.getAllLevels()) {
            LevelBuildResult levelResult = buildLevel(level);
            levelsTag.put(level.dimension().location().toString(), levelResult.levelTag());
            townCount += levelResult.townCount();
            contractCount += levelResult.contractCount();
        }
        root.put("levels", levelsTag);

        CompoundTag marketTag = new CompoundTag();
        GlobalMarket.get().save(marketTag);
        root.put("market", marketTag);
        boolean marketPresent = marketTag.contains("prices") || marketTag.contains("volume");

        return new DumpResult(root, townCount, contractCount, marketPresent);
    }

    static CompoundTag createEnvelope(long dumpTimestampMs, MinecraftServer server) {
        CompoundTag root = new CompoundTag();
        root.putInt("dumpVersion", DUMP_VERSION);
        root.putLong("dumpTimestamp", dumpTimestampMs);
        root.putString("dumpTimestampIso", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(dumpTimestampMs)));

        ServerLevel overworld = server.overworld();
        if (overworld != null) {
            root.putLong("minecraftTime", overworld.getGameTime());
        }
        return root;
    }

    /**
     * Builds one town entry: {@code dataVersion}, {@code town} ({@link Town#save}), optional
     * platforms / slim blockEntity, and contracts that touch this town.
     */
    public static CompoundTag buildTownEntry(Town town, int dataVersion, ListTag platforms,
            CompoundTag blockEntity, Collection<Contract> levelContracts) {
        CompoundTag entry = new CompoundTag();
        entry.putInt(TownSavedData.DATA_VERSION_KEY, dataVersion);

        CompoundTag townTag = new CompoundTag();
        town.save(townTag);
        entry.put("town", townTag);

        if (platforms != null && !platforms.isEmpty()) {
            entry.put("platforms", platforms);
        }
        if (blockEntity != null && !blockEntity.isEmpty()) {
            entry.put("blockEntity", blockEntity);
        }

        List<Contract> touching = new ArrayList<>();
        for (Contract contract : levelContracts) {
            if (contractTouchesTown(contract, town.getId())) {
                touching.add(contract);
            }
        }
        entry.put("contracts", serializeContracts(touching));
        return entry;
    }

    public static ListTag serializeContracts(Collection<Contract> contracts) {
        ListTag list = new ListTag();
        for (Contract contract : contracts) {
            CompoundTag contractTag = new CompoundTag();
            contractTag.putString("type", contract.getType());
            contract.save(contractTag);
            list.add(contractTag);
        }
        return list;
    }

    /**
     * Town UUID match on issuer / buyer / winning / destination (not player bid UUIDs).
     */
    public static boolean contractTouchesTown(Contract contract, UUID townId) {
        if (townId == null || contract == null) {
            return false;
        }
        if (townId.equals(contract.getIssuerTownId())) {
            return true;
        }
        if (contract instanceof SellContract sell) {
            if (townId.equals(sell.getBuyerTownId()) || townId.equals(sell.getWinningTownId())) {
                return true;
            }
        }
        if (contract instanceof CourierContract courier) {
            return townId.equals(courier.getDestinationTownId());
        }
        return false;
    }

    private record LevelBuildResult(CompoundTag levelTag, int townCount, int contractCount) {
    }

    private static LevelBuildResult buildLevel(ServerLevel level) {
        TownManager townManager = TownManager.get(level);
        Map<UUID, Town> towns = townManager.getAllTowns();
        List<Contract> contracts = new ArrayList<>(ContractBoard.get(level).getContracts());

        CompoundTag townsTag = new CompoundTag();
        for (Town town : towns.values()) {
            BlockEntityBits bits = extractBlockEntityBits(level, town);
            CompoundTag entry = buildTownEntry(
                    town,
                    TownSavedData.CURRENT_DATA_VERSION,
                    bits.platforms(),
                    bits.blockEntity(),
                    contracts);
            townsTag.put(town.getId().toString(), entry);
        }

        CompoundTag levelTag = new CompoundTag();
        levelTag.put("towns", townsTag);
        levelTag.put("contractsAll", serializeContracts(contracts));
        return new LevelBuildResult(levelTag, towns.size(), contracts.size());
    }

    private record BlockEntityBits(ListTag platforms, CompoundTag blockEntity) {
    }

    private static BlockEntityBits extractBlockEntityBits(ServerLevel level, Town town) {
        TownInterfaceEntity entity = findTownInterface(level, town);
        if (entity == null) {
            return new BlockEntityBits(null, null);
        }

        CompoundTag beFull = new CompoundTag();
        try {
            entity.saveAdditional(beFull);
        } catch (Exception e) {
            LOGGER.warn("Failed to serialize TownInterfaceEntity at {} for town {}: {}",
                    town.getPosition(), town.getId(), e.toString());
            return new BlockEntityBits(null, null);
        }

        ListTag platforms = null;
        if (beFull.contains("platforms", Tag.TAG_LIST)) {
            platforms = beFull.getList("platforms", Tag.TAG_COMPOUND);
            beFull.remove("platforms");
        }
        return new BlockEntityBits(platforms, beFull);
    }

    private static TownInterfaceEntity findTownInterface(ServerLevel level, Town town) {
        BlockPos pos = town.getPosition();
        if (pos != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownInterfaceEntity tie) {
                UUID beTownId = tie.getTownId();
                if (beTownId == null || beTownId.equals(town.getId())) {
                    return tie;
                }
            }
        }
        return null;
    }
}
