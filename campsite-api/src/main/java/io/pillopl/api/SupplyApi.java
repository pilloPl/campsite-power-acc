package io.pillopl.api;

import io.pillopl.commons.DateRange;
import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.infrastructure.PedestalCommandDTO;
import io.pillopl.dto.supply.SupplyIncidentDTO;
import io.pillopl.dto.supply.SupplySessionDTO;
import io.pillopl.value.Username;

/**
 * Supply sessions of the caller's campsites (SITE scope): the live and recent sessions, the remote control of the
 * sockets (SOCKET_REMOTE_CONTROL) and the incidents of stops nobody could book.
 */
public interface SupplyApi {

    /**
     * Viewing needs PEDESTAL_VIEW. Active sessions first, then the newest.
     *
     * @param campsiteGroupId a SITE group of the caller's subtree, null for the whole scope
     * @param period          sessions overlapping the period (an open end is open), null for all
     */
    ListPage<SupplySessionDTO> findSupplySessions(Username authUsername, Long campsiteGroupId, String pedestalCode,
                                                  String socketCode, boolean activeOnly, DateRange period,
                                                  Pagination pagination);

    /**
     * Remote supply start of a socket for a camping card or a house card of the campsite (the pedestal reports the
     * session with its supplyStarted).
     */
    PedestalCommandDTO startSupply(Username authUsername, Long socketId, String cardRfid);

    /**
     * Remote supply stop of an active session (the pedestal reports the stop with its supplyStopped).
     */
    PedestalCommandDTO stopSupply(Username authUsername, Long sessionId);

    /**
     * Closes an active session with TIMEOUT the way the stale-session job does; refused while the socket or the
     * pedestal still report.
     */
    SupplySessionDTO closeStaleSession(Username authUsername, Long sessionId);

    /**
     * Incidents of the pedestals of the caller's scope (PEDESTAL_VIEW), newest first.
     */
    ListPage<SupplyIncidentDTO> findSupplyIncidents(Username authUsername, Long campsiteGroupId, String pedestalCode,
                                                    boolean unreviewedOnly, DateRange period, Pagination pagination);

    /**
     * Marks an incident reviewed (PEDESTAL_EDIT), once.
     */
    SupplyIncidentDTO reviewSupplyIncident(Username authUsername, Long incidentId, String comment);
}
