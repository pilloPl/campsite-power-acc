package io.pillopl.webservice.console.tariff;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Assigns a tariff to one socket from a start date on; no tariff removes the planned assignments.
 */
public record ApplyTariffToSocketRequest(Long socketId, Long tariffId, LocalDateTime startDate)
        implements Serializable {
}
