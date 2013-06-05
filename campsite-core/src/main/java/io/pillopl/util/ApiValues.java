package io.pillopl.util;

import io.pillopl.api.exception.InvalidInputException;

import java.util.Locale;

/**
 * Static helpers reading the plain values of the service interface calls (enum names as text).
 */
public final class ApiValues {

    private ApiValues() {
        throw new AssertionError();
    }

    /**
     * @throws InvalidInputException when the value is missing or no constant of the enum
     */
    public static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String name) {
        E result = parseEnumOrNull(type, value, name);
        if (result == null) {
            throw new InvalidInputException(name + " is missing");
        }
        return result;
    }

    /**
     * Blanks and dashes are read as underscores, case is ignored ("house cards only" gives HOUSE_CARDS_ONLY).
     *
     * @return null for a blank value
     * @throws InvalidInputException when the value is no constant of the enum
     */
    public static <E extends Enum<E>> E parseEnumOrNull(Class<E> type, String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException(String.format("Unknown %s: %s", name, value), e);
        }
    }
}
