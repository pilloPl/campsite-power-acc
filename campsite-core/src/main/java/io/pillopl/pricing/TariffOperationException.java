package io.pillopl.pricing;

/**
 * A tariff change conflicts with the existing tariffs: the name exists in the market, the market has a default
 * already, the tariff is in use. Invalid tariff content is an {@link IllegalArgumentException} instead.
 */
public class TariffOperationException extends IllegalStateException {

    public TariffOperationException(String message) {
        super(message);
    }
}
