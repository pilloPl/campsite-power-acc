package io.pillopl.value;

import io.pillopl.commons.TextUtil;

/**
 * Mail address of a user: one '@', a local part and a domain with a dot.
 */
public record EmailAddress(String value) {

    public static final int MAX_LENGTH = 256;

    public EmailAddress {
        value = TextUtil.trimToNull(value);
        if (value == null) {
            throw new InvalidValueException("EmailAddress", "is missing");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidValueException("EmailAddress", "longer than " + MAX_LENGTH + " characters");
        }
        int at = value.indexOf('@');
        if (at < 1 || at != value.lastIndexOf('@') || at == value.length() - 1) {
            throw new InvalidValueException("EmailAddress", "not an email address: " + value);
        }
        String domain = value.substring(at + 1);
        if (!domain.contains(".") || domain.startsWith(".") || domain.endsWith(".")
                || value.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidValueException("EmailAddress", "not an email address: " + value);
        }
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }

    /**
     * @return null for blank text (nothing to change)
     */
    public static EmailAddress ofNullable(String value) {
        return TextUtil.isBlank(value) ? null : new EmailAddress(value);
    }

    public String localPart() {
        return value.substring(0, value.indexOf('@'));
    }

    @Override
    public String toString() {
        return value;
    }
}
