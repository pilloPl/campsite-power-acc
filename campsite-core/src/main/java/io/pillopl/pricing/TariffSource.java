package io.pillopl.pricing;

/**
 * Where the resolved tariff of a supply came from, in the order of the resolution.
 */
public enum TariffSource {
    SEASONAL_CONTRACT,
    SOCKET,
    PEDESTAL,
    CAMPSITE_DEFAULT,
    MARKET_DEFAULT,
    /**
     * Chosen by the caller (price simulation of a given tariff, seasonal billing with the contract's tariff).
     */
    GIVEN
}
