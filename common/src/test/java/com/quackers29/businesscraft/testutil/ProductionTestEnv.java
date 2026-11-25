package com.quackers29.businesscraft.testutil;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.PlatformHelper;
import com.quackers29.businesscraft.api.RegistryHelper;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.config.ConfigWarnings;
import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.world.BiomeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Test fixture for the production / upgrade systems. Points the platform config directory at a
 * temp folder, installs a registry helper backed by the vanilla item registry, and snapshots and
 * restores the static CSV registries (and the {@link ConfigLoader} toggles) so tests stay isolated.
 * Call {@link McBootstrap#init()} first.
 */
public final class ProductionTestEnv implements AutoCloseable {
    private static final Class<?>[] REGISTRIES = {
            ProductionRegistry.class, UpgradeRegistry.class, ResourceRegistry.class, BiomeRegistry.class };

    private final Path configRoot;
    private final PlatformHelper savedPlatform;
    private final RegistryHelper savedRegistry;
    private final boolean savedProductionEnabled;
    private final boolean savedResearchEnabled;
    private final int savedMin;
    private final int savedExcess;
    private final int savedDaily;
    private final Map<Field, Object> savedState = new HashMap<>();

    private ProductionTestEnv(Path configRoot) {
        this.configRoot = configRoot;
        this.savedPlatform = PlatformAccess.platform;
        this.savedRegistry = PlatformAccess.registry;
        this.savedProductionEnabled = ConfigLoader.productionEnabled;
        this.savedResearchEnabled = ConfigLoader.researchEnabled;
        this.savedMin = ConfigLoader.minStockPercent;
        this.savedExcess = ConfigLoader.excessStockPercent;
        this.savedDaily = ConfigLoader.dailyTickInterval;
        snapshotRegistries();
        PlatformAccess.platform = new TempPlatform(configRoot);
        PlatformAccess.registry = new VanillaItemRegistry();
        ConfigWarnings.clear();
    }

    /** Installs the fixture. {@code configRoot} is normally a JUnit {@code @TempDir}. */
    public static ProductionTestEnv install(Path configRoot) {
        return new ProductionTestEnv(configRoot);
    }

    public Path businesscraftDir() {
        return configRoot.resolve("businesscraft");
    }

    /** Writes (replacing) a CSV in the temp config folder. */
    public void writeCsv(String fileName, String content) throws IOException {
        Files.createDirectories(businesscraftDir());
        Files.writeString(businesscraftDir().resolve(fileName), content);
    }

    /** Loads items, productions, upgrades and biomes the way a fresh install does (bundled CSVs). */
    public void loadBundledRegistries() {
        ConfigWarnings.clear();
        ResourceRegistry.load();
        ProductionRegistry.load();
        UpgradeRegistry.load();
        BiomeRegistry.load();
    }

    /** Loads only the item registry from the bundled items.csv. */
    public void loadBundledItems() {
        ResourceRegistry.load();
    }

    @SuppressWarnings("unchecked")
    private void snapshotRegistries() {
        try {
            for (Class<?> reg : REGISTRIES) {
                for (Field f : reg.getDeclaredFields()) {
                    if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                        continue;
                    Class<?> t = f.getType();
                    if (!Map.class.isAssignableFrom(t) && !List.class.isAssignableFrom(t))
                        continue;
                    f.setAccessible(true);
                    Object cur = f.get(null);
                    if (cur instanceof Map<?, ?> m) {
                        savedState.put(f, new HashMap<>(deepCopyValues((Map<Object, Object>) m)));
                    } else if (cur instanceof List<?> l) {
                        savedState.put(f, new ArrayList<>(l));
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> deepCopyValues(Map<Object, Object> m) {
        Map<Object, Object> copy = new HashMap<>();
        for (Map.Entry<Object, Object> e : m.entrySet()) {
            Object v = e.getValue();
            copy.put(e.getKey(), v instanceof List<?> l ? new ArrayList<>(l) : v);
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void close() {
        try {
            for (Map.Entry<Field, Object> e : savedState.entrySet()) {
                Object cur = e.getKey().get(null);
                if (cur instanceof Map<?, ?> m) {
                    ((Map<Object, Object>) m).clear();
                    ((Map<Object, Object>) m).putAll((Map<Object, Object>) e.getValue());
                } else if (cur instanceof List<?> l) {
                    ((List<Object>) l).clear();
                    ((List<Object>) l).addAll((List<Object>) e.getValue());
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        PlatformAccess.platform = savedPlatform;
        PlatformAccess.registry = savedRegistry;
        ConfigLoader.productionEnabled = savedProductionEnabled;
        ConfigLoader.researchEnabled = savedResearchEnabled;
        ConfigLoader.minStockPercent = savedMin;
        ConfigLoader.excessStockPercent = savedExcess;
        ConfigLoader.dailyTickInterval = savedDaily;
        ConfigWarnings.clear();
    }

    private record TempPlatform(Path configDir) implements PlatformHelper {
        @Override public Path getConfigDirectory() { return configDir; }
        @Override public String getModId() { return "businesscraft"; }
        @Override public boolean isClientSide() { return false; }
        @Override public boolean isServerSide() { return true; }
        @Override public String getPlatformName() { return "test"; }
    }

    private static final class VanillaItemRegistry implements RegistryHelper {
        @Override public <T extends net.minecraft.world.level.block.Block> Supplier<T> registerBlock(String n, Supplier<T> b) { return () -> null; }
        @Override public <T extends Item> Supplier<T> registerBlockItem(String n, Supplier<? extends net.minecraft.world.level.block.Block> b) { return () -> null; }
        @Override public <T extends net.minecraft.world.entity.EntityType<?>> Supplier<T> registerEntityType(String n, Supplier<T> e) { return () -> null; }
        @Override public <T extends net.minecraft.world.level.block.entity.BlockEntityType<?>> Supplier<T> registerBlockEntityType(String n, Supplier<T> e) { return () -> null; }
        @Override public <T extends net.minecraft.world.inventory.MenuType<?>> Supplier<T> registerMenuType(String n, Supplier<T> m) { return () -> null; }
        @Override public <T extends net.minecraft.world.inventory.AbstractContainerMenu> Supplier<net.minecraft.world.inventory.MenuType<T>> registerExtendedMenuType(String n, MenuFactory<T> f) { return () -> null; }
        @Override public Item getItem(ResourceLocation location) { return location == null ? null : BuiltInRegistries.ITEM.get(location); }
        @Override public ResourceLocation getItemKey(Item item) { return item == null ? null : BuiltInRegistries.ITEM.getKey(item); }
        @Override public Iterable<Item> getItems() { return List.of(); }
    }
}
