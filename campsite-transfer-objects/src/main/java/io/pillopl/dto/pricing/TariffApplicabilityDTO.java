package io.pillopl.dto.pricing;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A tariff assigned to a socket (socketId set) or a whole pedestal from a start date on.
 */
public record TariffApplicabilityDTO(Long id, Long tariffId, String tariffName, Long socketId, Long pedestalId,
                                     String pedestalCode, LocalDateTime startDate, String createdBy)
        implements Serializable {
}
