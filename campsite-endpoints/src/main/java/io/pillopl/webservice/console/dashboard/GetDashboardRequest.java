package io.pillopl.webservice.console.dashboard;

import java.io.Serializable;

/**
 * Key figures of the caller's campsites, optionally of one campsite group.
 */
public record GetDashboardRequest(Long campsiteGroupId) implements Serializable {
}
