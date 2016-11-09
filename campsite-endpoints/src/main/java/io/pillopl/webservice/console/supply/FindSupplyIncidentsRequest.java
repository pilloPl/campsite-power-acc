package io.pillopl.webservice.console.supply;

import java.io.Serializable;

/**
 * Incidents of the caller's pedestals: optionally of one campsite group or pedestal, only the unreviewed ones,
 * created in [from, to]; one page.
 */
public record FindSupplyIncidentsRequest(Long campsiteGroupId, String pedestalCode, Boolean unreviewedOnly,
                                         String from, String to, Integer page, Integer rowsPerPage)
        implements Serializable {
}
