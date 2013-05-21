package io.pillopl.commons;

import java.util.Locale;

/**
 * Direction of a sorted list.
 */
public enum SortDirection {
    ASC,
    DESC;

    /**
     * Lenient: "asc", "ASC", "ascending" give ASC; everything else (also null) the default.
     */
    public static SortDirection fromString(String value, SortDirection defaultDirection) {
        if (value == null || value.isBlank()) {
            return defaultDirection;
        }
        String v = value.trim().toUpperCase(Locale.ROOT);
        if (v.startsWith("ASC")) {
            return ASC;
        }
        if (v.startsWith("DESC")) {
            return DESC;
        }
        return defaultDirection;
    }
}
