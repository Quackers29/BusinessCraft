package com.quackers29.businesscraft.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Shared CSV splitting, row reading and bundled-default installation (review items A1 and A3). */
class CsvConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void splitLine_plainColumns() {
        assertArrayEquals(new String[] { "a", "b", "c" }, CsvConfig.splitLine("a,b,c"));
    }

    @Test
    void splitLine_keepsTrailingEmptyColumns() {
        String[] cols = CsvConfig.splitLine("a,b,,");
        assertEquals(4, cols.length);
        assertEquals("", cols[2]);
        assertEquals("", cols[3]);
    }

    @Test
    void splitLine_commaInsideQuotesDoesNotSplit_andQuotesAreStripped() {
        String[] cols = CsvConfig.splitLine("id,\"Safe, balanced start\",30");
        assertEquals(3, cols.length);
        assertEquals("Safe, balanced start", cols[1]);
    }

    @Test
    void splitLine_trimsColumns() {
        assertArrayEquals(new String[] { "a", "b" }, CsvConfig.splitLine("  a , b  "));
    }

    @Test
    void readRows_skipsHeaderBlankAndCommentLines_andNumbersLines() throws Exception {
        Path csv = tempDir.resolve("x.csv");
        Files.writeString(csv, "h1,h2\n\n# a comment\nr1,v1\n   \n  # indented comment\nr2,v2\n");

        List<CsvConfig.Row> rows = CsvConfig.readRows(csv.toFile());

        assertEquals(2, rows.size());
        assertEquals(4, rows.get(0).lineNumber());
        assertEquals("r1", rows.get(0).columns()[0]);
        assertEquals(7, rows.get(1).lineNumber());
    }

    @Test
    void ensureInstalled_copiesBundledFileWhenMissing() throws Exception {
        File f = CsvConfig.ensureInstalled(tempDir, "productions.csv");

        assertTrue(f.exists());
        assertEquals(tempDir.resolve("businesscraft").resolve("productions.csv").toFile(), f);
        assertTrue(Files.readString(f.toPath()).startsWith("prod_id,display_name,base_cycle_time_minutes"));
    }

    @Test
    void ensureInstalled_neverOverwritesExistingFile() throws Exception {
        Path dir = tempDir.resolve("businesscraft");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("upgrades.csv"), "mine\n");

        CsvConfig.ensureInstalled(tempDir, "upgrades.csv");

        assertEquals("mine\n", Files.readString(dir.resolve("upgrades.csv")));
    }

    @Test
    void ensureInstalled_missingBundledResource_leavesNoFile() {
        File f = CsvConfig.ensureInstalled(tempDir, "no_such_bundled_file.csv");
        assertFalse(f.exists());
    }

    @Test
    void ensureInstalled_installsAllFourBundledFiles() {
        for (String name : List.of("items.csv", "productions.csv", "upgrades.csv", "biomes.csv")) {
            assertTrue(CsvConfig.ensureInstalled(tempDir, name).exists(), name);
        }
    }
}
