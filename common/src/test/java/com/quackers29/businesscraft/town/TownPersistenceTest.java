package com.quackers29.businesscraft.town;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.data.TownSavedData;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.town.data.LegacyVisitHistoryMigration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownPersistenceTest {
    private static final UUID TOWN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final BlockPos POS = new BlockPos(3, 64, 9);

    private RegistryHelper savedRegistry;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() {
        savedRegistry = PlatformAccess.registry;
        PlatformAccess.registry = new ItemRegistry();
        TownManager.clearInstances();
    }

    @AfterEach
    void tearDown() {
        PlatformAccess.registry = savedRegistry;
        TownManager.clearInstances();
    }

    @Test
    void saveLoad_roundTripsEscrowedResources() {
        Town town = new Town(TOWN_ID, POS, "Escrow");
        town.addEscrowResource(Items.DIAMOND, 4);

        CompoundTag tag = new CompoundTag();
        town.save(tag);
        assertTrue(tag.contains("escrowedResources"));
        assertFalse(tag.contains("escrow"));

        Town loaded = Town.load(tag);
        assertEquals(4L, loaded.getEscrowResourceCount(Items.DIAMOND));
    }

    @Test
    void load_readsLegacyEscrowKey() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", TOWN_ID);
        tag.putString("name", "Legacy");
        tag.putInt("posX", 1);
        tag.putInt("posY", 64);
        tag.putInt("posZ", 1);
        CompoundTag escrow = new CompoundTag();
        escrow.putLong("minecraft:emerald", 9L);
        tag.put("escrow", escrow);

        Town loaded = Town.load(tag);
        assertEquals(9L, loaded.getEscrowResourceCount(Items.EMERALD));
    }

    @Test
    void saveLoad_roundTripsHappiness() {
        Town town = new Town(TOWN_ID, POS, "Happy");
        town.setHappiness(72.5f);

        CompoundTag tag = new CompoundTag();
        town.save(tag);
        Town loaded = Town.load(tag);
        assertEquals(72.5f, loaded.getHappiness(), 0.001f);
    }

    @Test
    void addResource_marksSavedDataDirty() {
        Town town = new Town(TOWN_ID, POS, "Dirty");
        TownSavedData data = TownSavedData.create();
        data.getTowns().put(town.getId(), town);
        TownManager manager = new TownManager(data);
        TownManager.installForTest(manager);
        data.setDirty(false);

        town.addResource(Items.BREAD, 3);

        assertTrue(data.isDirty());
        assertEquals(3L, town.getResourceCount(Items.BREAD));
    }

    @Test
    void legacyVisitHistory_importsOnceThenIgnoresRepeat() {
        Town town = new Town(TOWN_ID, POS, "Visits");
        UUID origin = UUID.fromString("55555555-5555-5555-5555-555555555555");
        CompoundTag chunk = visitTag(origin, 2);

        LegacyVisitHistoryMigration.apply(chunk, town);
        assertEquals(1, town.getVisitHistory().size());
        assertEquals(2, town.getVisitHistory().get(0).getCount());
        assertTrue(town.isLegacyVisitHistoryMigrated());
        assertFalse(chunk.contains("visitHistory"));

        LegacyVisitHistoryMigration.apply(visitTag(origin, 2), town);
        assertEquals(1, town.getVisitHistory().size());
    }

    @Test
    void saveLoad_roundTripsLegacyVisitFlag() {
        Town town = new Town(TOWN_ID, POS, "Flag");
        town.setLegacyVisitHistoryMigrated(true);
        CompoundTag tag = new CompoundTag();
        town.save(tag);
        Town loaded = Town.load(tag);
        assertTrue(loaded.isLegacyVisitHistoryMigrated());
    }

    private static CompoundTag visitTag(UUID origin, int count) {
        CompoundTag chunk = new CompoundTag();
        ListTag history = new ListTag();
        CompoundTag visit = new CompoundTag();
        visit.putLong("timestamp", 50L);
        visit.putUUID("townId", origin);
        visit.putInt("count", count);
        CompoundTag pos = new CompoundTag();
        pos.putInt("x", 8);
        pos.putInt("y", 64);
        pos.putInt("z", 8);
        visit.put("pos", pos);
        history.add(visit);
        chunk.put("visitHistory", history);
        return chunk;
    }

    private static final class ItemRegistry implements RegistryHelper {
        @Override
        public <T extends net.minecraft.world.level.block.Block> java.util.function.Supplier<T> registerBlock(
                String name, java.util.function.Supplier<T> block) {
            return () -> null;
        }

        @Override
        public <T extends Item> java.util.function.Supplier<T> registerBlockItem(String name,
                java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> block) {
            return () -> null;
        }

        @Override
        public <T extends net.minecraft.world.entity.EntityType<?>> java.util.function.Supplier<T> registerEntityType(
                String name, java.util.function.Supplier<T> entityType) {
            return () -> null;
        }

        @Override
        public <T extends net.minecraft.world.level.block.entity.BlockEntityType<?>> java.util.function.Supplier<T> registerBlockEntityType(
                String name, java.util.function.Supplier<T> blockEntityType) {
            return () -> null;
        }

        @Override
        public <T extends net.minecraft.world.inventory.MenuType<?>> java.util.function.Supplier<T> registerMenuType(
                String name, java.util.function.Supplier<T> menuType) {
            return () -> null;
        }

        @Override
        public <T extends net.minecraft.world.inventory.AbstractContainerMenu> java.util.function.Supplier<net.minecraft.world.inventory.MenuType<T>> registerExtendedMenuType(
                String name, MenuFactory<T> factory) {
            return () -> null;
        }

        @Override
        public Item getItem(ResourceLocation location) {
            return location == null ? null : BuiltInRegistries.ITEM.get(location);
        }

        @Override
        public ResourceLocation getItemKey(Item item) {
            return item == null ? null : BuiltInRegistries.ITEM.getKey(item);
        }

        @Override
        public Iterable<Item> getItems() {
            return java.util.List.of();
        }
    }
}
