package com.quackers29.businesscraft.debug.production;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.economy.ResourceType;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Builds typed events at stage changes. Formatting lives here so game systems
 * only name what completed.
 */
public final class ProductionEvents {
    private ProductionEvents() {
    }

    public static void townCreated(Town town) {
        if (town == null || !ProductionEventBus.listening()) {
            return;
        }
        ProductionEventBus.post(() -> event(town, ProductionEvent.TOWN_CREATED, town.getId().toString(),
                (double) town.getPopulation(),
                Detail.of(Map.of(
                        "x", Integer.toString(town.getPosition().getX()),
                        "y", Integer.toString(town.getPosition().getY()),
                        "z", Integer.toString(town.getPosition().getZ()),
                        "biome", town.getBiome() == null ? "" : town.getBiome())),
                stock(town, null)));
    }

    public static void population(Town town, long from, long to) {
        if (town == null || !ProductionEventBus.listening()) {
            return;
        }
        ProductionEventBus.post(() -> event(town, ProductionEvent.POPULATION_CHANGED, "pop", (double) to,
                Detail.of(Map.of("from", Long.toString(from), "to", Long.toString(to))),
                "pop=" + to));
    }

    public static void border(Town town, int from, int to) {
        if (town == null || !ProductionEventBus.listening()) {
            return;
        }
        ProductionEventBus.post(() -> event(town, ProductionEvent.BORDER_CHANGED, "border", (double) to,
                Detail.of(Map.of("from", Integer.toString(from), "to", Integer.toString(to))),
                "border=" + to));
    }

    public static void workUnits(Town town, long from, long to) {
        if (town == null || !ProductionEventBus.listening()) {
            return;
        }
        ProductionEventBus.post(() -> event(town, ProductionEvent.WORK_UNITS_CHANGED, "wu", (double) to,
                Detail.of(Map.of("from", Long.toString(from), "to", Long.toString(to))),
                "wu=" + to));
    }

    public static void recipeCompleted(Town town, String recipeId, long outputUnits,
            Map<String, Long> inputs, Map<String, Long> outputs) {
        if (town == null || !ProductionEventBus.listening()) {
            return;
        }
        ProductionEventBus.post(() -> {
            Map<String, String> details = new LinkedHashMap<>();
            details.put("inputs", Detail.amounts(inputs));
            details.put("outputs", Detail.amounts(outputs));
            return event(town, ProductionEvent.RECIPE_COMPLETED, recipeId, (double) outputUnits,
                    Detail.of(details), stock(town, union(inputs, outputs)));
        });
    }

    public static void recipeStall(Town town, String recipeId, String previous, String reason) {
        if (town == null || !ProductionEventBus.listening()) {
            return;
        }
        boolean resumed = reason == null;
        ProductionEventBus.post(() -> event(town,
                resumed ? ProductionEvent.RECIPE_RESUMED : ProductionEvent.RECIPE_STALLED,
                recipeId,
                null,
                Detail.of(Map.of(
                        "previous", previous == null ? "" : previous,
                        "reason", reason == null ? "" : reason)),
                ""));
    }

    public static void researchStarted(Town town, String nodeId, int level, Map<String, Long> cost) {
        if (town == null || !ProductionEventBus.listening()) {
            return;
        }
        ProductionEventBus.post(() -> event(town, ProductionEvent.RESEARCH_STARTED, nodeId, (double) level,
                Detail.of(Map.of("cost", Detail.amounts(cost), "level", Integer.toString(level))),
                stock(town, cost == null ? Set.of() : cost.keySet())));
    }

    public static void researchCompleted(Town town, String nodeId, int level) {
        if (town == null || !ProductionEventBus.listening()) {
            return;
        }
        ProductionEventBus.post(() -> event(town, ProductionEvent.RESEARCH_COMPLETED, nodeId, (double) level,
                Detail.of(Map.of("level", Integer.toString(level))), ""));
    }

    public static void upgradeLevel(Town town, String nodeId, int level) {
        if (town == null || !ProductionEventBus.listening()) {
            return;
        }
        ProductionEventBus.post(() -> event(town, ProductionEvent.UPGRADE_LEVEL_GAINED, nodeId, (double) level,
                Detail.of(Map.of("level", Integer.toString(level))), ""));
    }

