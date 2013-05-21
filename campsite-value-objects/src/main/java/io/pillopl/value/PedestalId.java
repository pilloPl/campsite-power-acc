package io.pillopl.value;

/**
 * Database id of a pedestal.
 */
public record PedestalId(long value) {

    public PedestalId {
        if (value < 1) {
            throw new InvalidValueException("PedestalId", "must be positive but is " + value);
        }
    }

    public static PedestalId of(Long value) {
        if (value == null) {
            throw new InvalidValueException("PedestalId", "is missing");
        }
        return new PedestalId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
