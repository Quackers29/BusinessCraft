package com.quackers29.businesscraft.debug.production;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import com.quackers29.businesscraft.production.ProductionRecipe;
import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionEventLogTest {
    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @AfterEach
    void closeLog() {
        ProductionEventLog.close();
        TownManager.clearInstances();
    }

    @Test
    void quotesCommasQuotesAndNewlines() {
        String raw = "a,b";
        assertEquals("\"a,b\"", Csv.quote(raw));
        assertEquals("\"say \"\"hi\"\"\"", Csv.quote("say \"hi\""));
        assertEquals(List.of("say \"hi\"", "a,b", "line\nbreak"),
                Csv.parse(Csv.row("say \"hi\"", "a,b", "line\nbreak")));
    }

    @Test
    void toggleOffDoesNotCallTheSupplierOrWrite() throws Exception {
        Path dir = Files.createTempDirectory("bc-log-off");
        boolean previous = ConfigLoader.productionEventLog;
        ConfigLoader.productionEventLog = false;
        try {
            ProductionEventLog.onServerStarted(dir);
            AtomicBoolean called = new AtomicBoolean();
            ProductionEventBus.post(() -> {
                called.set(true);
                return null;
            });
            assertFalse(called.get());
            assertEquals(0, Files.list(dir).count());
        } finally {
            ConfigLoader.productionEventLog = previous;
        }
    }

    @Test
    void summaryTotalsMatchTheEventRows() throws Exception {
        Path dir = Files.createTempDirectory("bc-log-summary");
        ProductionEventLog log = ProductionEventLog.open(dir);
        ProductionEventBus.post(() -> event(ProductionEvent.RECIPE_COMPLETED, "farming",
                "inputs=food:2;outputs=wood:5,food:1"));
        ProductionEventBus.post(() -> event(ProductionEvent.RECIPE_COMPLETED, "farming",
                "inputs=food:1;outputs=wood:5"));
        ProductionEventBus.post(() -> event(ProductionEvent.RECIPE_STALLED, "farming", "reason=output_full:wood"));
        ProductionEventBus.post(() -> event(ProductionEvent.RECIPE_STALLED, "farming", "reason=output_full:wood"));
        ProductionEventBus.post(() -> event(ProductionEvent.RESEARCH_STARTED, "border_expansion",
                "cost=wood:2;level=1"));
        ProductionEventBus.post(() -> event(ProductionEvent.CONTRACT_LISTED, "c1",
                "quantity=4;resource=wood"));
        Path events = log.eventsPath();
        Path summary = log.summaryCsvPath();
        ProductionEventLog.close();

        List<List<String>> rows = rows(events);
        assertEquals(6, rows.size());
        Map<String, Double> produced = totals(rows, ProductionEvent.RECIPE_COMPLETED, "outputs");
        Map<String, Double> consumed = totals(rows, ProductionEvent.RECIPE_COMPLETED, "inputs");
        assertEquals(10.0, produced.get("wood"));
        assertEquals(1.0, produced.get("food"));
        assertEquals(3.0, consumed.get("food"));

        List<List<String>> summaryRows = rows(summary);
        assertEquals("10", total(summaryRows, "produced", "wood"));
        assertEquals("1", total(summaryRows, "produced", "food"));
        assertEquals("3", total(summaryRows, "consumed", "food"));
        assertEquals("2", total(summaryRows, "stall", "output_full:wood"));
        assertEquals("1", total(summaryRows, "research_bought", "border_expansion"));
        assertEquals("2", total(summaryRows, "research_cost", "wood"));
        assertEquals("1", total(summaryRows, "contract", "listed"));
        assertTrue(Files.exists(dir.resolve(summary.getFileName().toString().replace(".csv", ".md"))));
    }

    @Test
    void completionAndStallFireOncePerChange() throws Exception {
        RegistryHelper previous = PlatformAccess.registry;
        boolean production = ConfigLoader.productionEnabled;
        PlatformAccess.registry = new SimItems();
        ConfigLoader.productionEnabled = true;
        Map<String, ProductionRecipe> recipes = recipes();
        ProductionRecipe cycle = new ProductionRecipe("test_cycle", "Cycle", 1f / 1200f,
                List.of(), List.of(new ResourceAmount("pop", "1")), List.of());
        ProductionRecipe stall = new ProductionRecipe("test_stall", "Stall", 1f / 1200f,
                List.of(), List.of(new ResourceAmount("tourist", "1")), List.of());
        recipes.put(cycle.getId(), cycle);
        recipes.put(stall.getId(), stall);
        List<ProductionEvent> seen = new ArrayList<>();
        ProductionEventListener listener = seen::add;
        ProductionEventBus.add(listener);
        try {
            Town town = new Town(UUID.randomUUID(), BlockPos.ZERO, "Test");
            TownManager.createHeadless().addExistingTown(town);
            town.getUpgrades().addFlatModifier("test_cycle", 1f);
            town.getUpgrades().addFlatModifier("test_stall", 1f);
            for (int i = 0; i < 5; i++) {
                ProductionEventContext.setGameTick(i + 1L);
                town.tick();
            }
            long completed = seen.stream().filter(e -> ProductionEvent.RECIPE_COMPLETED.equals(e.eventType())
                    && "test_cycle".equals(e.subjectId())).count();
            long stalls = seen.stream().filter(e -> ProductionEvent.RECIPE_STALLED.equals(e.eventType())
                    && "test_stall".equals(e.subjectId())).count();
            assertEquals(5, completed);
            assertEquals(1, stalls);
            assertTrue(seen.stream().noneMatch(e -> e.eventType().contains("progress")));
        } finally {
            ProductionEventBus.remove(listener);
            recipes.remove("test_cycle");
            recipes.remove("test_stall");
            ConfigLoader.productionEnabled = production;
            PlatformAccess.registry = previous;
        }
    }

    private static ProductionEvent event(String type, String subject, String details) {
        return new ProductionEvent(1L, "Oakvale", "town-1", type, subject, 1.0, details, "");
    }

    private static List<List<String>> rows(Path file) throws Exception {
        List<String> lines = Files.readAllLines(file);
        List<List<String>> parsed = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (!lines.get(i).isBlank()) {
                parsed.add(Csv.parse(lines.get(i)));
            }
        }
        return parsed;
    }

    private static Map<String, Double> totals(List<List<String>> rows, String type, String detailKey) {
        Map<String, Double> totals = new java.util.LinkedHashMap<>();
        for (List<String> row : rows) {
            if (!type.equals(row.get(4))) {
                continue;
            }
            String packed = detail(row.get(7), detailKey);
            for (String part : packed.split(",")) {
                int colon = part.indexOf(':');
                if (colon > 0) {
                    totals.merge(part.substring(0, colon), Double.parseDouble(part.substring(colon + 1)), Double::sum);
                }
            }
        }
        return totals;
    }

    private static String detail(String details, String key) {
        for (String part : details.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0 && part.substring(0, eq).equals(key)) {
                return part.substring(eq + 1);
            }
        }
        return "";
    }

    private static String total(List<List<String>> rows, String metric, String key) {
        for (List<String> row : rows) {
            if (metric.equals(row.get(2)) && key.equals(row.get(3))) {
                return row.get(4);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ProductionRecipe> recipes() throws Exception {
        Field field = ProductionRegistry.class.getDeclaredField("RECIPES");
        field.setAccessible(true);
        return (Map<String, ProductionRecipe>) field.get(null);
    }

    private static final class SimItems implements RegistryHelper {
        @Override
        public <T extends Block> java.util.function.Supplier<T> registerBlock(String name, java.util.function.Supplier<T> block) {
            return () -> null;
        }

        @Override
        public <T extends Item> java.util.function.Supplier<T> registerBlockItem(String name,
                java.util.function.Supplier<? extends Block> block) {
            return () -> null;
        }

        @Override
        public <T extends EntityType<?>> java.util.function.Supplier<T> registerEntityType(String name,
                java.util.function.Supplier<T> entityType) {
            return () -> null;
        }

        @Override
        public <T extends BlockEntityType<?>> java.util.function.Supplier<T> registerBlockEntityType(String name,
                java.util.function.Supplier<T> blockEntityType) {
            return () -> null;
        }

        @Override
        public <T extends MenuType<?>> java.util.function.Supplier<T> registerMenuType(String name,
                java.util.function.Supplier<T> menuType) {
            return () -> null;
        }

        @Override
        public <T extends AbstractContainerMenu> java.util.function.Supplier<MenuType<T>> registerExtendedMenuType(
                String name, RegistryHelper.MenuFactory<T> factory) {
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
            return BuiltInRegistries.ITEM;
        }
    }
}
