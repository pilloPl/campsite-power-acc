package io.pillopl.webservice.console.dashboard;

import io.pillopl.dto.monitoring.DashboardDTO;

import java.io.Serializable;

/**
 * The key figures at the time of the call.
 */
public record GetDashboardResponse(DashboardDTO dashboard) implements Serializable {
}
