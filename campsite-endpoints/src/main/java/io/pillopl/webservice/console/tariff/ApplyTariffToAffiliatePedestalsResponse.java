package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.TariffApplicabilityDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The new assignments, one per pedestal of the owner.
 */
public record ApplyTariffToAffiliatePedestalsResponse(String ownerUsername, List<TariffApplicabilityDTO> assignments)
        implements Serializable {
}
