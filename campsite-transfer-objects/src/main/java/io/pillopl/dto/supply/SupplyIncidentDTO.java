package io.pillopl.dto.supply;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A stop of a pedestal the backend could not book (no transaction id, no session, session already closed) and its
 * review by the service team.
 */
public record SupplyIncidentDTO(Long id, String incidentType, String pedestalCode, Long campsiteGroupId,
                                Integer transactionId, String cardRfid, Long supplySessionId, BigDecimal meterKwh,
                                LocalDateTime remoteDate, String message, LocalDateTime createDate,
                                String reviewedBy, LocalDateTime reviewDate, String reviewComment)
        implements Serializable {
}
