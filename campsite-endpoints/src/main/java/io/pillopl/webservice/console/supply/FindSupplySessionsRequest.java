package io.pillopl.webservice.console.supply;

import java.io.Serializable;

/**
 * Sessions of the caller's campsites: optionally of one campsite group, pedestal or socket, only the active ones,
 * overlapping [from, to] (dates "2026-07-01" or date-times); one page (page from 0, default the first 50).
 */
public record FindSupplySessionsRequest(Long campsiteGroupId, String pedestalCode, String socketCode,
                                        Boolean activeOnly, String from, String to, Integer page,
                                        Integer rowsPerPage) implements Serializable {
}
