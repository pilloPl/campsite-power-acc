package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.TariffDTO;

import java.io.Serializable;

/**
 * The new default tariff of the market.
 */
public record SetMarketDefaultTariffResponse(TariffDTO tariff) implements Serializable {
}
