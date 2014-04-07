package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.TariffApplicabilityDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The new assignments (empty when assignments were removed).
 */
public record ApplyTariffToPedestalsResponse(List<TariffApplicabilityDTO> assignments) implements Serializable {
}
