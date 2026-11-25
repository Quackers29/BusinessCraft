package com.quackers29.businesscraft.debug.production;

import java.util.ArrayList;
import java.util.List;

/**
 * RFC 4180 quoting. Fields that contain a comma, quote, or line break are wrapped
 * in double quotes, and quotes inside a field are doubled.
 */
public final class Csv {
    private Csv() {
    }

    public static String quote(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean special = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ',' || c == '"' || c == '\n' || c == '\r') {
                special = true;
                break;
            }
        }
        if (!special) {
            return value;
        }
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                out.append("\"\"");
            } else {
                out.append(c);
            }
        }
        out.append('"');
        return out.toString();
    }

    public static String row(String... cells) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(quote(cells[i]));
        }
        return out.toString();
    }

    public static List<String> parse(String line) {
        List<String> cells = new ArrayList<>();
        if (line == null) {
            return cells;
        }
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString());
        return cells;
    }
}
