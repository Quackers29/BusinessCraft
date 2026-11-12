package com.quackers29.businesscraft.client.showcase;

import java.util.List;
import java.util.Locale;

/**
 * Ports and screenshot names shared by {@code tools/multiplayer-smoke.sh} and the two clients.
 * Forge defaults to 25565 and Fabric to 25566 so the two loaders can be told apart if a run is
 * left up. The script moves to the next free port when one is busy and passes it to the clients
 * as {@code businesscraft.multiplayerSmoke.port}.
 */
public final class MultiplayerSmokePlan {
    public static final int FORGE_PORT = 25565;
    public static final int FABRIC_PORT = 25566;
    public static final List<String> SHOTS = List.of("mp_nametags", "mp_track", "mp_platform");
    /** Set to {@code true} on the one client (BC_Beta) that runs the out-of-reach packet stage. */
    public static final String REACH_PROPERTY = "businesscraft.multiplayerSmoke.reach";
    /** How far BC_Beta walks from the Oakvale platform before sending platform packets. */
    public static final int REACH_TEST_DISTANCE = 120;

    private MultiplayerSmokePlan() {
    }

    public static int portFor(String loader) {
        if (loader == null) {
            throw new IllegalArgumentException("loader is required");
        }
        return switch (loader.trim().toLowerCase(Locale.ROOT)) {
            case "forge" -> FORGE_PORT;
            case "fabric" -> FABRIC_PORT;
            default -> throw new IllegalArgumentException("Unknown loader: " + loader);
        };
    }

    public static String fileName(String shot) {
        return "showcase_" + shot + ".png";
    }
}
