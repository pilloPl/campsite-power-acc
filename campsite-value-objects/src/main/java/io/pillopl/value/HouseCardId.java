package io.pillopl.value;

/**
 * Database id of a house card.
 */
public record HouseCardId(long value) {

    public HouseCardId {
        if (value < 1) {
            throw new InvalidValueException("HouseCardId", "must be positive but is " + value);
        }
    }

    public static HouseCardId of(Long value) {
        if (value == null) {
            throw new InvalidValueException("HouseCardId", "is missing");
        }
        return new HouseCardId(value);
    }

    public static HouseCardId ofNullable(Long value) {
        return value != null ? of(value) : null;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
