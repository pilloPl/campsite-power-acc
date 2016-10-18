package io.pillopl.webservice.console.issuer;

import io.pillopl.dto.admin.IssuerSyncStatusDTO;

import java.io.Serializable;
import java.util.List;

/**
 * One cell per partner and switched-on direction; status NOT_RUN for a direction that never ran.
 */
public record FindIssuerSyncStatusesResponse(List<IssuerSyncStatusDTO> syncStatuses) implements Serializable {

    public FindIssuerSyncStatusesResponse {
        syncStatuses = syncStatuses != null ? List.copyOf(syncStatuses) : List.of();
    }
}
