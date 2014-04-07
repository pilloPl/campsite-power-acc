package io.pillopl.webservice.console.tariff;

import java.io.Serializable;

/**
 * Makes a tariff the default of its market.
 */
public record SetMarketDefaultTariffRequest(String authUsername, Long tariffId) implements Serializable {
}
