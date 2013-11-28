package io.pillopl.value;

import java.math.BigDecimal;

/**
 * Energy read from a socket meter, in kWh with at most four decimals (Wh and a tenth of it). Never negative.
 */
public record Kwh(BigDecimal value) {

    public static final int MAX_SCALE = 4;

    public Kwh {
        if (value == null) {
            throw new InvalidValueException("Kwh", "is missing");
        }
        if (value.signum() < 0) {
            throw new InvalidValueException("Kwh", "must not be negative but is " + value.toPlainString());
        }
        if (value.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new InvalidValueException("Kwh", "has more than " + MAX_SCALE + " decimals: " + value.toPlainString());
        }
    }

    public static Kwh of(BigDecimal value) {
        return new Kwh(value);
    }

    public static Kwh of(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("Kwh", "is missing");
        }
        try {
            return new Kwh(new BigDecimal(value.trim()));
        } catch (NumberFormatException e) {
            throw new InvalidValueException("Kwh", "is no number: " + value);
        }
    }

    @Override
    public String toString() {
        return value.stripTrailingZeros().toPlainString() + " kWh";
    }
}
