package io.pillopl.webservice.console.issuer;

import io.pillopl.dto.admin.IssuerSyncStatusDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The sync status of every direction of every active partner after the run.
 */
public record RunIssuerExchangeResponse(List<IssuerSyncStatusDTO> syncStatuses) implements Serializable {
}
