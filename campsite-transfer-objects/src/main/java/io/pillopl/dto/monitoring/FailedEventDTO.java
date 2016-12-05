package io.pillopl.dto.monitoring;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * An event the queue refused and the retry gave up on (EVENT_RETRY FAILED).
 */
public record FailedEventDTO(Long id, String eventId, String eventType, String attributes, String failure,
                             int retryCount, LocalDateTime createDate, LocalDateTime lastRetryDate)
        implements Serializable {
}
