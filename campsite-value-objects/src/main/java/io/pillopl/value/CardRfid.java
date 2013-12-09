package io.pillopl.value;

import io.pillopl.commons.TextUtil;

/**
 * UID of a card as the pedestal readers send it: decimal digits only.
 */
public record CardRfid(String value) {

    public static final int MAX_LENGTH = 64;

    public CardRfid {
        value = TextUtil.trimToNull(value);
        if (value == null) {
            throw new InvalidValueException("CardRfid", "is missing");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidValueException("CardRfid", "longer than " + MAX_LENGTH + " characters");
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                throw new InvalidValueException("CardRfid", "must be the decimal UID of the reader: " + value);
            }
        }
    }

    public static CardRfid of(String value) {
        return new CardRfid(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
