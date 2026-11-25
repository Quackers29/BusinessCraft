package com.quackers29.businesscraft.debug.production;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.PlatformHelper;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.client.showcase.ShowcasePlayLayout;
import com.quackers29.businesscraft.client.showcase.ShowcaseTownSeed;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.economy.GlobalMarket;
import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.util.BcRandom;
import com.quackers29.businesscraft.util.GameClock;
import com.quackers29.businesscraft.world.BiomeRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ticks the live production, research, and contract code with no world.
 * Events go through {@link ProductionEvents} into {@link ProductionEventLog}.
 */
public final class ProductionSimulation {
    public static final String TICKS_PROPERTY = "businesscraft.productionSim.ticks";
    public static final String DIR_PROPERTY = "businesscraft.productionSim.dir";
    public static final String SEED_PROPERTY = "businesscraft.productionSim.seed";
    /** Short enough for {@code ./gradlew test}. {@code productionSim} raises it. */
    public static final int DEFAULT_TICKS = 1000;
    public static final long DEFAULT_SEED = 29L;
    private static final String KIT_BIOME = "minecraft:plains";
    private static final String KIT_NAME = "Farming Village";
    private static final long MILLIS_PER_TICK = 50L;

    private static volatile boolean booted;

    private ProductionSimulation() {
    }

    public record Request(int ticks, long seed, Path outputDir) {
        public static Request fromProperties() {
            return new Request(intProperty(TICKS_PROPERTY, DEFAULT_TICKS),
                    longProperty(SEED_PROPERTY, DEFAULT_SEED),
                    Path.of(System.getProperty(DIR_PROPERTY, Path.of("build", "production-sim").toString())));
        }
    }

    public record Result(Path eventsCsv, Path summaryCsv, Path summaryMarkdown, int ticks, long seed) {
    }

