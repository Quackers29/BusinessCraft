package com.quackers29.businesscraft.client.showcase;

import java.util.List;

/**
 * Plan of the walkable world built by {@code tools/showcase-play.sh} ({@link ShowcasePlayServer}).
 * Pure data and arithmetic, so the geometry and the sign text are unit tested without Minecraft.
 *
 * <p>Oakvale is the hub. Hillcrest and Rivertown lie north of it on one line, 120 blocks apart.
 * Pinewood stands east of Hillcrest. All offsets are relative to the first town centre (0, 0).
 */
public final class ShowcasePlayLayout {
    /** Value of {@code businesscraft.multiplayerSmoke} that selects the play world. */
    public static final String MODE = "play";
    /** Fresh towns use a border of 50, so centres must be at least 100 apart. */
    public static final int MIN_TOWN_DISTANCE = 100;
    /** The track runs this far east of a town centre. */
    public static final int TRACK_DX = 4;
    /** Departure stop, north of the centre (negative z). */
    public static final int DEPART_DZ = -7;
    /** Arrival stop, south of the centre. */
    public static final int ARRIVE_DZ = 7;
    /** Half length of a line platform's path, along the track. */
    public static final int LINE_HALF = 8;
    public static final int LINE_RADIUS = 3;
    /** Vanilla signs hold four lines of at most 15 characters in the default font. */
    public static final int SIGN_LINE_MAX = 15;

    public record TownSpec(String name, int dx, int dz, int population, String role) {
    }

    public record Segment(int from, int to) {
    }

    /** A standing sign: offset from its town centre, rotation 0..15 (0 faces south, 4 west), text. */
    public record SignSpec(int town, int dx, int dz, int rotation, List<String> lines) {
    }

    public static final List<TownSpec> TOWNS = List.of(
            new TownSpec("Oakvale", 0, 0, 60, "Hub: all destination modes"),
            new TownSpec("Hillcrest", 0, -120, 35, "Stop on the line"),
            new TownSpec("Rivertown", 0, -240, 20, "End of the line"),
            new TownSpec("Pinewood", 150, -120, 45, "Off the line: trade"));

    /** Oakvale to Hillcrest, then Hillcrest to Rivertown. */
    public static final List<Segment> SEGMENTS = List.of(new Segment(0, 1), new Segment(1, 2));

    public static final int OAKVALE = 0;
    public static final int HILLCREST = 1;
    public static final int RIVERTOWN = 2;
    public static final int PINEWOOD = 3;

    /** Where a joining player stands, relative to Oakvale, facing north at the town. */
    public static final int VIEW_DX = 2;
    public static final int VIEW_DZ = 9;

    public static final List<SignSpec> SIGNS = List.of(
            new SignSpec(OAKVALE, VIEW_DX + 2, VIEW_DZ - 1, 0,
                    List.of("BUSINESSCRAFT", "SHOWCASE TOUR", "Read every sign", "Fly: jump twice")),
            new SignSpec(OAKVALE, VIEW_DX + 4, VIEW_DZ - 1, 0,
                    List.of("1. Town Centre", "Right-click the", "big block:", "all its tabs")),
            new SignSpec(OAKVALE, VIEW_DX + 6, VIEW_DZ - 1, 0,
                    List.of("2. Tickets", "Machines east", "the town: ANY,", "AUTO, SPECIFIC")),
            new SignSpec(OAKVALE, VIEW_DX + 8, VIEW_DZ - 1, 0,
                    List.of("3. Chest", "Conductor's", "Clipboard and", "book inside")),
            new SignSpec(OAKVALE, VIEW_DX + 10, VIEW_DZ - 1, 0,
                    List.of("4. Rail line", "Follow track", "NORTH 120 blks", "to Hillcrest")),
            new SignSpec(OAKVALE, 7, 6, 4,
                    List.of("ANY platform", "Tourists go to", "any town. Open", "the machine")),
            new SignSpec(OAKVALE, 7, 12, 4,
                    List.of("AUTO platform", "Learns routes.", "Radius 5 and", "spread 4: wide")),
            new SignSpec(OAKVALE, 7, 18, 4,
                    List.of("OFF platform", "Switched off:", "the sign lamp", "stays dark")),
            new SignSpec(OAKVALE, TRACK_DX + 2, DEPART_DZ - 1, 0,
                    List.of("LINE to", "Hillcrest", "SPECIFIC mode.", "Cart leaves")),
            new SignSpec(HILLCREST, TRACK_DX + 2, ARRIVE_DZ + 1, 0,
                    List.of("HILLCREST", "Arrivals pay", "pay board:", "open the centre")),
            new SignSpec(HILLCREST, TRACK_DX + 2, DEPART_DZ - 1, 0,
                    List.of("LINE to", "Rivertown", "Riders go on", "from here")),
            new SignSpec(RIVERTOWN, TRACK_DX + 2, ARRIVE_DZ + 1, 0,
                    List.of("RIVERTOWN", "End of line.", "Small town:", "check Visitors")),
            new SignSpec(PINEWOOD, 0, 8, 0,
                    List.of("PINEWOOD", "Trade town:", "open Contracts", "for auctions")));

    private ShowcasePlayLayout() {
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

    /** Absolute z of a segment's departure stop. */
    public static int departZ(Segment segment) {
        return TOWNS.get(segment.from()).dz() + DEPART_DZ;
    }

    /** Absolute z of a segment's arrival stop. */
    public static int arriveZ(Segment segment) {
        return TOWNS.get(segment.to()).dz() + ARRIVE_DZ;
    }

    public static int trackX(int town) {
        return TOWNS.get(town).dx() + TRACK_DX;
    }

    public static String summary() {
        StringBuilder out = new StringBuilder();
        for (TownSpec town : TOWNS) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(town.name()).append(" (").append(town.dx()).append(',').append(town.dz()).append(')');
        }
        return out.toString();
    }

    /** Pages of the guide book in the Oakvale chest. Each page fits the book's 14-line limit. */
    public static List<String> guidePages() {
        return List.of(
                "BusinessCraft tour\n\nFour towns: Oakvale (you are here), Hillcrest and Rivertown "
                        + "north on one rail line, Pinewood to the east of Hillcrest.\n\nTime is noon "
                        + "and the weather is clear.",
                "Town Centre\n\nRight-click the big block in each town. Overview, Resources, "
                        + "Contracts, Payment board, Visitors and Settings all hold real data.",
                "Ticket machines\n\nOakvale has four east of the centre. Each shows a destination "
                        + "mode (Any, Auto, Specific), a search radius and spread, a path line and a "
                        + "sign lamp. One is off: its lamp is dark.",
                "Rail line\n\nA cart waits at each departure stop. Tourists board it, ride 113 blocks "
                        + "north and get off in the next town, which pays out on its payment board.",
                "Conductor's Clipboard\n\nHold it and look at a tourist or a cart to read origin "
                        + "and destination. Tourists wear name tags.",
                "Walk order\n\n1 Oakvale centre\n2 Tickets\n3 Chest\n4 Follow the rail north to "
                        + "Hillcrest\n5 Rivertown\n6 Pinewood, east of Hillcrest, for contracts.");
    }
}
