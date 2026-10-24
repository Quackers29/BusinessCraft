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
}
