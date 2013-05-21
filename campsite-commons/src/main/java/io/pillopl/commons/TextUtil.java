package io.pillopl.commons;

/**
 * Static text helpers.
 */
public final class TextUtil {

    private TextUtil() {
        throw new AssertionError();
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Cuts the text to the length, ending with "..." when cut.
     */
    public static String abbreviate(String s, int maxLength) {
        if (s == null || s.length() <= maxLength) {
            return s;
        }
        if (maxLength < 4) {
            return s.substring(0, maxLength);
        }
        return s.substring(0, maxLength - 3) + "...";
    }
}
