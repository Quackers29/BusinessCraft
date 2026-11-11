package com.quackers29.businesscraft.integration.jade;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearnedRoutesTooltipHelperTest {

    @Test
    void sharePercentRounded_clampsAndRounds() {
        assertEquals(0, LearnedRoutesTooltipHelper.sharePercentRounded(0f));
        assertEquals(42, LearnedRoutesTooltipHelper.sharePercentRounded(0.42f));
        assertEquals(100, LearnedRoutesTooltipHelper.sharePercentRounded(1.25f));
    }

    @Test
    void formatLearnedRoutesSummary_joinsNamedShares() {
        var lines = List.of(
                new LearnedRoutesTooltipHelper.LearnedRouteLine("Oakvale", 0.4f),
                new LearnedRoutesTooltipHelper.LearnedRouteLine("Hillcrest", 0.33f));
        assertEquals("Oakvale (40%), Hillcrest (33%)",
                LearnedRoutesTooltipHelper.formatLearnedRoutesSummary(lines));
    }

    @Test
    void buildLines_computesStrengthFromTotal() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        var top = List.of(Map.entry(a, 3.0), Map.entry(b, 1.0));
        var lines = LearnedRoutesTooltipHelper.buildLines(top, 4.0, id -> id.equals(a) ? "Alpha" : "Beta");
        assertEquals(2, lines.size());
        assertEquals("Alpha", lines.get(0).townName());
        assertEquals(75, LearnedRoutesTooltipHelper.sharePercentRounded(lines.get(0).strength()));
        assertEquals("Beta", lines.get(1).townName());
        assertEquals(25, LearnedRoutesTooltipHelper.sharePercentRounded(lines.get(1).strength()));
    }

    @Test
    void formatLearnedRoutesSummary_emptyReturnsBlank() {
        assertTrue(LearnedRoutesTooltipHelper.formatLearnedRoutesSummary(List.of()).isEmpty());
    }
}
