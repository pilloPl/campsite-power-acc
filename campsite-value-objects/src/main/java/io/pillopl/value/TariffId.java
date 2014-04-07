package io.pillopl.value;

/**
 * Database id of a tariff.
 */
public record TariffId(long value) {

    public TariffId {
        if (value < 1) {
            throw new InvalidValueException("TariffId", "must be positive but is " + value);
        }
    }

    public static TariffId of(Long value) {
        if (value == null) {
            throw new InvalidValueException("TariffId", "is missing");
        }
        return new TariffId(value);
    }

    /**
     * @return null when no tariff is given (e.g. to remove an assignment)
     */
    public static TariffId ofNullable(Long value) {
        return value == null ? null : new TariffId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
