package com.quackers29.businesscraft.world;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.api.PlatformHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeRegistryColumnTest {
    private PlatformHelper previous;
    private Map<String, List<?>> kits;
    private List<?> global;

    @AfterEach
    void restore() {
        PlatformAccess.platform = previous;
        restoreKits();
    }

    @Test
    void bundledColumnOrderKeepsValuesOutOfTheNodeList(@TempDir Path dir) throws Exception {
        snapshot();
        write(dir, "biome_ids,variant_id,variant_name,description,weight,starting_nodes,starting_values\n"
                + "minecraft:plains,farming,Farming Village,\"Strong farming\",60,basic_settlement;farming_basic,pop:6;food:1400\n");
        BiomeRegistry.load();
        BiomeRegistry.BiomeKit kit = BiomeRegistry.getSpecificKit("minecraft:plains", "Farming Village");
        assertEquals(List.of("basic_settlement", "farming_basic"), kit.startingNodes);
        assertEquals(1400f, kit.startingValues.get("food"));
        assertFalse(kit.startingNodes.contains("food"));
    }

    @Test
    void stubColumnOrderIsNotReadAsNodes(@TempDir Path dir) throws Exception {
        snapshot();
        write(dir, "biome_ids,variant_id,variant_name,weight,starting_nodes,starting_values,description\n"
                + "minecraft:plains,farming,Farming Village,60,basic_settlement;farming_basic,population:6;food:140,\"Strong farming\"\n");
        BiomeRegistry.load();
        BiomeRegistry.BiomeKit kit = BiomeRegistry.getSpecificKit("minecraft:plains", "Farming Village");
        assertEquals(List.of("basic_settlement", "farming_basic"), kit.startingNodes);
        assertEquals(140f, kit.startingValues.get("food"));
        assertTrue(kit.startingNodes.stream().noneMatch(node -> node.startsWith("food")));
    }

    private void write(Path dir, String csv) throws Exception {
        previous = PlatformAccess.platform;
        Path bc = dir.resolve("businesscraft");
        Files.createDirectories(bc);
        Files.writeString(bc.resolve("biomes.csv"), csv);
        PlatformAccess.platform = new PlatformHelper() {
            @Override public String getModId() { return "businesscraft"; }
            @Override public boolean isClientSide() { return false; }
            @Override public boolean isServerSide() { return true; }
            @Override public String getPlatformName() { return "test"; }
            @Override public Path getConfigDirectory() { return dir; }
        };
    }

    @SuppressWarnings("unchecked")
    private void snapshot() throws Exception {
        kits = new LinkedHashMap<>();
        Map<String, List<?>> live = (Map<String, List<?>>) field("GEN_KITS").get(null);
        for (Map.Entry<String, List<?>> entry : live.entrySet()) {
            kits.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        global = new ArrayList<>((List<?>) field("GLOBAL_KITS").get(null));
    }

    @SuppressWarnings("unchecked")
    private void restoreKits() {
        if (kits == null) {
            return;
        }
        try {
            Map<String, List<Object>> live = (Map<String, List<Object>>) field("GEN_KITS").get(null);
            live.clear();
            for (Map.Entry<String, List<?>> entry : kits.entrySet()) {
                live.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            List<Object> globals = (List<Object>) field("GLOBAL_KITS").get(null);
            globals.clear();
            globals.addAll(global);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field field(String name) throws ReflectiveOperationException {
        Field field = BiomeRegistry.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
