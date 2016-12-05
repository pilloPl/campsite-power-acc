package io.pillopl.dto.monitoring;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A live alert of the dashboard: its stable key (source and occurrence), type (SOCKET_FAULT, PEDESTAL_TIMEOUT,
 * STALE_SESSION, REVIEW_RECORD, FAILED_EVENT, ERP_ERROR), severity (ERROR, WARNING, INFO), time, text and the objects
 * it points to, with its acknowledgement if any.
 */
public record AlertDTO(String alertKey, String alertType, String severity, LocalDateTime alertTime, String text,
                       String pedestalCode, Long socketId, Long supplySessionId, Long consumptionRecordId,
                       Long objectId, boolean acknowledged, String acknowledgedBy, LocalDateTime acknowledgeDate,
                       String acknowledgeComment) implements Serializable {
}
