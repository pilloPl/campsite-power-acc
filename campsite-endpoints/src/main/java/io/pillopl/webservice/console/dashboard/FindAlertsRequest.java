package io.pillopl.webservice.console.dashboard;

import java.io.Serializable;

/**
 * Live alerts, optionally of one campsite group, with or without the acknowledged ones, at most maxRows (default
 * 200).
 */
public record FindAlertsRequest(Long campsiteGroupId, Boolean includeAcknowledged, Integer maxRows)
        implements Serializable {
}
