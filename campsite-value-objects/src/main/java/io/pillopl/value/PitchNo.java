package io.pillopl.value;

import io.pillopl.commons.TextUtil;

import java.util.Locale;

/**
 * Number of a pitch on the campsite, e.g. "A12". Stored upper case.
 */
public record PitchNo(String value) {

    public static final int MAX_LENGTH = 16;

    public PitchNo {
        value = TextUtil.trimToNull(value);
        if (value == null) {
            throw new InvalidValueException("PitchNo", "is missing");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidValueException("PitchNo", "longer than " + MAX_LENGTH + " characters");
        }
        value = value.toUpperCase(Locale.ROOT);
    }

    /**
     * @return null for blank text (no pitch, e.g. to remove an assignment)
     */
    public static PitchNo ofNullable(String value) {
        return TextUtil.isBlank(value) ? null : new PitchNo(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
