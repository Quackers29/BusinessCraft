package com.quackers29.businesscraft.debug;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.contract.CourierContract;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.data.TownSavedData;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Snapshot dump builder: nested save() keys appear without a hardcoded field list.
 */
class TownDataSnapshotBuilderTest {

    private static final UUID TOWN_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TOWN_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID TOWN_C = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private int savedDefaultPop;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() {
        savedDefaultPop = ConfigLoader.defaultStartingPopulation;
        ConfigLoader.defaultStartingPopulation = 5;
    }

    @AfterEach
    void tearDown() {
        ConfigLoader.defaultStartingPopulation = savedDefaultPop;
    }

    @Test
    void buildTownEntry_includesNestedEconomyAndPaymentBoardFromTownSave() {
        Town town = new Town(TOWN_A, new BlockPos(10, 64, 20), "Alpha");

        CompoundTag entry = TownDataSnapshotBuilder.buildTownEntry(
                town,
                TownSavedData.CURRENT_DATA_VERSION,
                null,
                null,
                List.of());

        assertEquals(TownSavedData.CURRENT_DATA_VERSION, entry.getInt(TownSavedData.DATA_VERSION_KEY));
        assertTrue(entry.contains("town", Tag.TAG_COMPOUND));

        CompoundTag townTag = entry.getCompound("town");
        assertEquals("Alpha", townTag.getString("name"));
        assertTrue(townTag.contains("economy", Tag.TAG_COMPOUND));
        assertTrue(townTag.getCompound("economy").contains("population"));
        assertTrue(townTag.contains("paymentBoard", Tag.TAG_COMPOUND));
        assertTrue(townTag.getCompound("paymentBoard").contains("rewards"));
        assertTrue(townTag.getCompound("paymentBoard").contains("bufferStorage"));
        assertTrue(entry.contains("contracts", Tag.TAG_LIST));
        assertEquals(0, entry.getList("contracts", Tag.TAG_COMPOUND).size());
    }

    @Test
    void buildTownEntry_attachesPlatformsAndBlockEntityWhenProvided() {
        Town town = new Town(TOWN_A, new BlockPos(0, 64, 0), "Alpha");
        ListTag platforms = new ListTag();
        CompoundTag platform = new CompoundTag();
        platform.putUUID("Id", UUID.randomUUID());
        platform.putString("Name", "Platform 1");
        platforms.add(platform);

        CompoundTag blockEntity = new CompoundTag();
        blockEntity.putUUID("TownId", TOWN_A);
        blockEntity.putString("name", "Alpha");
        blockEntity.putInt("searchRadius", 12);

        CompoundTag entry = TownDataSnapshotBuilder.buildTownEntry(
                town, TownSavedData.CURRENT_DATA_VERSION, platforms, blockEntity, List.of());

        assertEquals(1, entry.getList("platforms", Tag.TAG_COMPOUND).size());
        assertEquals("Alpha", entry.getCompound("blockEntity").getString("name"));
        assertEquals(12, entry.getCompound("blockEntity").getInt("searchRadius"));
    }

    @Test
    void contractTouchesTown_filtersIssuerBuyerWinningDestination() {
        SellContract sell = new SellContract(TOWN_A, "Alpha", 60_000L, "iron", 10, 1.5f);
        sell.setWinningTown(TOWN_B, "Bravo");

        CourierContract courier = new CourierContract(
                TOWN_A, "Alpha", new BlockPos(0, 64, 0), 50, 60_000L, "iron", 5, TOWN_C, "Charlie", 2.0f);

        SellContract unrelated = new SellContract(TOWN_C, "Charlie", 60_000L, "gold", 1, 1.0f);

        assertTrue(TownDataSnapshotBuilder.contractTouchesTown(sell, TOWN_A));
        assertTrue(TownDataSnapshotBuilder.contractTouchesTown(sell, TOWN_B));
        assertFalse(TownDataSnapshotBuilder.contractTouchesTown(sell, TOWN_C));

        assertTrue(TownDataSnapshotBuilder.contractTouchesTown(courier, TOWN_A));
        assertTrue(TownDataSnapshotBuilder.contractTouchesTown(courier, TOWN_C));
        assertFalse(TownDataSnapshotBuilder.contractTouchesTown(courier, TOWN_B));

        assertFalse(TownDataSnapshotBuilder.contractTouchesTown(unrelated, TOWN_A));

        CompoundTag entry = TownDataSnapshotBuilder.buildTownEntry(
                new Town(TOWN_A, new BlockPos(0, 64, 0), "Alpha"),
                TownSavedData.CURRENT_DATA_VERSION,
                null,
                null,
                List.of(sell, courier, unrelated));

        assertEquals(2, entry.getList("contracts", Tag.TAG_COMPOUND).size());
    }

    @Test
    void serializeContracts_writesTypeDiscriminator() {
        SellContract sell = new SellContract(TOWN_A, "Alpha", 60_000L, "iron", 3, 1.0f);
        ListTag list = TownDataSnapshotBuilder.serializeContracts(List.of(sell));
        assertEquals(1, list.size());
        assertEquals("sell", list.getCompound(0).getString("type"));
        assertTrue(list.getCompound(0).hasUUID("id"));
        assertTrue(list.getCompound(0).hasUUID("issuerTownId"));
    }
}
