package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.TariffDTO;

import java.io.Serializable;

/**
 * Creates a tariff in the caller's market.
 */
public record SaveTariffRequest(String authUsername, TariffDTO tariff) implements Serializable {
}
