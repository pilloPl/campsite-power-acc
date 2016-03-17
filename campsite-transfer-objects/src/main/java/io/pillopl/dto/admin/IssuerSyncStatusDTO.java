package io.pillopl.dto.admin;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * The last run of one direction (CARD_IMPORT, CARD_EXPORT, RECORD_EXPORT, RECORD_IMPORT) of the exchange with an
 * issuer partner: OK or ERROR, when it ran and last succeeded, the accepted and rejected lines and the message.
 */
public record IssuerSyncStatusDTO(String partnerName, String interfaceVersion, String direction, String status,
                                  LocalDateTime lastRun, LocalDateTime lastSuccess, int itemsAccepted,
                                  int itemsRejected, String message) implements Serializable {
}
