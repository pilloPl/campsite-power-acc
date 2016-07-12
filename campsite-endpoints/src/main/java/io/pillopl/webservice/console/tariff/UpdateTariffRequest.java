package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.TariffDTO;

import java.io.Serializable;

/**
 * Replaces description, currency and elements of a tariff that is not in use.
 */
public record UpdateTariffRequest(Long tariffId, TariffDTO tariff) implements Serializable {
}
