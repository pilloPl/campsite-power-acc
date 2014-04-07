package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.TariffDTO;

import java.io.Serializable;

/**
 * The saved tariff with its ids.
 */
public record SaveTariffResponse(TariffDTO tariff) implements Serializable {
}
