package io.pillopl.dto.admin;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One row of the audit log.
 */
public record AuditEntryDTO(Long id, String category, String userId, LocalDateTime actionDate, String actionType,
                            String actionName, String actionGroupId, String status, String errorReason,
                            String message) implements Serializable {
}
