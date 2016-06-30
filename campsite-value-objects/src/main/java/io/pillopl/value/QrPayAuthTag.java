package io.pillopl.value;

import io.pillopl.commons.TextUtil;

import java.util.Locale;

/**
 * Auth tag of a QR payment supply: the pedestal backend starts the socket remotely with "QR" + process id, the
 * pedestal reports the supply with it instead of a card UID.
 */
public record QrPayAuthTag(long processId) {

    public static final String PREFIX = "QR";

    public QrPayAuthTag {
        if (processId < 1) {
            throw new InvalidValueException("QrPayAuthTag", "process id must be positive but is " + processId);
        }
    }

    public static QrPayAuthTag of(long processId) {
        return new QrPayAuthTag(processId);
    }

    /**
     * @return the process id of a QR payment auth tag, null for anything else (card UIDs are hex digits, they never
     * start with the prefix followed by decimal digits only)
     */
    public static Long parse(String authTag) {
        String value = TextUtil.trimToNull(authTag);
        if (value == null || value.length() <= PREFIX.length()
                || !value.toUpperCase(Locale.ROOT).startsWith(PREFIX)) {
            return null;
        }
        String digits = value.substring(PREFIX.length());
        if (digits.length() > 18) {
            return null;
        }
        for (int i = 0; i < digits.length(); i++) {
            if (!Character.isDigit(digits.charAt(i))) {
                return null;
            }
        }
        long processId = Long.parseLong(digits);
        return processId > 0 ? processId : null;
    }

    public String value() {
        return PREFIX + processId;
    }

    @Override
    public String toString() {
        return value();
    }
}
