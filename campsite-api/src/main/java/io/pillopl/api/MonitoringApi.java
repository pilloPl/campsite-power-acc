package io.pillopl.api;

import io.pillopl.commons.DateRange;
import io.pillopl.dto.infrastructure.PedestalCommandDTO;
import io.pillopl.dto.monitoring.CommunicationLogDTO;
import io.pillopl.dto.monitoring.FailedEventDTO;
import io.pillopl.dto.monitoring.HeartbeatTimeoutDTO;
import io.pillopl.dto.monitoring.SocketStatusLogDTO;
import io.pillopl.dto.monitoring.StatusReportLineDTO;
import io.pillopl.value.Username;

import java.time.LocalDate;
import java.util.List;

/**
 * Monitoring and service of the caller's pedestals (SITE scope): reads need PEDESTAL_VIEW, commands PEDESTAL_EDIT,
 * the failed events of the whole network CONFIG_EDIT. A pedestal or socket outside the scope is unknown.
 */
public interface MonitoringApi {

    /**
     * Seconds per socket and status in the month of {@code month} (e.g. supplying without consumption).
     *
     * @param campsiteGroupId a SITE group of the caller's subtree, null for the whole scope
     */
    List<StatusReportLineDTO> getStatusReport(Username authUsername, LocalDate month, Long campsiteGroupId);

    /**
     * Statuses of the sockets of a pedestal (or of one socket), newest first, at most {@code maxRows}.
     *
     * @param period null for the last 7 days
     */
    List<SocketStatusLogDTO> findSocketStatusLogs(Username authUsername, String pedestalCode, Long socketId,
                                                  DateRange period, Integer maxRows);

    List<CommunicationLogDTO> findCommunicationLogs(Username authUsername, String pedestalCode, DateRange period,
                                                    Integer maxRows);

    List<HeartbeatTimeoutDTO> findHeartbeatTimeouts(Username authUsername, Long campsiteGroupId);

    List<FailedEventDTO> findFailedEvents(Username authUsername, Integer maxRows);

    /**
     * Asks the pedestal for the status of a socket (null: of all sockets).
     */
    PedestalCommandDTO requestStatus(Username authUsername, String pedestalCode, Integer socketNo);

    /**
     * Ends the fault of a FAULTED or BREAKER_TRIPPED socket: a tripped breaker of a pedestal type with remote
     * breaker reset is switched on remotely, otherwise the service team confirms the repair; the socket gets
     * AVAILABLE (OCCUPIED with an active session) and a status log row.
     */
    PedestalCommandDTO resetFault(Username authUsername, Long socketId);

    /**
     * Sends the service mail of the socket's last fault again.
     */
    PedestalCommandDTO resendFaultMail(Username authUsername, Long socketId);
}
