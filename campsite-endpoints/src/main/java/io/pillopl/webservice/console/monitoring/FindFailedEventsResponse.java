package io.pillopl.webservice.console.monitoring;

import io.pillopl.dto.monitoring.FailedEventDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Events the retry gave up on, newest first.
 */
public record FindFailedEventsResponse(List<FailedEventDTO> events) implements Serializable {

    public FindFailedEventsResponse {
        events = events != null ? List.copyOf(events) : List.of();
    }
}
