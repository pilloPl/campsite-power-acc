package io.pillopl.webservice.console.monitoring;

import io.pillopl.dto.monitoring.CommunicationLogDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Logged messages of the pedestal, newest first.
 */
public record FindCommunicationLogsResponse(List<CommunicationLogDTO> logs) implements Serializable {

    public FindCommunicationLogsResponse {
        logs = logs != null ? List.copyOf(logs) : List.of();
    }
}
