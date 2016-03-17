package io.pillopl.api;

import io.pillopl.dto.admin.IssuerSyncStatusDTO;
import io.pillopl.value.Username;

import java.util.List;

/**
 * The exchange of card lists and consumption records with the issuer partners, for the administration.
 */
public interface IssuerApi {

    /**
     * Runs the exchange with every active partner now (the job runs it as well).
     *
     * @return the sync status of every direction of every active partner after the run
     */
    List<IssuerSyncStatusDTO> runIssuerExchange(Username authUsername);
}
