package io.pillopl.webservice.console.monitoring;

import io.pillopl.dto.monitoring.HeartbeatTimeoutDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Pedestals whose link timed out, longest silent first.
 */
public record FindHeartbeatTimeoutsResponse(List<HeartbeatTimeoutDTO> timeouts) implements Serializable {

    public FindHeartbeatTimeoutsResponse {
        timeouts = timeouts != null ? List.copyOf(timeouts) : List.of();
    }
}