    public static Result run(Request request) throws IOException {
        if (request == null || request.ticks() < 1 || request.outputDir() == null) {
            throw new IllegalArgumentException("ticks and output directory are required");
        }
        boot();
        Files.createDirectories(request.outputDir());
        Path configDir = Files.createTempDirectory("businesscraft-sim-config");
        copyBundledCsvs(configDir.resolve("businesscraft"));

        PlatformHelper previousPlatform = PlatformAccess.platform;
        RegistryHelper previousRegistry = PlatformAccess.registry;
        boolean production = ConfigLoader.productionEnabled;
        boolean research = ConfigLoader.researchEnabled;
        boolean contracts = ConfigLoader.contractsEnabled;
        boolean eventLog = ConfigLoader.productionEventLog;
        Map<String, ?> recipes = copyMap(ProductionRegistry.class, "RECIPES");
        Map<String, ?> estimated = copyMap(ProductionRegistry.class, "ESTIMATED_VALUES");
        Map<String, ?> effort = copyMap(ProductionRegistry.class, "EFFORT_VALUES");
        Map<String, ?> nodes = copyMap(UpgradeRegistry.class, "NODES");
        Map<String, ?> resources = copyMap(ResourceRegistry.class, "RESOURCES");
        Map<String, List<?>> kits = copyKitMap();
        List<?> globalKits = copyGlobalKits();

        ProductionEventLog log = null;
        try {
            PlatformAccess.platform = new SimPlatform(configDir);
            PlatformAccess.registry = new SimRegistry();
            ResourceRegistry.load();
            ProductionRegistry.load();
            UpgradeRegistry.load();
            BiomeRegistry.load();

            ConfigLoader.productionEnabled = true;
            ConfigLoader.researchEnabled = true;
            ConfigLoader.contractsEnabled = true;
            ConfigLoader.productionEventLog = true;

            BcRandom.reseed(request.seed());
            GlobalMarket.get().reset();
            long[] millis = {0L};
            GameClock.set(() -> millis[0]);
            TownManager.clearInstances();

            TownManager manager = TownManager.createHeadless();
            List<Town> towns = new ArrayList<>();
            ContractBoard board = ContractBoard.headless(id -> manager.getTown(id));
            manager.attachHeadlessContracts(board);

            log = ProductionEventLog.open(request.outputDir());
            BiomeRegistry.BiomeKit kit = BiomeRegistry.getSpecificKit(KIT_BIOME, KIT_NAME);
            if (kit == null || !KIT_NAME.equals(kit.variantName)) {
                throw new IllegalStateException("Bundled biomes.csv has no " + KIT_NAME + " kit");
            }
            ProductionEventContext.setGameTick(0L);
            for (int i = 0; i < ShowcasePlayLayout.TOWNS.size(); i++) {
                ShowcasePlayLayout.TownSpec spec = ShowcasePlayLayout.TOWNS.get(i);
                UUID id = UUID.nameUUIDFromBytes(("businesscraft:showcase:" + spec.name()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                Town town = new Town(id, new BlockPos(spec.dx(), 64, spec.dz()), spec.name());
                manager.addExistingTown(town);
                town.setBiome(KIT_BIOME);
                town.setBiomeVariant(KIT_NAME);
                ProductionEvents.townCreated(town);
                TownManager.applyStartingKit(town, kit);
                ShowcaseTownSeed.apply(town, i);
                towns.add(town);
            }

            for (int tick = 1; tick <= request.ticks(); tick++) {
                millis[0] = tick * MILLIS_PER_TICK;
                ProductionEventContext.setGameTick(tick);
                for (Town town : towns) {
                    town.tick();
                }
                board.tick(null);
            }

            Result result = new Result(log.eventsPath(), log.summaryCsvPath(), log.summaryMarkdownPath(),
                    request.ticks(), request.seed());
            ProductionEventLog.close();
            log = null;
            return result;
        } finally {
            if (log != null) {
                ProductionEventLog.close();
            }
            GameClock.reset();
            BcRandom.reset();
            GlobalMarket.get().reset();
            TownManager.clearInstances();
            ContractBoard.clearInstances();
            ConfigLoader.productionEnabled = production;
            ConfigLoader.researchEnabled = research;
            ConfigLoader.contractsEnabled = contracts;
            ConfigLoader.productionEventLog = eventLog;
            restoreMap(ProductionRegistry.class, "RECIPES", recipes);
            restoreMap(ProductionRegistry.class, "ESTIMATED_VALUES", estimated);
            restoreMap(ProductionRegistry.class, "EFFORT_VALUES", effort);
            restoreMap(UpgradeRegistry.class, "NODES", nodes);
            restoreMap(ResourceRegistry.class, "RESOURCES", resources);
            restoreKitMap(kits);
            restoreGlobalKits(globalKits);
            PlatformAccess.platform = previousPlatform;
            PlatformAccess.registry = previousRegistry;
            deleteTree(configDir);
        }
    }

    private static void boot() {
        if (booted) {
            return;
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        booted = true;
    }

    private static void copyBundledCsvs(Path directory) throws IOException {
        Files.createDirectories(directory);
        for (String name : List.of("items.csv", "productions.csv", "upgrades.csv", "biomes.csv")) {
            String resource = "/assets/businesscraft/config/" + name;
            try (InputStream in = ProductionSimulation.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IOException("Missing bundled " + resource);
                }
                Files.copy(in, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static int intProperty(String key, int fallback) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(raw.trim());
    }

    private static long longProperty(String key, long fallback) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Long.parseLong(raw.trim());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> copyMap(Class<?> owner, String fieldName) {
        Map<String, ?> live = read(owner, fieldName);
        return new LinkedHashMap<>(live);
    }

    @SuppressWarnings("unchecked")
    private static void restoreMap(Class<?> owner, String fieldName, Map<String, ?> saved) {
        Map<String, Object> live = read(owner, fieldName);
        live.clear();
        live.putAll(saved);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<?>> copyKitMap() {
        Map<String, List<?>> live = read(BiomeRegistry.class, "GEN_KITS");
        Map<String, List<?>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<?>> entry : live.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static void restoreKitMap(Map<String, List<?>> saved) {
        Map<String, List<Object>> live = read(BiomeRegistry.class, "GEN_KITS");
        live.clear();
        for (Map.Entry<String, List<?>> entry : saved.entrySet()) {
            live.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<?> copyGlobalKits() {
        List<?> live = read(BiomeRegistry.class, "GLOBAL_KITS");
        return new ArrayList<>(live);
    }

    @SuppressWarnings("unchecked")
    private static void restoreGlobalKits(List<?> saved) {
        List<Object> live = read(BiomeRegistry.class, "GLOBAL_KITS");
        live.clear();
        live.addAll(saved);
    }

    @SuppressWarnings("unchecked")
    private static <T> T read(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(owner.getSimpleName() + "." + name, e);
        }
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // A leftover temp file does not change the simulation result.
                }
            });
        } catch (IOException ignored) {
            // Same as above.
        }
    }

    private static final class SimPlatform implements PlatformHelper {
        private final Path configDir;

        private SimPlatform(Path configDir) {
            this.configDir = configDir;
        }

        @Override
        public String getModId() {
            return "businesscraft";
        }

        @Override
        public boolean isClientSide() {
            return false;
        }

        @Override
        public boolean isServerSide() {
            return true;
        }

        @Override
        public String getPlatformName() {
            return "simulation";
        }

        @Override
        public Path getConfigDirectory() {
            return configDir;
        }
    }

    private static final class SimRegistry implements RegistryHelper {
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