    public static void contract(Town town, String townName, String townId, String type, String contractId,
            String resource, long quantity, double pricePerUnit, String counterpartId, String counterpartName) {
        if (!ProductionEventBus.listening()) {
            return;
        }
        String name = town != null ? town.getName() : townName;
        String id = town != null ? town.getId().toString() : (townId == null ? "" : townId);
        ProductionEventBus.post(() -> {
            double total = pricePerUnit * quantity;
            Map<String, String> details = new LinkedHashMap<>();
            details.put("counterpart", counterpartName == null ? "" : counterpartName);
            details.put("counterpart_id", counterpartId == null ? "" : counterpartId);
            details.put("price_per_unit", Detail.number(pricePerUnit));
            details.put("price_total", Detail.number(total));
            details.put("quantity", Long.toString(quantity));
            details.put("resource", resource == null ? "" : resource);
            String stock = town == null || resource == null || resource.isEmpty()
                    ? ""
                    : stock(town, Set.of(resource));
            return new ProductionEvent(ProductionEventContext.gameTick(),
                    name == null ? "" : name, id, type,
                    contractId == null ? "" : contractId,
                    (double) quantity, Detail.of(details), stock);
        });
    }

    public static void snapshot(Town town) {
        if (town == null || !ProductionEventBus.listening()) {
            return;
        }
        ProductionEventBus.post(() -> event(town, ProductionEvent.STATE_SNAPSHOT, town.getId().toString(),
                (double) town.getPopulation(),
                Detail.of(Map.of(
                        "border", Integer.toString(town.getBoundaryRadius()),
                        "pop", Long.toString(town.getPopulation()),
                        "wu", Long.toString(town.getWorkUnits()))),
                stock(town, null)));
    }

    private static ProductionEvent event(Town town, String type, String subject, Double amount,
            String details, String stock) {
        return new ProductionEvent(ProductionEventContext.gameTick(),
                town.getName(), town.getId().toString(), type,
                subject == null ? "" : subject, amount, details, stock);
    }

    private static Set<String> union(Map<String, Long> inputs, Map<String, Long> outputs) {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        if (inputs != null) {
            ids.addAll(inputs.keySet());
        }
        if (outputs != null) {
            ids.addAll(outputs.keySet());
        }
        return ids;
    }

    static String stock(Town town, Set<String> resourceIds) {
        TreeMap<String, Long> amounts = new TreeMap<>();
        if (resourceIds == null) {
            amounts.put("border", (long) town.getBoundaryRadius());
            amounts.put("pop", town.getPopulation());
            amounts.put("wu", town.getWorkUnits());
            if (town.getAllResources() != null) {
                for (Map.Entry<Item, Long> entry : town.getAllResources().entrySet()) {
                    ResourceType type = ResourceRegistry.getFor(entry.getKey());
                    if (type != null) {
                        amounts.merge(type.getId(), entry.getValue(), Long::sum);
                    }
                }
            }
        } else {
            for (String resourceId : resourceIds) {
                amounts.put(resourceId, count(town, resourceId));
            }
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Long> entry : amounts.entrySet()) {
            if (out.length() > 0) {
                out.append(';');
            }
            out.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return out.toString();
    }

    private static long count(Town town, String resourceId) {
        if ("pop".equals(resourceId)) {
            return town.getPopulation();
        }
        if ("wu".equals(resourceId)) {
            return town.getWorkUnits();
        }
        if ("border".equals(resourceId)) {
            return town.getBoundaryRadius();
        }
        if ("tourist".equals(resourceId)) {
            return town.getTouristCount() + town.getPendingTouristSpawns();
        }
        ResourceType type = ResourceRegistry.get(resourceId);
        if (type == null || type.getMcItemId() == null || PlatformAccess.registry == null) {
            return 0L;
        }
        Object item = PlatformAccess.registry.getItem(type.getMcItemId());
        if (item instanceof Item minecraftItem) {
            return town.getResourceCount(minecraftItem);
        }
        return 0L;
    }
}
