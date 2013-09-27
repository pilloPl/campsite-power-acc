package io.pillopl.pricing;

/**
 * No step of the tariff resolution found a tariff for the socket (no assignment, no campsite default, no market
 * default). Pricing ends as PRICING_FAILED.
 */
public class NoTariffFoundException extends RuntimeException {

    public NoTariffFoundException(String message) {
        super(message);
    }
}
