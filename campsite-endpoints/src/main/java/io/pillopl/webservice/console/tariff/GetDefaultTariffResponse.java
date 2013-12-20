package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.TariffDTO;

import java.io.Serializable;

/**
 * The default tariff of the market; null when the market has none.
 */
public record GetDefaultTariffResponse(TariffDTO tariff) implements Serializable {
}
