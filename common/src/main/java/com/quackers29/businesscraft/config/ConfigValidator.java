package com.quackers29.businesscraft.config;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.data.parsers.Condition;
import com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount;
import com.quackers29.businesscraft.data.parsers.Effect;
import com.quackers29.businesscraft.economy.ResourceRegistry;
import com.quackers29.businesscraft.production.ProductionRecipe;
import com.quackers29.businesscraft.production.ProductionRegistry;
import com.quackers29.businesscraft.production.UpgradeNode;
import com.quackers29.businesscraft.production.UpgradeRegistry;
import com.quackers29.businesscraft.world.BiomeRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

/**
 * Cross-checks the four CSV registries after they have loaded and records one warning per bad
 * reference through {@link ConfigWarnings}. Read only: it never changes what the registries hold
 * or how production and upgrades behave.
 */
public final class ConfigValidator {
    private static final String ITEMS = "items.csv";
    private static final String PRODUCTIONS = "productions.csv";
    private static final String UPGRADES = "upgrades.csv";
    private static final String BIOMES = "biomes.csv";

    /** Amount and stock ids that are not items but are handled by the town directly. */
    private static final Set<String> TOWN_STATS = Set.of("pop", "wu", "tourist");

    /** Modifier names read by the town code. {@code storage_cap_<resource>} is matched separately. */
    private static final Set<String> KNOWN_MODIFIERS = Set.of(
            "pop_cap", "tourist_cap", "wu_cap", "storage_cap_all", "border", "happiness", "research");

    /** Costs that are checked against a town statistic but never consumed. */
    private static final Set<String> REQUIREMENT_ONLY = Set.of("tourism", "tourism_dist", "pop", "happiness");

    private ConfigValidator() {
    }

    /** The only column order biomes.csv may use. */
    static final String BIOME_COLUMNS =
            "biome_ids,variant_id,variant_name,description,weight,starting_nodes,starting_values";

    /** Runs every check. Returns the number of warnings added by this pass. */
    public static int validate() {
        int before = ConfigWarnings.all().size();
        warnIfLegacyBiomeColumnOrder();
        for (ProductionRecipe recipe : ProductionRegistry.getAll()) {
            validateRecipe(recipe);
        }
        for (UpgradeNode node : UpgradeRegistry.getAll()) {
            validateNode(node);
        }
        for (BiomeRegistry.BiomeKit kit : BiomeRegistry.getAllKits()) {
            validateKit(kit);
        }
        return ConfigWarnings.all().size() - before;
    }

    /**
     * The generated stub put {@code weight} in column 4. That file is not read as a second format.
     * One warning names the file and the only supported header; the row is left as written.
     */
    private static void warnIfLegacyBiomeColumnOrder() {
        if (PlatformAccess.platform == null) {
            return;
        }
        File file = PlatformAccess.platform.getConfigDirectory().resolve("businesscraft").resolve(BIOMES).toFile();
        if (!file.isFile()) {
            return;
        }
        String header;
        try {
            header = Files.readString(file.toPath()).lines().findFirst().orElse("");
        } catch (IOException e) {
            ConfigWarnings.warn(BIOMES, 1, "could not be read: " + e.getMessage());
            return;
        }
        String[] columns = CsvConfig.splitLine(header);
        boolean headerIsOld = columns.length >= 4 && columns[3].equalsIgnoreCase("weight");
        boolean dataLooksOld = false;
        if (!headerIsOld) {
            try {
                for (CsvConfig.Row row : CsvConfig.readRows(file)) {
                    if (row.columns().length >= 4 && row.columns()[3].matches("\\d+")) {
                        dataLooksOld = true;
                    }
                    break;
                }
            } catch (IOException e) {
                ConfigWarnings.warn(BIOMES, 1, "could not be read: " + e.getMessage());
                return;
            }
        }
        if (headerIsOld || dataLooksOld) {
            ConfigWarnings.warn(BIOMES, 1,
                    "old column order (weight before description). The only supported order is "
                            + BIOME_COLUMNS
                            + ". Replace this file with the bundled copy; showcase-play --production overwrites a stale one. "
                            + "The values were not remapped.");
        }
    }

    // --- productions.csv ---

    private static void validateRecipe(ProductionRecipe recipe) {
        String id = recipe.getId();
        for (ResourceAmount input : recipe.getInputs()) {
            String resId = baseResourceId(input.resourceId);
            if (!isItem(resId) && !resId.equals("pop") && !resId.equals("wu")) {
                ConfigWarnings.warn(PRODUCTIONS, id, "input '" + resId + "' is not a resource in " + ITEMS
                        + " (nor pop or wu), so the recipe can never run");
            }
            checkExpression(id, "input " + resId, input);
        }
        for (ResourceAmount output : recipe.getOutputs()) {
            String resId = baseResourceId(output.resourceId);
            if (!isItem(resId) && !TOWN_STATS.contains(resId) && !isModifier(resId)) {
                ConfigWarnings.warn(PRODUCTIONS, id,
                        "output '" + resId + "' is not a resource in " + ITEMS + ", pop, wu, tourist or a known stat");
            }
            checkExpression(id, "output " + resId, output);
        }
        for (Condition cond : recipe.getConditions()) {
            checkCondition(id, cond);
        }
    }

