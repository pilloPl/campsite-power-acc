package io.pillopl.webservice.console.tariff;

import java.io.Serializable;

/**
 * Makes a tariff the default of its market.
 */
public record SetMarketDefaultTariffRequest(Long tariffId) implements Serializable {
}
