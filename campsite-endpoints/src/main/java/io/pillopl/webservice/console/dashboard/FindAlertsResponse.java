package io.pillopl.webservice.console.dashboard;

import io.pillopl.dto.monitoring.AlertDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The alerts, newest first.
 */
public record FindAlertsResponse(List<AlertDTO> alerts) implements Serializable {

    public FindAlertsResponse {
        alerts = alerts != null ? List.copyOf(alerts) : List.of();
    }
}
