package com.quackers29.businesscraft.platform;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Picks which tourist platform a Town Interface block links to.
 * <p>
 * Proximity is measured to a platform's stop (path start or end), not to the
 * town boundary. The same platform's two stops are one choice. Two different
 * platforms within {@link #AMBIGUOUS_WITHIN} blocks of each other are rejected
 * so the player has to stand closer to the stop they mean.
 */
public final class PlatformLink {
    /** Horizontal (XZ) radius, in blocks, from the placed block to a stop. */
    public static final int HORIZONTAL_RADIUS = 5;
    /** Vertical slack, in blocks. Stops are often one block below the kiosk. */
    public static final int VERTICAL_RADIUS = 3;
    /** Different platforms this close in horizontal distance are ambiguous. */
    public static final double AMBIGUOUS_WITHIN = 1.0;

    private PlatformLink() {
    }

    public enum Status {
        LINKED,
        NO_STOP,
        AMBIGUOUS
    }

    /** One end of a platform path. */
    public record Stop(UUID townId, UUID platformId, BlockPos pos) {
    }

    public record Choice(Status status, UUID townId, UUID platformId, BlockPos stop) {
        public static Choice none() {
            return new Choice(Status.NO_STOP, null, null, null);
        }

        public static Choice ambiguous() {
            return new Choice(Status.AMBIGUOUS, null, null, null);
        }

        public static Choice linked(UUID townId, UUID platformId, BlockPos stop) {
            return new Choice(Status.LINKED, townId, platformId, stop);
        }
    }

    public static Choice choose(BlockPos place, List<Stop> stops) {
        if (place == null || stops == null || stops.isEmpty()) {
            return Choice.none();
        }

        int radiusSq = HORIZONTAL_RADIUS * HORIZONTAL_RADIUS;
        List<Candidate> matches = new ArrayList<>();
        for (Stop stop : stops) {
            if (stop == null || stop.pos() == null || stop.townId() == null || stop.platformId() == null) {
                continue;
            }
            int dx = stop.pos().getX() - place.getX();
            int dz = stop.pos().getZ() - place.getZ();
            int horizontalSq = dx * dx + dz * dz;
            if (horizontalSq > radiusSq) {
                continue;
            }
            int vertical = Math.abs(stop.pos().getY() - place.getY());
            if (vertical > VERTICAL_RADIUS) {
                continue;
            }
            matches.add(new Candidate(stop, Math.sqrt(horizontalSq), vertical));
        }

        if (matches.isEmpty()) {
            return Choice.none();
        }

        matches.sort(Comparator.comparingDouble(Candidate::horizontal).thenComparingInt(Candidate::vertical));
        Candidate best = matches.get(0);
        for (int i = 1; i < matches.size(); i++) {
            Candidate other = matches.get(i);
            if (samePlatform(best.stop(), other.stop())) {
                continue;
            }
            if (other.horizontal() - best.horizontal() <= AMBIGUOUS_WITHIN) {
                return Choice.ambiguous();
            }
            break;
        }
        return Choice.linked(best.stop().townId(), best.stop().platformId(), best.stop().pos());
    }

    private static boolean samePlatform(Stop a, Stop b) {
        return a.townId().equals(b.townId()) && a.platformId().equals(b.platformId());
    }

    private record Candidate(Stop stop, double horizontal, int vertical) {
    }
}
