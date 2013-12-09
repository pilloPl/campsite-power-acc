package io.pillopl.value;

/**
 * Database id of a camping card.
 */
public record CardId(long value) {

    public CardId {
        if (value < 1) {
            throw new InvalidValueException("CardId", "must be positive but is " + value);
        }
    }

    public static CardId of(Long value) {
        if (value == null) {
            throw new InvalidValueException("CardId", "is missing");
        }
        return new CardId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
