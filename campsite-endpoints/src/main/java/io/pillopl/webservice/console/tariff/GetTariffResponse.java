package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.TariffDTO;

import java.io.Serializable;

/**
 * The tariff with its elements and price text.
 */
public record GetTariffResponse(TariffDTO tariff) implements Serializable {
}
