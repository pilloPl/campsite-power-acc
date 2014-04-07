package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.TariffDTO;

import java.io.Serializable;

/**
 * The default tariff now in effect for the group (its own or inherited), null when none.
 */
public record SetCampsiteDefaultTariffResponse(Long groupId, TariffDTO effectiveDefaultTariff) implements Serializable {
}
