package io.pillopl.value;

import io.pillopl.commons.TextUtil;

/**
 * Login name of a console user (staff, installer, affiliate owner, seasonal resident).
 */
public record Username(String value) {

    public static final int MAX_LENGTH = 128;

    public Username {
        value = TextUtil.trimToNull(value);
        if (value == null) {
            throw new InvalidValueException("Username", "is missing");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidValueException("Username", "longer than " + MAX_LENGTH + " characters");
        }
        if (value.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidValueException("Username", "must not contain blanks: " + value);
        }
    }

    public static Username of(String value) {
        return new Username(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
