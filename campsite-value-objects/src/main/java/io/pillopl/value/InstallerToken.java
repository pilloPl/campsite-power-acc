package io.pillopl.value;

import io.pillopl.commons.TextUtil;

import java.util.Locale;
import java.util.UUID;

/**
 * Token of an open commissioning handed to the installer (a UUID). Stored lower case.
 */
public record InstallerToken(String value) {

    public InstallerToken {
        value = TextUtil.trimToNull(value);
        if (value == null) {
            throw new InvalidValueException("InstallerToken", "is missing");
        }
        try {
            value = UUID.fromString(value).toString().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("InstallerToken", "not a token: " + value);
        }
    }

    public static InstallerToken of(String value) {
        return new InstallerToken(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
