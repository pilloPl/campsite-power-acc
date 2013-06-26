package io.pillopl.webservice.console.administration;

import io.pillopl.dto.admin.AuditEntryDTO;

import java.io.Serializable;
import java.util.List;

/**
 * First page of the matching audit rows.
 */
public record FindAuditEntriesResponse(List<AuditEntryDTO> entries) implements Serializable {

    public FindAuditEntriesResponse {
        entries = entries != null ? List.copyOf(entries) : List.of();
    }
}
