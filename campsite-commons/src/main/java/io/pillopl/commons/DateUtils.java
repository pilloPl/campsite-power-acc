package io.pillopl.commons;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Static date helpers for the request parameters of the console.
 */
public final class DateUtils {

    private DateUtils() {
        throw new AssertionError();
    }

    public static LocalDateTime startOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    /**
     * Last representable moment of the day (inclusive end of a range).
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date != null ? date.atTime(LocalTime.MAX) : null;
    }

    /**
     * @return null for blank text; a date-only text gives the start of that day
     * @throws IllegalArgumentException for text that is neither an ISO date nor an ISO date-time
     */
    public static LocalDateTime parseStartOfDay(String text) {
        return parse(text, false);
    }

    /**
     * @return null for blank text; a date-only text gives the end of that day
     * @throws IllegalArgumentException for text that is neither an ISO date nor an ISO date-time
     */
    public static LocalDateTime parseEndOfDay(String text) {
        return parse(text, true);
    }

    private static LocalDateTime parse(String text, boolean endOfDay) {
        String value = TextUtil.trimToNull(text);
        if (value == null) {
            return null;
        }
        try {
            if (value.length() <= 10) {
                LocalDate date = LocalDate.parse(value);
                return endOfDay ? endOfDay(date) : startOfDay(date);
            }
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Not a date: " + value, e);
        }
    }
}
