package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.TariffDTO;

import java.io.Serializable;

/**
 * The changed tariff.
 */
public record UpdateTariffResponse(TariffDTO tariff) implements Serializable {
}
