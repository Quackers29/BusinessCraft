package com.quackers29.businesscraft.ui.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiTextWrapTest {
    /** Stand-in font: 6px per character, matching the width class of the vanilla bitmap font. */
    private static final ToIntFunction<String> WIDTH = text -> text.length() * 6;

    @Test
    void shortValue_staysOneLine() {
        assertEquals(List.of("Maplehaven"), UiTextWrap.wrap("Maplehaven", 80, WIDTH));
    }

    @Test
    void longWaitingLine_breaksOnSpaces() {
        String value = "2 waiting: 1→Rivertown, 1→Hillcrest";
        List<String> lines = UiTextWrap.wrap(value, 90, WIDTH);
        assertTrue(lines.size() >= 2, lines.toString());
        assertEquals(value, String.join(" ", lines).replace("  ", " "));
        for (String line : lines) {
            assertTrue(WIDTH.applyAsInt(line) <= 90, line);
        }
    }

    @Test
    void ellipsize_keepsShortTextAndCutsLongText() {
        assertEquals("Oakvale", UiTextWrap.ellipsize("Oakvale", 80, WIDTH));
        String cut = UiTextWrap.ellipsize("Meadowbrook", 8 * 6, WIDTH);
        assertTrue(cut.endsWith("…"), cut);
        assertTrue(WIDTH.applyAsInt(cut) <= 48, cut);
    }

    @Test
    void placeY_nudgesOverlappingLabelsDown() {
        java.util.List<UiLabelLayout.Rect> placed = new java.util.ArrayList<>();
        int first = UiLabelLayout.placeY(0, 10, 40, 9, placed);
        int[] second = UiLabelLayout.place(10, 10, 40, 9, placed);
        assertEquals(10, first);
        boolean clear = second[0] >= 44 || second[1] >= 20;
        assertTrue(clear, "second label at " + second[0] + "," + second[1]);
    }
}
