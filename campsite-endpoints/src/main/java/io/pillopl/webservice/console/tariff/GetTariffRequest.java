package io.pillopl.webservice.console.tariff;

import java.io.Serializable;

/**
 * Reads one tariff of the caller's market.
 */
public record GetTariffRequest(Long tariffId) implements Serializable {
}
