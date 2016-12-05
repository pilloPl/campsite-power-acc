package io.pillopl.webservice.console.monitoring;

import io.pillopl.dto.monitoring.SocketStatusLogDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Statuses of the sockets, newest first, with the names of their error flags.
 */
public record FindSocketStatusLogsResponse(List<SocketStatusLogDTO> logs) implements Serializable {

    public FindSocketStatusLogsResponse {
        logs = logs != null ? List.copyOf(logs) : List.of();
    }
}
