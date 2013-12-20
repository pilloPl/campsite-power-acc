package io.pillopl.webservice.console.tariff;

import java.io.Serializable;

/**
 * Reads the default tariff of the caller's market.
 */
public record GetDefaultTariffRequest(String authUsername) implements Serializable {
}