    /** Mirrors the "pop*" / "*pop" id rewrite done when a recipe runs. */
    private static String baseResourceId(String rawId) {
        if (rawId.startsWith("pop*"))
            return rawId.substring(4);
        if (rawId.endsWith("*pop"))
            return rawId.substring(0, rawId.length() - 4);
        return rawId;
    }

    private static void checkExpression(String recipeId, String what, ResourceAmount amount) {
        String expr = amount.amountExpression;
        if (expr == null || expr.isEmpty()) {
            ConfigWarnings.warn(PRODUCTIONS, recipeId, what + " has an empty amount");
            return;
        }
        for (String token : expr.split("\\*")) {
            token = token.trim();
            if (token.isEmpty() || isNumber(token) || isExpressionVariable(token)) {
                continue;
            }
            ConfigWarnings.warn(PRODUCTIONS, recipeId,
                    what + " amount '" + expr + "' has unknown token '" + token + "' (evaluates as 0)");
        }
    }

    private static boolean isExpressionVariable(String token) {
        return token.equalsIgnoreCase("pop") || token.equalsIgnoreCase("happiness") || token.equalsIgnoreCase("wu")
                || isModifierTarget(token);
    }

    private static void checkCondition(String recipeId, Condition cond) {
        String target = cond.getTarget();
        String value = cond.getValue();
        if (target.equals("surplus")) {
            if (!isItem(value) && !TOWN_STATS.contains(value)) {
                ConfigWarnings.warn(PRODUCTIONS, recipeId, "surplus condition names unknown resource '" + value + "'");
            }
            return;
        }
        boolean knownTarget = target.equals("happiness") || target.equals("pop") || target.equals("wu")
                || target.equals("crowding") || isItem(target) || isModifierTarget(target);
        if (!knownTarget) {
            ConfigWarnings.warn(PRODUCTIONS, recipeId, "condition target '" + target + "' is not a resource, pop, "
                    + "happiness, wu or a known stat");
        }
        boolean validValue = value.equalsIgnoreCase("min") || value.equalsIgnoreCase("excess")
                || value.contains("pop_cap") || isNumber(value.endsWith("%") ? value.substring(0, value.length() - 1) : value);
        if (!validValue) {
            ConfigWarnings.warn(PRODUCTIONS, recipeId,
                    "condition '" + cond + "' has value '" + value + "' that is not a number, min, excess, % or pop_cap");
        }
    }

    // --- upgrades.csv ---

    private static void validateNode(UpgradeNode node) {
        String id = node.getId();
        for (String pre : node.getPrereqNodes()) {
            if (UpgradeRegistry.get(pre) == null) {
                ConfigWarnings.warn(UPGRADES, id, "prerequisite '" + pre + "' is not a node");
            }
        }
        List<ResourceAmount> costs = node.getCosts();
        if (costs != null) {
            for (ResourceAmount cost : costs) {
                String resId = cost.resourceId;
                boolean requirement = REQUIREMENT_ONLY.contains(resId) || resId.startsWith("tourism_");
                if (!isItem(resId) && !requirement) {
                    ConfigWarnings.warn(UPGRADES, id, "cost '" + resId + "' is not a resource in " + ITEMS);
                }
            }
        }
        for (Effect effect : node.getEffects()) {
            String target = effect.getTarget();
            if (ProductionRegistry.get(target) == null && UpgradeRegistry.get(target) == null
                    && !isModifierTarget(target)) {
                ConfigWarnings.warn(UPGRADES, id,
                        "effect target '" + target + "' is not a recipe, node or known stat");
            }
        }
        if (node.getResearchMinutes() < 0) {
            ConfigWarnings.warn(UPGRADES, id, "research_minutes is negative");
        }
    }

    // --- biomes.csv ---

    private static void validateKit(BiomeRegistry.BiomeKit kit) {
        String id = kit.variantId;
        for (String entry : kit.startingNodes) {
            String nodeId = entry.contains(":") ? entry.substring(0, entry.indexOf(':')) : entry;
            if (UpgradeRegistry.get(nodeId) == null) {
                ConfigWarnings.warn(BIOMES, id, "starting node '" + nodeId + "' is not in " + UPGRADES);
            }
        }
        for (String key : kit.startingValues.keySet()) {
            boolean known = key.equals("pop") || key.equals("happiness") || key.equals("tourist")
                    || key.equals("border") || key.endsWith("_cap") || key.endsWith("_modifier") || isItem(key);
            if (!known) {
                ConfigWarnings.warn(BIOMES, id, "starting value '" + key + "' is not a resource or known stat");
            }
        }
        if (kit.weight <= 0) {
            ConfigWarnings.warn(BIOMES, id, "weight " + kit.weight + " means this variant is never chosen");
        }
    }

    // --- helpers ---

    private static boolean isItem(String id) {
        return ResourceRegistry.get(id) != null;
    }

    /** Known town stat or {@code storage_cap_<resource>}. */
    private static boolean isModifier(String id) {
        if (KNOWN_MODIFIERS.contains(id))
            return true;
        return id.startsWith("storage_cap_") && isItem(id.substring("storage_cap_".length()));
    }

    /** Anything an upgrade modifier lookup can meaningfully return: a stat, a recipe or a node. */
    private static boolean isModifierTarget(String id) {
        return isModifier(id) || ProductionRegistry.get(id) != null || UpgradeRegistry.get(id) != null;
    }

    private static boolean isNumber(String s) {
        try {
            Float.parseFloat(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
