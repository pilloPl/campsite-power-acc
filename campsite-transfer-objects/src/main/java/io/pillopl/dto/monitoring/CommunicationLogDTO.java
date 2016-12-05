package io.pillopl.dto.monitoring;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A logged message of a pedestal (boot, consumption record) for the service team.
 */
public record CommunicationLogDTO(Long id, String pedestalCode, String logType, LocalDateTime date, String message)
        implements Serializable {
}
