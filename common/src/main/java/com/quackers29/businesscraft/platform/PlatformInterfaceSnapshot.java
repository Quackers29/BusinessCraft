package com.quackers29.businesscraft.platform;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * What the Town Interface screen shows. Built on the server from the linked
 * platform. The client does not need the town centre chunk loaded.
 */
public final class PlatformInterfaceSnapshot {
    private final BlockPos kioskPos;
    private final boolean active;
    private final String inactiveReason;
    private final String townName;
    private final String platformName;
    private final boolean enabled;
    private final boolean pathSet;
    private final String pathText;
    private final String waitingText;
    private final boolean townInReach;
    private final List<Destination> destinations;

    public PlatformInterfaceSnapshot(
            BlockPos kioskPos,
            boolean active,
            String inactiveReason,
            String townName,
            String platformName,
            boolean enabled,
            boolean pathSet,
            String pathText,
            String waitingText,
            boolean townInReach,
            List<Destination> destinations) {
        this.kioskPos = kioskPos;
        this.active = active;
        this.inactiveReason = inactiveReason == null ? "" : inactiveReason;
        this.townName = townName == null ? "" : townName;
        this.platformName = platformName == null ? "" : platformName;
        this.enabled = enabled;
        this.pathSet = pathSet;
        this.pathText = pathText == null ? "" : pathText;
        this.waitingText = waitingText == null ? "" : waitingText;
        this.townInReach = townInReach;
        this.destinations = destinations == null ? List.of() : List.copyOf(destinations);
    }

    public static PlatformInterfaceSnapshot inactive(BlockPos kioskPos, String reason) {
        return new PlatformInterfaceSnapshot(kioskPos, false, reason, "", "", false, false, "", "", false, List.of());
    }

    public BlockPos kioskPos() {
        return kioskPos;
    }

    public boolean active() {
        return active;
    }

    public String inactiveReason() {
        return inactiveReason;
    }

    public String townName() {
        return townName;
    }

    public String platformName() {
        return platformName;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean pathSet() {
        return pathSet;
    }

    public String pathText() {
        return pathText;
    }

    public String waitingText() {
        return waitingText;
    }

    public boolean townInReach() {
        return townInReach;
    }

    public List<Destination> destinations() {
        return destinations;
    }

    public record Destination(UUID townId, String name, boolean enabled, int distance) {
        public Destination {
            name = name == null ? "" : name;
        }
    }
}
