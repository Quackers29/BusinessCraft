package com.quackers29.businesscraft.client.showcase;

import java.util.List;

/**
 * Plan of the production world built by {@code tools/showcase-play.sh start --production}
 * ({@link ShowcaseProductionWorld}). Pure data, so the plan is unit tested without Minecraft.
 *
 * <p>Four towns sit on a square 120 blocks apart, each in a different production state. Nothing here
 * names a recipe or a resource amount in blocks or items: stock is a percentage of the town's own
 * storage cap, so it follows {@code upgrades.csv}, {@code [production] minStockPercent} and
 * {@code excessStockPercent}. Node and recipe ids are checked against the loaded CSVs when the
 * world is built.
 */
public final class ShowcaseProductionLayout {
    /** Value of {@code businesscraft.multiplayerSmoke} that selects the production world. */
    public static final String MODE = "play-production";
    /** Fresh towns use a border of 50, so centres must be at least 100 apart. */
    public static final int MIN_TOWN_DISTANCE = 100;
    /** Vanilla signs hold four lines of at most 15 characters in the default font. */
    public static final int SIGN_LINE_MAX = 15;
    /** Where the sign stands relative to its town centre (south of the pad, readable from the south). */
    public static final int SIGN_DX = 3;
    public static final int SIGN_DZ = 8;
    /** Where a joining player stands, relative to the first town, facing north at it. */
    public static final int VIEW_DX = 2;
    public static final int VIEW_DZ = 12;

    /** A resource stocked to {@code percentOfCap} of the town's storage cap for it. */
    public record Stock(String resource, int percentOfCap) {
    }

    /** An upgrade node unlocked {@code level} times, on top of the biome starting kit. */
    public record Unlock(String node, int level) {
    }

    /**
     * One town. {@code stalled} recipes get a flat modifier that cancels their upgrade modifier, which
     * stops them while the town keeps every other upgrade. {@code workUnitPercent} is a share of the
     * town's work unit cap, or -1 to leave the starting work units alone. A population of 0 keeps the
     * biome kit's. A town with no unlocks and no stock keeps exactly what its biome kit gave it.
     */
    public record TownSpec(String name, int dx, int dz, int population, String role, List<Unlock> unlocks,
            List<Stock> stock, int workUnitPercent, List<String> stalled, List<String> sign) {
    }

    public static final List<TownSpec> TOWNS = List.of(
            new TownSpec("Starterton", 0, 0, 0, "Starter: biome kit only",
                    List.of(), List.of(), -1, List.of(),
                    List.of("STARTERTON", "Starter town", "Biome kit only", "Nothing seeded")),
            new TownSpec("Farmstead", 120, 0, 40, "Farming: stock for recipes",
                    List.of(new Unlock("farming_advanced", 2), new Unlock("woodcutting_basic", 1),
                            new Unlock("sawmill_basic", 1), new Unlock("work_units", 1)),
                    List.of(new Stock("food", 90), new Stock("wood", 85), new Stock("money", 50)),
                    60, List.of(),
                    List.of("FARMSTEAD", "Farming + wood", "Full stores:", "recipes run")),
            new TownSpec("Highmark", 0, -120, 80, "Mid-game: research AI",
                    List.of(new Unlock("farming_advanced", 3), new Unlock("woodcutting_basic", 1),
                            new Unlock("sawmill_basic", 1), new Unlock("work_units", 1),
                            new Unlock("work_unit_upgrade", 2), new Unlock("cap_upgrade", 2),
                            new Unlock("research_basic", 1), new Unlock("tourism_basic", 1)),
                    List.of(new Stock("food", 75), new Stock("wood", 75), new Stock("planks", 60),
                            new Stock("money", 70)),
                    50, List.of(),
                    List.of("HIGHMARK", "Mid-game town", "Many upgrades,", "research active")),
            new TownSpec("Hungerford", 120, -120, 60, "Starving: min-stock blocks",
                    List.of(new Unlock("sawmill_basic", 1), new Unlock("work_units", 1)),
                    List.of(new Stock("food", 6), new Stock("wood", 12), new Stock("money", 5)),
                    5, List.of("farming", "woodcutting"),
                    List.of("HUNGERFORD", "Starving town", "Low food: sad,", "min-stock fails")));

    public static final int STARTER = 0;
    public static final int FARM = 1;
    public static final int MIDGAME = 2;
    public static final int STARVING = 3;

    private ShowcaseProductionLayout() {
    }

    public static int distance(TownSpec a, TownSpec b) {
        int dx = a.dx() - b.dx();
        int dz = a.dz() - b.dz();
        return (int) Math.floor(Math.sqrt((double) dx * dx + (double) dz * dz));
    }

    /** Smallest centre-to-centre gap between any two towns. */
    public static int minDistance() {
        int best = Integer.MAX_VALUE;
        for (int i = 0; i < TOWNS.size(); i++) {
            for (int j = i + 1; j < TOWNS.size(); j++) {
                best = Math.min(best, distance(TOWNS.get(i), TOWNS.get(j)));
            }
        }
        return best;
    }

    public static String summary() {
        StringBuilder out = new StringBuilder();
        for (TownSpec town : TOWNS) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(town.name()).append(" (").append(town.dx()).append(',').append(town.dz()).append(") ")
                    .append(town.role());
        }
        return out.toString();
    }
}
