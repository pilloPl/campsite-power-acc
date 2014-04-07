package io.pillopl.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * An amount of money in a currency, always at scale 2 rounded HALF_UP.
 */
public record Money(BigDecimal amount, String currency) {

    public static final int SCALE = 2;

    public Money {
        if (amount == null) {
            throw new InvalidValueException("Money", "amount is missing");
        }
        if (currency == null || !currency.trim().matches("[A-Za-z]{3}")) {
            throw new InvalidValueException("Money", "currency must be an ISO code of three letters: " + currency);
        }
        amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
        currency = currency.trim().toUpperCase(Locale.ROOT);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    /**
     * @return null when there is no amount (e.g. a pricing that failed)
     */
    public static Money ofNullable(BigDecimal amount, String currency) {
        return amount == null ? null : new Money(amount, currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
