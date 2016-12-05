package io.pillopl.api;

import io.pillopl.dto.monitoring.AlertDTO;
import io.pillopl.dto.monitoring.DashboardDTO;
import io.pillopl.value.Username;

import java.util.List;

/**
 * Dashboard of the console (PEDESTAL_VIEW): key figures and live alerts of the caller's campsites.
 */
public interface DashboardApi {

    /**
     * @param campsiteGroupId a SITE group of the caller's subtree, null for the whole scope
     */
    DashboardDTO getDashboard(Username authUsername, Long campsiteGroupId);

    /**
     * Newest first, at most {@code maxRows} (default 200).
     */
    List<AlertDTO> findAlerts(Username authUsername, Long campsiteGroupId, boolean includeAcknowledged,
                              Integer maxRows);

    /**
     * Acknowledges a live alert the caller sees; an alert is acknowledged once.
     */
    AlertDTO acknowledgeAlert(Username authUsername, String alertKey, String comment);
}
