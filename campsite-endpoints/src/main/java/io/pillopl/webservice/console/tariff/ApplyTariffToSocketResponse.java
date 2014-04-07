package io.pillopl.webservice.console.tariff;

import io.pillopl.dto.pricing.TariffApplicabilityDTO;

import java.io.Serializable;

/**
 * The new assignment, or removed = true when planned assignments were removed.
 */
public record ApplyTariffToSocketResponse(Long socketId, TariffApplicabilityDTO assignment, boolean removed)
        implements Serializable {
}
