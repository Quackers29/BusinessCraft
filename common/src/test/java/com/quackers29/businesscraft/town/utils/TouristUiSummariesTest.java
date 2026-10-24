package com.quackers29.businesscraft.town.utils;

import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.testutil.McBootstrap;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TouristUiSummariesTest {

    @BeforeAll
    static void bootstrap() {
        McBootstrap.init();
    }

    @Test
    void formatRecentArrivals_emptyHistory() {
        Town town = new Town(UUID.randomUUID(), new BlockPos(0, 64, 0), "Testville");
        assertEquals("None yet", TouristUiSummaries.formatRecentArrivals(town, id -> "X"));
    }

    @Test
    void formatRecentArrivals_showsLastEntries() {
        Town town = new Town(UUID.randomUUID(), new BlockPos(0, 64, 0), "Testville");
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        town.recordVisit(a, 2, new BlockPos(1, 64, 1));
        town.recordVisit(b, 1, new BlockPos(2, 64, 2));

        String summary = TouristUiSummaries.formatRecentArrivals(town, id -> id.equals(a) ? "Alpha" : "Beta");
        assertTrue(summary.contains("Alpha ×2"));
        assertTrue(summary.contains("Beta"));
        assertTrue(summary.indexOf("Beta") < summary.indexOf("Alpha"), "newest visit first");
    }
}
